package com.example.demo.services;

import com.example.demo.ApplicationConfigTest;
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
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationServiceTest extends ApplicationConfigTest {

    User USER_RECORD = new User("a", "b", "c", Role.ROLE_USER);
    RegisterDTO REGISTER_DTO_RECORD = new RegisterDTO(USER_RECORD.getUsername(), USER_RECORD.getPassword(), USER_RECORD.getEmail());
    LoginDTO LOGIN_DTO_RECORD = new LoginDTO(USER_RECORD.getUsername(), USER_RECORD.getPassword());
    ConfirmationToken CONFIRMATION_TOKEN_RECORD = new ConfirmationToken(USER_RECORD);
    @Autowired
    private AuthenticationService authenticationService;
    @MockBean
    private AuthenticationManager authenticationManager;
    @MockBean
    private TokenService tokenService;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private PasswordEncoder passwordEncoder;
    @MockBean
    private PasswordService passwordService;
    @MockBean
    private EmailSenderService senderService;
    @MockBean
    private ConfirmationTokenRepository confirmationTokenRepository;

    @Test
    @DisplayName("should return an user")
    void loadUserByUsername() {
        when(userRepository.findByUsername(anyString()))
                .thenReturn(USER_RECORD);

        UserDetails result = authenticationService.loadUserByUsername(USER_RECORD.getUsername());

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(USER_RECORD.getUsername());
        assertThat(result.getPassword()).isEqualTo(USER_RECORD.getPassword());

        verify(userRepository, times(1)).findByUsername(anyString());
    }

    @Test
    @DisplayName("should throw UsernameNotFoundException if there is no user")
    void loadUserByUsernameUsernameNotFoundException() {
        when(userRepository.findByUsername(anyString()))
                .thenReturn(null);

        assertThrows(UsernameNotFoundException.class, () ->
                authenticationService.loadUserByUsername(USER_RECORD.getUsername()));

        verify(userRepository, times(1)).findByUsername(anyString());
    }

    @Test
    @DisplayName("should return a string warning the user to confirm his email")
    void register() {
        String result = authenticationService.register(REGISTER_DTO_RECORD);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("Please confirm your email to complete registration!");

        verify(userRepository, times(1)).save(any(User.class));
        verify(confirmationTokenRepository, times(1)).save(any(ConfirmationToken.class));
    }

    @Test
    @DisplayName("should throw DuplicateKeyException if user already exists")
    void registerDuplicateKeyException() {
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException(anyString()));

        assertThrows(DuplicateKeyException.class,
                () -> authenticationService.register(REGISTER_DTO_RECORD));

        verify(userRepository, times(1)).save(any(User.class));
        verify(tokenService, never()).generateToken(any(User.class));
    }

    @Test
    @DisplayName("should return a token")
    void login() {
        ReflectionTestUtils.setField(USER_RECORD, "isEnabled", true);
        Authentication authenticate = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authenticate);
        when(authenticate.getPrincipal()).thenReturn(USER_RECORD);
        when(tokenService.generateToken(any(User.class))).thenReturn("token");

        String result = authenticationService.login(LOGIN_DTO_RECORD);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("token");

        verify(authenticationManager, times(1))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(authenticate, times(1)).getPrincipal();
        verify(tokenService, times(1)).generateToken(any(User.class));
    }

    @Test
    @DisplayName("should change the password")
    void changePassword() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(USER_RECORD);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(passwordService.isPasswordMatch(any(User.class), anyString())).thenReturn(true);
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            String newPassword = invocation.getArgument(1);
            user.setPassword(newPassword);
            return null;
        }).when(passwordService).changePassword(any(User.class), anyString());

        ChangePasswordDTO changePasswordDTO = new ChangePasswordDTO("oldPassword", "newPassword");

        assertDoesNotThrow(() -> authenticationService.changePassword(changePasswordDTO));

        assertThat(USER_RECORD.getPassword()).isEqualTo(changePasswordDTO.getNewPassword());

        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
        verify(passwordService, times(1)).isPasswordMatch(any(User.class), anyString());
        verify(passwordService, times(1)).changePassword(any(User.class), anyString());
    }

    @Test
    @DisplayName("should throw InvalidOldPasswordException if password is invalid")
    void changePasswordInvalidOldPasswordException() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(USER_RECORD);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(passwordService.isPasswordMatch(any(User.class), anyString())).thenThrow(new InvalidOldPasswordException("InvalidOldPasswordException"));

        ChangePasswordDTO changePasswordDTO = new ChangePasswordDTO("oldPassword", "newPassword");

        assertThrows(InvalidOldPasswordException.class, () ->
                authenticationService.changePassword(changePasswordDTO));

        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
        verify(passwordService, times(1)).isPasswordMatch(any(User.class), anyString());
        verify(passwordService, never()).changePassword(any(User.class), anyString());
    }

    @Test
    @DisplayName("should return an user by username")
    void getByUsername() {
        when(userRepository.findByUsername(anyString())).thenReturn(USER_RECORD);

        User result = authenticationService.getByUsername(anyString());

        assertEquals(result, USER_RECORD);

        verify(userRepository, times(1)).findByUsername(anyString());
    }

    @Test
    @DisplayName("should update the reset password token of user")
    void updateResetPasswordToken() throws Exception {
        when(userRepository.findByEmail(anyString())).thenReturn(USER_RECORD);
        UUID token = UUID.randomUUID();

        authenticationService.updateResetPasswordToken(token, USER_RECORD.getEmail());

        assertEquals(USER_RECORD.getResetPasswordToken(), token);

        verify(userRepository, times(1)).findByEmail(anyString());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("should throw EntityNotFound Exception if no user is found")
    void updateResetPasswordTokenException() {
        when(userRepository.findByEmail(anyString())).thenReturn(null);
        UUID token = UUID.randomUUID();

        assertThrows(EntityNotFoundException.class, () ->
                authenticationService.updateResetPasswordToken(token, USER_RECORD.getEmail()));

        verify(userRepository, times(1)).findByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("should return an user by reset password token")
    void getByResetPasswordToken() {
        when(userRepository.findByResetPasswordToken(any(UUID.class))).thenReturn(USER_RECORD);
        UUID token = UUID.randomUUID();

        User result = authenticationService.getByResetPasswordToken(token);

        assertEquals(result, USER_RECORD);

        verify(userRepository, times(1)).findByResetPasswordToken(any(UUID.class));
    }

    @Test
    @DisplayName("should update the reset password token of user to a UUID token")
    void changePasswordByUser() throws Exception {
        when(userRepository.findByEmail(anyString())).thenReturn(USER_RECORD);
        UUID token = UUID.randomUUID();

        authenticationService.updateResetPasswordToken(token, USER_RECORD.getEmail());

        assertEquals(USER_RECORD.getResetPasswordToken(), token);

        verify(userRepository, times(1)).findByEmail(anyString());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("should invoke sendEmail from senderService")
    void sendEmail() throws Exception {
        doNothing().when(senderService).sendEmail(anyString(), anyString(), anyString());

        senderService.sendEmail("", "", "");

        verify(senderService, times(1)).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("should return only the server url from the request url")
    void getSiteURL() {
        String URL = "http://localhost:8080/auth/path";

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer(URL));
        when(request.getServletPath()).thenReturn("/path");

        String result = authenticationService.getSiteURL(request);

        assertEquals(result, "http://localhost:8080/auth");

        verify(request, times(1)).getRequestURL();
        verify(request, times(1)).getServletPath();
    }

    @Test
    @DisplayName("should return a string warning the user that his email was verified")
    void confirmEmail() {
        when(confirmationTokenRepository.findByConfirmationToken(any(UUID.class)))
                .thenReturn(CONFIRMATION_TOKEN_RECORD);
        ReflectionTestUtils.setField(CONFIRMATION_TOKEN_RECORD, "id", UUID.randomUUID());
        when(userRepository.findByEmail(anyString())).thenReturn(USER_RECORD);

        String result = authenticationService.confirmEmail(UUID.randomUUID());

        assertThat(result).isNotNull();
        assertEquals(result, "Email verified successfully");

        verify(confirmationTokenRepository, times(1))
                .findByConfirmationToken(any(UUID.class));
        verify(userRepository, times(1)).findByEmail(anyString());
        verify(confirmationTokenRepository, never()).save(any(ConfirmationToken.class));
        verify(senderService, never()).sendEmail(anyString(), anyString(), anyString());
        verify(userRepository, times(1)).save(any(User.class));
        verify(confirmationTokenRepository, times(1)).deleteById(any(UUID.class));
    }

    @Test
    @DisplayName("should throw EntityNotFoundException if the token was not found")
    void confirmEmailEntityNotFoundException() {
        when(confirmationTokenRepository.findByConfirmationToken(any(UUID.class)))
                .thenReturn(null);

        assertThrows(EntityNotFoundException.class, () ->
                authenticationService.confirmEmail(UUID.randomUUID()));

        verify(confirmationTokenRepository, times(1))
                .findByConfirmationToken(any(UUID.class));
        verify(userRepository, never()).findByEmail(anyString());
        verify(confirmationTokenRepository, never()).save(any(ConfirmationToken.class));
        verify(senderService, never()).sendEmail(anyString(), anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(confirmationTokenRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    @DisplayName("should throw ResponseStatusException if the user is already enabled")
    void confirmEmailUserAlreadyEnabled() {
        ReflectionTestUtils.setField(USER_RECORD, "isEnabled", true);
        when(confirmationTokenRepository.findByConfirmationToken(any(UUID.class)))
                .thenReturn(CONFIRMATION_TOKEN_RECORD);
        when(userRepository.findByEmail(anyString())).thenReturn(USER_RECORD);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                authenticationService.confirmEmail(UUID.randomUUID()));

        String expectedMessage = "Email is already confirmed";
        assertEquals(exception.getStatusCode(), HttpStatus.BAD_REQUEST);
        assertEquals(exception.getReason(), expectedMessage);

        verify(confirmationTokenRepository, times(1))
                .findByConfirmationToken(any(UUID.class));
        verify(userRepository, times(1)).findByEmail(anyString());
        verify(confirmationTokenRepository, never()).save(any(ConfirmationToken.class));
        verify(senderService, never()).sendEmail(anyString(), anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(confirmationTokenRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    @DisplayName("should throw ResponseStatusException if the token is expired and send a new token to user email")
    void confirmEmailTokenExpired() {
        ReflectionTestUtils.setField
                (CONFIRMATION_TOKEN_RECORD, "expiryDate", Instant.now().minusSeconds(1));
        when(confirmationTokenRepository.findByConfirmationToken(any(UUID.class)))
                .thenReturn(CONFIRMATION_TOKEN_RECORD);
        when(userRepository.findByEmail(anyString())).thenReturn(USER_RECORD);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                authenticationService.confirmEmail(UUID.randomUUID()));

        String expectedMessage = "The confirmation token has expired, a new token has been sent to your email";
        assertEquals(exception.getStatusCode(), HttpStatus.BAD_REQUEST);
        assertEquals(exception.getReason(), expectedMessage);

        verify(confirmationTokenRepository, times(1))
                .findByConfirmationToken(any(UUID.class));
        verify(userRepository, times(1)).findByEmail(anyString());
        verify(confirmationTokenRepository, times(1)).save(any(ConfirmationToken.class));
        verify(senderService, times(1))
                .sendEmail(anyString(), anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(confirmationTokenRepository, never()).deleteById(any(UUID.class));
    }

}