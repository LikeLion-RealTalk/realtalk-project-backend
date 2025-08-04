package com.likelion.realtalk.domain.auth.repository;

import com.likelion.realtalk.domain.auth.entity.Auth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthRepository extends JpaRepository<Auth, Long> {
    Optional<Auth> findByProviderAndProviderId(String provider, String providerId);
    Optional<Auth> findByEmail(String email);
}
