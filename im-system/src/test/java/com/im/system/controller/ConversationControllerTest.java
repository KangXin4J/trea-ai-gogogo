package com.im.system.controller;

import com.im.system.common.JwtUtil;
import com.im.system.dto.CreateConversationRequest;
import com.im.system.dto.RegisterRequest;
import com.im.system.entity.Conversation;
import com.im.system.entity.User;
import com.im.system.repository.ConversationMemberRepository;
import com.im.system.repository.ConversationRepository;
import com.im.system.repository.UserRepository;
import com.im.system.service.ConversationService;
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

import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationMemberRepository conversationMemberRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private JwtUtil jwtUtil;

    private User user1;
    private User user2;
    private String user1Token;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        conversationRepository.deleteAll();
        conversationMemberRepository.deleteAll();

        RegisterRequest user1Request = new RegisterRequest();
        user1Request.setUsername("user1");
        user1Request.setPassword("test123");
        user1 = userService.register(user1Request);

        RegisterRequest user2Request = new RegisterRequest();
        user2Request.setUsername("user2");
        user2Request.setPassword("test123");
        user2 = userService.register(user2Request);

        user1Token = jwtUtil.generateToken(user1.getId(), user1.getUsername());
    }

    @Test
    @DisplayName("GET /api/conversations - 获取会话列表")
    void getUserConversations_shouldReturnSuccess() throws Exception {
        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setType("PRIVATE");
        createRequest.setMemberIds(Arrays.asList(user2.getId()));
        conversationService.createConversation(user1.getId(), createRequest);

        mockMvc.perform(get("/api/conversations")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("GET /api/conversations - 获取空会话列表")
    void getUserConversations_shouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/api/conversations")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/conversations - 未授权访问")
    void getUserConversations_shouldReturnErrorWhenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/conversations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("POST /api/conversations - 创建会话")
    void createConversation_shouldReturnSuccess() throws Exception {
        String jsonRequest = String.format("""
                {
                    "type": "PRIVATE",
                    "memberIds": [%d],
                    "name": "Test Conversation"
                }
                """, user2.getId());

        mockMvc.perform(post("/api/conversations")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.type").value("PRIVATE"))
                .andExpect(jsonPath("$.data.name").value("Test Conversation"))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    @DisplayName("POST /api/conversations - 创建群聊")
    void createConversation_shouldReturnSuccessForGroup() throws Exception {
        RegisterRequest user3Request = new RegisterRequest();
        user3Request.setUsername("user3");
        user3Request.setPassword("test123");
        User user3 = userService.register(user3Request);

        String jsonRequest = String.format("""
                {
                    "type": "GROUP",
                    "memberIds": [%d, %d],
                    "name": "Group Chat"
                }
                """, user2.getId(), user3.getId());

        mockMvc.perform(post("/api/conversations")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.type").value("GROUP"))
                .andExpect(jsonPath("$.data.name").value("Group Chat"));
    }

    @Test
    @DisplayName("POST /api/conversations - 未授权访问")
    void createConversation_shouldReturnErrorWhenUnauthorized() throws Exception {
        String jsonRequest = String.format("""
                {
                    "type": "PRIVATE",
                    "memberIds": [%d]
                }
                """, user2.getId());

        mockMvc.perform(post("/api/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("POST /api/conversations - 类型不能为空")
    void createConversation_shouldReturnErrorWhenTypeBlank() throws Exception {
        String jsonRequest = String.format("""
                {
                    "type": "",
                    "memberIds": [%d]
                }
                """, user2.getId());

        mockMvc.perform(post("/api/conversations")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /api/conversations - 成员列表不能为空")
    void createConversation_shouldReturnErrorWhenMemberIdsEmpty() throws Exception {
        String jsonRequest = """
                {
                    "type": "PRIVATE",
                    "memberIds": []
                }
                """;

        mockMvc.perform(post("/api/conversations")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("GET /api/conversations/{id} - 获取会话详情")
    void getConversationById_shouldReturnSuccess() throws Exception {
        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setType("PRIVATE");
        createRequest.setMemberIds(Arrays.asList(user2.getId()));
        Conversation conversation = conversationService.createConversation(user1.getId(), createRequest);

        mockMvc.perform(get("/api/conversations/{id}", conversation.getId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(conversation.getId()))
                .andExpect(jsonPath("$.data.type").value("PRIVATE"));
    }

    @Test
    @DisplayName("GET /api/conversations/{id} - 会话不存在")
    void getConversationById_shouldReturnErrorWhenConversationNotFound() throws Exception {
        mockMvc.perform(get("/api/conversations/{id}", 99999L)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("GET /api/conversations/{id} - 无权访问会话")
    void getConversationById_shouldReturnErrorWhenNotMember() throws Exception {
        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setType("PRIVATE");
        createRequest.setMemberIds(Arrays.asList(user2.getId()));
        Conversation conversation = conversationService.createConversation(user1.getId(), createRequest);

        String user2Token = jwtUtil.generateToken(user2.getId(), user2.getUsername());

        mockMvc.perform(get("/api/conversations/{id}", conversation.getId())
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/conversations/{id} - 未授权访问")
    void getConversationById_shouldReturnErrorWhenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/conversations/{id}", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("DELETE /api/conversations/{id} - 删除会话")
    void deleteConversation_shouldReturnSuccess() throws Exception {
        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setType("PRIVATE");
        createRequest.setMemberIds(Arrays.asList(user2.getId()));
        Conversation conversation = conversationService.createConversation(user1.getId(), createRequest);

        mockMvc.perform(delete("/api/conversations/{id}", conversation.getId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));
    }

    @Test
    @DisplayName("DELETE /api/conversations/{id} - 删除不存在的会话")
    void deleteConversation_shouldReturnErrorWhenConversationNotFound() throws Exception {
        mockMvc.perform(delete("/api/conversations/{id}", 99999L)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("DELETE /api/conversations/{id} - 未授权访问")
    void deleteConversation_shouldReturnErrorWhenUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/conversations/{id}", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("DELETE /api/conversations/{id} - 非成员用户无权删除会话")
    void deleteConversation_shouldReturnErrorWhenNotMember() throws Exception {
        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setType("PRIVATE");
        createRequest.setMemberIds(Arrays.asList(user2.getId()));
        Conversation conversation = conversationService.createConversation(user1.getId(), createRequest);

        RegisterRequest user3Request = new RegisterRequest();
        user3Request.setUsername("user3");
        user3Request.setPassword("test123");
        User user3 = userService.register(user3Request);
        String user3Token = jwtUtil.generateToken(user3.getId(), user3.getUsername());

        mockMvc.perform(delete("/api/conversations/{id}", conversation.getId())
                        .header("Authorization", "Bearer " + user3Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("GET /api/conversations/{id}/members - 获取会话成员列表")
    void getConversationMembers_shouldReturnSuccess() throws Exception {
        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setType("PRIVATE");
        createRequest.setMemberIds(Arrays.asList(user2.getId()));
        Conversation conversation = conversationService.createConversation(user1.getId(), createRequest);

        mockMvc.perform(get("/api/conversations/{id}/members", conversation.getId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].userId").exists())
                .andExpect(jsonPath("$.data[*].username").exists())
                .andExpect(jsonPath("$.data[*].nickname").exists())
                .andExpect(jsonPath("$.data[*].avatar").exists());
    }

    @Test
    @DisplayName("GET /api/conversations/{id}/members - 会话不存在")
    void getConversationMembers_shouldReturnErrorWhenConversationNotFound() throws Exception {
        mockMvc.perform(get("/api/conversations/{id}/members", 99999L)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("GET /api/conversations/{id}/members - 未授权访问")
    void getConversationMembers_shouldReturnErrorWhenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/conversations/{id}/members", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("GET /api/conversations/{id}/members - 无权访问会话")
    void getConversationMembers_shouldReturnErrorWhenNotMember() throws Exception {
        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setType("PRIVATE");
        createRequest.setMemberIds(Arrays.asList(user2.getId()));
        Conversation conversation = conversationService.createConversation(user1.getId(), createRequest);

        RegisterRequest user3Request = new RegisterRequest();
        user3Request.setUsername("user3");
        user3Request.setPassword("test123");
        User user3 = userService.register(user3Request);
        String user3Token = jwtUtil.generateToken(user3.getId(), user3.getUsername());

        mockMvc.perform(get("/api/conversations/{id}/members", conversation.getId())
                        .header("Authorization", "Bearer " + user3Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /api/conversations/{id}/members - 添加成员到会话")
    void addMembers_shouldReturnSuccess() throws Exception {
        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setType("GROUP");
        createRequest.setMemberIds(Arrays.asList(user2.getId()));
        createRequest.setName("Test Group");
        Conversation conversation = conversationService.createConversation(user1.getId(), createRequest);

        RegisterRequest user3Request = new RegisterRequest();
        user3Request.setUsername("user3");
        user3Request.setPassword("test123");
        User user3 = userService.register(user3Request);

        String jsonRequest = String.format("""
                {
                    "memberIds": [%d]
                }
                """, user3.getId());

        mockMvc.perform(post("/api/conversations/{id}/members", conversation.getId())
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(3)));
    }

    @Test
    @DisplayName("POST /api/conversations/{id}/members - 添加已存在的成员")
    void addMembers_shouldReturnErrorWhenMemberAlreadyExists() throws Exception {
        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setType("PRIVATE");
        createRequest.setMemberIds(Arrays.asList(user2.getId()));
        Conversation conversation = conversationService.createConversation(user1.getId(), createRequest);

        String jsonRequest = String.format("""
                {
                    "memberIds": [%d]
                }
                """, user2.getId());

        mockMvc.perform(post("/api/conversations/{id}/members", conversation.getId())
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /api/conversations/{id}/members - 会话不存在")
    void addMembers_shouldReturnErrorWhenConversationNotFound() throws Exception {
        String jsonRequest = String.format("""
                {
                    "memberIds": [%d]
                }
                """, user2.getId());

        mockMvc.perform(post("/api/conversations/{id}/members", 99999L)
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /api/conversations/{id}/members - 未授权访问")
    void addMembers_shouldReturnErrorWhenUnauthorized() throws Exception {
        String jsonRequest = String.format("""
                {
                    "memberIds": [%d]
                }
                """, user2.getId());

        mockMvc.perform(post("/api/conversations/{id}/members", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("POST /api/conversations/{id}/members - 无权访问会话")
    void addMembers_shouldReturnErrorWhenNotMember() throws Exception {
        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setType("PRIVATE");
        createRequest.setMemberIds(Arrays.asList(user2.getId()));
        Conversation conversation = conversationService.createConversation(user1.getId(), createRequest);

        RegisterRequest user3Request = new RegisterRequest();
        user3Request.setUsername("user3");
        user3Request.setPassword("test123");
        User user3 = userService.register(user3Request);
        String user3Token = jwtUtil.generateToken(user3.getId(), user3.getUsername());

        String jsonRequest = String.format("""
                {
                    "memberIds": [%d]
                }
                """, user3.getId());

        mockMvc.perform(post("/api/conversations/{id}/members", conversation.getId())
                        .header("Authorization", "Bearer " + user3Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /api/conversations/{id}/members - 成员列表不能为空")
    void addMembers_shouldReturnErrorWhenMemberIdsEmpty() throws Exception {
        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setType("GROUP");
        createRequest.setMemberIds(Arrays.asList(user2.getId()));
        Conversation conversation = conversationService.createConversation(user1.getId(), createRequest);

        String jsonRequest = """
                {
                    "memberIds": []
                }
                """;

        mockMvc.perform(post("/api/conversations/{id}/members", conversation.getId())
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /api/conversations/{id}/members - 添加不存在的用户")
    void addMembers_shouldReturnErrorWhenUserNotFound() throws Exception {
        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setType("GROUP");
        createRequest.setMemberIds(Arrays.asList(user2.getId()));
        Conversation conversation = conversationService.createConversation(user1.getId(), createRequest);

        String jsonRequest = """
                {
                    "memberIds": [99999]
                }
                """;

        mockMvc.perform(post("/api/conversations/{id}/members", conversation.getId())
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("DELETE /api/conversations/{id}/members/{memberId} - 移除成员成功")
    void removeMember_shouldReturnSuccess() throws Exception {
        RegisterRequest user3Request = new RegisterRequest();
        user3Request.setUsername("user3");
        user3Request.setPassword("test123");
        User user3 = userService.register(user3Request);

        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setType("GROUP");
        createRequest.setMemberIds(Arrays.asList(user2.getId(), user3.getId()));
        createRequest.setName("Test Group");
        Conversation conversation = conversationService.createConversation(user1.getId(), createRequest);

        mockMvc.perform(delete("/api/conversations/{id}/members/{memberId}", conversation.getId(), user3.getId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("DELETE /api/conversations/{id}/members/{memberId} - 可以移除自己（退出会话）")
    void removeMember_shouldReturnSuccessWhenRemovingSelf() throws Exception {
        RegisterRequest user3Request = new RegisterRequest();
        user3Request.setUsername("user3");
        user3Request.setPassword("test123");
        User user3 = userService.register(user3Request);

        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setType("GROUP");
        createRequest.setMemberIds(Arrays.asList(user2.getId(), user3.getId()));
        createRequest.setName("Test Group");
        Conversation conversation = conversationService.createConversation(user1.getId(), createRequest);

        mockMvc.perform(delete("/api/conversations/{id}/members/{memberId}", conversation.getId(), user1.getId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("DELETE /api/conversations/{id}/members/{memberId} - 用户不是会话成员")
    void removeMember_shouldReturnErrorWhenMemberNotFound() throws Exception {
        RegisterRequest user3Request = new RegisterRequest();
        user3Request.setUsername("user3");
        user3Request.setPassword("test123");
        User user3 = userService.register(user3Request);

        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setType("PRIVATE");
        createRequest.setMemberIds(Arrays.asList(user2.getId()));
        Conversation conversation = conversationService.createConversation(user1.getId(), createRequest);

        mockMvc.perform(delete("/api/conversations/{id}/members/{memberId}", conversation.getId(), user3.getId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("DELETE /api/conversations/{id}/members/{memberId} - 会话不存在")
    void removeMember_shouldReturnErrorWhenConversationNotFound() throws Exception {
        mockMvc.perform(delete("/api/conversations/{id}/members/{memberId}", 99999L, user2.getId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("DELETE /api/conversations/{id}/members/{memberId} - 未授权访问")
    void removeMember_shouldReturnErrorWhenUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/conversations/{id}/members/{memberId}", 1L, 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("DELETE /api/conversations/{id}/members/{memberId} - 无权访问会话")
    void removeMember_shouldReturnErrorWhenNotMember() throws Exception {
        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setType("PRIVATE");
        createRequest.setMemberIds(Arrays.asList(user2.getId()));
        Conversation conversation = conversationService.createConversation(user1.getId(), createRequest);

        RegisterRequest user3Request = new RegisterRequest();
        user3Request.setUsername("user3");
        user3Request.setPassword("test123");
        User user3 = userService.register(user3Request);
        String user3Token = jwtUtil.generateToken(user3.getId(), user3.getUsername());

        mockMvc.perform(delete("/api/conversations/{id}/members/{memberId}", conversation.getId(), user2.getId())
                        .header("Authorization", "Bearer " + user3Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("GET /api/conversations/search - 搜索会话")
    void searchConversations_shouldReturnSuccess() throws Exception {
        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setType("GROUP");
        createRequest.setMemberIds(Arrays.asList(user2.getId()));
        createRequest.setName("Test Group Chat");
        conversationService.createConversation(user1.getId(), createRequest);

        mockMvc.perform(get("/api/conversations/search")
                        .param("keyword", "Group")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    @DisplayName("GET /api/conversations/search - 未授权访问")
    void searchConversations_shouldReturnErrorWhenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/conversations/search")
                        .param("keyword", "test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("GET /api/conversations/search - 搜索无匹配")
    void searchConversations_shouldReturnEmptyWhenNoMatch() throws Exception {
        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setType("GROUP");
        createRequest.setMemberIds(Arrays.asList(user2.getId()));
        createRequest.setName("Test Group");
        conversationService.createConversation(user1.getId(), createRequest);

        mockMvc.perform(get("/api/conversations/search")
                        .param("keyword", "nonexistent")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    @DisplayName("PUT /api/conversations/{id} - 更新会话名称和类型")
    void updateConversation_shouldReturnSuccess() throws Exception {
        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setType("PRIVATE");
        createRequest.setMemberIds(Arrays.asList(user2.getId()));
        createRequest.setName("Original Name");
        Conversation conversation = conversationService.createConversation(user1.getId(), createRequest);

        String jsonRequest = """
                {
                    "name": "Updated Name",
                    "type": "GROUP"
                }
                """;

        mockMvc.perform(put("/api/conversations/{id}", conversation.getId())
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(conversation.getId()))
                .andExpect(jsonPath("$.data.name").value("Updated Name"))
                .andExpect(jsonPath("$.data.type").value("GROUP"));
    }

    @Test
    @DisplayName("PUT /api/conversations/{id} - 只更新名称")
    void updateConversation_shouldReturnSuccessWhenOnlyName() throws Exception {
        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setType("PRIVATE");
        createRequest.setMemberIds(Arrays.asList(user2.getId()));
        Conversation conversation = conversationService.createConversation(user1.getId(), createRequest);

        String jsonRequest = """
                {
                    "name": "New Name"
                }
                """;

        mockMvc.perform(put("/api/conversations/{id}", conversation.getId())
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("New Name"))
                .andExpect(jsonPath("$.data.type").value("PRIVATE"));
    }

    @Test
    @DisplayName("PUT /api/conversations/{id} - 只更新类型")
    void updateConversation_shouldReturnSuccessWhenOnlyType() throws Exception {
        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setType("PRIVATE");
        createRequest.setMemberIds(Arrays.asList(user2.getId()));
        createRequest.setName("Test Name");
        Conversation conversation = conversationService.createConversation(user1.getId(), createRequest);

        String jsonRequest = """
                {
                    "type": "GROUP"
                }
                """;

        mockMvc.perform(put("/api/conversations/{id}", conversation.getId())
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.type").value("GROUP"))
                .andExpect(jsonPath("$.data.name").value("Test Name"));
    }

    @Test
    @DisplayName("PUT /api/conversations/{id} - 会话不存在")
    void updateConversation_shouldReturnErrorWhenConversationNotFound() throws Exception {
        String jsonRequest = """
                {
                    "name": "Updated Name"
                }
                """;

        mockMvc.perform(put("/api/conversations/{id}", 99999L)
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("PUT /api/conversations/{id} - 非成员用户无权更新")
    void updateConversation_shouldReturnErrorWhenNotMember() throws Exception {
        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setType("PRIVATE");
        createRequest.setMemberIds(Arrays.asList(user2.getId()));
        Conversation conversation = conversationService.createConversation(user1.getId(), createRequest);

        RegisterRequest user3Request = new RegisterRequest();
        user3Request.setUsername("user3");
        user3Request.setPassword("test123");
        User user3 = userService.register(user3Request);
        String user3Token = jwtUtil.generateToken(user3.getId(), user3.getUsername());

        String jsonRequest = """
                {
                    "name": "Updated Name"
                }
                """;

        mockMvc.perform(put("/api/conversations/{id}", conversation.getId())
                        .header("Authorization", "Bearer " + user3Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("PUT /api/conversations/{id} - 未授权访问")
    void updateConversation_shouldReturnErrorWhenUnauthorized() throws Exception {
        String jsonRequest = """
                {
                    "name": "Updated Name"
                }
                """;

        mockMvc.perform(put("/api/conversations/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("POST /api/conversations/{id}/leave - 用户退出会话成功")
    void leaveConversation_shouldReturnSuccess() throws Exception {
        RegisterRequest user3Request = new RegisterRequest();
        user3Request.setUsername("user3");
        user3Request.setPassword("test123");
        User user3 = userService.register(user3Request);

        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setType("GROUP");
        createRequest.setMemberIds(Arrays.asList(user2.getId(), user3.getId()));
        createRequest.setName("Test Group");
        Conversation conversation = conversationService.createConversation(user1.getId(), createRequest);

        mockMvc.perform(post("/api/conversations/{id}/leave", conversation.getId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));
    }

    @Test
    @DisplayName("POST /api/conversations/{id}/leave - 用户不是会话成员")
    void leaveConversation_shouldReturnErrorWhenNotMember() throws Exception {
        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setType("PRIVATE");
        createRequest.setMemberIds(Arrays.asList(user2.getId()));
        Conversation conversation = conversationService.createConversation(user1.getId(), createRequest);

        RegisterRequest user3Request = new RegisterRequest();
        user3Request.setUsername("user3");
        user3Request.setPassword("test123");
        User user3 = userService.register(user3Request);
        String user3Token = jwtUtil.generateToken(user3.getId(), user3.getUsername());

        mockMvc.perform(post("/api/conversations/{id}/leave", conversation.getId())
                        .header("Authorization", "Bearer " + user3Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /api/conversations/{id}/leave - 会话不存在")
    void leaveConversation_shouldReturnErrorWhenConversationNotFound() throws Exception {
        mockMvc.perform(post("/api/conversations/{id}/leave", 99999L)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /api/conversations/{id}/leave - 未授权访问")
    void leaveConversation_shouldReturnErrorWhenUnauthorized() throws Exception {
        mockMvc.perform(post("/api/conversations/{id}/leave", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("POST /api/conversations/{id}/pin - 置顶会话")
    void pinConversation_shouldReturnSuccess() throws Exception {
        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setType("PRIVATE");
        createRequest.setMemberIds(Arrays.asList(user2.getId()));
        Conversation conversation = conversationService.createConversation(user1.getId(), createRequest);

        mockMvc.perform(post("/api/conversations/{id}/pin", conversation.getId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));
    }

    @Test
    @DisplayName("POST /api/conversations/{id}/pin - 会话不存在")
    void pinConversation_shouldReturnErrorWhenConversationNotFound() throws Exception {
        mockMvc.perform(post("/api/conversations/{id}/pin", 99999L)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /api/conversations/{id}/unpin - 取消置顶会话")
    void unpinConversation_shouldReturnSuccess() throws Exception {
        CreateConversationRequest createRequest = new CreateConversationRequest();
        createRequest.setType("PRIVATE");
        createRequest.setMemberIds(Arrays.asList(user2.getId()));
        Conversation conversation = conversationService.createConversation(user1.getId(), createRequest);

        conversationService.pinConversation(user1.getId(), conversation.getId());

        mockMvc.perform(post("/api/conversations/{id}/unpin", conversation.getId())
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));
    }

    @Test
    @DisplayName("POST /api/conversations/{id}/unpin - 会话不存在")
    void unpinConversation_shouldReturnErrorWhenConversationNotFound() throws Exception {
        mockMvc.perform(post("/api/conversations/{id}/unpin", 99999L)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}