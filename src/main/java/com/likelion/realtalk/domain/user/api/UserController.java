package com.likelion.realtalk.domain.user.api;

import com.likelion.realtalk.domain.user.dto.UserProfileDto;
import com.likelion.realtalk.domain.user.service.UserService;
import com.likelion.realtalk.global.security.core.CustomUserDetails;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @GetMapping("/me")
  public ResponseEntity<UserProfileDto> getMyProfile(
      @AuthenticationPrincipal CustomUserDetails userDetail) {
    UserProfileDto profile = userService.getUserProfile(userDetail.getUserId());
    return ResponseEntity.ok(profile);
  }

  @PutMapping("/me")
  public ResponseEntity<Void> updateMyProfile(
      @AuthenticationPrincipal CustomUserDetails userDetail,
      @RequestBody UserProfileDto dto) {
    userService.updateUserProfile(userDetail.getUserId(), dto);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/check-username")
  public Map<String, Boolean> checkUsername(@RequestParam("value") String username) {
    boolean exists = userService.checkUsernameExists(username);
    return Map.of("exists", exists);
  }

  @PostMapping("/setup-username")
  public ResponseEntity<Map<String, String>> setupUsername(
      @AuthenticationPrincipal CustomUserDetails userDetail,
      @RequestBody Map<String, String> request) {
    String newUsername = request.get("username");

    if (newUsername == null || newUsername.trim().isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("error", "사용자명을 입력해주세요."));
    }

    try {
      userService.setupUsername(userDetail.getUserId(), newUsername.trim());
      return ResponseEntity.ok(Map.of("message", "사용자명이 설정되었습니다."));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  @GetMapping("/username-status")
  public ResponseEntity<Map<String, Boolean>> getUsernameStatus(
      @AuthenticationPrincipal CustomUserDetails userDetail) {
    boolean isConfirmed = userService.isUsernameConfirmed(userDetail.getUserId());
    return ResponseEntity.ok(Map.of("isUsernameConfirmed", isConfirmed));
  }
}
