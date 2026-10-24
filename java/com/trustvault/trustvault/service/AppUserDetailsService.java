
package com.trustvault.trustvault.service;

import com.trustvault.trustvault.AppUser;
import com.trustvault.trustvault.AppUserRepository;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository repository;

    public AppUserDetailsService(AppUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException(
                    "Username cannot be empty");
        }

        String cleanUsername = username.trim();

        AppUser appUser = repository.findByUsername(cleanUsername)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found: " + cleanUsername));

        String role = appUser.getRole();

        if (role == null || role.isBlank()) {
            role = "USER";
        }

        role = role.trim().toUpperCase();

        // Prevent duplicate ROLE_ prefix.
        if (role.startsWith("ROLE_")) {
            role = role.substring(5);
        }

        return User.withUsername(appUser.getUsername())
                .password(appUser.getPassword())
                .roles(role)
                .build();
    }
}
