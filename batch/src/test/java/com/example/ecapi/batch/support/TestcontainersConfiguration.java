package com.example.ecapi.batch.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * backend/src/test/java/com/example/ecapi/support/TestcontainersConfigurationと同じ方針（Postgresのみ、Redisは不要）。
 * batchはbackendが所有するFlywayマイグレーションを持たないため、schema検証はHibernateのDDL自動生成（{@code
 * ddl-auto=create-drop}）に委ね、実際のPAYMENT/CustomerOrderDetail結合クエリの挙動を実DBで検証する。
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17"));
    }
}
