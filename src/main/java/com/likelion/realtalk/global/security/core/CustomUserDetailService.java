package com.likelion.realtalk.global.security.core;

import com.likelion.realtalk.domain.user.entity.User;
import com.likelion.realtalk.domain.user.repository.UserRepository;
import com.likelion.realtalk.global.exception.UserNotFoundException;
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
    User user = userRepository.findByUsername(username).orElseThrow(UserNotFoundException::new);
    return new CustomUserDetails(user);
  }



  public UserDetails loadUserById(Long id) throws UsernameNotFoundException {
    User user = userRepository.findById(id).orElseThrow(UserNotFoundException::new);
    return new CustomUserDetails(user);
  }

}
