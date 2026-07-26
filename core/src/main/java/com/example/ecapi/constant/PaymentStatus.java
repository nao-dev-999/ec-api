package com.example.ecapi.constant;

/** {@code payment}（決済の正マスタ）レコード自体のステータス。 */
public enum PaymentStatus {
    AUTHORIZED, // オーソリ成功（確定はまだ）
    CAPTURED, // 決済確定
    FAILED, // 確定に至らず失敗
    REFUNDED // 返金
}
