package com.example.ecapi.batch.job.dailysales;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

/**
 * paymentIntakeJob/salesAggregationJob/settlementExportJob共通の targetDateFrom/targetDateTo
 * 組み立てロジック（{@code --targetDate=}引数、未指定時は「前日」（JST基準）を対象日とする）。
 * 3Jobそれぞれ別パッケージ（job.paymentintake/job.salesaggregation/job.settlementexport）の {@code
 * JobParametersProvider}実装から参照されるため、Jobをまたがない{@code job.dailysales}パッケージに置く。
 */
public final class TargetDateRangeJobParameters {

    private static final ZoneId JST = ZoneId.of("Asia/Tokyo");
    private static final String TARGET_DATE_ARG_PREFIX = "--targetDate=";

    private TargetDateRangeJobParameters() {}

    public static JobParameters resolve(String[] args) {
        LocalDate targetDate = resolveTargetDate(args);
        Instant from = targetDate.atStartOfDay(JST).toInstant();
        Instant to = targetDate.plusDays(1).atStartOfDay(JST).toInstant();

        return new JobParametersBuilder()
                .addString("targetDateFrom", from.toString())
                .addString("targetDateTo", to.toString())
                .toJobParameters();
    }

    private static LocalDate resolveTargetDate(String[] args) {
        for (String arg : args) {
            if (arg.startsWith(TARGET_DATE_ARG_PREFIX)) {
                return LocalDate.parse(arg.substring(TARGET_DATE_ARG_PREFIX.length()));
            }
        }
        return LocalDate.now(JST).minusDays(1); // 未指定時は「前日分」
    }
}
