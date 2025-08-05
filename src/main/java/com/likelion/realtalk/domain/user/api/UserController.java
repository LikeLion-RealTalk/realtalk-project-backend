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

  // 로그인 한 사용자 정보 조회 - 테스트용!
  @GetMapping("/me")
  public ResponseEntity<UserInfoDto> getMyProfile(
      @AuthenticationPrincipal CustomUserDetails userDetail) {
    UserInfoDto userInfo = UserInfoDto.withProvider(
        userDetail.getUserId(),
        userDetail.getUsername(),
        userDetail.getUser().getRole(),
        userDetail.getUser().getAuth().getProvider()
    );
    return ResponseEntity.ok(userInfo);
  }
}
