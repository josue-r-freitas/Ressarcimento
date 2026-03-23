package br.com.empresa.ressarcimento.declarante;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração que validam o cadastro de declarante pela API REST.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeclaranteApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String API_DECLARANTE = "/api/declarante";

    @Nested
    @DisplayName("POST /api/declarante - Cadastro e atualização")
    class Cadastro {

        @Test
        @DisplayName("Retorna 201 e dados ao cadastrar declarante válido")
        @Sql(scripts = "/declarante/clean-declarante.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        void post_comDadosValidos_retorna201eBody() throws Exception {
            String body = """
                    {
                        "cnpjRaiz": "12345678",
                        "ieContribuinteDeclarante": "12345678901",
                        "razaoSocial": "Empresa Teste LTDA",
                        "nomeResponsavel": "João da Silva",
                        "foneResponsavel": "92999991234",
                        "emailResponsavel": "joao@empresa.com"
                    }
                    """;

            mockMvc.perform(post(API_DECLARANTE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.cnpjRaiz").value("12345678"))
                    .andExpect(jsonPath("$.ieContribuinteDeclarante").value("12345678901"))
                    .andExpect(jsonPath("$.razaoSocial").value("Empresa Teste LTDA"))
                    .andExpect(jsonPath("$.nomeResponsavel").value("João da Silva"))
                    .andExpect(jsonPath("$.foneResponsavel").value("92999991234"))
                    .andExpect(jsonPath("$.emailResponsavel").value("joao@empresa.com"));
        }

        @Test
        @DisplayName("Após cadastrar, GET retorna os mesmos dados")
        @Sql(scripts = "/declarante/clean-declarante.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        void get_aposCadastro_retornaDadosSalvos() throws Exception {
            String body = """
                    {
                        "cnpjRaiz": "87654321",
                        "ieContribuinteDeclarante": "87654321",
                        "razaoSocial": "Outra Empresa SA",
                        "nomeResponsavel": "Maria Santos",
                        "foneResponsavel": "92988887654",
                        "emailResponsavel": "maria@outra.com"
                    }
                    """;

            mockMvc.perform(post(API_DECLARANTE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isCreated());

            mockMvc.perform(get(API_DECLARANTE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cnpjRaiz").value("87654321"))
                    .andExpect(jsonPath("$.razaoSocial").value("Outra Empresa SA"))
                    .andExpect(jsonPath("$.emailResponsavel").value("maria@outra.com"));
        }

        @Test
        @DisplayName("POST com mesmo CNPJ raiz atualiza o declarante existente")
        @Sql(scripts = "/declarante/clean-declarante.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        void post_comCnpjExistente_atualiza() throws Exception {
            String primeiro = """
                    {
                        "cnpjRaiz": "11112222",
                        "ieContribuinteDeclarante": "11112222",
                        "razaoSocial": "Razão Original",
                        "nomeResponsavel": "Fulano",
                        "foneResponsavel": "92911112222",
                        "emailResponsavel": "fulano@teste.com"
                    }
                    """;
            String atualizado = """
                    {
                        "cnpjRaiz": "11112222",
                        "ieContribuinteDeclarante": "11112222",
                        "razaoSocial": "Razão Atualizada",
                        "nomeResponsavel": "Fulano Atualizado",
                        "foneResponsavel": "92933334444",
                        "emailResponsavel": "fulano.novo@teste.com"
                    }
                    """;

            mockMvc.perform(post(API_DECLARANTE).contentType(MediaType.APPLICATION_JSON).content(primeiro))
                    .andExpect(status().isCreated());
            mockMvc.perform(post(API_DECLARANTE).contentType(MediaType.APPLICATION_JSON).content(atualizado))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.razaoSocial").value("Razão Atualizada"));

            mockMvc.perform(get(API_DECLARANTE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.razaoSocial").value("Razão Atualizada"))
                    .andExpect(jsonPath("$.emailResponsavel").value("fulano.novo@teste.com"));
        }
    }

    @Nested
    @DisplayName("POST /api/declarante - Validações")
    class Validacoes {

        @Test
        @DisplayName("Retorna 400 quando CNPJ raiz não tem 8 dígitos")
        void post_cnpjRaizInvalido_retorna400() throws Exception {
            String body = """
                    {
                        "cnpjRaiz": "123",
                        "ieContribuinteDeclarante": "12345678",
                        "razaoSocial": "Empresa",
                        "nomeResponsavel": "João",
                        "foneResponsavel": "92999999999",
                        "emailResponsavel": "joao@teste.com"
                    }
                    """;

            mockMvc.perform(post(API_DECLARANTE).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("validação")))
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("cnpjRaiz")));
        }

        @Test
        @DisplayName("Retorna 400 quando e-mail é inválido")
        void post_emailInvalido_retorna400() throws Exception {
            String body = """
                    {
                        "cnpjRaiz": "12345678",
                        "ieContribuinteDeclarante": "12345678",
                        "razaoSocial": "Empresa",
                        "nomeResponsavel": "João",
                        "foneResponsavel": "92999999999",
                        "emailResponsavel": "email-invalido"
                    }
                    """;

            mockMvc.perform(post(API_DECLARANTE).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("emailResponsavel")));
        }

        @Test
        @DisplayName("Retorna 400 quando campos obrigatórios estão vazios")
        void post_camposVazios_retorna400() throws Exception {
            String body = """
                    {
                        "cnpjRaiz": "",
                        "ieContribuinteDeclarante": "",
                        "razaoSocial": "",
                        "nomeResponsavel": "",
                        "foneResponsavel": "",
                        "emailResponsavel": ""
                    }
                    """;

            mockMvc.perform(post(API_DECLARANTE).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors").isArray())
                    .andExpect(jsonPath("$.fieldErrors.length()").value(greaterThanOrEqualTo(1)));
        }
    }

    @Nested
    @DisplayName("GET /api/declarante - Consulta")
    class Consulta {

        @Test
        @DisplayName("Retorna 404 quando não existe declarante cadastrado")
        @Sql(scripts = "/declarante/clean-declarante.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
        void get_semDeclarante_retorna404() throws Exception {
            mockMvc.perform(get(API_DECLARANTE))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").exists());
        }
    }
}
