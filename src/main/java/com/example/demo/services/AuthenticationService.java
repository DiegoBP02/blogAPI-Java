package com.example.demo.services;

import com.auth0.jwt.exceptions.TokenExpiredException;
import com.example.demo.controllers.exceptions.InvalidTokenException;
import com.example.demo.dtos.ChangePasswordDTO;
import com.example.demo.dtos.LoginDTO;
import com.example.demo.dtos.RegisterDTO;
import com.example.demo.entities.ConfirmationToken;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.Role;
import com.example.demo.repositories.ConfirmationTokenRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.exceptions.DuplicateKeyException;
import com.example.demo.services.exceptions.InvalidOldPasswordException;
import com.example.demo.services.exceptions.ResourceNotFoundException;
import com.example.demo.services.exceptions.UserNotEnabledException;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

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

    @Autowired
    private ConfirmationTokenRepository confirmationTokenRepository;

    public static final int MAX_FAILED_ATTEMPTS = 3;

    private static final long LOCK_TIME_DURATION = 5 * 60 * 1000; // 5 minutes

    @Autowired
    private EmailSenderService senderService;

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("Username not found: " + username);
        }
        return user;
    }

    public String register(RegisterDTO register) {
        try {
            User user = new User(register.getUsername(), register.getEmail(), passwordEncoder.encode(register.getPassword()), Role.ROLE_USER);
            userRepository.save(user);

            ConfirmationToken confirmationToken = new ConfirmationToken(user);

            confirmationTokenRepository.save(confirmationToken);

            String subject = "Complete registration!";

            String content = "To confirm your account, please click here: "
                    + "http://localhost:8080/auth/confirm-account?token=" +
                    confirmationToken.getConfirmationToken();

            sendEmail(user.getEmail(), subject, content);

            return "Please confirm your email to complete registration!";
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

        User user = (User) authenticate.getPrincipal();

        return tokenService.generateToken(user);
    }

    public void changePassword(ChangePasswordDTO changePasswordDTO) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!passwordService.isPasswordMatch(user, changePasswordDTO.getOldPassword())) {
            throw new InvalidOldPasswordException("Incorrect password. Please make sure the password is correct.");
        }
        ;

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

    public void forgotPassword(HttpServletRequest request, String email) {
        UUID token = UUID.randomUUID();

        updateResetPasswordToken(token, email);
        String resetPasswordLink = getSiteURL(request) + "/reset_password?token=" + token;

        String subject = "Here's the link to reset your password";

        String content = "<p>Hello,</p>"
                + "<p>You have requested to reset your password.</p>"
                + "<p>Click the link below to change your password:</p>"
                + "<p><a href=\"" + resetPasswordLink + "\">Change my password</a></p>"
                + "<br>"
                + "<p>Ignore this email if you do remember your password, "
                + "or you have not made the request.</p>";

        sendEmail(email, subject, content);
    }

    public void resetPassword(String tokenString, String password) {
        UUID token = UUID.fromString(tokenString);

        User user = getByResetPasswordToken(token);

        if (user == null) {
            throw new InvalidTokenException();
        }

        changePasswordByUser(user, password);
    }

    public void updateResetPasswordToken(UUID token, String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new EntityNotFoundException("Could not find any user with the email " + email);
        }
        user.setResetPasswordToken(token);
        userRepository.save(user);
    }

    public User getByResetPasswordToken(UUID token) {
        return userRepository.findByResetPasswordToken(token);
    }

    public void changePasswordByUser(User user, String newPassword) {
        String encodedPassword = passwordService.hashPassword(newPassword);
        user.setPassword(encodedPassword);

        user.setResetPasswordToken(null);
        userRepository.save(user);
    }

    public void sendEmail(String recipientEmail, String subject, String content) {
        senderService.sendEmail(recipientEmail,
                subject, content);
    }

    public String getSiteURL(HttpServletRequest request) {
        String siteURL = request.getRequestURL().toString();
        return siteURL.replace(request.getServletPath(), "");
    }

    public String confirmEmail(UUID confirmationToken) {
        ConfirmationToken token = confirmationTokenRepository.findByConfirmationToken(confirmationToken);

        if (token == null) {
            throw new EntityNotFoundException("Token not found");
        }

        User user = userRepository.findByEmail(token.getUser().getEmail());

        if (user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already confirmed");
        }

        if(token.isTokenExpired()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The confirmation token has expired, please try again with a new token");
        }

        user.setEnabled(true);
        userRepository.save(user);
        return "Email verified successfully";
    }
}