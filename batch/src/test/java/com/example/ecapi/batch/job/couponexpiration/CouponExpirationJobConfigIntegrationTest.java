package com.example.ecapi.batch.job.couponexpiration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ecapi.batch.config.BatchAuditConfig;
import com.example.ecapi.batch.support.TestcontainersConfiguration;
import com.example.ecapi.entity.Coupon;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * {@code expiredCouponIdReader}のカットオフ判定（{@code valid_to < asOf}、境界値は対象外）が
 * 実DB（Testcontainers）上で正しく機能することを検証する。{@link OrderDetailKeysetItemReaderTest}と同様の方針。
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan("com.example.ecapi.entity")
@Import({TestcontainersConfiguration.class, BatchAuditConfig.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class CouponExpirationJobConfigIntegrationTest {

    @Autowired private TestEntityManager entityManager;
    @Autowired private DataSource dataSource;

    private final CouponExpirationJobConfig config = new CouponExpirationJobConfig();

    private Coupon persistCoupon(String code, Instant validTo, boolean active) {
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDiscountAmount(BigDecimal.valueOf(500));
        coupon.setValidTo(validTo);
        coupon.setActive(active);
        return entityManager.persistFlushFind(coupon);
    }

    private List<Long> readExpiredCouponIds(Instant asOf) throws Exception {
        ItemStreamReader<Long> reader = config.expiredCouponIdReader(dataSource, asOf.toString());
        reader.open(new ExecutionContext());
        try {
            List<Long> ids = new ArrayList<>();
            Long id;
            while ((id = reader.read()) != null) {
                ids.add(id);
            }
            return ids;
        } finally {
            reader.close();
        }
    }

    @Test
    @DisplayName(
            "active=trueかつvalid_toがasOfより過去の行のみを対象にし、"
                    + "既にinactive・期限内・無期限（valid_to=null）・境界値ちょうどの行は対象外になること")
    void shouldSelectOnlyActiveCouponsExpiredBeforeAsOf() throws Exception {
        Instant asOf = Instant.parse("2026-08-12T00:00:00Z");

        Coupon expiredActive1 = persistCoupon("EXPIRED-1", asOf.minusSeconds(3600), true);
        Coupon expiredActive2 = persistCoupon("EXPIRED-2", asOf.minusSeconds(60), true);
        persistCoupon("ALREADY-INACTIVE", asOf.minusSeconds(3600), false);
        persistCoupon("NOT-YET-EXPIRED", asOf.plusSeconds(3600), true);
        persistCoupon("NO-EXPIRY", null, true);
        persistCoupon("AT-BOUNDARY", asOf, true);

        List<Long> result = readExpiredCouponIds(asOf);

        assertThat(result).containsExactly(expiredActive1.getId(), expiredActive2.getId());
    }
}
