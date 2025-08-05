package com.likelion.realtalk.domain.user.service;

import com.likelion.realtalk.domain.user.dto.UserProfileDto;
import com.likelion.realtalk.domain.user.entity.User;
import com.likelion.realtalk.domain.user.entity.UserProfile;
import com.likelion.realtalk.domain.user.repository.UserProfileRepository;
import com.likelion.realtalk.domain.user.repository.UserRepository;
import com.likelion.realtalk.global.exception.CustomException;
import com.likelion.realtalk.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final UserProfileRepository userProfileRepository;

  public UserProfileDto getUserProfile(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    UserProfile profile = userProfileRepository.findByUser(user)
        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    return new UserProfileDto(profile.getNickname(), profile.getBio());
  }

  public void updateUserProfile(Long userId, UserProfileDto dto) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    UserProfile profile = userProfileRepository.findByUser(user)
        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    profile.setNickname(dto.getNickname());
    profile.setBio(dto.getBio());
    userProfileRepository.save(profile);
  }

  public boolean checkUsernameExists(String username) {
    return userRepository.existsByUsername(username);
  }

  @Transactional
  public void setupUsername(Long userId, String newUsername) {
    // 1. 사용자명 중복 체크
    if (checkUsernameExists(newUsername)) {
      throw new RuntimeException("이미 사용 중인 사용자명입니다.");
    }

    // 2. 사용자명 유효성 체크 (예: 길이, 특수문자 등)
    if (newUsername.length() < 3 || newUsername.length() > 20) {
      throw new RuntimeException("사용자명은 3-20자 사이여야 합니다.");
    }

    if (!newUsername.matches("^[a-zA-Z0-9가-힣_]+$")) {
      throw new RuntimeException("사용자명은 영문, 한글, 숫자, 밑줄(_)만 사용 가능합니다.");
    }

    // 3. 사용자 조회 및 업데이트
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    user.setUsername(newUsername);
    user.setIsUsernameConfirmed(true);
    userRepository.save(user);
  }

  public boolean isUsernameConfirmed(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    return user.getIsUsernameConfirmed();
  }
}
