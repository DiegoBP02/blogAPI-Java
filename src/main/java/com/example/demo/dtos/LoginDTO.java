package com.example.demo.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginDTO {
    @NotBlank
    @Size(min = 3, max = 20)
    private String username;
    @NotBlank
    @Size(min = 4, max = 30)
    private String password;
}
