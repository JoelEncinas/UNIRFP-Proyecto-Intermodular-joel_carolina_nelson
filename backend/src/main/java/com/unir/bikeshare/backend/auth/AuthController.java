package com.unir.bikeshare.backend.auth;

import com.unir.bikeshare.backend.users.model.User;
import com.unir.bikeshare.backend.users.model.UserRole;
import com.unir.bikeshare.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import com.unir.bikeshare.backend.auth.AuthReponseDTO;
import com.unir.bikeshare.backend.security.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;

  @PostMapping("/register")
  public AuthReponseDTO register(@RequestBody RegisterRequestDTO request) {

    if (userRepository.findByUsername(request.getUsername()).isPresent()) {
      throw new IllegalStateException("Username already exists");
    }

    if (userRepository.findByEmail(request.getEmail()).isPresent()) {
      throw new IllegalStateException("Email already exists");
    }

    User user = new User();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setRole(UserRole.RIDER); // Asignar un rol predeterminado, por ejemplo, RIDER

    userRepository.save(user);
    return new AuthReponseDTO(jwtService.generateToken(user.getUsername()));
  }

  @PostMapping("/login")
  public AuthReponseDTO login(@RequestBody LoginRequestDTO request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

    String token = jwtService.generateToken(request.getUsername());
    return new AuthReponseDTO(token);
  }

}
