package com.example.ecapi.exception;

import com.example.ecapi.entity.BaseEntity;

/**
 * クライアントが送ってきた version が DB の現在値と異なる（＝他の更新が先に入った）場合の 409。
 *
 * <p>Hibernate は管理状態のエンティティに {@code setVersion()} しても楽観ロックの判定に使わない（読み込み時の version で判定する）。
 * そのため「画面表示時点の version」との比較はサービス層で {@link #throwIfStale} により明示的に行う。読み込みからコミットまでの間の競合は、従来どおり JPA の
 * {@code @Version} が検出する。
 */
public class OptimisticLockConflictException extends ConflictException {

    public OptimisticLockConflictException() {
        super(ErrorCode.OPTIMISTIC_LOCK_CONFLICT);
    }

    /**
     * @param entity DB から読み込んだエンティティ
     * @param requestedVersion クライアントが送ってきた version
     * @throws OptimisticLockConflictException version が一致しない場合
     */
    public static void throwIfStale(BaseEntity entity, int requestedVersion) {
        if (entity.getVersion() != requestedVersion) {
            throw new OptimisticLockConflictException();
        }
    }
}
