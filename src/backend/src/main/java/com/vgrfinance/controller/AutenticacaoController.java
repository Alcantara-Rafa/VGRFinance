package com.vgrfinance.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vgrfinance.dto.LoginRequest;
import com.vgrfinance.dto.UsuarioResponse;
import com.vgrfinance.seguranca.UsuarioAutenticado;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/** Entrar e consultar quem está logado. Sair é feito pelo filtro de logout (POST /api/auth/logout). */
@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {

    private final AuthenticationManager gerenciador;
    private final SecurityContextRepository contextos;

    public AutenticacaoController(AuthenticationManager gerenciador, SecurityContextRepository contextos) {
        this.gerenciador = gerenciador;
        this.contextos = contextos;
    }

    /** Carregar o token faz o Spring gravar o cookie XSRF-TOKEN, que o front-end devolve no cabeçalho. */
    @GetMapping("/csrf")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void csrf(CsrfToken token) {
        token.getToken();
    }

    @PostMapping("/login")
    public UsuarioResponse entrar(@Valid @RequestBody LoginRequest requisicao,
                                  HttpServletRequest request, HttpServletResponse response) {
        Authentication autenticacao = gerenciador.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(requisicao.email(), requisicao.senha()));

        // Troca o id da sessão no login: quem tinha o id antigo não herda o acesso.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacao);
        SecurityContextHolder.setContext(contexto);
        contextos.saveContext(contexto, request, response);

        return paraResposta((UsuarioAutenticado) autenticacao.getPrincipal());
    }

    @GetMapping("/eu")
    public UsuarioResponse eu(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return paraResposta(usuario);
    }

    private static UsuarioResponse paraResposta(UsuarioAutenticado usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getUsername());
    }
}
