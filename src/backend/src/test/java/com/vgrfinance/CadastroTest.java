package com.vgrfinance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.vgrfinance.model.TipoCriacao;
import com.vgrfinance.repository.CategoriaRepository;
import com.vgrfinance.repository.UsuarioRepository;

/** Cadastro de ponta a ponta: conta, sessão aberta, categorias padrão e as travas delas. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CadastroTest {

    private static final String EMAIL = "cadastro@vgr.local";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private CategoriaRepository categorias;

    private final String senha = UUID.randomUUID().toString();

    @Test
    void cadastroCriaContaJaLogadaComAsSeteCategoriasPadrao() throws Exception {
        MockHttpSession sessao = cadastrar(EMAIL);

        mvc.perform(get("/api/auth/eu").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Pessoa Nova"))
                .andExpect(jsonPath("$.email").value(EMAIL));

        mvc.perform(get("/api/categorias").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[*].nome").value(containsInAnyOrder(
                        "Moradia", "Alimentação", "Transporte", "Saúde", "Lazer", "Delivery", "Assinaturas")))
                .andExpect(jsonPath("$[*].tipoCriacao").value(everyItem(is("PADRAO"))));

        Integer id = usuarios.findByEmailIgnoreCase(EMAIL).orElseThrow().getId();
        assertThat(categorias.findByUsuarioIdOrderByNomeAsc(id))
                .allMatch(c -> c.getTipoCriacao() == TipoCriacao.PADRAO);
    }

    @Test
    void emailRepetidoMesmoComOutraCaixaResponde409() throws Exception {
        cadastrar(EMAIL);

        mvc.perform(post("/api/auth/cadastro").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Outra Pessoa", EMAIL.toUpperCase(), senha)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe uma conta com este e-mail."));
    }

    @Test
    void senhaCurtaResponde400() throws Exception {
        mvc.perform(post("/api/auth/cadastro").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Pessoa Nova", EMAIL, "123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A senha precisa ter de 6 a 72 caracteres."));
    }

    @Test
    void cadastroSemCsrfEhRecusado() throws Exception {
        mvc.perform(post("/api/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Pessoa Nova", EMAIL, senha)))
                .andExpect(status().isForbidden());
    }

    @Test
    void categoriaPadraoSoMudaOEssencialECustomizadaAceitaTudo() throws Exception {
        MockHttpSession sessao = cadastrar(EMAIL);
        Integer usuarioId = usuarios.findByEmailIgnoreCase(EMAIL).orElseThrow().getId();
        Integer lazer = categorias.findByUsuarioIdOrderByNomeAsc(usuarioId).stream()
                .filter(c -> c.getNome().equals("Lazer")).findFirst().orElseThrow().getId();

        mvc.perform(put("/api/categorias/{id}", lazer).session(sessao).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Lazer\",\"essencial\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.essencial").value(true));

        mvc.perform(put("/api/categorias/{id}", lazer).session(sessao).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Diversão\",\"essencial\":true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("O nome de uma categoria padrão não pode ser alterado."));

        mvc.perform(delete("/api/categorias/{id}", lazer).session(sessao).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Categorias padrão não podem ser excluídas."));

        String criada = mvc.perform(post("/api/categorias").session(sessao).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Pets\",\"essencial\":false}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoCriacao").value("CUSTOMIZADA"))
                .andReturn().getResponse().getContentAsString();
        Integer pets = JsonPath.read(criada, "$.id");

        mvc.perform(put("/api/categorias/{id}", pets).session(sessao).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Animais\",\"essencial\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Animais"));

        mvc.perform(delete("/api/categorias/{id}", pets).session(sessao).with(csrf()))
                .andExpect(status().is2xxSuccessful());
    }

    private MockHttpSession cadastrar(String email) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/cadastro").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("  Pessoa Nova ", email, senha)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Pessoa Nova"))
                .andReturn().getRequest().getSession();
    }

    private static String corpo(String nome, String email, String segredo) {
        return "{\"nome\":\"%s\",\"email\":\"%s\",\"senha\":\"%s\"}".formatted(nome, email, segredo);
    }
}
