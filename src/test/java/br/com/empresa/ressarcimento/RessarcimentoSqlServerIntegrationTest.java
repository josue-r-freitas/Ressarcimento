package br.com.empresa.ressarcimento;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração contra SQL Server real via Testcontainers.
 * Valida que Flyway, JPA e a API funcionam com o banco de produção.
 * Requer Docker em execução. Para pular: mvn test -DexcludedGroups=testcontainers
 */
@Tag("testcontainers")
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("testcontainers")
class RessarcimentoSqlServerIntegrationTest {

    private static final DockerImageName MSSQL_IMAGE =
            DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-latest");

    @Container
    @org.springframework.boot.testcontainers.service.connection.ServiceConnection
    static MSSQLServerContainer<?> sqlserver = new MSSQLServerContainer<>(MSSQL_IMAGE)
            .acceptLicense();

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
        String body = """
                {
                    "cnpjRaiz": "99998888",
                    "ieContribuinteDeclarante": "99998888",
                    "razaoSocial": "Empresa Testcontainers LTDA",
                    "nomeResponsavel": "Responsavel Teste",
                    "foneResponsavel": "92999990000",
                    "emailResponsavel": "teste@testcontainers.com"
                }
                """;

        mockMvc.perform(post("/api/declarante")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cnpjRaiz").value("99998888"))
                .andExpect(jsonPath("$.razaoSocial").value("Empresa Testcontainers LTDA"));

        mockMvc.perform(get("/api/declarante"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnpjRaiz").value("99998888"));
    }
}
