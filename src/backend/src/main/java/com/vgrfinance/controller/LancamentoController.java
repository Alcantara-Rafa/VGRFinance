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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vgrfinance.dto.LancamentoRequest;
import com.vgrfinance.dto.LancamentoResponse;
import com.vgrfinance.service.LancamentoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/lancamentos")
public class LancamentoController {

    private final LancamentoService servico;

    public LancamentoController(LancamentoService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<LancamentoResponse> listar() {
        return servico.listar();
    }

    @GetMapping("/{id}")
    public LancamentoResponse buscar(@PathVariable Integer id) {
        return servico.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LancamentoResponse criar(@Valid @RequestBody LancamentoRequest requisicao) {
        return servico.criar(requisicao);
    }

    @PutMapping("/{id}")
    public LancamentoResponse atualizar(@PathVariable Integer id, @Valid @RequestBody LancamentoRequest requisicao) {
        return servico.atualizar(id, requisicao);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Integer id) {
        servico.excluir(id);
    }
}
