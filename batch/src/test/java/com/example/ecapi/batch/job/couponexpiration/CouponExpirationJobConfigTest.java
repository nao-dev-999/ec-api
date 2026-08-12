package com.example.ecapi.batch.job.couponexpiration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.transaction.PlatformTransactionManager;

/** couponExpirationStepのBeanファクトリメソッドを直接呼び出し、Stepが例外なくビルドできることを確認するスモークテスト。 */
@ExtendWith(MockitoExtension.class)
class CouponExpirationJobConfigTest {

    @Mock private JobRepository jobRepository;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private ItemStreamReader<Long> expiredCouponIdReader;
    @Mock private JdbcBatchItemWriter<Long> couponDeactivateWriter;

    private final CouponExpirationJobConfig config = new CouponExpirationJobConfig();

    @Test
    @DisplayName("couponExpirationStepが期限切れクーポンID読み取り→失効更新のchunk構成で正しくビルドされること")
    void shouldBuildCouponExpirationStepAsChunkOrientedStep() {
        Step step =
                config.couponExpirationStep(
                        jobRepository,
                        transactionManager,
                        expiredCouponIdReader,
                        couponDeactivateWriter);

        assertThat(step).isNotNull();
        assertThat(step.getName()).isEqualTo("couponExpirationStep");
    }
}
