package com.im.system.service;

import com.im.system.dto.LoginRequest;
import com.im.system.dto.RegisterRequest;
import com.im.system.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    String login(LoginRequest request);

    User register(RegisterRequest request);

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    List<User> findAll();

    User updateUserStatus(Long userId, String status);
}
