package com.example.ecapi.batch.job.paymentintake;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ecapi.batch.dto.PaymentConfirmationRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.transaction.PlatformTransactionManager;

/** paymentConfirmationIntakeStepのBeanファクトリメソッドを直接呼び出し、Stepが例外なくビルドできることを確認するスモークテスト。 */
@ExtendWith(MockitoExtension.class)
class PaymentIntakeJobConfigTest {

    @Mock private JobRepository jobRepository;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private FlatFileItemReader<PaymentConfirmationRow> paymentConfirmationFileReader;
    @Mock private JdbcBatchItemWriter<PaymentConfirmationRow> paymentConfirmationStagingWriter;

    private final PaymentIntakeJobConfig config = new PaymentIntakeJobConfig();

    @Test
    @DisplayName("paymentConfirmationIntakeStepがCSV読み取り→ステージング書き込みのchunk構成で正しくビルドされること")
    void shouldBuildPaymentConfirmationIntakeStepAsChunkOrientedStep() {
        Step step =
                config.paymentConfirmationIntakeStep(
                        jobRepository,
                        transactionManager,
                        paymentConfirmationFileReader,
                        paymentConfirmationStagingWriter);

        assertThat(step).isNotNull();
        assertThat(step.getName()).isEqualTo("paymentConfirmationIntakeStep");
    }
}
