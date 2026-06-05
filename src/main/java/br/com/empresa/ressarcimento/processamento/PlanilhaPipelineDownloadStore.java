package br.com.empresa.ressarcimento.processamento;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Armazena temporariamente o XLSX gerado no pipeline (Fluxo A) na sessão HTTP para download one-shot.
 */
@Component
public class PlanilhaPipelineDownloadStore {

    private static final String SESSION_KEY_PREFIX = "planilhaPipelineDownload.";

    public void registrar(HttpSession session, long processamentoId, byte[] xlsx) {
        if (session == null || xlsx == null || xlsx.length == 0) {
            return;
        }
        session.setAttribute(chave(processamentoId), xlsx);
    }

    public Optional<byte[]> consumir(HttpSession session, long processamentoId) {
        if (session == null) {
            return Optional.empty();
        }
        String key = chave(processamentoId);
        Object value = session.getAttribute(key);
        session.removeAttribute(key);
        if (value instanceof byte[] bytes && bytes.length > 0) {
            return Optional.of(bytes);
        }
        return Optional.empty();
    }

    private static String chave(long processamentoId) {
        return SESSION_KEY_PREFIX + processamentoId;
    }
}
