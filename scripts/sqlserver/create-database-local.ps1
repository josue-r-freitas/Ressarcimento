# Cria o banco ressarcimento no SQL Server instalado na maquina (sem Docker).
# Requisitos: sqlcmd no PATH (instale "SQL Server Command Line Utilities" ou SSMS inclui ferramentas)
# e autenticacao SQL (ex.: login sa ou outro utilizador com permissao para criar BD).
#
# Defina no .env ou no ambiente antes de executar:
#   RESSARCIMENTO_DB_USERNAME, RESSARCIMENTO_DB_PASSWORD
# Opcional: SQLCMD_SERVER — ex. localhost, localhost\SQLEXPRESS, ou localhost,1433

$ErrorActionPreference = "Stop"

$server = $env:SQLCMD_SERVER
if (-not $server) { $server = "localhost" }

$user = $env:RESSARCIMENTO_DB_USERNAME
if (-not $user) { $user = "app_elementar" }

$pass = $env:RESSARCIMENTO_DB_PASSWORD
if (-not $pass) {
    Write-Error "Defina RESSARCIMENTO_DB_PASSWORD no ambiente ou num ficheiro .env carregado antes de correr este script."
}

$sql = "IF DB_ID('ressarcimento') IS NULL CREATE DATABASE ressarcimento;"

$sqlcmd = Get-Command sqlcmd -ErrorAction SilentlyContinue
if (-not $sqlcmd) {
    Write-Error "sqlcmd nao encontrado no PATH. Instale as ferramentas de linha de comando do SQL Server ou use o SSMS para executar: $sql"
}

# -C = confiar no certificado do servidor (util em desenvolvimento local com encrypt)
& sqlcmd -S $server -U $user -P $pass -C -Q $sql
if ($LASTEXITCODE -ne 0) {
    Write-Error "sqlcmd falhou (codigo $LASTEXITCODE). Verifique SQLCMD_SERVER, credenciais, TCP/IP ativo e instancia correta."
}

Write-Host "Banco ressarcimento criado ou ja existia."
