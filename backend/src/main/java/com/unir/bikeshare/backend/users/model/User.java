package com.unir.bikeshare.backend.users.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name="username", nullable=false, unique=true, length=50)
	private String username;
	
	@Column(name="email", nullable=false, unique=true, length=100)
    private String email;
	
	@Column(name="password", nullable=false, length=255)
    private String password;
	
	@Enumerated(EnumType.STRING)
    @Column(name="role", nullable=false)
    private UserRole role = UserRole.RIDER;
	
	// TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @PrePersist
    private void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (role == null) role = UserRole.RIDER;
    }
    
 // ===== getters / setters =====

    public Long getId() { return id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public Instant getCreatedAt() { return createdAt; }
}
