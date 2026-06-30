package com.example.ecapi.service.customer;

import com.example.ecapi.entity.Customer;
import com.example.ecapi.exception.CustomerEmailDuplicateException;
import com.example.ecapi.exception.CustomerNotFoundException;
import com.example.ecapi.exception.InvalidCurrentPasswordException;
import com.example.ecapi.repository.CustomerRepository;
import com.example.ecapi.service.customer.dto.CustomerResult;
import com.example.ecapi.service.customer.dto.UpdateCustomerEmail;
import com.example.ecapi.service.customer.dto.UpdateCustomerPassword;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerMeService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerResult findMe(Long customerId) {
        return customerRepository
                .findById(customerId)
                .map(this::toResult)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    @Transactional
    public CustomerResult updateEmail(Long customerId, UpdateCustomerEmail dto) {
        customerRepository
                .findByEmail(dto.email())
                .filter(c -> !c.getId().equals(customerId))
                .ifPresent(
                        c -> {
                            throw new CustomerEmailDuplicateException(dto.email());
                        });
        Customer customer =
                customerRepository
                        .findById(customerId)
                        .orElseThrow(() -> new CustomerNotFoundException(customerId));
        customer.setEmail(dto.email());
        customer.setVersion(dto.version());
        Customer saved = customerRepository.save(customer);
        log.info("Customer email updated customerId={}", customerId);
        return toResult(saved);
    }

    @Transactional
    public void updatePassword(Long customerId, UpdateCustomerPassword dto) {
        Customer customer =
                customerRepository
                        .findById(customerId)
                        .orElseThrow(() -> new CustomerNotFoundException(customerId));
        if (!passwordEncoder.matches(dto.currentPassword(), customer.getPassword())) {
            throw new InvalidCurrentPasswordException();
        }
        customer.setVersion(dto.version());
        customer.setPassword(passwordEncoder.encode(dto.newPassword()));
        customerRepository.save(customer);
        log.info("Customer password updated customerId={}", customerId);
    }

    private CustomerResult toResult(Customer customer) {
        return new CustomerResult(
                customer.getId(),
                customer.getEmail(),
                LocalDateTime.ofInstant(customer.getCreatedAt(), ZoneId.systemDefault()),
                LocalDateTime.ofInstant(customer.getUpdatedAt(), ZoneId.systemDefault()),
                customer.getVersion());
    }
}
