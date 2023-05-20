package com.example.demo.config;

import com.auth0.jwt.exceptions.TokenExpiredException;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class FilterToken extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(FilterToken.class);
    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token;

        var authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null) {
            try {
                token = authorizationHeader.replace("Bearer ", "");
                var subject = this.tokenService.getSubject(token);

                var user = this.userRepository.findByUsername(subject);

                var authentication = new UsernamePasswordAuthenticationToken(user,
                        null, user.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);

                request.setAttribute("user", user);
            } catch (TokenExpiredException e) {
                request.setAttribute("tokenExpired", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}