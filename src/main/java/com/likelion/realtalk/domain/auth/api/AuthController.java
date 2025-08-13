package com.likelion.realtalk.domain.auth.api;

import com.likelion.realtalk.domain.auth.service.AuthService;
import com.likelion.realtalk.domain.user.dto.UserInfoDto;
import com.likelion.realtalk.global.security.core.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;

  @PostMapping("/refresh")
  public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    authService.reissueTokens(request, response);
    return ResponseEntity.ok().build();
  }

  // 로그인 한 사용자 정보 조회
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
