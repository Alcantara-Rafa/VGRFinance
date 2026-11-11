package com.vgrfinance.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vgrfinance.dto.DiagnosticoResponse;
import com.vgrfinance.service.DiagnosticoService;

@RestController
@RequestMapping("/api/diagnosticos")
public class DiagnosticoController {

    private final DiagnosticoService servico;

    public DiagnosticoController(DiagnosticoService servico) {
        this.servico = servico;
    }

    @GetMapping
    public DiagnosticoResponse gerar(@RequestParam String mes) {
        return servico.gerar(mes);
    }
}
