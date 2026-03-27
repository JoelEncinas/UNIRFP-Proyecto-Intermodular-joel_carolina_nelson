package com.unir.bikeshare.backend.security;

import com.unir.bikeshare.backend.users.dto.UserResponse;
import com.unir.bikeshare.backend.users.service.UserService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CurrentUserAccessService {

  private static final String DEFAULT_FORBIDDEN_MESSAGE = "You can only access your own resources";
  private final UserService userService;

  public boolean isAdmin(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
  }

  public UserResponse getCurrentUser(Authentication authentication) {
    return userService.getByUsername(authentication.getName());
  }

  public void ensureUserOwnsResource(Authentication authentication, Long ownerUserId) {
    ensureUserOwnsResource(authentication, ownerUserId, DEFAULT_FORBIDDEN_MESSAGE);
  }

  public void ensureUserOwnsResource(Authentication authentication, Long ownerUserId, String message) {
    if (isAdmin(authentication)) {
      return;
    }

    UserResponse currentUser = getCurrentUser(authentication);
    if (!Objects.equals(currentUser.id(), ownerUserId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }
  }
}
