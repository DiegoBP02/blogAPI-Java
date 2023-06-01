package com.example.demo.services;

import com.example.demo.dtos.ChangePasswordDTO;
import com.example.demo.dtos.LoginDTO;
import com.example.demo.dtos.RegisterDTO;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.Role;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.exceptions.DuplicateKeyException;
import com.example.demo.services.exceptions.InvalidOldPasswordException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;

@Service
public class AuthenticationService implements UserDetailsService {
    @Lazy
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordService passwordService;

    public static final int MAX_FAILED_ATTEMPTS = 3;

    private static final long LOCK_TIME_DURATION = 5 * 60 * 1000; // 5 minutes

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username);
        if(user == null){
            throw new UsernameNotFoundException("Username not found: " + username);
        }
        return user;
    }

    public String register(RegisterDTO register) {
        try {
            User user = new User(register.getUsername(), register.getEmail(), passwordEncoder.encode(register.getPassword()), Role.ROLE_USER);
            userRepository.save(user);

            return tokenService.generateToken(user);
        } catch (
                DataIntegrityViolationException e) {
            String errorMessage = e.getRootCause() != null ? e.getRootCause().getMessage() : e.getMessage();
            throw new DuplicateKeyException(errorMessage);
        }
    }

    public String login(LoginDTO login) {
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                    new UsernamePasswordAuthenticationToken(login.getUsername(), login.getPassword());

            Authentication authenticate = this.authenticationManager
                    .authenticate(usernamePasswordAuthenticationToken);

            var user = (User) authenticate.getPrincipal();

            return tokenService.generateToken(user);
    }

    public void changePassword(ChangePasswordDTO changePasswordDTO) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(!passwordService.isPasswordMatch(user, changePasswordDTO.getOldPassword())) {
            throw new InvalidOldPasswordException("Incorrect password. Please make sure the password is correct.");
        };

        passwordService.changePassword(user, changePasswordDTO.getNewPassword());
    }

    public void increaseFailedAttempts(User user) {
        int newFailAttempts = user.getFailedAttempt() + 1;
        userRepository.updateFailedAttempts(newFailAttempts, user.getUsername());
    }

    public void resetFailedAttempts(String username) {
        userRepository.updateFailedAttempts(0, username);
    }

    public void lock(User user) {
        user.setAccountNonLocked(false);
        user.setLockTime(new Date());

        userRepository.save(user);
    }

    public boolean unlockWhenTimeExpired(User user) {
        long lockTimeInMillis = user.getLockTime().getTime();
        long currentTimeInMillis = System.currentTimeMillis();

        if (lockTimeInMillis + LOCK_TIME_DURATION < currentTimeInMillis) {
            user.setAccountNonLocked(true);
            user.setLockTime(null);
            user.setFailedAttempt(0);

            userRepository.save(user);

            return true;
        }

        return false;
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}