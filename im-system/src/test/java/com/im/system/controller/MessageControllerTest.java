package com.im.system.controller;

import com.im.system.common.JwtUtil;
import com.im.system.dto.RegisterRequest;
import com.im.system.dto.SendMessageRequest;
import com.im.system.entity.Message;
import com.im.system.entity.User;
import com.im.system.repository.MessageRepository;
import com.im.system.repository.UserRepository;
import com.im.system.service.MessageService;
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
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private com.im.system.service.ConversationService conversationService;

    @Autowired
    private JwtUtil jwtUtil;

    private User sender;
    private User receiver;
    private String senderToken;
    private com.im.system.entity.Conversation conversation;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        messageRepository.deleteAll();

        RegisterRequest senderRequest = new RegisterRequest();
        senderRequest.setUsername("sender");
        senderRequest.setPassword("test123");
        sender = userService.register(senderRequest);

        RegisterRequest receiverRequest = new RegisterRequest();
        receiverRequest.setUsername("receiver");
        receiverRequest.setPassword("test123");
        receiver = userService.register(receiverRequest);

        senderToken = jwtUtil.generateToken(sender.getId(), sender.getUsername());

        com.im.system.dto.CreateConversationRequest createRequest = new com.im.system.dto.CreateConversationRequest();
        createRequest.setType("PRIVATE");
        createRequest.setMemberIds(java.util.Arrays.asList(receiver.getId()));
        conversation = conversationService.createConversation(sender.getId(), createRequest);
    }

    @Test
    @DisplayName("POST /api/messages/send - 成功发送消息")
    void sendMessage_shouldReturnSuccess() throws Exception {
        String jsonRequest = String.format("""
                {
                    "receiverId": %d,
                    "conversationId": %d,
                    "content": "Hello, World!"
                }
                """, receiver.getId(), conversation.getId());

        mockMvc.perform(post("/api/messages/send")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.content").value("Hello, World!"))
                .andExpect(jsonPath("$.data.senderId").value(sender.getId()))
                .andExpect(jsonPath("$.data.receiverId").value(receiver.getId()))
                .andExpect(jsonPath("$.data.isRead").value(false))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    @DisplayName("POST /api/messages/send - 未授权访问")
    void sendMessage_shouldReturnErrorWhenUnauthorized() throws Exception {
        String jsonRequest = String.format("""
                {
                    "receiverId": %d,
                    "conversationId": %d,
                    "content": "Hello"
                }
                """, receiver.getId(), conversation.getId());

        mockMvc.perform(post("/api/messages/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("POST /api/messages/send - 消息内容不能为空")
    void sendMessage_shouldReturnErrorWhenContentBlank() throws Exception {
        String jsonRequest = String.format("""
                {
                    "receiverId": %d,
                    "conversationId": %d,
                    "content": ""
                }
                """, receiver.getId(), conversation.getId());

        mockMvc.perform(post("/api/messages/send")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /api/messages/send - 接收者ID和会话ID都为空")
    void sendMessage_shouldReturnErrorWhenReceiverIdAndConversationIdNull() throws Exception {
        String jsonRequest = """
                {
                    "receiverId": null,
                    "content": "Hello"
                }
                """;

        mockMvc.perform(post("/api/messages/send")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /api/messages/send - 接收者不存在")
    void sendMessage_shouldReturnErrorWhenReceiverNotExists() throws Exception {
        String jsonRequest = """
                {
                    "receiverId": 99999,
                    "content": "Hello"
                }
                """;

        mockMvc.perform(post("/api/messages/send")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /api/messages/send - 会话不存在")
    void sendMessage_shouldReturnErrorWhenConversationNotExists() throws Exception {
        String jsonRequest = """
                {
                    "conversationId": 99999,
                    "content": "Hello"
                }
                """;

        mockMvc.perform(post("/api/messages/send")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("GET /api/messages/conversation/{userId} - 获取对话消息")
    void getConversationMessages_shouldReturnSuccess() throws Exception {
        SendMessageRequest sendRequest = new SendMessageRequest();
        sendRequest.setReceiverId(receiver.getId());
        sendRequest.setConversationId(conversation.getId());
        sendRequest.setContent("First message");
        messageService.sendMessage(sender.getId(), sendRequest);

        mockMvc.perform(get("/api/messages/conversation/{userId}", receiver.getId())
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].content").value("First message"));
    }

    @Test
    @DisplayName("GET /api/messages/conversation/{userId} - 未授权访问")
    void getConversationMessages_shouldReturnErrorWhenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/messages/conversation/{userId}", receiver.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("GET /api/messages/unread-count - 获取未读消息数")
    void getUnreadCount_shouldReturnSuccess() throws Exception {
        SendMessageRequest sendRequest = new SendMessageRequest();
        sendRequest.setReceiverId(receiver.getId());
        sendRequest.setConversationId(conversation.getId());
        sendRequest.setContent("Unread message");
        messageService.sendMessage(sender.getId(), sendRequest);

        String receiverToken = jwtUtil.generateToken(receiver.getId(), receiver.getUsername());

        mockMvc.perform(get("/api/messages/unread-count")
                        .header("Authorization", "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("GET /api/messages/unread-count - 无未读消息")
    void getUnreadCount_shouldReturnZeroWhenNoUnread() throws Exception {
        mockMvc.perform(get("/api/messages/unread-count")
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(0));
    }

    @Test
    @DisplayName("GET /api/messages/unread-count - 未授权访问")
    void getUnreadCount_shouldReturnErrorWhenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/messages/unread-count"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("POST /api/messages/mark-read/{conversationId} - 标记消息已读")
    void markAsRead_shouldReturnSuccess() throws Exception {
        com.im.system.dto.CreateConversationRequest createRequest = new com.im.system.dto.CreateConversationRequest();
        createRequest.setType("PRIVATE");
        createRequest.setMemberIds(java.util.Arrays.asList(receiver.getId()));
        com.im.system.entity.Conversation conversation = conversationService.createConversation(sender.getId(), createRequest);

        SendMessageRequest sendRequest = new SendMessageRequest();
        sendRequest.setReceiverId(receiver.getId());
        sendRequest.setContent("Test message");
        sendRequest.setConversationId(conversation.getId());
        messageService.sendMessage(sender.getId(), sendRequest);

        String receiverToken = jwtUtil.generateToken(receiver.getId(), receiver.getUsername());

        mockMvc.perform(post("/api/messages/mark-read/{conversationId}", conversation.getId())
                        .header("Authorization", "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));
    }

    @Test
    @DisplayName("POST /api/messages/mark-read/{conversationId} - 未授权访问")
    void markAsRead_shouldReturnErrorWhenUnauthorized() throws Exception {
        mockMvc.perform(post("/api/messages/mark-read/{conversationId}", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("GET /api/messages/conversation/{conversationId}/paged - 分页获取消息")
    void getMessagesByConversationIdPaged_shouldReturnSuccess() throws Exception {
        SendMessageRequest sendRequest1 = new SendMessageRequest();
        sendRequest1.setReceiverId(receiver.getId());
        sendRequest1.setConversationId(conversation.getId());
        sendRequest1.setContent("First message");
        messageService.sendMessage(sender.getId(), sendRequest1);

        SendMessageRequest sendRequest2 = new SendMessageRequest();
        sendRequest2.setReceiverId(receiver.getId());
        sendRequest2.setConversationId(conversation.getId());
        sendRequest2.setContent("Second message");
        messageService.sendMessage(sender.getId(), sendRequest2);

        mockMvc.perform(get("/api/messages/conversation/{conversationId}/paged", conversation.getId())
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("GET /api/messages/conversation/{conversationId}/paged - 无权访问会话")
    void getMessagesByConversationIdPaged_shouldReturnErrorWhenNotMember() throws Exception {
        RegisterRequest user3Request = new RegisterRequest();
        user3Request.setUsername("user3");
        user3Request.setPassword("test123");
        User user3 = userService.register(user3Request);
        String user3Token = jwtUtil.generateToken(user3.getId(), user3.getUsername());

        mockMvc.perform(get("/api/messages/conversation/{conversationId}/paged", conversation.getId())
                        .header("Authorization", "Bearer " + user3Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("GET /api/messages/conversation/{conversationId}/paged - 会话不存在")
    void getMessagesByConversationIdPaged_shouldReturnErrorWhenConversationNotFound() throws Exception {
        mockMvc.perform(get("/api/messages/conversation/{conversationId}/paged", 99999L)
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("GET /api/messages/conversation/{conversationId}/search - 搜索消息")
    void searchMessages_shouldReturnSuccess() throws Exception {
        SendMessageRequest sendRequest1 = new SendMessageRequest();
        sendRequest1.setReceiverId(receiver.getId());
        sendRequest1.setConversationId(conversation.getId());
        sendRequest1.setContent("Hello World");
        messageService.sendMessage(sender.getId(), sendRequest1);

        SendMessageRequest sendRequest2 = new SendMessageRequest();
        sendRequest2.setReceiverId(receiver.getId());
        sendRequest2.setConversationId(conversation.getId());
        sendRequest2.setContent("Goodbye World");
        messageService.sendMessage(sender.getId(), sendRequest2);

        mockMvc.perform(get("/api/messages/conversation/{conversationId}/search", conversation.getId())
                        .param("keyword", "World")
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/messages/conversation/{conversationId}/search - 搜索无匹配")
    void searchMessages_shouldReturnEmptyWhenNoMatch() throws Exception {
        SendMessageRequest sendRequest = new SendMessageRequest();
        sendRequest.setReceiverId(receiver.getId());
        sendRequest.setConversationId(conversation.getId());
        sendRequest.setContent("Hello World");
        messageService.sendMessage(sender.getId(), sendRequest);

        mockMvc.perform(get("/api/messages/conversation/{conversationId}/search", conversation.getId())
                        .param("keyword", "nonexistent")
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }
}