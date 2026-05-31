package com.example.ecapi.service.auth;

import java.util.List;

import com.example.ecapi.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final CustomerRepository customerRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return customerRepository
                .findByEmail(email)
                .map(
                        customer ->
                                new org.springframework.security.core.userdetails.User(
                                        customer.getEmail(),
                                        customer.getPassword(),
                                        List.of(new SimpleGrantedAuthority(customer.getRole()))))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
