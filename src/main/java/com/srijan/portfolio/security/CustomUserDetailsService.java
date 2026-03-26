package com.srijan.portfolio.security;

import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.entity.UserStatus;
import com.srijan.portfolio.repository.UserRepository;
import com.srijan.portfolio.service.AuthSupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private static final String NO_PASSWORD_HASH = "$2a$10$e0NR3O4N8xI9G9S0lG2Y4uFj63HgFUsVTNff7kwh28ykVfoCkb0mq";

    private final UserRepository userRepository;
    private final AuthSupportService authSupportService;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        String normalized = authSupportService.resolveIdentifier(identifier, null);
        User user = authSupportService.isEmailIdentifier(normalized)
                ? userRepository.findByEmailIgnoreCase(normalized)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + identifier))
                : userRepository.findByUsernameIgnoreCase(normalized)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + identifier));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new DisabledException("Account is suspended");
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash() == null || user.getPasswordHash().isBlank() ? NO_PASSWORD_HASH : user.getPasswordHash(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole())));
    }
}
