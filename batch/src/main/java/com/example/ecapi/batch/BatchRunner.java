package com.example.ecapi.batch;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

/**
 * spring.batch.job.enabled=false により標準のJobLauncherApplicationRunnerを無効化した上で、
 * このRunnerが唯一の起動経路になる（14.6節: 二重起動防止、14.8節: JobParametersによる冪等性）。 どのJobを起動するかは{@code
 * --job=}引数（Bean名）で選択する。未指定時は既存運用（EventBridge Scheduler経由のECS
 * RunTaskが引数なしで起動する現行の日次売上集計）との互換のためデフォルトJobを使う。 JobParametersの形はJob毎に異なりうるため、組み立ては{@link
 * JobParametersProvider}にJob単位で委譲する。
 */
@Component
public class BatchRunner implements CommandLineRunner, ExitCodeGenerator {

    private static final String JOB_ARG_PREFIX = "--job=";
    private static final String DEFAULT_JOB_NAME = "dailySalesAggregationJob";

    private final JobOperator jobOperator;
    private final JobRepository jobRepository;
    private final Map<String, Job> jobsByName;
    private final Map<String, JobParametersProvider> parametersProvidersByJobName;

    private volatile int exitCode = 0;

    public BatchRunner(
            JobOperator jobOperator,
            JobRepository jobRepository,
            Map<String, Job> jobsByName,
            List<JobParametersProvider> jobParametersProviders) {
        this.jobOperator = jobOperator;
        this.jobRepository = jobRepository;
        this.jobsByName = jobsByName;
        this.parametersProvidersByJobName =
                jobParametersProviders.stream()
                        .collect(Collectors.toMap(JobParametersProvider::jobName, p -> p));
    }

    @Override
    public void run(String... args) throws Exception {
        Job job = resolveJob(args);
        String jobName = job.getName();
        if (!jobRepository.findRunningJobExecutions(jobName).isEmpty()) {
            throw new IllegalStateException("ジョブが既に実行中のため起動を中止します: " + jobName);
        }

        JobParameters jobParameters = resolveJobParameters(jobName, args);

        JobExecution execution = jobOperator.start(job, jobParameters);
        exitCode = execution.getStatus() == BatchStatus.COMPLETED ? 0 : 1;
    }

    private Job resolveJob(String[] args) {
        String jobName = DEFAULT_JOB_NAME;
        for (String arg : args) {
            if (arg.startsWith(JOB_ARG_PREFIX)) {
                jobName = arg.substring(JOB_ARG_PREFIX.length());
                break;
            }
        }
        Job job = jobsByName.get(jobName);
        if (job == null) {
            throw new IllegalArgumentException(
                    "未知のジョブ名です: " + jobName + "（利用可能: " + jobsByName.keySet() + "）");
        }
        return job;
    }

    private JobParameters resolveJobParameters(String jobName, String[] args) {
        JobParametersProvider provider = parametersProvidersByJobName.get(jobName);
        if (provider == null) {
            throw new IllegalStateException("JobParametersProviderが未登録のジョブです: " + jobName);
        }
        return provider.resolve(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
