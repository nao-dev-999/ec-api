package com.example.ecapi.service.auth;

import com.example.ecapi.repository.EnployeeRepository;
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

    private final EnployeeRepository enployeeRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return enployeeRepository
                .findByEmail(email)
                .map(
                        employee -> {
                            log.info(
                                    "Loaded employee: email={}, role={}",
                                    employee.getEmail(),
                                    employee.getRole());

                            return new User(
                                    employee.getEmail(),
                                    employee.getPassword(),
                                    List.of(
                                            new SimpleGrantedAuthority(
                                                    "ROLE_" + employee.getRole())));
                        })
                .orElseThrow(() -> new UsernameNotFoundException("employee not found: " + email));
    }
}
