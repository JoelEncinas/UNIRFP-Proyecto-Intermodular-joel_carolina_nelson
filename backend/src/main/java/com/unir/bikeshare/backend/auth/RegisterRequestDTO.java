package com.unir.bikeshare.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequestDTO {
  @NotBlank(message = "Username is required")
  @Size(max = 50, message = "Username must be 50 characters or fewer")
  private String username;

  @NotBlank(message = "Email is required")
  @Email(message = "Email format is invalid")
  @Size(max = 100, message = "Email must be 100 characters or fewer")
  private String email;

  @NotBlank(message = "Password is required")
  @Size(min = 6, max = 255, message = "Password must be between 6 and 255 characters")
  private String password;

}
