package com.vgrfinance.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroRequest(
        @NotBlank(message = "Informe o nome.")
        @Size(max = 100, message = "Use no máximo 100 caracteres no nome.")
        String nome,

        @NotBlank(message = "Informe o e-mail.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 255, message = "Use no máximo 255 caracteres no e-mail.")
        String email,

        // O BCrypt só considera os primeiros 72 bytes da senha.
        @NotBlank(message = "Informe a senha.")
        @Size(min = 6, max = 72, message = "A senha precisa ter de 6 a 72 caracteres.")
        String senha) {
}
