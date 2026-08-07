package com.example.ecapi.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.ecapi.helper.MessageHelper;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class RateLimitingFilterTest {

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    private RedisClient redisClient;
    private StatefulRedisConnection<byte[], byte[]> connection;
    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        RedisURI uri = RedisURI.create(REDIS.getHost(), REDIS.getMappedPort(6379));
        redisClient = RedisClient.create(uri);
        connection = redisClient.connect(ByteArrayCodec.INSTANCE);
        ProxyManager<byte[]> proxyManager = Bucket4jLettuce.casBasedBuilder(connection).build();

        MessageHelper messageHelper = mock(MessageHelper.class);
        when(messageHelper.get(anyString())).thenReturn("リクエスト数の上限を超えました");

        filter = new RateLimitingFilter(proxyManager, messageHelper);
    }

    @AfterEach
    void tearDown() {
        connection.close();
        redisClient.shutdown();
    }

    @Test
    void loginエンドポイントは上限を超えると429を返しchainを呼ばない() throws Exception {
        AtomicInteger chainInvocations = new AtomicInteger();
        FilterChain chain = (req, res) -> chainInvocations.incrementAndGet();

        // LimitTier.LOGIN の上限は10回/分
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = loginRequest("203.0.113.10");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, chain);
            assertThat(response.getStatus()).isEqualTo(200);
        }
        assertThat(chainInvocations.get()).isEqualTo(10);

        MockHttpServletRequest exceedingRequest = loginRequest("203.0.113.10");
        MockHttpServletResponse exceedingResponse = new MockHttpServletResponse();
        filter.doFilter(exceedingRequest, exceedingResponse, chain);

        assertThat(exceedingResponse.getStatus()).isEqualTo(429);
        assertThat(chainInvocations.get()).isEqualTo(10);
    }

    @Test
    void クライアントごとに独立したバケットで制限される() throws Exception {
        AtomicInteger chainInvocations = new AtomicInteger();
        FilterChain chain = (req, res) -> chainInvocations.incrementAndGet();

        for (int i = 0; i < 10; i++) {
            filter.doFilter(loginRequest("198.51.100.1"), new MockHttpServletResponse(), chain);
        }
        MockHttpServletResponse exhaustedIpResponse = new MockHttpServletResponse();
        filter.doFilter(loginRequest("198.51.100.1"), exhaustedIpResponse, chain);
        assertThat(exhaustedIpResponse.getStatus()).isEqualTo(429);

        // 別IPは独立したバケットを持つため、上限に達していない
        MockHttpServletResponse otherIpResponse = new MockHttpServletResponse();
        filter.doFilter(loginRequest("198.51.100.2"), otherIpResponse, chain);
        assertThat(otherIpResponse.getStatus()).isEqualTo(200);
    }

    private MockHttpServletRequest loginRequest(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
