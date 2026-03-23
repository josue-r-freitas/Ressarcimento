package br.com.empresa.ressarcimento.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistema de Ressarcimento ICMS-ST – SEFAZ/AM")
                        .description("API REST para geração de XMLs de ressarcimento (produtos MATRI-NAC e pedidos versão 2.00). "
                                + "Cadastro de declarante, importação de planilhas e geração de arquivos para envio ao DT-e.")
                        .version("1.0"));
    }
}
