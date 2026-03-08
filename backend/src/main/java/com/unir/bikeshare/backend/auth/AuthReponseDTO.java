package com.unir.bikeshare.backend.auth;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class AuthReponseDTO {
  private String token;
}
