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

}
