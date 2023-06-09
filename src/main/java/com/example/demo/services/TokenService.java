package com.example.demo.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.demo.entities.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@Service
public class TokenService {

    public String generateToken(User user) {
        return JWT.create()
                .withSubject(user.getUsername())
                .withClaim("id", user.getId().toString())
                .withIssuedAt(new Date(System.currentTimeMillis()))
                .withExpiresAt(LocalDateTime.now()
                        .plusDays(1)
                        .toInstant(ZoneOffset.of("-03:00"))
                ).sign(Algorithm.HMAC256("secret"));
    }

    public String getSubject(String token) {
        return JWT.require(Algorithm.HMAC256("secret"))
                .build().verify(token).getSubject();
    }
}
