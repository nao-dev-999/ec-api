package com.example.ecapi.batch.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot既定の{@code BatchAutoConfiguration}はJDBCを使わない{@code
 * ResourcelessJobRepository}（インメモリ、永続化なし）を提供するだけであり、DataSourceの有無に関わらずBATCH_JOB_*テーブルへは書き込まれない（Spring
 * Batch 6の仕様）。{@code @EnableJdbcJobRepository}でJDBC永続化を明示的に有効化する。
 */
@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository
public class BatchJdbcJobRepositoryConfig {}
