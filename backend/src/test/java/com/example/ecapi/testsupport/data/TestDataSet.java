package com.example.ecapi.testsupport.data;

import java.util.List;
import java.util.Map;

/**
 * パース済みのテストデータ全体を表す。
 *
 * <p>フォーマット（Excel/YAML/CSV）に関わらず、パーサーはこの共通モデルに変換する。 これにより {@link TestDataLoader}
 * 以降のロジックはフォーマットを一切意識しない。
 *
 * @param tables テーブル名 → 行データのリスト（DB への INSERT に使う）
 * @param mocks Mockito モックのスタブ定義（DB を介さずサービス層のみをテストする場合に使う）
 */
public record TestDataSet(Map<String, List<Map<String, Object>>> tables, List<MockStub> mocks) {

    /**
     * モックのスタブ定義1件分。
     *
     * <p>YAML 例:
     *
     * <pre>{@code
     * mocks:
     *   - bean: productRepository
     *     method: findByIdAndDeletedFalse
     *     args: [1]
     *     returnType: java.util.Optional
     *     returns:
     *       id: 1
     *       name: "商品A"
     *       price: 1000
     * }</pre>
     *
     * @param bean Spring Bean名（@MockitoBean で登録されたモックのBean名と一致させる）
     * @param method スタブ対象のメソッド名
     * @param args メソッド呼び出し時の引数（オーバーロード解決は引数の個数のみで行う簡易実装）
     * @param returnType 戻り値の実クラス名（Optional の場合は中身の型をラップして生成する）
     * @param returns 戻り値オブジェクトを組み立てるためのフィールドマップ（null 許容）
     */
    public record MockStub(
            String bean,
            String method,
            List<Object> args,
            String returnType,
            Map<String, Object> returns) {}
}
