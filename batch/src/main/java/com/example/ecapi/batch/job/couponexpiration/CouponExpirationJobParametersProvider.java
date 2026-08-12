package com.example.ecapi.batch.job.couponexpiration;

import com.example.ecapi.batch.JobParametersProvider;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.stereotype.Component;

/**
 * couponExpirationJob専用のJobParameters組み立て。
 *
 * <p>他3Jobが共有する{@code TargetDateRangeJobParameters}（{@code targetDateFrom}/{@code
 * targetDateTo}という「範囲」）とは異なり、このJobが必要とするのは「対象日の開始時刻（{@code
 * asOf}）より前にvalid_toが過ぎているか」という単一のカットオフ判定のみである。形状が異なるため共有クラスは使わず、
 *単一Jobからしか参照されないロジックとしてこのパッケージに閉じる（{@code job.dailysales}パッケージの対象外）。
 *
 * <p>{@code --targetDate}未指定時は他3Jobの「前日」ではなく「当日」（JST）を対象日とする点に注意。
 */
@Component
public class CouponExpirationJobParametersProvider implements JobParametersProvider {

    private static final ZoneId JST = ZoneId.of("Asia/Tokyo");
    private static final String TARGET_DATE_ARG_PREFIX = "--targetDate=";

    @Override
    public String jobName() {
        return "couponExpirationJob";
    }

    @Override
    public JobParameters resolve(String[] args) {
        LocalDate targetDate = resolveTargetDate(args);
        Instant asOf = targetDate.atStartOfDay(JST).toInstant();
        return new JobParametersBuilder().addString("asOf", asOf.toString()).toJobParameters();
    }

    private LocalDate resolveTargetDate(String[] args) {
        for (String arg : args) {
            if (arg.startsWith(TARGET_DATE_ARG_PREFIX)) {
                return LocalDate.parse(arg.substring(TARGET_DATE_ARG_PREFIX.length()));
            }
        }
        return LocalDate.now(JST); // 未指定時は「当日」（当日開始時点で期限切れのクーポンを失効させる）
    }
}
