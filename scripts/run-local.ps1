# Carrega .env na raiz do projeto e executa mvn spring-boot:run.
# Uso: .\scripts\run-local.ps1
# Argumentos extra sao repassados ao Maven, ex.: .\scripts\run-local.ps1 -DskipTests

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$envFileDot = Join-Path $root ".env"
$envFileNamed = Join-Path $root "ressarcimento.env"

function Import-DotEnv {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) { return }
    Get-Content -LiteralPath $Path -Encoding UTF8 | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq "" -or $line.StartsWith("#")) { return }
        $eq = $line.IndexOf("=")
        if ($eq -lt 1) { return }
        $key = $line.Substring(0, $eq).Trim()
        $val = $line.Substring($eq + 1).Trim()
        if (
            ($val.Length -ge 2 -and $val.StartsWith('"') -and $val.EndsWith('"')) -or
            ($val.Length -ge 2 -and $val.StartsWith("'") -and $val.EndsWith("'"))
        ) {
            $val = $val.Substring(1, $val.Length - 2)
        }
        Set-Item -Path "env:$key" -Value $val
    }
}

# ressarcimento.env primeiro, depois .env (este sobrescreve chaves repetidas)
Import-DotEnv -Path $envFileNamed
Import-DotEnv -Path $envFileDot
if (Test-Path -LiteralPath $envFileDot) {
    Write-Host "Carregado: $envFileDot"
} elseif (Test-Path -LiteralPath $envFileNamed) {
    Write-Host "Carregado: $envFileNamed"
} else {
    Write-Host "Aviso: nem .env nem ressarcimento.env em $root (variaveis do sistema / application.yml serao usadas)."
}

Push-Location $root
try {
    & mvn spring-boot:run @args
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
