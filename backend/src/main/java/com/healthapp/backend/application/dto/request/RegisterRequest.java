package com.healthapp.backend.application.dto.request;

import com.healthapp.backend.domain.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "User registration request")
public class RegisterRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    @NotNull
    private Role role;

    @Schema(description = "Required when role is DOCTOR")
    private String specialization;

    @Schema(description = "Optional phone for patients")
    private String phone;
}
