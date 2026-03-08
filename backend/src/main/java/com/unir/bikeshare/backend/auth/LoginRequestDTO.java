package com.unir.bikeshare.backend.auth;

import lombok.Data;

@Data
public class LoginRequestDTO {

  private String username;
  private String password;

}
