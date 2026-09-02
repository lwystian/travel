[CmdletBinding()]
param(
    [Parameter(Position = 0, ValueFromRemainingArguments = $true)]
    [string[]]$Services,

    [string]$Registry = "ccr.ccs.tencentyun.com/lwystian/travel",

    [string]$RegistryUsername = "100031278382",

    [switch]$NoPush,

    [switch]$PushOnly,

    [switch]$NoCache
)

$ErrorActionPreference = "Stop"

function Write-Section {
    param([string]$Message)

    Write-Host ""
    Write-Host "[RUNNING] $Message" -ForegroundColor Cyan
}

function Write-Success {
    param([string]$Message)

    Write-Host "[DONE] $Message" -ForegroundColor Green
}

function Invoke-Docker {
    param([string[]]$Arguments)

    & docker @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker command failed: docker $($Arguments -join ' ')"
    }
}

function Invoke-DockerWithRetry {
    param(
        [string[]]$Arguments,
        [int]$MaxAttempts = 4
    )

    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        & docker @Arguments
        if ($LASTEXITCODE -eq 0) {
            return
        }

        if ($attempt -lt $MaxAttempts) {
            $delaySeconds = 5 * $attempt
            Write-Host "[RETRY] Transfer interrupted. Retrying in $delaySeconds seconds; uploaded layers will be reused." -ForegroundColor Yellow
            Start-Sleep -Seconds $delaySeconds
        }
    }

    throw "Docker command failed after $MaxAttempts attempts: docker $($Arguments -join ' ')"
}

function Test-DockerRegistryCredential {
    param([string]$RegistryHost)

    try {
        $configPath = Join-Path $HOME ".docker\config.json"
        if (-not (Test-Path -LiteralPath $configPath)) {
            return $false
        }

        $config = Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json
        return $null -ne $config.auths.PSObject.Properties[$RegistryHost]
    } catch {
        return $false
    }
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker was not found. Install and start Docker Desktop first."
}

if ($NoPush -and $PushOnly) {
    throw "-NoPush and -PushOnly cannot be used together."
}

if ($NoCache -and $PushOnly) {
    throw "-NoCache cannot be used with -PushOnly."
}

$Registry = $Registry.Trim().TrimEnd([char]'/')
if ($Registry -notmatch '^[A-Za-z0-9.-]+(?::[0-9]+)?/[A-Za-z0-9._/-]+$') {
    throw "Invalid image registry path: $Registry"
}

$RegistryUsername = $RegistryUsername.Trim()
if ([string]::IsNullOrWhiteSpace($RegistryUsername)) {
    throw "Registry username cannot be empty."
}

$registryHost = ($Registry -split '/', 2)[0]
$deployDir = $PSScriptRoot
$projectRoot = Split-Path -Parent $deployDir
$composeFile = Join-Path $deployDir "docker-compose.yml"
$localEnv = Join-Path $deployDir ".env"
$exampleEnv = Join-Path $deployDir ".env.example"

if (-not (Test-Path -LiteralPath $composeFile)) {
    throw "Compose file was not found: $composeFile"
}

if (Test-Path -LiteralPath $localEnv) {
    $envFile = $localEnv
} elseif (Test-Path -LiteralPath $exampleEnv) {
    $envFile = $exampleEnv
    Write-Host "[NOTICE] deploy/.env was not found. The example file is used only to parse Compose." -ForegroundColor Yellow
} else {
    throw "Neither deploy/.env nor deploy/.env.example was found."
}

$allImages = @(
    [PSCustomObject]@{
        Service = "backend"
        Local = "travel-pc-backend:latest"
        Remote = "$Registry`:pc-backend-latest"
        Name = "Java backend"
    },
    [PSCustomObject]@{
        Service = "frontend"
        Local = "travel-pc-frontend:latest"
        Remote = "$Registry`:pc-frontend-latest"
        Name = "PC frontend"
    }
)

$selectedServices = New-Object System.Collections.Generic.List[string]
foreach ($rawService in @($Services)) {
    foreach ($part in ($rawService -split ',')) {
        $service = $part.Trim().ToLowerInvariant()
        if ([string]::IsNullOrWhiteSpace($service)) {
            continue
        }
        if ($service -notin @("backend", "frontend")) {
            throw "Unsupported service: $part. Valid values: backend, frontend."
        }
        if (-not $selectedServices.Contains($service)) {
            $selectedServices.Add($service)
        }
    }
}

if ($selectedServices.Count -eq 0) {
    $images = @($allImages)
} else {
    $images = @($allImages | Where-Object { $selectedServices.Contains($_.Service) })
}

Write-Section "Checking Docker Desktop"
Invoke-Docker -Arguments @("info", "--format", "Docker {{.ServerVersion}} {{.OSType}}/{{.Architecture}}")

$oldPlatform = $env:DOCKER_DEFAULT_PLATFORM
$env:DOCKER_DEFAULT_PLATFORM = "linux/amd64"

try {
    Write-Host "[INFO] Project: $projectRoot"
    Write-Host "[INFO] Registry: $Registry"
    Write-Host "[INFO] Platform: linux/amd64"
    Write-Host "[INFO] Services: $($images.Service -join ', ')"

    if ($PushOnly) {
        Write-Host "[INFO] Push-only mode: local images will be reused." -ForegroundColor Yellow
        foreach ($image in $images) {
            Invoke-Docker -Arguments @("image", "inspect", $image.Local, "--format", "Found {{index .RepoTags 0}}, ID={{.Id}}")
        }
    } else {
        $index = 0
        foreach ($image in $images) {
            $index++
            Write-Section "[$index/$($images.Count)] Building $($image.Name)"

            $arguments = @(
                "compose",
                "--env-file", $envFile,
                "-f", $composeFile,
                "build"
            )
            if ($NoCache) {
                $arguments += "--no-cache"
            }
            $arguments += $image.Service

            Invoke-Docker -Arguments $arguments
            Invoke-Docker -Arguments @("image", "inspect", $image.Local, "--format", "Built {{index .RepoTags 0}}, ID={{.Id}}, size={{.Size}} bytes")
            Write-Success "$($image.Name) image built"
        }
    }

    if ($NoPush) {
        Write-Host ""
        Write-Success "All selected local images were built. Push was skipped."
        exit 0
    }

    if (Test-DockerRegistryCredential -RegistryHost $registryHost) {
        Write-Success "Saved registry credentials found"
    } else {
        Write-Section "Logging in to Tencent Cloud Container Registry"
        Invoke-Docker -Arguments @("login", "--username", $RegistryUsername, $registryHost)
    }

    $index = 0
    foreach ($image in $images) {
        $index++
        Write-Section "[$index/$($images.Count)] Pushing $($image.Name)"
        Invoke-Docker -Arguments @("tag", $image.Local, $image.Remote)
        Invoke-DockerWithRetry -Arguments @("push", $image.Remote)
        Write-Success "$($image.Remote) pushed"
    }

    Write-Host ""
    Write-Success "All selected PC images were published"
    Write-Host "Server command: cd /root/travel/deploy && bash deploy.sh update" -ForegroundColor Green
} finally {
    if ($null -eq $oldPlatform) {
        Remove-Item Env:DOCKER_DEFAULT_PLATFORM -ErrorAction SilentlyContinue
    } else {
        $env:DOCKER_DEFAULT_PLATFORM = $oldPlatform
    }
}
