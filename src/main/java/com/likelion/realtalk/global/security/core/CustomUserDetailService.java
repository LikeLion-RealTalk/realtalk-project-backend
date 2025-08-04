package com.likelion.realtalk.global.security.core;

import com.likelion.realtalk.domain.user.entity.User;
import com.likelion.realtalk.domain.user.repository.UserRepository;
import com.likelion.realtalk.global.exception.CustomException;
import com.likelion.realtalk.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {
  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = userRepository.findByUsername(username).orElseThrow(
        ()-> new CustomException(ErrorCode.USER_NOT_FOUND));
    return new CustomUserDetails(user);
  }



  public UserDetails loadUserById(Long id) throws UsernameNotFoundException {
    User user = userRepository.findById(id).orElseThrow(
        ()-> new CustomException(ErrorCode.USER_NOT_FOUND));
    return new CustomUserDetails(user);
  }

}
