package com.example.ecapi.constant;

/** 注文の決済ステータス。発送等の履行ステータス（{@link OrderStatus}）とは別軸で管理する。 */
public enum OrderPaymentStatus {
    AUTHORIZED, // オーソリ成功（確定はまだ）
    CAPTURED, // 決済確定（夜間バッチで反映）
    CANCELLED, // 確定に至らず取消
    REFUNDED // 返金
}
