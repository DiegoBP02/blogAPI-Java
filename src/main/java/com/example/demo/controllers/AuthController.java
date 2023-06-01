package com.example.demo.controllers;

import com.example.demo.controllers.exceptions.RateLimitException;
import com.example.demo.dtos.ChangePasswordDTO;
import com.example.demo.dtos.LoginDTO;
import com.example.demo.dtos.RegisterDTO;
import com.example.demo.exceptions.StandardError;
import com.example.demo.services.AuthenticationService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping(value = "/auth")
public class AuthController {

    @Autowired
    private AuthenticationService authenticationService;

    private final Bucket bucket;

    private final StandardError limitError = new StandardError(Instant.now(), HttpStatus.TOO_MANY_REQUESTS.value(), "Too Many Requests", "You have exhausted your API Request Quota");

    public AuthController(){
        Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));
        this.bucket = Bucket.builder()
                .addLimit(limit)
                .build();
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterDTO register) {
        if(bucket.tryConsume(1)){
            return ResponseEntity.ok().body(authenticationService.register(register));
        }
        throw new RateLimitException();
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginDTO login) {
        if(bucket.tryConsume(1)){
            return ResponseEntity.ok().body(authenticationService.login(login));
        }
        throw new RateLimitException();
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordDTO changePasswordDTO) {
        if(bucket.tryConsume(1)){
            authenticationService.changePassword(changePasswordDTO);
            String message = "Password updated successfully!";
            return ResponseEntity.ok().body(message);
        }
        throw new RateLimitException();
    }
}
