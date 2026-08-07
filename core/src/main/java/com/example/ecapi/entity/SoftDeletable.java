package com.example.ecapi.entity;

/** 論理削除（{@code deleted} 属性）を持つエンティティが実装するマーカーインタフェース。 */
public interface SoftDeletable {

    boolean isDeleted();

    void setDeleted(boolean deleted);
}
