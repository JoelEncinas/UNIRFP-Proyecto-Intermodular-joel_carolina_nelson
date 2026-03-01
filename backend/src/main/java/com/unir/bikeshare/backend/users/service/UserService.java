package com.unir.bikeshare.backend.users.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unir.bikeshare.backend.common.exception.BusinessException;
import com.unir.bikeshare.backend.common.exception.NotFoundException;
import com.unir.bikeshare.backend.users.dto.LoginRequest;
import com.unir.bikeshare.backend.users.dto.UserRegisterRequest;
import com.unir.bikeshare.backend.users.dto.UserResponse;
import com.unir.bikeshare.backend.users.mapper.UserMapper;
import com.unir.bikeshare.backend.users.model.User;
import com.unir.bikeshare.backend.users.repository.UserRepository;


@Service
@Transactional
public class UserService {
	private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAll()
                .stream()
                .map(UserMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return UserMapper.toResponse(user);
    }

    public UserResponse register(UserRegisterRequest req) {

        if (userRepository.existsByUsername(req.username())) {
            throw new BusinessException("Username already exists");
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new BusinessException("Email already exists");
        }

        User user = new User();
        user.setUsername(req.username());
        user.setEmail(req.email());

        user.setPassword(passwordEncoder.encode(req.password()));

        User saved = userRepository.save(user);
        return UserMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest req) {

        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new BusinessException("Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new BusinessException("Invalid credentials");
        }

        // De momento devolvemos UserResponse (sin JWT).
        return UserMapper.toResponse(user);
    }
}
