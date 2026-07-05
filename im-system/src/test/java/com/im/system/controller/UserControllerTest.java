package com.im.system.controller;

import com.im.system.common.JwtUtil;
import com.im.system.dto.RegisterRequest;
import com.im.system.entity.User;
import com.im.system.repository.UserRepository;
import com.im.system.service.UserService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    private User testUser;
    private String token;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("test123");
        registerRequest.setNickname("Test User");
        testUser = userService.register(registerRequest);
        token = jwtUtil.generateToken(testUser.getId(), testUser.getUsername());
    }

    @Test
    @DisplayName("GET /api/users/me - 获取当前用户信息")
    void getCurrentUser_shouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.id").value(testUser.getId()));
    }

    @Test
    @DisplayName("GET /api/users/me - 未授权访问")
    void getCurrentUser_shouldReturnErrorWhenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("GET /api/users/me - 无效token")
    void getCurrentUser_shouldReturnErrorWhenInvalidToken() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer invalidtoken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("GET /api/users - 获取所有用户列表")
    void getAllUsers_shouldReturnSuccess() throws Exception {
        RegisterRequest registerRequest2 = new RegisterRequest();
        registerRequest2.setUsername("testuser2");
        registerRequest2.setPassword("test123");
        userService.register(registerRequest2);

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("GET /api/users - 未授权访问")
    void getAllUsers_shouldReturnErrorWhenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("GET /api/users/{id} - 获取用户详情")
    void getUserById_shouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/users/{id}", testUser.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.id").value(testUser.getId()));
    }

    @Test
    @DisplayName("GET /api/users/{id} - 用户不存在")
    void getUserById_shouldReturnErrorWhenUserNotFound() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 99999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("GET /api/users/{id} - 未授权访问")
    void getUserById_shouldReturnErrorWhenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/{id}", testUser.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("PUT /api/users/status/{status} - 更新用户状态")
    void updateStatus_shouldReturnSuccess() throws Exception {
        mockMvc.perform(put("/api/users/status/{status}", "online")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.status").value("online"))
                .andExpect(jsonPath("$.data.id").value(testUser.getId()));
    }

    @Test
    @DisplayName("PUT /api/users/status/{status} - 更新为离线状态")
    void updateStatus_shouldReturnSuccessForOffline() throws Exception {
        mockMvc.perform(put("/api/users/status/{status}", "offline")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("offline"));
    }

    @Test
    @DisplayName("PUT /api/users/status/{status} - 未授权访问")
    void updateStatus_shouldReturnErrorWhenUnauthorized() throws Exception {
        mockMvc.perform(put("/api/users/status/{status}", "online"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}