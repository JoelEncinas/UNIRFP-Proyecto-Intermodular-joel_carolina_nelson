package com.unir.bikeshare.backend.security;

import org.springframework.stereotype.Service;
import com.unir.bikeshare.backend.users.model.User;
import lombok.RequiredArgsConstructor;
import com.unir.bikeshare.backend.users.repository.UserRepository;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
@EnableWebSecurity
@Service
@RequiredArgsConstructor

public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

    return org.springframework.security.core.userdetails.User
        .withUsername(user.getUsername())
        .password(user.getPassword())
        .authorities("USER") // Aquí puedes asignar roles o permisos según tu modelo de usuario
        .build();
  }

}
