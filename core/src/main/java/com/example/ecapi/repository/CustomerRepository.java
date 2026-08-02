package com.example.ecapi.repository;

import com.example.ecapi.entity.Customer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    /** ログイン用。論理削除済みアカウントは認証させない。 */
    Optional<Customer> findByEmailAndDeletedFalse(String email);

    /**
     * メールアドレス重複チェック用。emailカラムの物理UNIQUE制約は論理削除済み行にも及ぶため、あえて{@code deleted}で絞り込まない
     * （絞り込むと、論理削除済みアカウントと同じメールアドレスを許可してしまい、後続のINSERT/UPDATEがDataIntegrityViolationExceptionで失敗する）。
     */
    Optional<Customer> findByEmail(String email);
}
