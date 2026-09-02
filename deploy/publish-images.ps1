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
    Write-Host "[进行中] $Message" -ForegroundColor Cyan
}

function Write-Success {
    param([string]$Message)

    Write-Host "[完成] $Message" -ForegroundColor Green
}

function Invoke-Docker {
    param([string[]]$Arguments)

    & docker @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker 命令执行失败：docker $($Arguments -join ' ')"
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
            Write-Host "[重试] 网络传输中断，$delaySeconds 秒后重试；已上传的镜像层会继续复用。" -ForegroundColor Yellow
            Start-Sleep -Seconds $delaySeconds
        }
    }

    throw "Docker 命令连续 $MaxAttempts 次执行失败：docker $($Arguments -join ' ')"
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
    throw "没有找到 Docker，请先安装并启动 Docker Desktop。"
}

if ($NoPush -and $PushOnly) {
    throw "-NoPush 与 -PushOnly 不能同时使用。"
}

if ($NoCache -and $PushOnly) {
    throw "-NoCache 不能与 -PushOnly 同时使用。"
}

$Registry = $Registry.Trim().TrimEnd([char]'/')
if ($Registry -notmatch '^[A-Za-z0-9.-]+(?::[0-9]+)?/[A-Za-z0-9._/-]+$') {
    throw "镜像仓库地址格式不正确：$Registry"
}

$RegistryUsername = $RegistryUsername.Trim()
if ([string]::IsNullOrWhiteSpace($RegistryUsername)) {
    throw "腾讯云镜像仓库账号不能为空。"
}

$registryHost = ($Registry -split '/', 2)[0]
$deployDir = $PSScriptRoot
$projectRoot = Split-Path -Parent $deployDir
$composeFile = Join-Path $deployDir "docker-compose.yml"
$localEnv = Join-Path $deployDir ".env"
$exampleEnv = Join-Path $deployDir ".env.example"

if (-not (Test-Path -LiteralPath $composeFile)) {
    throw "没有找到 Compose 文件：$composeFile"
}

if (Test-Path -LiteralPath $localEnv) {
    $envFile = $localEnv
} elseif (Test-Path -LiteralPath $exampleEnv) {
    $envFile = $exampleEnv
    Write-Host "[提示] 没有找到 deploy/.env，本次仅使用示例配置解析 Compose。" -ForegroundColor Yellow
} else {
    throw "没有找到 deploy/.env 或 deploy/.env.example。"
}

$allImages = @(
    [PSCustomObject]@{
        Service = "backend"
        Local = "travel-pc-backend:latest"
        Remote = "$Registry`:pc-backend-latest"
        Name = "Java 后端"
    },
    [PSCustomObject]@{
        Service = "frontend"
        Local = "travel-pc-frontend:latest"
        Remote = "$Registry`:pc-frontend-latest"
        Name = "PC 前端"
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
            throw "不支持的服务：$part。可选值为 backend、frontend。"
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

Write-Section "检查 Docker Desktop"
Invoke-Docker -Arguments @("info", "--format", "Docker 服务端 {{.ServerVersion}}，平台 {{.OSType}}/{{.Architecture}}")

$oldPlatform = $env:DOCKER_DEFAULT_PLATFORM
$env:DOCKER_DEFAULT_PLATFORM = "linux/amd64"

try {
    Write-Host "[信息] 项目目录：$projectRoot"
    Write-Host "[信息] 镜像仓库：$Registry"
    Write-Host "[信息] 构建平台：linux/amd64"
    Write-Host "[信息] 本次服务：$($images.Service -join '、')"

    if ($PushOnly) {
        Write-Host "[信息] 续传模式：跳过构建，直接复用本地镜像。" -ForegroundColor Yellow
        foreach ($image in $images) {
            Invoke-Docker -Arguments @("image", "inspect", $image.Local, "--format", "已找到 {{index .RepoTags 0}}，ID={{.Id}}")
        }
    } else {
        $index = 0
        foreach ($image in $images) {
            $index++
            Write-Section "[$index/$($images.Count)] 构建 $($image.Name)镜像"

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
            Invoke-Docker -Arguments @("image", "inspect", $image.Local, "--format", "镜像 {{index .RepoTags 0}} 已生成，ID={{.Id}}，大小={{.Size}} 字节")
            Write-Success "$($image.Name)镜像构建完成"
        }
    }

    if ($NoPush) {
        Write-Host ""
        Write-Success "所选本地镜像均已构建，本次按参数要求未推送。"
        exit 0
    }

    if (Test-DockerRegistryCredential -RegistryHost $registryHost) {
        Write-Success "已找到本机保存的腾讯云登录凭据"
    } else {
        Write-Section "登录腾讯云容器镜像仓库"
        Invoke-Docker -Arguments @("login", "--username", $RegistryUsername, $registryHost)
    }

    $index = 0
    foreach ($image in $images) {
        $index++
        Write-Section "[$index/$($images.Count)] 推送 $($image.Name)镜像"
        Invoke-Docker -Arguments @("tag", $image.Local, $image.Remote)
        Invoke-DockerWithRetry -Arguments @("push", $image.Remote)
        Write-Success "$($image.Remote) 推送完成"
    }

    Write-Host ""
    Write-Success "所选 PC 镜像已全部发布"
    Write-Host "服务器执行：cd /root/travel/deploy && ./deploy.sh update" -ForegroundColor Green
} finally {
    if ($null -eq $oldPlatform) {
        Remove-Item Env:DOCKER_DEFAULT_PLATFORM -ErrorAction SilentlyContinue
    } else {
        $env:DOCKER_DEFAULT_PLATFORM = $oldPlatform
    }
}
