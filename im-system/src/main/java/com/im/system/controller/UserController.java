package com.im.system.controller;

import com.im.system.common.JwtUtil;
import com.im.system.common.Result;
import com.im.system.entity.User;
import com.im.system.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @GetMapping("/me")
    public Result<User> getCurrentUser(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        User user = userService.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        return Result.success(user);
    }

    @GetMapping
    public Result<List<User>> getAllUsers() {
        return Result.success(userService.findAll());
    }

    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userService.findById(id).orElseThrow(() -> new RuntimeException("用户不存在"));
        return Result.success(user);
    }

    @PutMapping("/status/{status}")
    public Result<User> updateStatus(@PathVariable String status, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        User user = userService.updateUserStatus(userId, status);
        return Result.success(user);
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.getUserIdFromToken(token);
        }
        throw new RuntimeException("未授权");
    }
}
