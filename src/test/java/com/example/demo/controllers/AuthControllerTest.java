package com.example.demo.controllers;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.LoginDTO;
import com.example.demo.dtos.RegisterDTO;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.Role;
import com.example.demo.services.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AuthControllerTest")
class AuthControllerTest extends ApplicationConfigTest {

    @MockBean
    private AuthenticationService authenticationService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String PATH = "/auth";

    User USER_RECORD = new User("username", "email@email.com", "password", Role.ROLE_USER);
    RegisterDTO REGISTER_DTO_RECORD = new RegisterDTO(USER_RECORD.getUsername(), USER_RECORD.getPassword(),USER_RECORD.getEmail());
    LoginDTO LOGIN_DTO_RECORD = new LoginDTO(USER_RECORD.getUsername(), USER_RECORD.getPassword());

    @Test
    @DisplayName("should register a user")
    void register() throws Exception {
        when(authenticationService.register(any(RegisterDTO.class))).thenReturn("token");

        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .post(PATH + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(REGISTER_DTO_RECORD));

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(content().string("token"));

        verify(authenticationService, times(1)).register(any(RegisterDTO.class));
    }

    @Test
    @DisplayName("should throw MethodArgumentNotValidException for invalid request body")
    void registerInvalidBody() throws Exception {
        RegisterDTO registerDTO = new RegisterDTO();

        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .post(PATH + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(registerDTO));

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException));
    }

    @Test
    @DisplayName("should login a user")
    void login() throws Exception {
        when(authenticationService.login(any(LoginDTO.class))).thenReturn("token");

        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .post(PATH + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(LOGIN_DTO_RECORD));

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(content().string("token"));

        verify(authenticationService, times(1)).login(any(LoginDTO.class));
    }

    @Test
    @DisplayName("should throw MethodArgumentNotValidException for invalid request body")
    void loginInvalidBody() throws Exception {
        LoginDTO loginDTO = new LoginDTO();

        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .post(PATH + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(loginDTO));

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException));
    }
}