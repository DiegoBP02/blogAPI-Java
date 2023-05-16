package com.example.demo.controllers;

import com.example.demo.dto.Login;
import com.example.demo.entities.User;
import com.example.demo.services.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenService tokenService;

    @PostMapping("/login")
    public String login(@RequestBody Login login){
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                    new UsernamePasswordAuthenticationToken(login.username(), login.password());

            Authentication authenticate = this.authenticationManager.authenticate(usernamePasswordAuthenticationToken);

            var user = (User) authenticate.getPrincipal();

            return tokenService.generateToken(user);
        }

}
