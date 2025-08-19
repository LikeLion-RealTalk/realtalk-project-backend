package com.likelion.realtalk.domain.user.repository;

import com.likelion.realtalk.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByUsername(String username);
  boolean existsByUsername(String username);

  @Modifying
  @Query("update User u set u.refreshToken = null where u.id = :userId")
  void clearRefreshTokenById(@Param("userId") Long userId);
}
