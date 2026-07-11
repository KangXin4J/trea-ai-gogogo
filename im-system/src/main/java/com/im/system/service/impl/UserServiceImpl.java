package com.im.system.service.impl;

import com.im.system.common.AccountLockedException;
import com.im.system.common.JwtUtil;
import com.im.system.common.UserStatus;
import com.im.system.dto.LoginRequest;
import com.im.system.dto.PageResponse;
import com.im.system.dto.RegisterRequest;
import com.im.system.dto.UpdateUserRequest;
import com.im.system.entity.LoginAttempt;
import com.im.system.entity.User;
import com.im.system.repository.LoginAttemptRepository;
import com.im.system.repository.UserRepository;
import com.im.system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginAttemptRepository loginAttemptRepository;

    @Override
    @Transactional
    public String login(LoginRequest request) {
        String username = request.getUsername().trim();

        LocalDateTime lockTimeWindow = LocalDateTime.now().minusMinutes(LOCK_DURATION_MINUTES);
        long failedAttempts = loginAttemptRepository.countByUsernameAndAttemptTimeAfterAndSuccessFalse(username, lockTimeWindow);

        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            throw new AccountLockedException("账号已被锁定，请15分钟后再试");
        }

        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            recordLoginAttempt(null, username, false);
            throw new RuntimeException("用户名或密码错误");
        }

        User user = userOptional.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordLoginAttempt(user.getId(), username, false);
            throw new RuntimeException("用户名或密码错误");
        }

        loginAttemptRepository.deleteByUsernameAndSuccessFalse(username);
        recordLoginAttempt(user.getId(), username, true);

        user.setStatus("ONLINE");
        userRepository.save(user);

        return jwtUtil.generateToken(user.getId(), user.getUsername());
    }

    private void recordLoginAttempt(Long userId, String username, Boolean success) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setUserId(userId);
        attempt.setUsername(username);
        attempt.setSuccess(success);
        loginAttemptRepository.save(attempt);
        log.info("登录尝试记录: username={}, userId={}, success={}", username, userId, success);
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
    public PageResponse<User> searchUsers(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findByUsernameContainingIgnoreCase(keyword, pageable);
        return new PageResponse<>(userPage.getContent(), page, size, userPage.getTotalElements());
    }

    @Override
    @Transactional
    public User updateUserStatus(Long userId, String status) {
        if (status == null) {
            throw new RuntimeException("用户状态值不能为空");
        }
        try {
            UserStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("无效的用户状态值: " + status + "，有效值为 ONLINE、OFFLINE、BUSY、AWAY");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setStatus(status.toUpperCase());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getSignature() != null) {
            user.setSignature(request.getSignature());
        }
        return userRepository.save(user);
    }
}
