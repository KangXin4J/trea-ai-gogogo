package com.im.system.controller;

import com.im.system.common.Result;
import com.im.system.dto.LoginRequest;
import com.im.system.dto.RegisterRequest;
import com.im.system.entity.User;
import com.im.system.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

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
}
