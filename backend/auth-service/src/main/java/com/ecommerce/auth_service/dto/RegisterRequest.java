package com.ecommerce.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(
                max = 150,
                message = "El correo no puede superar los 150 caracteres"
        )
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(
                min = 12,
                max = 72,
                message = "La contraseña debe tener entre 12 y 72 caracteres"
        )
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._#-]).{12,72}$",
                message = "La contraseña debe incluir una mayúscula, una minúscula, un número y un carácter especial"
        )
        String password
) {
}