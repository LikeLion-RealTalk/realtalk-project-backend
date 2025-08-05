package com.likelion.realtalk.domain.user.api;

import com.likelion.realtalk.global.security.core.CustomUserDetails;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
  @GetMapping("/me")
  public ResponseEntity<Map<String, Object>> getMyProfile(
      @AuthenticationPrincipal CustomUserDetails userDetail) {
    Map<String, Object> userInfo = Map.of(
        "id", userDetail.getUserId(),
        "username", userDetail.getUsername(),
        "role", userDetail.getUser().getRole(),
        "roleName", userDetail.getUser().getRole().getValue()
    );
    return ResponseEntity.ok(userInfo);
  }
}
