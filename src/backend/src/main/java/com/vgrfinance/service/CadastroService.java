package com.vgrfinance.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vgrfinance.dto.CadastroRequest;
import com.vgrfinance.exception.RegraDeNegocioException;
import com.vgrfinance.model.Usuario;
import com.vgrfinance.repository.CategoriaRepository;
import com.vgrfinance.repository.UsuarioRepository;

/** Cria a conta e, na mesma transação, as categorias padrão dela. */
@Service
public class CadastroService {

    static final String MSG_EMAIL_REPETIDO = "Já existe uma conta com este e-mail.";

    private final UsuarioRepository usuarios;
    private final CategoriaRepository categorias;
    private final PasswordEncoder codificador;

    public CadastroService(UsuarioRepository usuarios, CategoriaRepository categorias, PasswordEncoder codificador) {
        this.usuarios = usuarios;
        this.categorias = categorias;
        this.codificador = codificador;
    }

    @Transactional
    public Usuario cadastrar(CadastroRequest requisicao) {
        String email = requisicao.email().strip();
        if (usuarios.findByEmailIgnoreCase(email).isPresent()) {
            throw new RegraDeNegocioException(MSG_EMAIL_REPETIDO);
        }
        Usuario usuario;
        try {
            // flush agora: se dois cadastros com o mesmo e-mail chegarem juntos, o índice único
            // LOWER(email) recusa o segundo aqui, com a mensagem certa em vez de erro 500.
            usuario = usuarios.saveAndFlush(
                    new Usuario(requisicao.nome().strip(), email, codificador.encode(requisicao.senha())));
        } catch (DataIntegrityViolationException e) {
            throw new RegraDeNegocioException(MSG_EMAIL_REPETIDO);
        }
        categorias.saveAll(CategoriasPadrao.para(usuario));
        return usuario;
    }
}
