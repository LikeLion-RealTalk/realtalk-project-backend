package com.likelion.realtalk.domain.user.repository;

import com.likelion.realtalk.domain.user.entity.User;
import com.likelion.realtalk.domain.user.entity.UserProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

  Optional<UserProfile> findByUser(User user);

}
