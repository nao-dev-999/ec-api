package com.example.ecapi.batch.job.settlementexport;

import com.example.ecapi.batch.JobParametersProvider;
import com.example.ecapi.batch.job.dailysales.TargetDateRangeJobParameters;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.stereotype.Component;

@Component
public class SettlementExportJobParametersProvider implements JobParametersProvider {

    @Override
    public String jobName() {
        return "settlementExportJob";
    }

    @Override
    public JobParameters resolve(String[] args) {
        return TargetDateRangeJobParameters.resolve(args);
    }
}
