package com.im.system.repository;

import com.im.system.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    long countByUsernameAndAttemptTimeAfterAndSuccessFalse(String username, LocalDateTime attemptTime);

    void deleteByUsernameAndSuccessFalse(String username);
}