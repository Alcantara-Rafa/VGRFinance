package com.vgrfinance.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vgrfinance.dto.AjusteItemRequest;
import com.vgrfinance.dto.OrcamentoRequest;
import com.vgrfinance.dto.OrcamentoResponse;
import com.vgrfinance.service.OrcamentoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orcamentos")
public class OrcamentoController {

    private final OrcamentoService servico;

    public OrcamentoController(OrcamentoService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<OrcamentoResponse> listar() {
        return servico.listar();
    }

    @GetMapping(params = "mes")
    public OrcamentoResponse buscarPorMes(@RequestParam String mes) {
        return servico.buscarPorMes(mes);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrcamentoResponse criar(@Valid @RequestBody OrcamentoRequest requisicao) {
        return servico.criar(requisicao);
    }

    @PutMapping("/{id}/itens/{categoriaId}")
    public OrcamentoResponse ajustarItem(@PathVariable Integer id, @PathVariable Integer categoriaId,
                                         @Valid @RequestBody AjusteItemRequest requisicao) {
        return servico.ajustarItem(id, categoriaId, requisicao);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Integer id) {
        servico.excluir(id);
    }
}
