package com.example.demo.services;

import com.example.demo.dtos.LoginDTO;
import com.example.demo.dtos.RegisterDTO;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.Role;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.exceptions.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

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


    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username);
        if(user == null){
            throw new UsernameNotFoundException("Username not found: " + username);
        }
        return user;
    }

    public String register(RegisterDTO register) {
        try {
            User user = new User(register.getUsername(), register.getEmail(), passwordEncoder.encode(register.getPassword()), Role.ROLE_USER);
            userRepository.save(user);

            return tokenService.generateToken(user);
        } catch (
                DataIntegrityViolationException e) {
            throw new DuplicateKeyException(Objects.requireNonNull(e.getRootCause()).getMessage());
        }
    }

    public String login(LoginDTO login) {
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                    new UsernamePasswordAuthenticationToken(login.getUsername(), login.getPassword());

            Authentication authenticate = this.authenticationManager
                    .authenticate(usernamePasswordAuthenticationToken);

            var user = (User) authenticate.getPrincipal();

            return tokenService.generateToken(user);
    }
}