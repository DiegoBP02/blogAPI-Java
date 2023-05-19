package com.example.demo.dtos;


import com.example.demo.entities.enums.PostCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDTO {
    @Size(min = 5, max = 100)
    @NotBlank
    private String title;
    @Size(min = 30, max = 1000)
    private String content;
    @NotBlank
    private Set<PostCategory> categories;
}
