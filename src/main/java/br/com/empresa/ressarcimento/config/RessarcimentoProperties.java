package br.com.empresa.ressarcimento.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ressarcimento")
public class RessarcimentoProperties {

    /**
     * Diretório com planilhas resumonf.xlsx (resumo de notas).
     */
    private String resumoNotasDir = "";

    /**
     * Diretório com arquivos texto EFD ICMS/IPI (SPED).
     */
    private String efdsDir = "";

    /**
     * Diretório com XML de NF-e de entrada.
     */
    private String nfesDir = "";
}
