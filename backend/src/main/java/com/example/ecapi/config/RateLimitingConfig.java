package com.example.ecapi.config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
public class RateLimitingConfig {

    // ✅ Spring Data Redis の RedisConnectionFactory から host/port/password/TLS設定を取得し、
    //    Bucket4j専用のRedisClientを別途生成する（library間で接続を共有しない）。
    //    host/portだけをRedisURI.create(host, port)で組み立てるとpassword/SSLが
    //    引き継がれず、AUTH・TLSを要求する環境（ECS等）で接続に失敗するため、
    //    RedisStandaloneConfiguration側の設定を明示的に反映する。
    @Bean(destroyMethod = "shutdown")
    public RedisClient rateLimitRedisClient(RedisConnectionFactory connectionFactory) {
        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) connectionFactory;
        var standaloneConfig = lettuceFactory.getStandaloneConfiguration();
        RedisURI.Builder uriBuilder =
                RedisURI.builder()
                        .withHost(standaloneConfig.getHostName())
                        .withPort(standaloneConfig.getPort())
                        .withSsl(lettuceFactory.getClientConfiguration().isUseSsl());
        standaloneConfig.getPassword().toOptional().ifPresent(uriBuilder::withPassword);
        return RedisClient.create(uriBuilder.build());
    }

    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<byte[], byte[]> rateLimitRedisConnection(
            RedisClient rateLimitRedisClient) {
        return rateLimitRedisClient.connect(ByteArrayCodec.INSTANCE);
    }

    @Bean
    public ProxyManager<byte[]> rateLimitProxyManager(
            StatefulRedisConnection<byte[], byte[]> rateLimitRedisConnection) {
        return Bucket4jLettuce.casBasedBuilder(rateLimitRedisConnection).build();
    }
}
