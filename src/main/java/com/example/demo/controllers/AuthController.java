package com.example.demo.controllers;

import com.example.demo.dtos.ChangePasswordDTO;
import com.example.demo.dtos.LoginDTO;
import com.example.demo.dtos.RegisterDTO;
import com.example.demo.services.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/auth")
public class AuthController {

    @Autowired
    private AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterDTO register) {
            return ResponseEntity.ok().body(authenticationService.register(register));
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginDTO login) {
        return ResponseEntity.ok().body(authenticationService.login(login));
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordDTO changePasswordDTO) {
        authenticationService.changePassword(changePasswordDTO);
        String message = "Password updated successfully!";
        return ResponseEntity.ok().body(message);
    }
}
