package br.com.empresa.ressarcimento;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integração com SQL Server real (instância local ou rede), sem Docker/Testcontainers.
 *
 * <p>Desativado por omissão para {@code mvn test} não depender de SQL Server. Para executar:
 *
 * <ul>
 *   <li>{@code RESSARCIMENTO_SQLSERVER_INTEGRATION=true}
 *   <li>{@code RESSARCIMENTO_DB_URL}, {@code RESSARCIMENTO_DB_USERNAME}, {@code RESSARCIMENTO_DB_PASSWORD}
 *       (ou omitir URL e usar a base padrão {@code ressarcimento_integration} em localhost no YAML)
 * </ul>
 *
 * <p>Crie a base (ex.: {@code CREATE DATABASE ressarcimento_integration}) antes da primeira execução; o Flyway
 * aplica as migrações. Num pipeline futuro com Docker, pode voltar-se a usar Testcontainers em paralelo a este
 * perfil.
 */
@Tag("integration-sqlserver")
@EnabledIfEnvironmentVariable(named = "RESSARCIMENTO_SQLSERVER_INTEGRATION", matches = "true")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration-sqlserver")
class RessarcimentoSqlServerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Contexto sobe com SQL Server (Flyway + JPA)")
    void contextLoads() {
    }

    @Test
    @DisplayName("Cadastro e consulta de declarante contra SQL Server")
    @Sql(scripts = "/declarante/clean-declarante.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void declarante_cadastroEConsulta_comSqlServer() throws Exception {
        String body =
                """
                {
                    "cnpjRaiz": "99998888",
                    "ieContribuinteDeclarante": "99998888",
                    "razaoSocial": "Empresa Integracao SQL Server LTDA",
                    "nomeResponsavel": "Responsavel Teste",
                    "foneResponsavel": "92999990000",
                    "emailResponsavel": "teste@integracao.local"
                }
                """;

        mockMvc.perform(post("/api/declarante")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cnpjRaiz").value("99998888"))
                .andExpect(jsonPath("$.razaoSocial").value("Empresa Integracao SQL Server LTDA"));

        mockMvc.perform(get("/api/declarante"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnpjRaiz").value("99998888"));
    }
}
