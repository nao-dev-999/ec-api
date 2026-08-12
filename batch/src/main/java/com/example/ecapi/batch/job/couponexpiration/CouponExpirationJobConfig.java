package com.example.ecapi.batch.job.couponexpiration;

import com.example.ecapi.batch.config.BatchAuditConfig;
import java.sql.Timestamp;
import java.time.Instant;
import javax.sql.DataSource;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * クーポン期限失効バッチ。{@code coupon.valid_to}を過ぎても{@code active=true}のままの行を日次で一括更新する。
 *
 * <p>{@code CouponService#findApplicableCoupon}（クーポン利用時）は都度{@code validTo}と{@code
 * active}をその場で判定しているため、このJobが動かなくても不正なクーポンが適用されることはない。 ただし{@code active}が実体（有効期限切れかどうか）に追随しないままだと、管理画面の「有効クーポン一覧」表示や
 * 将来{@code active}のみを条件にする集計・検索処理が誤った結果を返しうるため、フラグを実体に同期させる。
 *
 * <p>他3Job（{@code paymentIntakeJob}等）の対象日は「前日」を指すが、このJobの対象日は「当日」を指す
 * （{@code --targetDate}未指定時はJST当日）。対象日の開始時刻を{@code asOf}とし、{@code valid_to < asOf}
 * （＝対象日が始まる前に有効期限が切れている）の行のみを失効させることで、同一対象日内での再実行は常に同じ結果になる
 * （14.8節のリスタート・冪等性設計と同じ考え方。積算ではなく「valid_toが過ぎているかどうか」の判定自体が
 * 元来べき等なため、ステージング+置き換えのような特別な仕組みは不要）。
 *
 * <p>クーポン件数は商品・注文と比べて小規模（管理者が個別発行する運用、{@code
 * AdminCouponController}参照）であるため、14.5節のパーティショニングやStatelessSessionは不要と判断し、
 * 単純なchunk指向Step + カーソル読み取りで構成する。
 */
@Configuration
public class CouponExpirationJobConfig {

    private static final String EXPIRED_COUPON_ID_QUERY =
            """
            SELECT id FROM coupon
            WHERE active = true AND valid_to IS NOT NULL AND valid_to < ?
            ORDER BY id
            """;

    private static final String DEACTIVATE_SQL =
            """
            UPDATE coupon
            SET active = false,
                version = version + 1,
                updated_by = :systemUserId,
                updated_at = now()
            WHERE id = :id
            """;

    @Bean
    public Job couponExpirationJob(JobRepository jobRepository, Step couponExpirationStep) {
        return new JobBuilder("couponExpirationJob", jobRepository)
                .start(couponExpirationStep)
                .build();
    }

    @Bean
    public Step couponExpirationStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemStreamReader<Long> expiredCouponIdReader,
            JdbcBatchItemWriter<Long> couponDeactivateWriter) {
        return new StepBuilder("couponExpirationStep", jobRepository)
                .<Long, Long>chunk(500)
                .transactionManager(transactionManager)
                .reader(expiredCouponIdReader)
                .writer(couponDeactivateWriter)
                .build();
    }

    @Bean
    @StepScope
    public ItemStreamReader<Long> expiredCouponIdReader(
            DataSource dataSource, @Value("#{jobParameters['asOf']}") String asOf) {
        return new JdbcCursorItemReaderBuilder<Long>()
                .name("expiredCouponIdReader")
                .dataSource(dataSource)
                .sql(EXPIRED_COUPON_ID_QUERY)
                .queryArguments(Timestamp.from(Instant.parse(asOf)))
                .rowMapper((rs, rowNum) -> rs.getLong("id"))
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<Long> couponDeactivateWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Long>()
                .dataSource(dataSource)
                .sql(DEACTIVATE_SQL)
                .itemSqlParameterSourceProvider(
                        id -> {
                            var params = new MapSqlParameterSource();
                            params.addValue("id", id);
                            params.addValue("systemUserId", BatchAuditConfig.BATCH_SYSTEM_USER_ID);
                            return params;
                        })
                .build();
    }
}
