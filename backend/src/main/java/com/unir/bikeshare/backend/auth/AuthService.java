package com.unir.bikeshare.backend.auth;

import com.unir.bikeshare.backend.security.JwtService;
import com.unir.bikeshare.backend.users.model.User;
import com.unir.bikeshare.backend.users.model.UserRole;
import com.unir.bikeshare.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;

  public AuthReponseDTO register(RegisterRequestDTO request) {
    if (userRepository.findByUsername(request.getUsername()).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
    }

    if (userRepository.findByEmail(request.getEmail()).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
    }

    User user = new User();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setRole(UserRole.RIDER);

    User savedUser = userRepository.save(user);
    return new AuthReponseDTO(jwtService.generateToken(savedUser));
  }

  public AuthReponseDTO login(LoginRequestDTO request) {
    final Authentication authentication;

    try {
      authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(
              request.getUsername(),
              request.getPassword()));
    } catch (AuthenticationException ex) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    String username = authentication.getName();
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

    String token = jwtService.generateToken(user);
    return new AuthReponseDTO(token);
  }
}
