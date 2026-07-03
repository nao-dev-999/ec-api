package com.example.ecapi.service.auth;

import com.example.ecapi.repository.CustomerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;

    @Override
    public LoginUserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return customerRepository
                .findByEmail(email)
                .map(
                        customer ->
                                new LoginUserDetails(
                                        customer.getId(),
                                        customer.getEmail(),
                                        customer.getPassword(),
                                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .orElseThrow(() -> new UsernameNotFoundException("customer not found: " + email));
    }
}
