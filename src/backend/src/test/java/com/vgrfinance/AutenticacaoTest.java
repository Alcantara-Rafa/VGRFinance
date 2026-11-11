package com.vgrfinance;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.vgrfinance.model.Usuario;
import com.vgrfinance.repository.UsuarioRepository;

/** Login de ponta a ponta: sessão, 401 sem login e dados separados por usuário. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AutenticacaoTest {

    private static final String EMAIL = "autenticacao@vgr.local";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private PasswordEncoder codificador;

    // Senha aleatória a cada execução: nenhuma senha fica escrita no código.
    private final String senha = UUID.randomUUID().toString();

    @BeforeEach
    void criarUsuario() {
        usuarios.save(new Usuario("Autenticação", EMAIL, codificador.encode(senha)));
    }

    @Test
    void semLoginAApiResponde401() throws Exception {
        mvc.perform(get("/api/categorias")).andExpect(status().isUnauthorized());
    }

    @Test
    void senhaErradaResponde401ComMensagem() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais(EMAIL, "outra-" + senha)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos."));
    }

    @Test
    void loginAbreSessaoECadaUsuarioVeSoSeusDados() throws Exception {
        MockHttpSession sessao = (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais(EMAIL, senha)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Autenticação"))
                .andReturn().getRequest().getSession();

        // O usuário novo não enxerga as categorias do seed, que são de outro usuário.
        mvc.perform(get("/api/categorias").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mvc.perform(get("/api/auth/eu").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    private static String credenciais(String email, String segredo) {
        return "{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, segredo);
    }
}
