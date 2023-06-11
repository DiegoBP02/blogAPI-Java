package com.example.demo.entities.enums;

public enum PostCategory {
    technology(1),
    sports(2),
    travel(3),
    food_and_recipes(4),
    fashion_and_style(5),
    health_and_fitness(6),
    arts_and_culture(7),
    personal_development(8);

    private int code;

    private PostCategory(int code) {
        this.code = code;
    }

    public static PostCategory valueOf(int code) {
        for (PostCategory value : PostCategory.values()) {
            if (value.getCode() == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid category code");
    }

    public int getCode() {
        return code;
    }
}
