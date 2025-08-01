package com.likelion.realtalk.domain.auth.repository;

import com.likelion.realtalk.domain.auth.entity.Auth;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthRepository extends JpaRepository<Auth, Long> {

}
