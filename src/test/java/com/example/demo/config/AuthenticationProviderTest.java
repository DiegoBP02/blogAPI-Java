package com.example.demo.config;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.Role;
import com.example.demo.services.AuthenticationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationProviderTest extends ApplicationConfigTest {

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationProvider authenticationProvider;

    @Test
    @DisplayName("should return UsernamePasswordAuthenticationToken if credentials are valid")
    void validCredentials() {
        User user = new User("a", "b", "b", Role.ROLE_USER);

        when(authenticationService.getByUsername(anyString())).thenReturn(user);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword());

        Authentication result = authenticationProvider.authenticate(authentication);

        assertEquals(user, result.getPrincipal());

        verify(authenticationService, times(1)).getByUsername(anyString());
        verify(passwordEncoder, times(1)).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("should return UsernamePasswordAuthenticationToken and reset user " +
            "attempts if credentials are valid and user has more than one login attempt")
    void validCredentialsResetAttempts() {
        User user = new User("a", "b", "b", Role.ROLE_USER);
        user.setFailedAttempt(1);

        when(authenticationService.getByUsername(anyString())).thenReturn(user);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        doAnswer(invocation -> {
            user.setFailedAttempt(0);
            return null;
        }).when(authenticationService).resetFailedAttempts(anyString());

        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword());

        Authentication result = authenticationProvider.authenticate(authentication);

        assertEquals(user, result.getPrincipal());
        assertThat(((User) result.getPrincipal()).getFailedAttempt()).isEqualTo(0);

        verify(authenticationService, times(1)).getByUsername(anyString());
        verify(authenticationService, times(1)).resetFailedAttempts(anyString());
        verify(passwordEncoder, times(1)).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("should return BadCredentialsException and increase failed attempts if credentials are invalid")
    void invalidLogin() {
        User user = new User("a", "b", "b", Role.ROLE_USER);

        when(authenticationService.getByUsername(anyString())).thenReturn(user);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        doAnswer(invocation -> {
            user.setFailedAttempt(user.getFailedAttempt() + 1);
            return null;
        }).when(authenticationService).increaseFailedAttempts(any(User.class));

        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getUsername(), "wrongPassword");

        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> authenticationProvider.authenticate(authentication));
        assertThat(exception.getMessage()).isEqualTo("Wrong password or username.");
        assertEquals(user.getFailedAttempt(), 1);

        verify(authenticationService, times(1)).getByUsername(anyString());
        verify(authenticationService, times(1)).increaseFailedAttempts(any(User.class));
        verify(passwordEncoder, times(1)).matches(anyString(), anyString());
    }


    @Test
    @DisplayName("should lock the user account after 3 failed login attempts")
    void invalidLoginLockAccount() {
        User user = new User("a", "b", "b", Role.ROLE_USER);
        user.setFailedAttempt(2);
        Date newDate = new Date();

        when(authenticationService.getByUsername(anyString())).thenReturn(user);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        doAnswer(invocation -> {
            user.setAccountNonLocked(false);
            user.setLockTime(newDate);
            return null;
        }).when(authenticationService).lock(any(User.class));

        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getUsername(), "wrongPassword");

        LockedException exception = assertThrows(LockedException.class, () -> authenticationProvider.authenticate(authentication));
        assertThat(exception.getMessage()).isEqualTo("Your account has been locked due to 3 failed login attempts."
                        + " It will be unlocked after 24 hours.");
        assertFalse(user.isAccountNonLocked());
        assertEquals(user.getLockTime(), newDate);

        verify(authenticationService, times(1)).getByUsername(anyString());
        verify(passwordEncoder, times(1)).matches(anyString(), anyString());
        verify(authenticationService, times(1)).lock(any(User.class));
    }

    @Test
    @DisplayName("should throw LockedException to warn the user that his account has been unlocked")
    void unlockWarning() {
        User user = new User("a", "b", "b", Role.ROLE_USER);
        user.setAccountNonLocked(false);

        when(authenticationService.getByUsername(anyString())).thenReturn(user);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        doAnswer(invocation -> {
            user.setAccountNonLocked(true);
            return true;
        }).when(authenticationService).unlockWhenTimeExpired(any(User.class));

        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getUsername(), "wrongPassword");

        LockedException exception = assertThrows(LockedException.class, () -> authenticationProvider.authenticate(authentication));
        assertThat(exception.getMessage()).isEqualTo("Your account has been unlocked. Please try to login again.");
        assertTrue(user.isAccountNonLocked());

        verify(authenticationService, times(1)).getByUsername(anyString());
        verify(passwordEncoder, times(1)).matches(anyString(), anyString());
        verify(authenticationService, times(1)).unlockWhenTimeExpired(any(User.class));
    }

    @Test
    @DisplayName("should throw LockedException if he tries to login while the account is locked")
    void lockedWarning() {
        User user = new User("a", "b", "b", Role.ROLE_USER);
        user.setAccountNonLocked(false);

        when(authenticationService.getByUsername(anyString())).thenReturn(user);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        when(authenticationService.unlockWhenTimeExpired(any(User.class))).thenReturn(false);

        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getUsername(), "wrongPassword");

        LockedException exception = assertThrows(LockedException.class, () -> authenticationProvider.authenticate(authentication));

        assertThat(exception.getMessage()).isEqualTo("Your account has been locked due to 3 failed login attempts. " +
                "Please try again later.");

        verify(authenticationService, times(1)).getByUsername(anyString());
        verify(passwordEncoder, times(1)).matches(anyString(), anyString());
        verify(authenticationService, times(1)).unlockWhenTimeExpired(any(User.class));
    }
}