package com.jobflow.service;

import com.jobflow.persistence.entity.UserEntity;
import com.jobflow.persistence.repository.UserJpaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Profile("mysql")
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserJpaRepository users;

    public DatabaseUserDetailsService(UserJpaRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = users.findByEmailIgnoreCase(username)
                .filter(account -> account.getPasswordHash() != null)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found."));
        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();
    }
}
