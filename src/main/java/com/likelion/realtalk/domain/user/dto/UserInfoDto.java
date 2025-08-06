package com.likelion.realtalk.domain.user.dto;

import com.likelion.realtalk.domain.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDto {

    private Long id;
    private String username;
    private Role role;

    // 인증 관련 추가 정보
    private String provider;           // OAuth 제공자 (google, kakao)

    // 테스트용: id, username, role, provider만 포함하는 간단한 생성자
    public static UserInfoDto withProvider(Long id, String username, Role role, String provider) {
        return new UserInfoDto(
            id,
            username,
            role,
            provider
        );
    }

}
