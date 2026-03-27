package com.unir.bikeshare.backend.users.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unir.bikeshare.backend.common.exception.ConflictException;
import com.unir.bikeshare.backend.common.exception.NotFoundException;
import com.unir.bikeshare.backend.users.dto.UserResponse;
import com.unir.bikeshare.backend.users.dto.UserUpdateRequest;
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

    @Transactional(readOnly = true)
    public UserResponse getByUsername(String username) {
        return UserMapper.toResponse(getUserByUsername(username));
    }

    // Nuevo método para actualizar un usuario
    public UserResponse update(Long id, UserUpdateRequest req) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        applyUpdateRequest(user, req);

        User updated = userRepository.save(user);

        return UserMapper.toResponse(updated);
    }

    public UserResponse updateByUsername(String username, UserUpdateRequest req) {
        User user = getUserByUsername(username);

        applyUpdateRequest(user, req);

        User updated = userRepository.save(user);
        return UserMapper.toResponse(updated);
    }

    // Nuevo método para eliminar un usuario
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
        userRepository.delete(user);
    }

    public void deleteByUsername(String username) {
        User user = getUserByUsername(username);
        userRepository.delete(user);
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private void applyUpdateRequest(User user, UserUpdateRequest req) {
        if (req.username() != null) {
            validateUsernameAvailableForUpdate(user, req.username());
            user.setUsername(req.username());
        }

        if (req.email() != null) {
            validateEmailAvailableForUpdate(user, req.email());
            user.setEmail(req.email());
        }

        if (req.password() != null && !req.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(req.password()));
        }
    }

    private void validateUsernameAvailableForUpdate(User user, String newUsername) {
        if (newUsername.equals(user.getUsername())) {
            return;
        }

        if (userRepository.existsByUsername(newUsername)) {
            throw new ConflictException("Username already exists");
        }
    }

    private void validateEmailAvailableForUpdate(User user, String newEmail) {
        if (newEmail.equals(user.getEmail())) {
            return;
        }

        if (userRepository.existsByEmail(newEmail)) {
            throw new ConflictException("Email already exists");
        }
    }
}
