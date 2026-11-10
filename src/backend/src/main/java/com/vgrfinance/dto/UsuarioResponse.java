package com.vgrfinance.dto;

import com.vgrfinance.model.Papel;

public record UsuarioResponse(Integer id, String nome, String email, Papel papel) {
}
