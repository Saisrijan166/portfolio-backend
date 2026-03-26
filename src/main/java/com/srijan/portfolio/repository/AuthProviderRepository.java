package com.srijan.portfolio.repository;

import com.srijan.portfolio.entity.AuthProvider;
import com.srijan.portfolio.entity.AuthProviderType;
import com.srijan.portfolio.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthProviderRepository extends JpaRepository<AuthProvider, UUID> {

    Optional<AuthProvider> findByProviderAndProviderUserId(AuthProviderType provider, String providerUserId);

    Optional<AuthProvider> findByUserAndProvider(User user, AuthProviderType provider);

    List<AuthProvider> findAllByUserId(Long userId);
}
