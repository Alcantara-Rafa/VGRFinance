package com.vgrfinance.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vgrfinance.dto.AcompanhamentoResponse;
import com.vgrfinance.service.AcompanhamentoService;

@RestController
@RequestMapping("/api/acompanhamento")
public class AcompanhamentoController {

    private final AcompanhamentoService servico;

    public AcompanhamentoController(AcompanhamentoService servico) {
        this.servico = servico;
    }

    @GetMapping
    public AcompanhamentoResponse porMes(@RequestParam String mes) {
        return servico.consultarPorMes(mes);
    }
}
