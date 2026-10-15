package com.riobranco.mouseomanos.produto_api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class ProdutoControllerTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private static final String VALIDO =
            "{\"categoria\":\"Mouse\",\"nome\":\"Mouse Gamer\",\"descricao\":\"RGB\",\"valor\":99.9}";

    @Test
    void criaProdutoValido() throws Exception {
        mvc.perform(post("/produtos").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void retorna400ParaCamposInvalidos() throws Exception {
        mvc.perform(post("/produtos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"valor\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes.nome").exists())
                .andExpect(jsonPath("$.detalhes.categoria").exists())
                .andExpect(jsonPath("$.detalhes.valor").exists());
    }

    @Test
    void retorna400ParaJsonMalformado() throws Exception {
        mvc.perform(post("/produtos").contentType(MediaType.APPLICATION_JSON).content("{nome:"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retorna400ParaIdNaoNumerico() throws Exception {
        mvc.perform(get("/produtos/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void retorna404ParaIdInexistente() throws Exception {
        mvc.perform(get("/produtos/99999")).andExpect(status().isNotFound());
        mvc.perform(put("/produtos/99999").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/produtos/99999")).andExpect(status().isNotFound());
    }

    @Test
    void retorna404ParaRotaInexistente() throws Exception {
        mvc.perform(get("/nao-existe")).andExpect(status().isNotFound());
    }

    @Test
    void retorna405ParaMetodoNaoPermitido() throws Exception {
        mvc.perform(patch("/produtos")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void retorna415ParaContentTypeNaoSuportado() throws Exception {
        mvc.perform(post("/produtos").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType());
    }
}
