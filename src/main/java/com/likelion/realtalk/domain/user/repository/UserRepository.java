package com.likelion.realtalk.domain.user.repository;

import com.likelion.realtalk.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

}
