package com.im.system.websocket;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class WebSocketHandshakeInterceptorTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    private String validToken;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("test123");
        registerRequest.setNickname("Test User");
        User testUser = userService.register(registerRequest);

        String jsonRequest = """
                {
                    "username": "testuser",
                    "password": "test123"
                }
                """;

        String response = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest)
                ).andReturn().getResponse().getContentAsString();
        validToken = response.split("\"token\":\"")[1].split("\"")[0];
    }

    @Test
    @DisplayName("WebSocket连接 - 不传token应该被拒绝")
    void handshakeWithoutToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/ws"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("WebSocket连接 - 传无效token应该被拒绝")
    void handshakeWithInvalidToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/ws?token=invalidtoken123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("WebSocket连接 - 传过期token应该被拒绝")
    void handshakeWithExpiredToken_shouldReturnUnauthorized() throws Exception {
        String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwidXNlcm5hbWUiOiJ0ZXN0dXNlciIsImlhdCI6MTUxNjIzOTAyMiwiZXhwIjoxNTE2MjM5MDIyfQ.invalid_signature";
        mockMvc.perform(get("/ws?token=" + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("WebSocket连接 - 传有效token不应被拦截器拒绝")
    void handshakeWithValidToken_shouldNotBeRejectedByInterceptor() throws Exception {
        int status = mockMvc.perform(get("/ws?token=" + validToken))
                .andReturn().getResponse().getStatus();
        assert status != 401 : "Valid token should not return 401";
    }
}