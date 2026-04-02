# Ajuda a diagnosticar porque a app nao liga ao SQL Server no Windows.
# Execute: .\scripts\sqlserver\diagnostico-sqlserver.ps1

Write-Host "=== Servicos SQL Server (deve haver pelo menos um 'SQL Server (XXX)' Iniciado) ===" 
Get-Service -Name "*SQL*" -ErrorAction SilentlyContinue |
    Sort-Object DisplayName |
    Format-Table -AutoSize Status, Name, DisplayName

Write-Host "`n=== Porta 1433 (instancia predefinida / TCP fixo) ===" 
$t = Test-NetConnection -ComputerName localhost -Port 1433 -WarningAction SilentlyContinue
Write-Host "TcpTestSucceeded:" $t.TcpTestSucceeded

Write-Host "`n=== SQL Server Browser (UDP 1434) — necessario para instanceName=SQLEXPRESS com porta dinamica ===" 
$browser = Get-Service "SQLBrowser" -ErrorAction SilentlyContinue
if ($browser) {
    Write-Host "SQLBrowser status:" $browser.Status
    if ($browser.Status -ne "Running") {
        Write-Host "AVISO: Inicie o servico 'SQL Server Browser' se usar SQLEXPRESS sem porta fixa no JDBC."
    }
} else {
    Write-Host "Servico SQLBrowser nao encontrado (instalacao minima?)."
}

Write-Host "`n=== Dica ===" 
Write-Host "Express (SQLEXPRESS): perfil spring 'local' + Browser em execucao, ou fixe TCP na porta em Configuration Manager e use localhost,PORTA no RESSARCIMENTO_DB_URL."
Write-Host "Instancia predefinida: ative TCP/IP na porta 1433 e confirme Test-NetConnection acima = True."
