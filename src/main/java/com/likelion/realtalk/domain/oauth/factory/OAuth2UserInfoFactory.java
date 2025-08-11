package com.likelion.realtalk.domain.oauth.factory;

import com.likelion.realtalk.domain.oauth.userinfo.GoogleOAuth2UserInfo;
import com.likelion.realtalk.domain.oauth.userinfo.KakaoOAuth2UserInfo;
import com.likelion.realtalk.domain.oauth.userinfo.OAuth2UserInfo;
import java.util.Map;

public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if ("kakao".equalsIgnoreCase(registrationId)) {
            return new KakaoOAuth2UserInfo(attributes);
        } else if ("google".equalsIgnoreCase(registrationId)) {
            return new GoogleOAuth2UserInfo(attributes);
        } else {
            throw new IllegalArgumentException("지원하지 않는 소셜 로그인 타입입니다: " + registrationId);
        }
    }
}
