package com.example.demo.services.utils;

import com.example.demo.entities.User;
import com.example.demo.services.exceptions.UnauthorizedAccessException;

import java.util.UUID;

public class checkOwnership {
    public static boolean checkOwnership(User user, UUID objAuthorId) {
        UUID userId = user.getId();
        if (userId.equals(objAuthorId)) return true;
        throw new UnauthorizedAccessException("You are not authorized to update this object. It does not belong to you");
    }
}
