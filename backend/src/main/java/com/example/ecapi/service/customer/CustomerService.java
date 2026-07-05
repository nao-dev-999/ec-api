package com.example.ecapi.service.customer;

import com.example.ecapi.entity.Customer;
import com.example.ecapi.exception.CustomerNotFoundException;
import com.example.ecapi.repository.CustomerRepository;
import com.example.ecapi.service.customer.dto.CustomerResult;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;

    public List<CustomerResult> findAll() {
        return customerRepository.findAll().stream().map(this::toCustomerResult).toList();
    }

    public CustomerResult findById(Long id) {
        return customerRepository
                .findById(id)
                .map(this::toCustomerResult)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    @Transactional
    public void delete(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new CustomerNotFoundException(id);
        }
        log.info("Customer deleted customerId={}", id);
        customerRepository.deleteById(id);
    }

    private CustomerResult toCustomerResult(Customer customer) {
        return new CustomerResult(
                customer.getId(),
                customer.getEmail(),
                customer.getLastName(),
                customer.getFirstName(),
                customer.getLastNameKana(),
                customer.getFirstNameKana(),
                customer.getPhoneNumber(),
                customer.getPostalCode(),
                customer.getPrefecture(),
                customer.getCity(),
                customer.getAddressLine1(),
                customer.getAddressLine2(),
                LocalDateTime.ofInstant(customer.getCreatedAt(), ZoneId.systemDefault()),
                LocalDateTime.ofInstant(customer.getUpdatedAt(), ZoneId.systemDefault()),
                customer.getVersion());
    }
}
