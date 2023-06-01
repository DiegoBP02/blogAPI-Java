package com.example.demo.config;

import com.example.demo.entities.User;
import com.example.demo.services.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationProvider implements org.springframework.security.authentication.AuthenticationProvider {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();
        User user = authenticationService.getByUsername(username);

        if (user != null) {
            if ((passwordEncoder.matches(password, user.getPassword())&& user.isAccountNonLocked())) {
                if (user.getFailedAttempt() > 0) {
                    authenticationService.resetFailedAttempts(user.getUsername());
                }
                return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            } else {
                if (user.isEnabled() && user.isAccountNonLocked()) {
                    if (user.getFailedAttempt() < AuthenticationService.MAX_FAILED_ATTEMPTS - 1) {
                        authenticationService.increaseFailedAttempts(user);
                        throw new BadCredentialsException("Wrong password or username.");
                    } else {
                        authenticationService.lock(user);
                        throw new LockedException(("Your account has been locked due to 3 failed login attempts."
                                + " It will be unlocked after 24 hours."));
                    }
                } else if (!user.isAccountNonLocked()) {
                    if (authenticationService.unlockWhenTimeExpired(user)) {
                        throw new LockedException("Your account has been unlocked. Please try to login again.");
                    } else{
                        throw new LockedException("Your account has been locked due to 3 failed login attempts. " +
                                "Please try again later.");
                    }
                }
            }
        }

        return authentication;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
