package com.likelion.realtalk.domain.oauth.service;

import com.likelion.realtalk.domain.auth.entity.Auth;
import com.likelion.realtalk.domain.auth.repository.AuthRepository;
import com.likelion.realtalk.domain.oauth.factory.OAuth2UserInfoFactory;
import com.likelion.realtalk.domain.oauth.type.OAuth2Provider;
import com.likelion.realtalk.domain.oauth.userinfo.OAuth2UserInfo;
import com.likelion.realtalk.domain.user.entity.User;
import com.likelion.realtalk.domain.user.entity.UserProfile;
import com.likelion.realtalk.domain.user.repository.UserRepository;
import com.likelion.realtalk.domain.user.repository.UserProfileRepository;
import com.likelion.realtalk.global.security.core.CustomUserDetails;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuth2UserService extends DefaultOAuth2UserService {

  private final AuthRepository authRepository;
  private final UserRepository userRepository;
  private final UserProfileRepository userProfileRepository;

  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2User oauth2User = super.loadUser(userRequest);

    String registrationId = userRequest.getClientRegistration()
        .getRegistrationId(); // "kakao" or "google"
    OAuth2Provider provider = OAuth2Provider.from(registrationId);

    Map<String, Object> attributes = oauth2User.getAttributes();
    OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, attributes);

    // 기존 Auth 정보 조회
    Optional<Auth> authOpt = authRepository.findByProviderAndProviderId(
        provider.name().toLowerCase(),
        userInfo.getProviderId()
    );

    User user;
    if (authOpt.isPresent()) {
      // 기존 사용자
      user = authOpt.get().getUser();
    } else {
      // 신규 사용자 - 자동 회원가입
      user = createNewUser(userInfo, provider.name().toLowerCase());
    }

    return new CustomUserDetails(user, attributes);
  }

  private User createNewUser(OAuth2UserInfo userInfo, String provider) {
    // 임시 username 생성 (소셜 로그인 이름 기반)
    String tempUsername = generateTempUsername(userInfo.getNickname());

    // 새 사용자 생성 (username 미확정 상태)
    User user = User.builder()
        .username(tempUsername)
        .role("ROLE_USER")
        .isUsernameConfirmed(false) // 처음엔 미확정 상태
        .build();

    User savedUser = userRepository.save(user);

    // 사용자 프로필 생성
    UserProfile profile = UserProfile.builder()
        .user(savedUser)
        .nickname(userInfo.getNickname())
        .bio(null) // 기본값
        .build();

    userProfileRepository.save(profile);

    // OAuth2 연동 정보 저장
    Auth auth = Auth.builder()
        .user(savedUser)
        .provider(provider)
        .providerId(userInfo.getProviderId())
        .providerEmail(userInfo.getEmail())
        .build();

    authRepository.save(auth);

    return savedUser;
  }

  private String generateTempUsername(String baseName) {
    // 임시 username을 "temp_" prefix로 생성하여 나중에 사용자가 변경할 수 있도록 함
    return "temp_" + System.currentTimeMillis() + "_" + baseName.replaceAll("[^a-zA-Z0-9가-힣]", "");
  }
}
