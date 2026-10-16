param(
    [ValidateRange(1, 65535)][int]$Porta = 8080,
    [switch]$SomenteCompilar,
    [switch]$NaoAbrirNavegador
)

$ErrorActionPreference = 'Stop'
$projectDir = $PSScriptRoot
$runtimeDir = Join-Path $projectDir '.runtime-tmp'
$jarPath = Join-Path $projectDir 'target\produto-api-0.0.1-SNAPSHOT.jar'
$pidPath = Join-Path $runtimeDir 'servidor.pid'
$url = "http://127.0.0.1:$Porta"

try {
    # Prefer Java 17, independently of the Java selected in the Windows PATH.
    $candidates = @()
    foreach ($base in @("$env:ProgramFiles\Java", "$env:ProgramFiles\Eclipse Adoptium", "$env:ProgramFiles\Microsoft")) {
        if (Test-Path -LiteralPath $base) {
            $candidates += Get-ChildItem -LiteralPath $base -Directory |
                Sort-Object @{ Expression = { if ($_.Name -match '^jdk-?17(?:\D|$)') { 0 } else { 1 } } }, Name |
                Select-Object -ExpandProperty FullName
        }
    }
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    $javaCommand = Get-Command java.exe -CommandType Application -ErrorAction SilentlyContinue
    if ($javaCommand) { $candidates += Split-Path (Split-Path $javaCommand.Source) }

    $jdkDir = $null
    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        $releasePath = Join-Path $candidate 'release'
        if (!(Test-Path -LiteralPath (Join-Path $candidate 'bin\javac.exe')) -or !(Test-Path -LiteralPath $releasePath)) { continue }
        $version = Select-String -LiteralPath $releasePath -Pattern '^JAVA_VERSION="(\d+)'
        if ($version -and [int]$version.Matches[0].Groups[1].Value -ge 17) {
            $jdkDir = $candidate
            break
        }
    }
    if (!$jdkDir) { throw 'JDK 17 ou superior nao encontrado. Instale um JDK ou configure JAVA_HOME.' }

    New-Item -ItemType Directory -Path $runtimeDir -Force | Out-Null
    if (!$SomenteCompilar -and (Get-NetTCPConnection -LocalPort $Porta -State Listen -ErrorAction SilentlyContinue)) {
        throw "A porta $Porta ja esta em uso. Se este projeto estiver rodando, abra $url ou execute parar.cmd antes de iniciar novamente."
    }
    $env:JAVA_HOME = $jdkDir
    $env:Path = "$jdkDir\bin;$env:Path"
    $env:MAVEN_USER_HOME = Join-Path $projectDir '.maven-cache'
    Push-Location $projectDir
    try {
        Write-Host "Compilando e testando com $jdkDir ..."
        & (Join-Path $projectDir 'mvnw.cmd') -B -ntp "-Dmaven.repo.local=$projectDir\.maven-cache\repository" package
        if ($LASTEXITCODE -ne 0) { throw 'Falha na compilacao. Consulte as mensagens acima.' }
    } finally {
        Pop-Location
    }
    if ($SomenteCompilar) { exit 0 }

    $server = Start-Process -FilePath (Join-Path $jdkDir 'bin\java.exe') -ArgumentList @(
        "`"-Djava.io.tmpdir=$runtimeDir`"", '-jar', "`"$jarPath`"", "--server.port=$Porta", '--server.address=127.0.0.1'
    ) -WorkingDirectory $projectDir -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtimeDir 'servidor.log') -RedirectStandardError (Join-Path $runtimeDir 'servidor-erros.log')
    Set-Content -LiteralPath $pidPath -Value $server.Id -Encoding ASCII
    $deadline = (Get-Date).AddSeconds(60)
    do {
        $server.Refresh()
        if ($server.HasExited) { throw "O servidor encerrou. Consulte $runtimeDir\servidor.log e servidor-erros.log." }
        try {
            $response = Invoke-WebRequest -Uri "$url/produtos" -UseBasicParsing -TimeoutSec 2
            if ($response.StatusCode -eq 200) {
                Write-Host "X-Squad rodando em $url (PID $($server.Id))."
                Write-Host 'Para encerrar, execute parar.cmd. O cardapio e os pedidos ficam salvos na pasta data.'
                if (!$NaoAbrirNavegador) { Start-Process $url }
                exit 0
            }
        } catch { }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)
    throw "O servidor nao respondeu a tempo. Consulte $runtimeDir\servidor.log."
} catch {
    if ($server -and !$server.HasExited) { Stop-Process -Id $server.Id -ErrorAction SilentlyContinue }
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}
