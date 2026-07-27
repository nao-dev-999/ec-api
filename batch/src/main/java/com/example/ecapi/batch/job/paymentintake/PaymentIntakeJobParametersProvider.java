package com.example.ecapi.batch.job.paymentintake;

import com.example.ecapi.batch.JobParametersProvider;
import com.example.ecapi.batch.job.dailysales.TargetDateRangeJobParameters;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.stereotype.Component;

@Component
public class PaymentIntakeJobParametersProvider implements JobParametersProvider {

    @Override
    public String jobName() {
        return "paymentIntakeJob";
    }

    @Override
    public JobParameters resolve(String[] args) {
        return TargetDateRangeJobParameters.resolve(args);
    }
}
