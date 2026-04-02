package br.com.empresa.ressarcimento.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ressarcimento")
public class RessarcimentoProperties {

    /**
     * Diretório com planilhas resumonf.xlsx (resumo de notas).
     */
    private String resumoNotasDir = "C:\\Users\\josue\\OneDrive\\Documentos\\TrabalhosExtras\\Ressarcimento\\Processamento\\Entrada\\resumo";

    /**
     * Diretório com arquivos texto EFD ICMS/IPI (SPED).
     */
    private String efdsDir = "C:\\Users\\josue\\OneDrive\\Documentos\\TrabalhosExtras\\Ressarcimento\\Processamento\\Entrada\\efds";

    /**
     * Diretório com XML de NF-e de entrada.
     */
    private String nfesDir = "C:\\Users\\josue\\OneDrive\\Documentos\\TrabalhosExtras\\Ressarcimento\\Processamento\\Entrada\\nfes";

    /**
     * Diretório com XML de NF-e de saída (Fluxo B).
     */
    private String nfesSaidaDir =
            "C:\\Users\\josue\\OneDrive\\Documentos\\TrabalhosExtras\\Ressarcimento\\Processamento\\Entrada\\nfes-saida";
}
