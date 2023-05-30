package com.example.demo.services;

import com.example.demo.entities.User;
import com.example.demo.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    public String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    public boolean isPasswordMatch(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    public void changePassword(User user, String newPassword) {
        user.setPassword(this.hashPassword(newPassword));
        userRepository.save(user);
    }
}
