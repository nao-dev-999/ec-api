package com.example.ecapi.repository.support;

import com.example.ecapi.entity.SoftDeletable;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

/**
 * 論理削除（deleted = false フィルタ）を全 Repository に機械的に強制する基底実装。
 *
 * <p>{@code @EnableJpaRepositories(repositoryBaseClass = SoftDeleteRepositoryImpl.class)} で
 * 有効化すると、アプリ内の全 {@code JpaRepository} 継承インターフェースの実体がこのクラスになる。 以後、開発者が {@code
 * findByIdAndDeletedFalse} のような命名を忘れても、 標準の {@code findById} / {@code findAll} が自動的に削除済みレコードを除外する。
 *
 * <p><b>挙動の変更点（重要・チーム周知必須）:</b>
 *
 * <ul>
 *   <li>{@code findById} / {@code findAll} / {@code count} / {@code existsById}: {@code deleted =
 *       false} のレコードのみを対象にする
 *   <li>{@code deleteById} / {@code delete}: 物理 DELETE ではなく {@code deleted = true} への
 *       UPDATE（論理削除）に置き換わる
 *   <li>物理削除が本当に必要な場合は {@link #hardDeleteById(Object)} を明示的に呼ぶ
 * </ul>
 *
 * <p><b>deleted フィールドを持たないエンティティ:</b> エンティティが {@link SoftDeletable} を実装しているかどうかで判定し、
 * 実装していないエンティティに対しては一切の挙動変更をしない （標準の {@link SimpleJpaRepository} と同じ動作にフォールバックする）。
 * これにより集計テーブルや中間テーブルなど論理削除対象外のエンティティが混在しても安全。
 *
 * <p><b>このクラスで防げないもの（規約側で引き続き担保が必要）:</b>
 *
 * <ul>
 *   <li>{@code @Query} で開発者が自分で書いた JPQL / ネイティブSQL
 *   <li>メソッド名から自動導出されるクエリ（{@code findByName} 等）
 *   <li>{@code Specification} を使う検索（呼び出し側で {@link #notDeleted()} を合成すること）
 * </ul>
 *
 * これらは PR チェックリストと ArchUnit テストで補完する（README参照）。
 */
public class SoftDeleteRepositoryImpl<T, ID> extends SimpleJpaRepository<T, ID> {

    private static final String DELETED_ATTRIBUTE = "deleted";

    private final EntityManager entityManager;
    private final JpaEntityInformation<T, ?> entityInformation;

    /** このエンティティが deleted 属性を持つか（{@link SoftDeletable} を実装しているか） */
    private final boolean softDeletable;

    public SoftDeleteRepositoryImpl(
            JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
        this.entityInformation = entityInformation;
        this.softDeletable = SoftDeletable.class.isAssignableFrom(entityInformation.getJavaType());
    }

    @Override
    public Optional<T> findById(ID id) {
        if (!softDeletable) {
            return super.findById(id);
        }
        return findOne(idEquals(id).and(notDeletedSpec()));
    }

    @Override
    public boolean existsById(ID id) {
        if (!softDeletable) {
            return super.existsById(id);
        }
        return count(idEquals(id).and(notDeletedSpec())) > 0;
    }

    @Override
    public List<T> findAll() {
        return softDeletable ? findAll(notDeletedSpec()) : super.findAll();
    }

    @Override
    public List<T> findAll(Sort sort) {
        return softDeletable ? findAll(notDeletedSpec(), sort) : super.findAll(sort);
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        return softDeletable ? findAll(notDeletedSpec(), pageable) : super.findAll(pageable);
    }

    @Override
    public long count() {
        return softDeletable ? count(notDeletedSpec()) : super.count();
    }

    @Override
    public void deleteById(ID id) {
        if (!softDeletable) {
            super.deleteById(id);
            return;
        }
        T entity =
                findById(id)
                        .orElseThrow(
                                () ->
                                        new EmptyResultDataAccessException(
                                                "No %s entity with id %s exists!"
                                                        .formatted(
                                                                entityInformation.getJavaType(),
                                                                id),
                                                1));
        delete(entity);
    }

    @Override
    public void delete(T entity) {
        if (!softDeletable) {
            super.delete(entity);
            return;
        }
        markDeleted(entity);
        entityManager.merge(entity);
    }

    /** 論理削除属性の有無に関わらず、常に物理DELETEする。deleted属性を持つエンティティで本当に物理削除が必要な場合のみ明示的に呼ぶこと。 */
    public void hardDeleteById(ID id) {
        super.deleteById(id);
    }

    private void markDeleted(T entity) {
        ((SoftDeletable) entity).setDeleted(true);
    }

    private Specification<T> notDeletedSpec() {
        return (root, query, cb) -> cb.isFalse(root.get(DELETED_ATTRIBUTE));
    }

    private Specification<T> idEquals(ID id) {
        return (root, query, cb) ->
                cb.equal(root.get(entityInformation.getIdAttribute().getName()), id);
    }
}
