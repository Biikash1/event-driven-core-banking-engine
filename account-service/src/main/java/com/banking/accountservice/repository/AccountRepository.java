package com.banking.accountservice.repository;

import com.banking.accountservice.enitity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {

    boolean existsByEmail(String email);

    boolean existsByAccountNumber(String accountNumber);

}
