package com.example.ecapi.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 全ビジネス例外の基底クラス。
 *
 * <p>HTTP ステータスは {@link ErrorCode} が保持する。具象クラスはこのクラスを直接継承せず、ステータスごとの中間抽象クラス（{@link
 * BadRequestException} / {@link ResourceNotFoundException} / {@link ConflictException}）を継承する。
 */
@Getter
public abstract class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Object[] args;

    protected BusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode.getMessageKey());
        this.errorCode = errorCode;
        this.args = args == null ? new Object[0] : args;
    }

    public HttpStatus getStatus() {
        return errorCode.getStatus();
    }

    /**
     * 中間抽象クラスが自分の担当ステータスと異なる {@link ErrorCode} を受け取っていないか検査する。
     *
     * <p>例: {@code ResourceNotFoundException} の具象クラスに 409 の ErrorCode を渡した場合は即座に失敗させる。
     */
    protected static ErrorCode requireStatus(ErrorCode errorCode, HttpStatus expected) {
        if (errorCode.getStatus() != expected) {
            throw new IllegalArgumentException(
                    "ErrorCode %s has status %s, but %s is required"
                            .formatted(errorCode, errorCode.getStatus(), expected));
        }
        return errorCode;
    }
}
