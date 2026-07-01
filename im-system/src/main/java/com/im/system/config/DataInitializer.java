package com.im.system.config;

import com.im.system.entity.User;
import com.im.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            createUser("admin", "123456", "管理员");
            createUser("user1", "123456", "用户1");
            createUser("user2", "123456", "用户2");
            createUser("user3", "123456", "用户3");
            log.info("测试用户初始化完成，默认密码: 123456");
        }
    }

    private void createUser(String username, String password, String nickname) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setStatus("OFFLINE");
        userRepository.save(user);
    }
}
