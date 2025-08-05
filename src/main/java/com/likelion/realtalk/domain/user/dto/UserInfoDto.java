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
    private String roleName;

    public static UserInfoDto from(Long id, String username, Role role) {
        return new UserInfoDto(id, username, role, role.getValue());
    }
}
