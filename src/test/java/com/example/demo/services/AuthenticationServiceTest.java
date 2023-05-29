package com.example.demo.services;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.LoginDTO;
import com.example.demo.dtos.RegisterDTO;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.Role;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.exceptions.DuplicateKeyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AuthenticationServiceTest extends ApplicationConfigTest {

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

    User USER_RECORD = new User("a", "b", "c", Role.ROLE_USER);
    RegisterDTO REGISTER_DTO_RECORD = new RegisterDTO(USER_RECORD.getUsername(), USER_RECORD.getPassword(),USER_RECORD.getEmail());
    LoginDTO LOGIN_DTO_RECORD = new LoginDTO(USER_RECORD.getUsername(), USER_RECORD.getPassword());

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

        assertThrows(UsernameNotFoundException.class,() ->
                authenticationService.loadUserByUsername(USER_RECORD.getUsername()));

        verify(userRepository, times(1)).findByUsername(anyString());
    }

    @Test
    @DisplayName("should return a token")
    void register() {

        when(userRepository.save(any(User.class))).thenReturn(null);
        when(tokenService.generateToken(any(User.class))).thenReturn("token");

        String result = authenticationService.register(REGISTER_DTO_RECORD);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("token");

        verify(userRepository, times(1)).save(any(User.class));
        verify(tokenService, times(1)).generateToken(any(User.class));
    }

    @Test
    @DisplayName("should throw DuplicateKeyException if user alredy exists")
    void registerDuplicateKeyException() {
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("exception"));

        assertThrows(DuplicateKeyException.class,
                () -> authenticationService.register(REGISTER_DTO_RECORD));

        verify(userRepository, times(1)).save(any(User.class));
        verify(tokenService, never()).generateToken(any(User.class));
    }

    @Test
    @DisplayName("should return a token")
    void login() {
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
}