package com.unir.bikeshare.backend.auth;

import lombok.Data;

@Data
public class RegisterRequestDTO {
  private String username;
  private String email;
  private String password;

}
