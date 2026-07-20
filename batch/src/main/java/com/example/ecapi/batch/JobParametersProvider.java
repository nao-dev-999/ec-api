package com.example.ecapi.batch;

import org.springframework.batch.core.job.parameters.JobParameters;

/** Job毎に異なりうるJobParametersの組み立てを{@link BatchRunner}から切り離すための拡張点。 */
public interface JobParametersProvider {

    /** 対応するJobのBean名（{@link BatchRunner}が{@code --job=}引数と突き合わせるキー）。 */
    String jobName();

    JobParameters resolve(String[] args);
}
