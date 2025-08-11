package com.likelion.realtalk.domain.oauth.userinfo;

import java.util.Map;

public class GoogleOAuth2UserInfo extends OAuth2UserInfo{
  public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
    super(attributes);
  }

  @Override
  public String getProviderId() {
    // Google은 "sub"이 유저 고유값
    return (String) attributes.get("sub");
  }

  @Override
  public String getEmail() {
    return (String) attributes.get("email");
  }

  @Override
  public String getNickname() {
    // Google은 "name" 또는 "given_name"
    return (String) attributes.get("name");
  }


}
