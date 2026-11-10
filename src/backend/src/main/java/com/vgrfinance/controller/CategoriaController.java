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

import com.vgrfinance.dto.CategoriaRequest;
import com.vgrfinance.dto.CategoriaResponse;
import com.vgrfinance.service.CategoriaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService servico;

    public CategoriaController(CategoriaService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<CategoriaResponse> listar() {
        return servico.listar();
    }

    @GetMapping("/{id}")
    public CategoriaResponse buscar(@PathVariable Integer id) {
        return servico.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaResponse criar(@Valid @RequestBody CategoriaRequest requisicao) {
        return servico.criar(requisicao);
    }

    @PutMapping("/{id}")
    public CategoriaResponse atualizar(@PathVariable Integer id, @Valid @RequestBody CategoriaRequest requisicao) {
        return servico.atualizar(id, requisicao);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Integer id) {
        servico.excluir(id);
    }
}
