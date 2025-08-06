package com.likelion.realtalk.domain.oauth.userinfo;

import java.util.Map;

public class KakaoOAuth2UserInfo extends OAuth2UserInfo {
  public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
    super(attributes);
  }

  @Override
  public String getProviderId() {
    // Kakao는 id(Long) root에 있음
    return String.valueOf(attributes.get("id"));
  }

  @Override
  public String getEmail() {
    // kakao_account.email
    Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
    if (kakaoAccount != null) {
      return (String) kakaoAccount.get("email");
    }
    return null;
  }

  @Override
  public String getNickname() {
    // kakao_account.profile.nickname
    Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
    if (kakaoAccount != null) {
      Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
      if (profile != null) {
        return (String) profile.get("nickname");
      }
    }
    return null;
  }
}
