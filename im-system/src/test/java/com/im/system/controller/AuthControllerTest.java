package com.im.system.controller;

import com.im.system.common.JwtUtil;
import com.im.system.dto.LoginRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("test123");
        registerRequest.setNickname("Test User");
        testUser = userService.register(registerRequest);
    }

    @Test
    @DisplayName("POST /api/auth/register - 成功注册用户")
    void register_shouldReturnSuccess() throws Exception {
        String jsonRequest = """
                {
                    "username": "newuser",
                    "password": "new123",
                    "nickname": "New User"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.username").value("newuser"))
                .andExpect(jsonPath("$.data.nickname").value("New User"))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    @DisplayName("POST /api/auth/register - 用户名已存在")
    void register_shouldReturnErrorWhenUsernameExists() throws Exception {
        String jsonRequest = """
                {
                    "username": "testuser",
                    "password": "test123",
                    "nickname": "Duplicate"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/auth/register - 用户名不能为空")
    void register_shouldReturnErrorWhenUsernameBlank() throws Exception {
        String jsonRequest = """
                {
                    "username": "",
                    "password": "test123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /api/auth/register - 密码不能为空")
    void register_shouldReturnErrorWhenPasswordBlank() throws Exception {
        String jsonRequest = """
                {
                    "username": "newuser",
                    "password": ""
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /api/auth/login - 成功登录")
    void login_shouldReturnSuccess() throws Exception {
        String jsonRequest = """
                {
                    "username": "testuser",
                    "password": "test123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.user.username").value("testuser"))
                .andExpect(jsonPath("$.data.user.id").value(testUser.getId()));
    }

    @Test
    @DisplayName("POST /api/auth/login - 用户名不存在")
    void login_shouldReturnErrorWhenUserNotFound() throws Exception {
        String jsonRequest = """
                {
                    "username": "nonexistent",
                    "password": "test123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /api/auth/login - 密码错误")
    void login_shouldReturnErrorWhenPasswordWrong() throws Exception {
        String jsonRequest = """
                {
                    "username": "testuser",
                    "password": "wrongpassword"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /api/auth/login - 参数缺失")
    void login_shouldReturnErrorWhenParamsMissing() throws Exception {
        String jsonRequest = """
                {
                    "username": "testuser"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /api/auth/login - 用户名带空格应该被trim")
    void login_shouldTrimUsername() throws Exception {
        String jsonRequest = """
                {
                    "username": "  testuser  ",
                    "password": "test123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").exists());
    }

    @Test
    @DisplayName("POST /api/auth/logout - 登出后状态变为OFFLINE")
    void logout_shouldSetStatusToOffline() throws Exception {
        String token = jwtUtil.generateToken(testUser.getId(), testUser.getUsername());
        userService.updateUserStatus(testUser.getId(), "ONLINE");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assert updatedUser.getStatus().equals("OFFLINE");
    }
}