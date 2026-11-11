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

import com.vgrfinance.dto.AporteRequest;
import com.vgrfinance.dto.MetaRequest;
import com.vgrfinance.dto.MetaResponse;
import com.vgrfinance.service.MetaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/metas")
public class MetaController {

    private final MetaService servico;

    public MetaController(MetaService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<MetaResponse> listar(@RequestParam(required = false) String mes) {
        return servico.listar(mes);
    }

    @GetMapping("/{id}")
    public MetaResponse buscar(@PathVariable Integer id, @RequestParam(required = false) String mes) {
        return servico.buscar(id, mes);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MetaResponse criar(@Valid @RequestBody MetaRequest requisicao) {
        return servico.criar(requisicao);
    }

    @PutMapping("/{id}")
    public MetaResponse atualizar(@PathVariable Integer id, @Valid @RequestBody MetaRequest requisicao) {
        return servico.atualizar(id, requisicao);
    }

    @PostMapping("/{id}/aportes")
    public MetaResponse aportar(@PathVariable Integer id, @Valid @RequestBody AporteRequest requisicao) {
        return servico.aportar(id, requisicao);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Integer id) {
        servico.excluir(id);
    }
}
