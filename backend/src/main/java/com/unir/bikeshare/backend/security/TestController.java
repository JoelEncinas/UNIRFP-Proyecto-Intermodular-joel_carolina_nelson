package com.unir.bikeshare.backend.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController

public class TestController {

  @GetMapping("/test")
  public String health() {
    return "BikeShare API is running 🚴‍♂️!";
  }

}
