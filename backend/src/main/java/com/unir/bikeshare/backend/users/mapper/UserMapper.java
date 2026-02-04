package com.unir.bikeshare.backend.users.mapper;

import com.unir.bikeshare.backend.users.dto.UserResponse;
import com.unir.bikeshare.backend.users.model.User;


public class UserMapper {
	
	private UserMapper() {}
	
	public static UserResponse toResponse(User user) {
		return new UserResponse(
				user.getId(),
				user.getUsername(),
				user.getEmail(),
				user.getRole(),
				user.getCreatedAt()
				);
	}

}
