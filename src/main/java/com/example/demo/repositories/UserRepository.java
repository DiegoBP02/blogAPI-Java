package com.example.demo.repositories;

import com.example.demo.entities.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepository extends CrudRepository<User, UUID> {
    User findByUsername(String username);
    @Transactional
    @Query("UPDATE users SET failedAttempt = ?1 WHERE username = ?2")
    @Modifying
    void updateFailedAttempts(int failAttempts, String username);
}
