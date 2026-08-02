package com.example.ecapi.repository;

import com.example.ecapi.entity.Employee;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    /** ログイン用。論理削除済みアカウントは認証させない。 */
    Optional<Employee> findByEmailAndDeletedFalse(String email);

    /**
     * メールアドレス重複チェック用。emailカラムの物理UNIQUE制約は論理削除済み行にも及ぶため、あえて{@code deleted}で絞り込まない
     * （絞り込むと、論理削除済みアカウントと同じメールアドレスを許可してしまい、後続のINSERT/UPDATEがDataIntegrityViolationExceptionで失敗する）。
     */
    Optional<Employee> findByEmail(String email);
}
