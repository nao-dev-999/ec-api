package com.example.ecapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.util.Map;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 読み取り頻度が高いレスポンスをRedisにキャッシュする設定。
 *
 * <p>在庫のように別サービス（{@code OrderService}）経由で頻繁に更新される値は、
 * {@code @CacheEvict}だけでは追従できない（注文確定時の在庫減算がキャッシュを
 * 無効化しないため）。そのため商品キャッシュ（products）はTTLを短く抑え、
 * 古い在庫数を表示し続ける期間を最大60秒に限定している。
 * カテゴリ（categories）は更新頻度が低くCategoryService経由でのみ変更されるため、
 * 長めのTTL + 更新時の明示的なCacheEvictで整合性を保っている。
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final String PRODUCTS_CACHE = "products";
    private static final String CATEGORIES_CACHE = "categories";

    // TransactionAwareCacheManagerProxyでラップし、@Transactionalメソッド内のCacheEvict/Cacheable操作を
    // トランザクションのコミット後まで遅延させる（ロールバック時にキャッシュだけ食い違うのを防ぐため）。
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        GenericJackson2JsonRedisSerializer valueSerializer =
                new GenericJackson2JsonRedisSerializer(mapper);

        RedisCacheConfiguration baseConfig =
                RedisCacheConfiguration.defaultCacheConfig()
                        .serializeKeysWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(
                                        new StringRedisSerializer()))
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(
                                        valueSerializer));

        RedisCacheManager redisCacheManager =
                RedisCacheManager.builder(connectionFactory)
                        .cacheDefaults(baseConfig.entryTtl(Duration.ofMinutes(10)))
                        .withInitialCacheConfigurations(
                                Map.of(
                                        PRODUCTS_CACHE, baseConfig.entryTtl(Duration.ofSeconds(60)),
                                        CATEGORIES_CACHE,
                                                baseConfig.entryTtl(Duration.ofMinutes(10))))
                        .build();
        return new TransactionAwareCacheManagerProxy(redisCacheManager);
    }
}
