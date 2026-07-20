package com.example.ecapi.batch.job;

import com.example.ecapi.batch.JobParametersProvider;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.stereotype.Component;

@Component
public class DailySalesJobParametersProvider implements JobParametersProvider {

    private static final ZoneId JST = ZoneId.of("Asia/Tokyo");
    private static final String TARGET_DATE_ARG_PREFIX = "--targetDate=";

    @Override
    public String jobName() {
        return "dailySalesAggregationJob";
    }

    @Override
    public JobParameters resolve(String[] args) {
        LocalDate targetDate = resolveTargetDate(args);
        Instant from = targetDate.atStartOfDay(JST).toInstant();
        Instant to = targetDate.plusDays(1).atStartOfDay(JST).toInstant();

        return new JobParametersBuilder()
                .addString("targetDateFrom", from.toString())
                .addString("targetDateTo", to.toString())
                .toJobParameters();
    }

    private LocalDate resolveTargetDate(String[] args) {
        for (String arg : args) {
            if (arg.startsWith(TARGET_DATE_ARG_PREFIX)) {
                return LocalDate.parse(arg.substring(TARGET_DATE_ARG_PREFIX.length()));
            }
        }
        return LocalDate.now(JST).minusDays(1); // 未指定時は「前日分」（14.2節）
    }
}
