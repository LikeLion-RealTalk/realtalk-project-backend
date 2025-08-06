package com.likelion.realtalk.domain.oauth.userinfo;

import java.util.Map;
import lombok.Getter;

@Getter
public abstract class OAuth2UserInfo {
  protected Map<String, Object> attributes;

  public OAuth2UserInfo(Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  public abstract String getProviderId();
  public abstract String getEmail();
  public abstract String getNickname();

}
