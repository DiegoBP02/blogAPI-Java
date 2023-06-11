package com.example.demo.config;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.Role;
import com.example.demo.services.AuthenticationService;
import com.example.demo.services.exceptions.UserNotEnabledException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationProviderTest extends ApplicationConfigTest {

    User USER_RECORD = new User("username", "email@email.com", "password", Role.ROLE_USER);
    @MockBean
    private AuthenticationService authenticationService;
    @MockBean
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthenticationProvider authenticationProvider;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(USER_RECORD, "isEnabled", true);
    }

    @Test
    @DisplayName("should return UsernamePasswordAuthenticationToken if credentials are valid")
    void validCredentials() {

        when(authenticationService.getByUsername(anyString())).thenReturn(USER_RECORD);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        Authentication authentication = new UsernamePasswordAuthenticationToken(USER_RECORD.getUsername(), USER_RECORD.getPassword());

        Authentication result = authenticationProvider.authenticate(authentication);

        assertEquals(USER_RECORD, result.getPrincipal());

        verify(authenticationService, times(1)).getByUsername(anyString());
        verify(passwordEncoder, times(1)).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("should return UsernamePasswordAuthenticationToken and reset user " +
            "attempts if credentials are valid and user has more than one login attempt")
    void validCredentialsResetAttempts() {
        USER_RECORD.setFailedAttempt(1);

        when(authenticationService.getByUsername(anyString())).thenReturn(USER_RECORD);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        doAnswer(invocation -> {
            USER_RECORD.setFailedAttempt(0);
            return null;
        }).when(authenticationService).resetFailedAttempts(anyString());

        Authentication authentication = new UsernamePasswordAuthenticationToken(USER_RECORD.getUsername(), USER_RECORD.getPassword());

        Authentication result = authenticationProvider.authenticate(authentication);

        assertEquals(USER_RECORD, result.getPrincipal());
        assertThat(((User) result.getPrincipal()).getFailedAttempt()).isEqualTo(0);

        verify(authenticationService, times(1)).getByUsername(anyString());
        verify(authenticationService, times(1)).resetFailedAttempts(anyString());
        verify(passwordEncoder, times(1)).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("should return BadCredentialsException and increase failed attempts if credentials are invalid")
    void invalidLogin() {
        when(authenticationService.getByUsername(anyString())).thenReturn(USER_RECORD);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        doAnswer(invocation -> {
            USER_RECORD.setFailedAttempt(USER_RECORD.getFailedAttempt() + 1);
            return null;
        }).when(authenticationService).increaseFailedAttempts(any(User.class));

        Authentication authentication = new UsernamePasswordAuthenticationToken(USER_RECORD.getUsername(), "wrongPassword");

        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> authenticationProvider.authenticate(authentication));
        assertThat(exception.getMessage()).isEqualTo("Wrong password or username.");
        assertEquals(USER_RECORD.getFailedAttempt(), 1);

        verify(authenticationService, times(1)).getByUsername(anyString());
        verify(authenticationService, times(1)).increaseFailedAttempts(any(User.class));
        verify(passwordEncoder, times(1)).matches(anyString(), anyString());
    }


    @Test
    @DisplayName("should lock the user account after 3 failed login attempts")
    void invalidLoginLockAccount() {
        USER_RECORD.setFailedAttempt(2);
        Date newDate = new Date();

        when(authenticationService.getByUsername(anyString())).thenReturn(USER_RECORD);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        doAnswer(invocation -> {
            USER_RECORD.setAccountNonLocked(false);
            USER_RECORD.setLockTime(newDate);
            return null;
        }).when(authenticationService).lock(any(User.class));

        Authentication authentication = new UsernamePasswordAuthenticationToken(USER_RECORD.getUsername(), "wrongPassword");

        LockedException exception = assertThrows(LockedException.class, () -> authenticationProvider.authenticate(authentication));
        assertThat(exception.getMessage()).isEqualTo("Your account has been locked due to 3 failed login attempts."
                + " It will be unlocked after 24 hours.");
        assertFalse(USER_RECORD.isAccountNonLocked());
        assertEquals(USER_RECORD.getLockTime(), newDate);

        verify(authenticationService, times(1)).getByUsername(anyString());
        verify(passwordEncoder, times(1)).matches(anyString(), anyString());
        verify(authenticationService, times(1)).lock(any(User.class));
    }

    @Test
    @DisplayName("should throw LockedException to warn the user that his account has been unlocked")
    void unlockWarning() {
        USER_RECORD.setAccountNonLocked(false);

        when(authenticationService.getByUsername(anyString())).thenReturn(USER_RECORD);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        doAnswer(invocation -> {
            USER_RECORD.setAccountNonLocked(true);
            return true;
        }).when(authenticationService).unlockWhenTimeExpired(any(User.class));

        Authentication authentication = new UsernamePasswordAuthenticationToken(USER_RECORD.getUsername(), "wrongPassword");

        LockedException exception = assertThrows(LockedException.class, () -> authenticationProvider.authenticate(authentication));
        assertThat(exception.getMessage()).isEqualTo("Your account has been unlocked. Please try to login again.");
        assertTrue(USER_RECORD.isAccountNonLocked());

        verify(authenticationService, times(1)).getByUsername(anyString());
        verify(passwordEncoder, times(1)).matches(anyString(), anyString());
        verify(authenticationService, times(1)).unlockWhenTimeExpired(any(User.class));
    }

    @Test
    @DisplayName("should throw LockedException if he tries to login while the account is locked")
    void lockedWarning() {
        USER_RECORD.setAccountNonLocked(false);

        when(authenticationService.getByUsername(anyString())).thenReturn(USER_RECORD);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        when(authenticationService.unlockWhenTimeExpired(any(User.class))).thenReturn(false);

        Authentication authentication = new UsernamePasswordAuthenticationToken(USER_RECORD.getUsername(), "wrongPassword");

        LockedException exception = assertThrows(LockedException.class, () -> authenticationProvider.authenticate(authentication));

        assertThat(exception.getMessage()).isEqualTo("Your account has been locked due to 3 failed login attempts. " +
                "Please try again later.");

        verify(authenticationService, times(1)).getByUsername(anyString());
        verify(passwordEncoder, times(1)).matches(anyString(), anyString());
        verify(authenticationService, times(1)).unlockWhenTimeExpired(any(User.class));
    }

    @Test
    @DisplayName("should throw UserNotEnabledException if the user is not enabled")
    void userNotEnabled() {
        USER_RECORD.setEnabled(false);

        when(authenticationService.getByUsername(anyString())).thenReturn(USER_RECORD);

        Authentication authentication = new UsernamePasswordAuthenticationToken(USER_RECORD.getUsername(), USER_RECORD.getPassword());

        UserNotEnabledException exception = assertThrows(UserNotEnabledException.class,
                () -> authenticationProvider.authenticate(authentication));

        assertThat(exception.getMessage()).isEqualTo("User is not enabled, please verify your email first");

        verify(authenticationService, times(1)).getByUsername(anyString());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(authenticationService, never()).unlockWhenTimeExpired(any(User.class));
    }
}