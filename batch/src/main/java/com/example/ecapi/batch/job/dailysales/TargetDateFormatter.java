package com.example.ecapi.batch.job.dailysales;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * {@code jobParameters['targetDateFrom']}をファイル名等に埋め込むJST基準yyyyMMdd文字列へ変換する。
 * job.paymentintake/job.settlementexportの複数Jobから参照される共通ロジックのため、 Jobをまたがない{@code
 * job.dailysales}パッケージに置く。
 */
public final class TargetDateFormatter {

    private TargetDateFormatter() {}

    public static String yyyyMMdd(Object jobParametersTargetDateFrom) {
        return Instant.parse(String.valueOf(jobParametersTargetDateFrom))
                .atZone(ZoneId.of("Asia/Tokyo"))
                .toLocalDate()
                .format(DateTimeFormatter.BASIC_ISO_DATE);
    }
}
