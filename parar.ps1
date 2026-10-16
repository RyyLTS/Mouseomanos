$ErrorActionPreference = 'Stop'
$pidPath = Join-Path $PSScriptRoot '.runtime-tmp\servidor.pid'
$jarPath = Join-Path $PSScriptRoot 'target\produto-api-0.0.1-SNAPSHOT.jar'

try {
    if (!(Test-Path -LiteralPath $pidPath)) {
        Write-Host 'Nenhum servidor iniciado por iniciar.cmd foi registrado.'
        exit 0
    }
    $serverId = [int](Get-Content -LiteralPath $pidPath -Raw).Trim()
    $server = Get-CimInstance Win32_Process -Filter "ProcessId = $serverId"
    if ($server) {
        if ($server.Name -ne 'java.exe' -or !$server.CommandLine.Contains('"' + $jarPath + '"')) {
            throw 'O PID registrado pertence a outro processo. Nenhum processo foi encerrado.'
        }
        Stop-Process -Id $serverId
        Write-Host 'X-Squad encerrado. Os dados salvos na pasta data foram preservados.'
    } else {
        Write-Host 'O servidor ja estava encerrado.'
    }
    Remove-Item -LiteralPath $pidPath
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}
