package com.likelion.realtalk.domain.user.api;

import com.likelion.realtalk.domain.user.dto.UserProfileDto;
import com.likelion.realtalk.domain.user.service.UserService;
import com.likelion.realtalk.global.entity.CustomUserDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @GetMapping("/me")
  public ResponseEntity<UserProfileDto> getMyProfile(
      @AuthenticationPrincipal CustomUserDetail userDetail) {
    UserProfileDto profile = userService.getUserProfile(userDetail.getUserId());
    return ResponseEntity.ok(profile);
  }

  @PutMapping("/me")
  public ResponseEntity<Void> updateMyProfile(
      @AuthenticationPrincipal CustomUserDetail userDetail,
      @RequestBody UserProfileDto dto) {
    userService.updateUserProfile(userDetail.getUserId(), dto);
    return ResponseEntity.noContent().build();
  }
}
