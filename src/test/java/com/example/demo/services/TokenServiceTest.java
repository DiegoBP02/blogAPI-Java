package com.example.demo.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.demo.ApplicationConfigTest;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TokenServiceTest")
class TokenServiceTest extends ApplicationConfigTest {
    @Autowired
    TokenService tokenService;

    User USER_RECORD = new User("a", "b", "c", Role.ROLE_USER);

    @BeforeEach
    void setupSecurityContext() {
        ReflectionTestUtils.setField(USER_RECORD, "id", UUID.randomUUID());
    }

    @Test
    @DisplayName("should generate a token")
    void create() {
        String token = tokenService.generateToken(USER_RECORD);

        assertThat(token).isNotNull();
    }

    @Test
    @DisplayName("should genretate a token with correct claim")
    void createCorrectClaims(){
        String token = tokenService.generateToken(USER_RECORD);

        DecodedJWT decodedToken = JWT.decode(token);

        assertThat(decodedToken.getSubject()).isEqualTo(USER_RECORD.getUsername());
        assertThat(decodedToken.getClaim("id").asString()).isEqualTo(USER_RECORD.getId().toString());
    }

    @Test
    @DisplayName("should generate a token with correct expiration")
    void createCorrectExpiration(){
        String token = tokenService.generateToken(USER_RECORD);

        DecodedJWT decodedToken = JWT.decode(token);
        Date expiration = decodedToken.getExpiresAt();
        Date expectedExpiration = Date.from(LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.of("-03:00")));

        long toleranceMilliseconds = 1000;
        long expirationTime = expiration.getTime();
        long expectedExpirationTime = expectedExpiration.getTime();

        assertThat(expirationTime).isCloseTo(expectedExpirationTime, within(toleranceMilliseconds));
    }

    @Test
    @DisplayName("should generate a token with correct signature")
    void createCorrectSignature(){
        String token = tokenService.generateToken(USER_RECORD);

        Algorithm algorithm = Algorithm.HMAC256("secret");
        JWTVerifier verifier = JWT.require(algorithm).build();
        verifier.verify(token);
    }

    @Test
    @DisplayName("should retrieve the subject from the token")
    void getSubject(){
        String token = JWT.create()
                .withSubject(USER_RECORD.getUsername())
                .withClaim("id", USER_RECORD.getId().toString())
                .withExpiresAt(LocalDateTime.now()
                        .plusDays(1)
                        .toInstant(ZoneOffset.of("-03:00"))
                ).sign(Algorithm.HMAC256("secret"));

        String retrievedSubject = tokenService.getSubject(token);
        assertThat(retrievedSubject).isEqualTo(USER_RECORD.getUsername());
    }
}