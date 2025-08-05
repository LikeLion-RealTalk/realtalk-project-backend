package com.likelion.realtalk.domain.user.api;

import com.likelion.realtalk.domain.user.dto.UserInfoDto;
import com.likelion.realtalk.global.security.core.CustomUserDetails;
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
  public ResponseEntity<UserInfoDto> getMyProfile(
      @AuthenticationPrincipal CustomUserDetails userDetail) {
    UserInfoDto userInfo = UserInfoDto.from(
        userDetail.getUserId(),
        userDetail.getUsername(),
        userDetail.getUser().getRole()
    );
    return ResponseEntity.ok(userInfo);
  }
}
