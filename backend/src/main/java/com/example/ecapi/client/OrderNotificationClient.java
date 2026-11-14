package com.example.ecapi.client;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 通知サービスのクライアント
 *
 * <p>注文確定メールなど、顧客へのメール送信を通知サービスに依頼する。
 */
@Slf4j
@Component
public class OrderNotificationClient {

    private final RestClient restClient;

    public OrderNotificationClient(@Value("${app.notification.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    /** 注文確定メールの送信を通知サービスに依頼します。 */
    public void notifyOrderCreated(Long orderId, String email) {
        restClient
                .post()
                .uri("/v1/notifications/order-created")
                .body(Map.of("orderId", orderId, "email", email))
                .retrieve()
                .toBodilessEntity();
        log.info("Order notification requested orderId={}", orderId);
    }
}
