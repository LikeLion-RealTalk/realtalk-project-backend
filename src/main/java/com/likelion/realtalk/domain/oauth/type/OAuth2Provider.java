package com.likelion.realtalk.domain.oauth.type;

public enum OAuth2Provider {
  KAKAO,
  GOOGLE;

  public static OAuth2Provider from(String provider) {
    for (OAuth2Provider p : OAuth2Provider.values()) {
      if (p.name().equalsIgnoreCase(provider)) {
        return p;
      }
    }
    throw new IllegalStateException("지원하지 않는 소셜 로그인 타입: " + provider);
  }

}
