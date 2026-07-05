package com.im.system.service.impl;

import com.im.system.common.JwtUtil;
import com.im.system.dto.LoginRequest;
import com.im.system.dto.RegisterRequest;
import com.im.system.entity.User;
import com.im.system.repository.UserRepository;
import com.im.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public String login(LoginRequest request) {
        String username = request.getUsername().trim();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        user.setStatus("ONLINE");
        userRepository.save(user);

        return jwtUtil.generateToken(user.getId(), user.getUsername());
    }

    @Override
    @Transactional
    public User register(RegisterRequest request) {
        String username = request.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : username);
        user.setStatus("OFFLINE");

        return userRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public User updateUserStatus(Long userId, String status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setStatus(status);
        return userRepository.save(user);
    }
}
