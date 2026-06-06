package com.example.ecapi.service.auth;

import com.example.ecapi.repository.CustomerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final CustomerRepository customerRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return customerRepository
                .findByEmail(email)
                .map(
                        customer -> {
                            log.info(
                                    "Loaded user: email={}, role={}",
                                    customer.getEmail(),
                                    customer.getRole());

                            return new User(
                                    customer.getEmail(),
                                    customer.getPassword(),
                                    List.of(
                                            new SimpleGrantedAuthority(
                                                    "ROLE_" + customer.getRole())));
                        })
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
