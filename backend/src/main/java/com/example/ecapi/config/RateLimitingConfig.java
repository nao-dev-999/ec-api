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

    // ✅ Spring Data Redis の RedisConnectionFactory から host/port を取得し、
    //    Bucket4j専用のRedisClientを別途生成する（library間で接続を共有しない）
    @Bean(destroyMethod = "shutdown")
    public RedisClient rateLimitRedisClient(RedisConnectionFactory connectionFactory) {
        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) connectionFactory;
        RedisURI uri =
                RedisURI.create(
                        lettuceFactory.getStandaloneConfiguration().getHostName(),
                        lettuceFactory.getStandaloneConfiguration().getPort());
        return RedisClient.create(uri);
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
