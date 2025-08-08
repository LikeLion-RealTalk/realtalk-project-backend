package com.likelion.realtalk.domain.oauth.service;

import static com.likelion.realtalk.domain.user.entity.Role.USER;

import com.likelion.realtalk.domain.auth.entity.Auth;
import com.likelion.realtalk.domain.auth.repository.AuthRepository;
import com.likelion.realtalk.domain.oauth.factory.OAuth2UserInfoFactory;
import com.likelion.realtalk.domain.oauth.type.OAuth2Provider;
import com.likelion.realtalk.domain.oauth.userinfo.OAuth2UserInfo;
import com.likelion.realtalk.domain.user.entity.User;
import com.likelion.realtalk.domain.user.repository.UserRepository;
import com.likelion.realtalk.global.security.core.CustomUserDetails;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserService extends DefaultOAuth2UserService {

  private final AuthRepository authRepository;
  private final UserRepository userRepository;

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
    // 소셜 로그인에서 가져온 이름을 바로 username으로 사용
    String username = userInfo.getNickname();

    // 새 사용자 생성
    User user = User.of(username, USER);

    User savedUser = userRepository.save(user);

    log.info("새 사용자 생성 완료: username={}, role={}", savedUser.getUsername(), savedUser.getRole());

    // OAuth2 연동 정보 저장
    Auth auth = Auth.of(savedUser, provider, userInfo.getProviderId(), userInfo.getEmail());

    authRepository.save(auth);

    return savedUser;
  }

}
