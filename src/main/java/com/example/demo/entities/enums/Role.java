package com.example.demo.entities.enums;

public enum Role {
    ROLE_USER(1),
    ROLE_ADMIN(2);

    private int code;

    private Role(int code){
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static Role valueOf(int code){
        for (Role value : Role.values()){
            if(value.getCode() == code){
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid role code");
    }
}
