package com.im.system.controller;

import com.im.system.common.JwtUtil;
import com.im.system.common.Result;
import com.im.system.dto.LoginRequest;
import com.im.system.dto.RegisterRequest;
import com.im.system.entity.BlacklistedToken;
import com.im.system.entity.User;
import com.im.system.repository.BlacklistedTokenRepository;
import com.im.system.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        String token = userService.login(request);
        String username = request.getUsername().trim();
        User user = userService.findByUsername(username).orElseThrow();

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);

        return Result.success(data);
    }

    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return Result.success(user);
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith("Bearer ")) {
            return Result.error(401, "无效的登录凭证");
        }

        String token = bearerToken.substring(7);

        if (!jwtUtil.validateToken(token)) {
            return Result.error(401, "无效的登录凭证");
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        Date expiresAt = jwtUtil.getExpirationDateFromToken(token);

        userService.updateUserStatus(userId, "OFFLINE");

        BlacklistedToken blacklistedToken = new BlacklistedToken();
        blacklistedToken.setToken(token);
        blacklistedToken.setUserId(userId);
        blacklistedToken.setExpiresAt(expiresAt);

        blacklistedTokenRepository.save(blacklistedToken);

        return Result.success(null);
    }
}