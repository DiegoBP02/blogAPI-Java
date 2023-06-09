package com.example.demo.controllers;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.controllers.exceptions.BadRequestException;
import com.example.demo.dtos.ChangePasswordDTO;
import com.example.demo.dtos.LoginDTO;
import com.example.demo.dtos.RegisterDTO;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.Role;
import com.example.demo.services.AuthenticationService;
import com.example.demo.services.exceptions.InvalidOldPasswordException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AuthControllerTest")
class AuthControllerTest extends ApplicationConfigTest {

    private static final String PATH = "/auth";
    User USER_RECORD = new User("username", "email@email.com", "password", Role.ROLE_USER);
    RegisterDTO REGISTER_DTO_RECORD = new RegisterDTO(USER_RECORD.getUsername(), USER_RECORD.getPassword(), USER_RECORD.getEmail());
    LoginDTO LOGIN_DTO_RECORD = new LoginDTO(USER_RECORD.getUsername(), USER_RECORD.getPassword());
    @MockBean
    private AuthenticationService authenticationService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("should register a user")
    void register() throws Exception {
        String expectedString = "Please confirm your email to complete registration!";
        when(authenticationService.register(any(RegisterDTO.class))).thenReturn(expectedString);

        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .post(PATH + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(REGISTER_DTO_RECORD));

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(content().string(expectedString));

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

    @Test
    @WithMockUser
    @DisplayName("should change the password")
    void changePassword() throws Exception {
        ChangePasswordDTO changePasswordDTO = new ChangePasswordDTO("oldPassword", "newPassword");
        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .post(PATH + "/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(changePasswordDTO));

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(content().string("Password updated successfully!"));
    }

    @Test
    @WithMockUser
    @DisplayName("should throw InvalidOldPasswordException if password is wrong")
    void changePasswordInvalidUser() throws Exception {
        doThrow(InvalidOldPasswordException.class).when(authenticationService)
                .changePassword(any(ChangePasswordDTO.class));

        ChangePasswordDTO changePasswordDTO = new ChangePasswordDTO("oldPasswordWrong", "newPassword");
        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .post(PATH + "/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(changePasswordDTO));

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof InvalidOldPasswordException));
    }

    @Test
    @DisplayName("should return ok if user resetPasswordToken was updated and email was successfully sent")
    void forgotPassword() throws Exception {
        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .post(PATH + "/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .param("email", "email@email.com");

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(content().string("We have sent a reset password link to your email. Please check."));
    }

    @Test
    @DisplayName("should throw EntityNotFoundException if user with the provided email doesn't exists")
    void forgotPasswordInvalidEmail() throws Exception {
        doThrow(EntityNotFoundException.class).when(authenticationService)
                .forgotPassword(any(HttpServletRequest.class), anyString());

        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .post(PATH + "/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .param("email", "email@email.com");

        mockMvc.perform(mockRequest)
                .andExpect(status().isNotFound())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof EntityNotFoundException));

        verify(authenticationService, times(1)).forgotPassword(any(), any());
    }

    @Test
    @DisplayName("should throw BadRequestException if email param is missing")
    void forgotPasswordInvalidParam() throws Exception {
        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .post(PATH + "/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof BadRequestException));
    }

    @Test
    @DisplayName("should return ok if user password was updated")
    void resetPassword() throws Exception {
        when(authenticationService.getByResetPasswordToken(any(UUID.class)))
                .thenReturn(USER_RECORD);

        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .post(PATH + "/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .param("token", UUID.randomUUID().toString())
                .param("password", "password");

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(content().string("You have successfully changed your password."));
    }

    @Test
    @DisplayName("should throw BadRequestException if password and token params are missing")
    void resetPasswordInvalidParams() throws Exception {
        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .post(PATH + "/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof BadRequestException));
    }

    @Test
    @DisplayName("should throw BadRequestException if password param is missing")
    void resetPasswordInvalidParamPassword() throws Exception {
        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .post(PATH + "/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .param("token", UUID.randomUUID().toString());

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof BadRequestException));
    }

    @Test
    @DisplayName("should throw BadRequestException if token param is missing")
    void resetPasswordInvalidParamToken() throws Exception {
        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .post(PATH + "/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .param("password", "password");

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof BadRequestException));
    }

    @Test
    @DisplayName("should return a string warning the user that his email was verified")
    void confirmUserAccount() throws Exception {
        String expectedResult = "Email verified successfully";
        when(authenticationService.confirmEmail(any(UUID.class))).thenReturn(expectedResult);
        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .get(PATH + "/confirm-account")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .param("token", UUID.randomUUID().toString());

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(content().string(expectedResult));

        verify(authenticationService, times(1)).confirmEmail(any(UUID.class));
    }

    @Test
    @DisplayName("should throw EntityNotFoundException if the token was not found")
    void confirmEmailEntityNotFoundException() throws Exception {
        when(authenticationService.confirmEmail(any(UUID.class)))
                .thenThrow(EntityNotFoundException.class);

        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .get(PATH + "/confirm-account")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .param("token", UUID.randomUUID().toString());

        mockMvc.perform(mockRequest)
                .andExpect(status().isNotFound())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof EntityNotFoundException));

        verify(authenticationService, times(1)).confirmEmail(any(UUID.class));
    }

    @Test
    @DisplayName("should throw ResponseStatusException if the user is already enabled")
    void confirmEmailUserAlreadyEnabled() throws Exception {
        String exceptionMessage = "Email is already confirmed";
        when(authenticationService.confirmEmail(any(UUID.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, exceptionMessage));

        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .get(PATH + "/confirm-account")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .param("token", UUID.randomUUID().toString());

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof ResponseStatusException))
                .andExpect(result -> {
                    String responseBody = result.getResponse().getContentAsString();
                    assertTrue(responseBody.contains(exceptionMessage));
                });

        verify(authenticationService, times(1)).confirmEmail(any(UUID.class));
    }

    @Test
    @DisplayName("should throw ResponseStatusException if the token is expired")
    void confirmEmailTokenExpired() throws Exception {
        String exceptionMessage = "The confirmation token has expired, please try again with a new token";
        when(authenticationService.confirmEmail(any(UUID.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, exceptionMessage));

        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .get(PATH + "/confirm-account")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .param("token", UUID.randomUUID().toString());

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof ResponseStatusException))
                .andExpect(result -> {
                    String responseBody = result.getResponse().getContentAsString();
                    assertTrue(responseBody.contains(exceptionMessage));
                });

        verify(authenticationService, times(1)).confirmEmail(any(UUID.class));
    }

}
