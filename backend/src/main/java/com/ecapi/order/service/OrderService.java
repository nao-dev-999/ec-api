package com.ecapi.order.service;

import com.ecapi.order.entity.Order;
import com.ecapi.order.entity.OrderItem;
import com.ecapi.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * =========================================================== SonarQube / GitHub Copilot 比較デモ用サンプル
 * =========================================================== このファイルには意図的に3種類の問題を仕込んであります。
 * 実際にSonarQubeとCopilotの両方にレビューさせて、 どちらが何を検出するかを比較するためのデモ用コードです。
 *
 * <p>本番の {@code com.example.ecapi} 配下とは独立したパッケージであり、Spring の
 * コンポーネントスキャン対象にもならない（デモ・学習目的専用。本番コードにマージしないこと）。
 *
 * <p>[問題A] ルールベースで機械的に検出しやすい問題（SonarQube向き） [問題B] セキュリティの定番アンチパターン（SonarQube向き） [問題C]
 * 意図理解が必要な業務ロジックの矛盾（Copilot向き） ===========================================================
 */
@Service
public class OrderService {

    @Autowired private OrderRepository orderRepository;

    @Autowired private JdbcTemplate jdbcTemplate;

    // ---------------------------------------------------------
    // [問題A-1] 未使用フィールド（Code Smell）
    // 宣言されているが、どこからも参照されていない。
    // ---------------------------------------------------------
    private String unusedDebugFlag = "DEBUG_MODE";

    // ---------------------------------------------------------
    // [問題B] SQLインジェクション（Vulnerability）
    // ユーザー入力を文字列結合で直接SQLに埋め込んでいる。
    // 正しくは PreparedStatement / JdbcTemplateの引数バインドを使うべき。
    // ---------------------------------------------------------
    public List<Order> searchOrdersByCustomerName(String customerName) {
        String sql = "SELECT * FROM orders WHERE customer_name = '" + customerName + "'";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRowToOrder(rs));
    }

    // ---------------------------------------------------------
    // [問題A-2] リソースリーク（Bug）
    // Connection / Statement / ResultSet を try-with-resources で
    // クローズしていない。例外発生時にリークする。
    // ---------------------------------------------------------
    public int countPendingOrders(Connection connection) throws Exception {
        Statement statement = connection.createStatement();
        ResultSet resultSet =
                statement.executeQuery("SELECT COUNT(*) FROM orders WHERE status = 'PENDING'");
        resultSet.next();
        return resultSet.getInt(1);
        // statement.close() / resultSet.close() が呼ばれていない
    }

    // ---------------------------------------------------------
    // [問題A-3] 循環的複雑度が高すぎるメソッド（Code Smell）
    // ネストしたif文が深く、可読性・保守性が低い。
    // ---------------------------------------------------------
    public String classifyOrder(Order order) {
        if (order != null) {
            if (order.getTotalAmount() != null) {
                if (order.getTotalAmount().compareTo(BigDecimal.valueOf(100000)) > 0) {
                    if (order.getCustomerType() != null) {
                        if (order.getCustomerType().equals("VIP")) {
                            if (order.getItems() != null && order.getItems().size() > 10) {
                                return "VIP_BULK_HIGH_VALUE";
                            } else {
                                return "VIP_HIGH_VALUE";
                            }
                        } else {
                            return "STANDARD_HIGH_VALUE";
                        }
                    } else {
                        return "UNKNOWN_HIGH_VALUE";
                    }
                } else {
                    return "STANDARD";
                }
            } else {
                return "NO_AMOUNT";
            }
        } else {
            return "NULL_ORDER";
        }
    }

    // ---------------------------------------------------------
    // [問題C] 業務ロジックの矛盾（Copilot向き）
    // 本来は「小計に割引を適用してから税金を計算する」べきだが、
    // 実装では「税金を計算してから割引を適用」している。
    // 文法的には正しく、SonarQubeのルールには一切引っかからないが、
    // 意図と実装がズレているため計算結果が誤りになる。
    // ---------------------------------------------------------
    public BigDecimal calculateFinalPrice(
            BigDecimal subtotal, BigDecimal discountRate, BigDecimal taxRate) {
        // 本来あるべき順序: 小計 → 割引適用 → 税金計算
        // 実際の実装: 小計 → 税金計算 → 割引適用（順序が逆）
        BigDecimal priceWithTax = subtotal.add(subtotal.multiply(taxRate));
        BigDecimal finalPrice = priceWithTax.subtract(priceWithTax.multiply(discountRate));
        return finalPrice;
    }

    // ---------------------------------------------------------
    // [問題C] 命名と実装の不一致（Copilot向き）
    // メソッド名は isOrderCancellable（キャンセル可能か）だが、
    // 実装は発送済み(SHIPPED)の場合に true を返してしまっている。
    // 型・構文上は完全に正しいため、静的解析では検出されない。
    // ---------------------------------------------------------
    public boolean isOrderCancellable(Order order) {
        if (order.getStatus().equals("SHIPPED")) {
            return true; // 本来は false であるべき（発送済みはキャンセル不可）
        }
        if (order.getStatus().equals("PENDING")) {
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------
    // [問題C] 呼び出し元との不整合（Copilot向き / ファイル横断）
    // applyItemDiscount の意味が「割引"後"の単価を返す」に変更されたが、
    // 呼び出し元 recalculateOrderTotal は「割引"額"が返る」前提のまま
    // 合計金額を計算しており、二重に割引が適用されてしまう。
    // ---------------------------------------------------------
    public BigDecimal applyItemDiscount(OrderItem item) {
        // 変更後: 割引適用"後"の単価そのものを返す仕様に変わった
        BigDecimal discountAmount = item.getUnitPrice().multiply(item.getDiscountRate());
        return item.getUnitPrice().subtract(discountAmount);
    }

    public BigDecimal recalculateOrderTotal(Order order) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            BigDecimal discount = applyItemDiscount(item); // 実際は「割引後単価」が返る
            // ここでは「割引額」だと誤解したまま、単価から再度引いてしまっている
            BigDecimal itemTotal =
                    item.getUnitPrice()
                            .subtract(discount)
                            .multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(itemTotal);
        }
        return total;
    }

    private Order mapRowToOrder(ResultSet rs) {
        // デモ用の簡易マッピング（本サンプルでは実装省略）
        return new Order();
    }
}
