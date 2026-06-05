# Configuração e variáveis de ambiente

## Banco de dados (SQL Server)

| Variável | Descrição |
|----------|-----------|
| `RESSARCIMENTO_DB_URL` | JDBC completo (host, instância, `databaseName`, `encrypt`, etc.) |
| `RESSARCIMENTO_DB_USERNAME` | Utilizador da aplicação |
| `RESSARCIMENTO_DB_PASSWORD` | Senha (não versionar em ficheiros reais) |

O Spring Boot lê `spring.datasource.*` a partir destas chaves quando configurado (por exemplo via `ressarcimento.env` na raiz do projeto ou `.env`). Ver comentários em `ressarcimento.env.example`.

## Pastas dos fluxos automáticos

| Variável | Uso |
|----------|-----|
| `RESSARCIMENTO_RESUMO_NOTAS_DIR` | Diretório com `resumonf` (.xlsx) |
| `RESSARCIMENTO_EFDS_DIR` | EFD (pipe) |
| `RESSARCIMENTO_NFES_DIR` | XMLs de NF-e de **entrada** |
| `RESSARCIMENTO_NFES_SAIDA_DIR` | XMLs de NF-e de **saída** (Fluxo B) |

Valores por defeito em `application.yml` apontam para `Ressarcimento-midia/Processamento/Entrada/...` (dados fora do repositório Git). Sobrescreva com `RESSARCIMENTO_*_DIR` se a pasta estiver noutro local.

## Logs

- Padrão: linha de texto com MDC `idProcessamento` quando definido (`logging.pattern.console` em `application.yml`).
- JSON: ative o perfil Spring **`json-logging`** (ex.: `SPRING_PROFILES_ACTIVE=json-logging`). Requer `logstash-logback-encoder` (já no `pom.xml`).
