# Testa login SQL com as mesmas credenciais que a aplicacao (ressarcimento.env / .env na raiz do projeto).
# Uso: na raiz do repo, .\scripts\sqlserver\testar-login.ps1
# Se isto falhar, o SSMS tambem falhara com autenticacao SQL — corrija a senha ou o login no SQL Server.

$ErrorActionPreference = "Stop"
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (-not (Test-Path (Join-Path $root "pom.xml"))) {
    $root = Split-Path $PSScriptRoot -Parent
}

function Import-EnvFile {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) { return }
    Get-Content -LiteralPath $Path -Encoding UTF8 | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq "" -or $line.StartsWith("#")) { return }
        $eq = $line.IndexOf("=")
        if ($eq -lt 1) { return }
        $k = $line.Substring(0, $eq).Trim()
        $v = $line.Substring($eq + 1).Trim()
        if ($v.Length -ge 2 -and (($v.StartsWith('"') -and $v.EndsWith('"')) -or ($v.StartsWith("'") -and $v.EndsWith("'")))) {
            $v = $v.Substring(1, $v.Length - 2)
        }
        Set-Item -Path "env:$k" -Value $v
    }
}

Import-EnvFile (Join-Path $root "ressarcimento.env")
Import-EnvFile (Join-Path $root ".env")

function Get-SqlCmdServerFromJdbcUrl {
    param([string]$Url)
    if (-not $Url) { return $null }
    $m = [regex]::Match($Url, 'jdbc:sqlserver://([^;]+)', [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
    if (-not $m.Success) { return $null }
    $hostPart = $m.Groups[1].Value.Trim()
    $im = [regex]::Match($Url, 'instanceName=([^;]+)', [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
    if ($im.Success) {
        $inst = $im.Groups[1].Value.Trim()
        return "$hostPart\$inst"
    }
    return $hostPart
}

$user = $env:RESSARCIMENTO_DB_USERNAME
if (-not $user) { $user = "app_elementar" }
$pass = $env:RESSARCIMENTO_DB_PASSWORD
$server = $env:SQLCMD_SERVER
if (-not $server) {
    $server = Get-SqlCmdServerFromJdbcUrl -Url $env:RESSARCIMENTO_DB_URL
}
if (-not $server) { $server = "localhost\SQLEXPRESS" }

if (-not $pass) {
    Write-Error "Defina RESSARCIMENTO_DB_PASSWORD em ressarcimento.env ou .env na raiz: $root"
}

$sqlcmd = Get-Command sqlcmd -ErrorAction SilentlyContinue
if (-not $sqlcmd) {
    Write-Error "sqlcmd nao encontrado. Instale as ferramentas de linha de comando do SQL Server."
}

Write-Host "A testar: servidor=$server utilizador=$user (senha com $($pass.Length) caracteres)"
& sqlcmd -S $server -U $user -P $pass -C -Q "SELECT 1 AS ok;"
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "Dica: execute como sa o script scripts\sqlserver\reset-login-app-elementar.sql (troque a senha no SQL e no ressarcimento.env)."
    Write-Error "Login falhou (codigo $LASTEXITCODE). Servidor testado: $server"
}
Write-Host "Login OK."
