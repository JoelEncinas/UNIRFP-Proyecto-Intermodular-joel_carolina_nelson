package com.unir.bikeshare.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequestDTO {

  @NotBlank(message = "Username is required")
  @Size(max = 50, message = "Username must be 50 characters or fewer")
  private String username;

  @NotBlank(message = "Password is required")
  @Size(max = 255, message = "Password must be 255 characters or fewer")
  private String password;

}
