[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateSet("run", "stop")]
    [string] $Action = "run",

    [Alias("bg")]
    [switch] $Background,

    [switch] $Fresh
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$Logs = Join-Path $Root ".dev-logs"
$BackendPort = 8080
$FrontendPort = 3000
$BackendLog = Join-Path $Logs "backend.log"
$FrontendLog = Join-Path $Logs "frontend.log"
$PowerShellExe = (Get-Process -Id $PID).Path

New-Item -ItemType Directory -Path $Logs -Force | Out-Null

function Write-Info([string] $Message) {
    Write-Host "[dev] $Message" -ForegroundColor Cyan
}

function Write-Ok([string] $Message) {
    Write-Host "[dev] $Message" -ForegroundColor Green
}

function Stop-WithError([string] $Message) {
    Write-Host "[dev] $Message" -ForegroundColor Red
    exit 1
}

function Get-RequiredCommand([string] $Name) {
    $Command = Get-Command $Name -ErrorAction SilentlyContinue
    if (-not $Command) {
        Stop-WithError "Missing command '$Name'. Install it and run this script again."
    }
    return $Command.Source
}

function Get-NativeCommandOutput([string] $FilePath, [string] $Arguments) {
    # Windows PowerShell 5.1 turns a native program's stderr into NativeCommandError
    # when ErrorActionPreference is Stop. Java intentionally prints its version to
    # stderr, so capture both streams through ProcessStartInfo instead.
    $StartInfo = [Diagnostics.ProcessStartInfo]::new()
    $StartInfo.FileName = $FilePath
    $StartInfo.Arguments = $Arguments
    $StartInfo.UseShellExecute = $false
    $StartInfo.CreateNoWindow = $true
    $StartInfo.RedirectStandardOutput = $true
    $StartInfo.RedirectStandardError = $true

    $Process = [Diagnostics.Process]::new()
    $Process.StartInfo = $StartInfo
    try {
        [void] $Process.Start()
        $StandardOutput = $Process.StandardOutput.ReadToEnd()
        $StandardError = $Process.StandardError.ReadToEnd()
        $Process.WaitForExit()
        return $StandardOutput + $StandardError
    }
    finally {
        $Process.Dispose()
    }
}

function Invoke-NativeCommandToLog(
    [string] $FilePath,
    [string[]] $ArgumentList,
    [string] $WorkingDirectory,
    [string] $LogPath
) {
    # Run .cmd tools through cmd.exe and capture their streams without passing
    # stderr through PowerShell 5.1's NativeCommandError conversion.
    $QuotedArguments = ($ArgumentList | ForEach-Object {
        '"' + $_.Replace('"', '\"') + '"'
    }) -join " "

    $StartInfo = [Diagnostics.ProcessStartInfo]::new()
    $StartInfo.FileName = $env:ComSpec
    $StartInfo.Arguments = "/d /s /c `"`"$FilePath`" $QuotedArguments`""
    $StartInfo.WorkingDirectory = $WorkingDirectory
    $StartInfo.UseShellExecute = $false
    $StartInfo.CreateNoWindow = $true
    $StartInfo.RedirectStandardOutput = $true
    $StartInfo.RedirectStandardError = $true

    $Process = [Diagnostics.Process]::new()
    $Process.StartInfo = $StartInfo
    try {
        [void] $Process.Start()
        $StandardOutputTask = $Process.StandardOutput.ReadToEndAsync()
        $StandardErrorTask = $Process.StandardError.ReadToEndAsync()
        $Process.WaitForExit()
        [Threading.Tasks.Task]::WaitAll(@($StandardOutputTask, $StandardErrorTask))
        [IO.File]::WriteAllText(
            $LogPath,
            $StandardOutputTask.Result + $StandardErrorTask.Result,
            [Text.UTF8Encoding]::new($false)
        )
        return $Process.ExitCode
    }
    finally {
        $Process.Dispose()
    }
}

function Get-PortProcessIds([int] $Port) {
    try {
        return @(
            Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction Stop |
                Select-Object -ExpandProperty OwningProcess -Unique
        )
    }
    catch {
        return @()
    }
}

function Stop-ProcessTree([int] $ProcessId) {
    if (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue) {
        & taskkill.exe /PID $ProcessId /T /F 2>$null | Out-Null
    }
}

function Stop-One([string] $Name, [int] $Port) {
    $PidFile = Join-Path $Logs "$Name.pid"
    if (Test-Path $PidFile) {
        $SavedPid = 0
        [void][int]::TryParse((Get-Content $PidFile -Raw).Trim(), [ref] $SavedPid)
        if ($SavedPid -gt 0 -and (Get-Process -Id $SavedPid -ErrorAction SilentlyContinue)) {
            Write-Info "Stopping old $Name process tree (PID $SavedPid)"
            Stop-ProcessTree $SavedPid
        }
        Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
    }

    # Maven/Next can briefly outlive their PowerShell wrapper while Windows
    # reparents child processes. Retry the port cleanup so a late child cannot
    # remain orphaned after Ctrl+C or a startup failure.
    for ($Attempt = 1; $Attempt -le 10; $Attempt++) {
        $PortProcessIds = @(Get-PortProcessIds $Port)
        if ($PortProcessIds.Count -eq 0) { break }
        foreach ($PortPid in $PortProcessIds) {
            if ($PortPid -gt 0) {
                Write-Info "Port $Port is in use (PID $PortPid); stopping it before restarting $Name"
                Stop-ProcessTree $PortPid
            }
        }
        if ($Attempt -lt 10) { Start-Sleep -Milliseconds 300 }
    }
}

function Stop-All {
    Stop-One "frontend" $FrontendPort
    Stop-One "backend" $BackendPort
}

function ConvertTo-SingleQuotedPowerShellLiteral([string] $Value) {
    return "'" + $Value.Replace("'", "''") + "'"
}

function Start-LoggedProcess(
    [string] $Name,
    [string] $FilePath,
    [string[]] $ArgumentList,
    [string] $WorkingDirectory,
    [string] $LogPath
) {
    Set-Content -Path $LogPath -Value "" -Encoding UTF8

    $Executable = ConvertTo-SingleQuotedPowerShellLiteral $FilePath
    $Arguments = ($ArgumentList | ForEach-Object {
        ConvertTo-SingleQuotedPowerShellLiteral $_
    }) -join " "
    $LogLiteral = ConvertTo-SingleQuotedPowerShellLiteral $LogPath
    $ChildCommand = "& $Executable $Arguments *> $LogLiteral; exit `$LASTEXITCODE"
    $EncodedCommand = [Convert]::ToBase64String(
        [Text.Encoding]::Unicode.GetBytes($ChildCommand)
    )

    $Process = Start-Process `
        -FilePath $PowerShellExe `
        -ArgumentList @("-NoProfile", "-NonInteractive", "-EncodedCommand", $EncodedCommand) `
        -WorkingDirectory $WorkingDirectory `
        -WindowStyle Hidden `
        -PassThru

    Set-Content -Path (Join-Path $Logs "$Name.pid") -Value $Process.Id -Encoding ASCII
    return $Process
}

function Wait-Http(
    [string] $Name,
    [string] $Url,
    [Diagnostics.Process] $Process,
    [int] $TimeoutSeconds
) {
    for ($Attempt = 1; $Attempt -le $TimeoutSeconds; $Attempt++) {
        try {
            Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 2 | Out-Null
            return
        }
        catch {
            if ($Process.HasExited) {
                Get-Content (Join-Path $Logs "$Name.log") -Tail 40 -ErrorAction SilentlyContinue
                Stop-WithError "$Name stopped while starting. See .dev-logs/$Name.log"
            }
            Start-Sleep -Seconds 1
        }
    }

    Get-Content (Join-Path $Logs "$Name.log") -Tail 40 -ErrorAction SilentlyContinue
    Stop-WithError "$Name was not ready after $TimeoutSeconds seconds. See .dev-logs/$Name.log"
}

function Test-Tools {
    $script:DockerPath = Get-RequiredCommand "docker"
    $script:MavenPath = Get-RequiredCommand "mvn.cmd"
    $script:NpmPath = Get-RequiredCommand "npm.cmd"
    $JavaPath = Get-RequiredCommand "java"

    $JavaVersionText = Get-NativeCommandOutput $JavaPath "-version"
    if ($JavaVersionText -notmatch 'version "(?<major>\d+)') {
        Stop-WithError "Could not determine the installed Java version. Java 21 or newer is required."
    }
    if ([int] $Matches.major -lt 21) {
        Stop-WithError "Java 21 or newer is required (found Java $($Matches.major)). Set JAVA_HOME to JDK 21."
    }

    & $script:DockerPath info *> $null
    if ($LASTEXITCODE -ne 0) {
        Stop-WithError "Docker Desktop is not running or Docker is unavailable."
    }
}

function Start-Database {
    Write-Info "Starting PostgreSQL"
    & $script:DockerPath compose -f (Join-Path $Root "docker-compose.yml") up -d postgres | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Stop-WithError "Could not start PostgreSQL with Docker Compose."
    }

    for ($Attempt = 1; $Attempt -le 60; $Attempt++) {
        $Status = (& $script:DockerPath inspect -f "{{.State.Health.Status}}" aar-postgres 2>$null)
        if ($Status -eq "healthy") {
            Write-Ok "PostgreSQL is ready"
            return
        }
        Start-Sleep -Seconds 1
    }
    Stop-WithError "PostgreSQL was not ready after 60 seconds. Run: docker compose logs postgres"
}

function Reset-Database {
    Write-Info "Resetting PostgreSQL and demo data"
    & $script:DockerPath compose -f (Join-Path $Root "docker-compose.yml") down --volumes --remove-orphans | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Stop-WithError "Could not reset the PostgreSQL data volume."
    }
    Write-Ok "PostgreSQL data was reset"
}

function Start-Backend {
    Write-Info "Starting backend (the first run may take a few minutes)..."
    $script:BackendProcess = Start-LoggedProcess `
        -Name "backend" `
        -FilePath $script:MavenPath `
        -ArgumentList @("-q", "spring-boot:run", "-Dspring-boot.run.profiles=dev") `
        -WorkingDirectory (Join-Path $Root "backend") `
        -LogPath $BackendLog

    Wait-Http "backend" "http://localhost:$BackendPort/actuator/health" $script:BackendProcess 300
    Write-Ok "Backend is ready: http://localhost:$BackendPort (Swagger: /swagger-ui.html)"
}

function Start-Frontend {
    $FrontendDirectory = Join-Path $Root "frontend"
    $EnvLocal = Join-Path $FrontendDirectory ".env.local"
    if (-not (Test-Path $EnvLocal)) {
        Copy-Item (Join-Path $FrontendDirectory ".env.example") $EnvLocal
        Write-Info "Created frontend/.env.local from .env.example"
    }

    $NodeModules = Join-Path $FrontendDirectory "node_modules"
    $InstalledLock = Join-Path $NodeModules ".package-lock.json"
    $PackageLock = Join-Path $FrontendDirectory "package-lock.json"
    $NeedsInstall = -not (Test-Path $NodeModules)
    if (-not $NeedsInstall -and (Test-Path $PackageLock)) {
        $NeedsInstall = -not (Test-Path $InstalledLock) -or
            ((Get-Item $PackageLock).LastWriteTimeUtc -gt (Get-Item $InstalledLock).LastWriteTimeUtc)
    }

    if ($NeedsInstall) {
        Write-Info "Installing frontend dependencies (npm ci)..."
        $NpmLog = Join-Path $Logs "npm-ci.log"
        $NpmExitCode = Invoke-NativeCommandToLog `
            -FilePath $script:NpmPath `
            -ArgumentList @("ci", "--no-audit", "--no-fund") `
            -WorkingDirectory $FrontendDirectory `
            -LogPath $NpmLog
        if ($NpmExitCode -ne 0) {
            Get-Content $NpmLog -Tail 30 -ErrorAction SilentlyContinue
            Stop-WithError "npm ci failed. See .dev-logs/npm-ci.log"
        }
    }

    Write-Info "Starting frontend"
    $script:FrontendProcess = Start-LoggedProcess `
        -Name "frontend" `
        -FilePath $script:NpmPath `
        -ArgumentList @("run", "dev", "--", "--port", "$FrontendPort") `
        -WorkingDirectory $FrontendDirectory `
        -LogPath $FrontendLog

    Wait-Http "frontend" "http://localhost:$FrontendPort" $script:FrontendProcess 120
    Write-Ok "Frontend is ready: http://localhost:$FrontendPort"
}

function Write-NewLogContent([string] $Path, [ref] $Offset) {
    if (-not (Test-Path $Path)) { return }
    $Stream = [IO.File]::Open($Path, "Open", "Read", "ReadWrite")
    try {
        if ($Stream.Length -lt $Offset.Value) { $Offset.Value = 0L }
        [void] $Stream.Seek($Offset.Value, "Begin")
        $Reader = [IO.StreamReader]::new($Stream, [Text.Encoding]::UTF8, $true, 4096, $true)
        try {
            $Text = $Reader.ReadToEnd()
            if ($Text.Length -gt 0) { Write-Host $Text -NoNewline }
            $Offset.Value = $Stream.Position
        }
        finally {
            $Reader.Dispose()
        }
    }
    finally {
        $Stream.Dispose()
    }
}

if ($Action -eq "stop") {
    Stop-All
    Write-Ok "Stopped backend and frontend. To stop PostgreSQL too: docker compose stop"
    exit 0
}

Test-Tools
$Started = $false
try {
    Stop-All
    if ($Fresh) {
        Reset-Database
    }
    Start-Database
    Start-Backend
    Start-Frontend
    $Started = $true

    Write-Host ""
    Write-Ok "Done. Open http://localhost:$FrontendPort"
    Write-Info "Sample accounts: manager@aar.local / staff1@aar.local; password: Password@123"

    if ($Background) {
        Write-Info "Running in background. Logs: .dev-logs; stop: .\scripts\dev.ps1 stop"
        return
    }

    Write-Info "Showing backend and frontend logs. Press Ctrl+C to stop both."
    [long] $BackendOffset = (Get-Item $BackendLog).Length
    [long] $FrontendOffset = (Get-Item $FrontendLog).Length
    while (-not $script:BackendProcess.HasExited -and -not $script:FrontendProcess.HasExited) {
        Write-NewLogContent $BackendLog ([ref] $BackendOffset)
        Write-NewLogContent $FrontendLog ([ref] $FrontendOffset)
        Start-Sleep -Milliseconds 500
    }
    Stop-WithError "Backend or frontend stopped. See logs in .dev-logs."
}
finally {
    if (-not $Background -and ($Started -or $script:BackendProcess -or $script:FrontendProcess)) {
        Stop-All
        Write-Ok "Stopped backend and frontend."
    }
}
