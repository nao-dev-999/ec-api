package com.example.ecapi.service.auth;

import com.example.ecapi.repository.EmployeeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    @Override
    public LoginUserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return employeeRepository
                .findByEmail(email)
                .map(
                        employee ->
                                new LoginUserDetails(
                                        employee.getId(),
                                        employee.getEmail(),
                                        employee.getPassword(),
                                        List.of(
                                                new SimpleGrantedAuthority(
                                                        "ROLE_" + employee.getRole()))))
                .orElseThrow(() -> new UsernameNotFoundException("employee not found: " + email));
    }
}
