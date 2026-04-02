# Cria o banco ressarcimento no container ressarcimento-sqlserver (docker compose).
# Aguarde ~20s apos "docker compose up -d" antes de executar.

$ErrorActionPreference = "Stop"
$pass = $env:MSSQL_SA_PASSWORD
if (-not $pass) { $pass = "YourStrong@Passw0rd" }

$sql = "IF DB_ID('ressarcimento') IS NULL CREATE DATABASE ressarcimento;"

function Test-SqlCmd {
    param([string]$SqlCmdPath)
    & docker exec ressarcimento-sqlserver $SqlCmdPath `
        -S localhost -U sa -P $pass -C -Q $sql 2>&1 | Out-Null
    return ($LASTEXITCODE -eq 0)
}

$paths = @(
    "/opt/mssql-tools18/bin/sqlcmd",
    "/opt/mssql-tools/bin/sqlcmd"
)

$ok = $false
foreach ($p in $paths) {
    if (Test-SqlCmd -SqlCmdPath $p) { $ok = $true; break }
}

if (-not $ok) {
    Write-Error "sqlcmd falhou. Verifique se o container ressarcimento-sqlserver esta rodando e a senha (MSSQL_SA_PASSWORD / compose)."
}

Write-Host "Banco ressarcimento criado ou ja existia."
