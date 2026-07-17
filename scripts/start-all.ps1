#!/usr/bin/env pwsh
# ============================================================================
# AI-FMS 一键启动脚本
# 检查所有依赖 → 缺失项提示下载 → 全部就绪后启动前后端
# ============================================================================
$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$host.UI.RawUI.WindowTitle = "AI-FMS Startup"

# ── 配置 ──────────────────────────────────────────────────────────
$PROJECT_ROOT = Split-Path -Parent $MyInvocation.MyCommand.Path
$BACKEND_DIR  = Join-Path $PROJECT_ROOT "backend"
$FRONTEND_DIR = Join-Path $PROJECT_ROOT "frontend"
$OLLAMA_DIR   = "$env:LOCALAPPDATA\Programs\Ollama\ollama.exe"
$MINIO_DIR    = "$env:USERPROFILE\tools\minio"
$REDIS_DIR    = "$env:USERPROFILE\tools\redis"
$MINIO_DATA   = Join-Path $PROJECT_ROOT "data\minio"

$LLM_KEY      = $env:LLM_API_KEY

# ── 颜色输出辅助 ──────────────────────────────────────────────────
function Write-Step   { Write-Host "`n── $args ──" -ForegroundColor Cyan }
function Write-OK     { Write-Host "  ✔ $args" -ForegroundColor Green }
function Write-Warn   { Write-Host "  ⚠ $args" -ForegroundColor Yellow }
function Write-Fail   { Write-Host "  ✘ $args" -ForegroundColor Red }
function Write-Info   { Write-Host "  ℹ $args" -ForegroundColor Gray }

# ── TUI 选择框（使用 Out-GridView 或者简单的 Read-Host 二选一） ──
function Prompt-YesNo($message, $defaultYes = $true) {
    $yn = if ($defaultYes) { "[Y/n]" } else { "[y/N]" }
    $reply = Read-Host "  ? $message $yn"
    if ([string]::IsNullOrWhiteSpace($reply)) { return $defaultYes }
    return $reply -match "^(y|yes)$"
}

# ── 端口检测 ──────────────────────────────────────────────────────
function Test-Port($port) {
    try { $tcp = New-Object Net.Sockets.TcpClient("localhost", $port); $tcp.Close(); return $true } catch { return $false }
}

# ── 状态追踪（每个组件: ok | fixed | failed | skipped） ──────────
$STATUS = @{}

# ═══════════════════════════════════════════════════════════════════
# 1：Java
# ═══════════════════════════════════════════════════════════════════
Write-Step "1/8  Java 21+"

$javaVer = try { & java -version 2>&1 | Select-Object -First 1 } catch { $null }
if ($javaVer -match '(\d+)\.\d+\.\d+' -and [int]$matches[1] -ge 21) {
    Write-OK "Java $($matches[0])"
    $STATUS.java = "ok"
} else {
    $found = $javaVer -replace "`n"," " -replace "`r",""
    Write-Warn "Java 21+ 未找到 (当前: $found)"
    if (Prompt-YesNo "是否打开 Oracle JDK 下载页？") {
        Start-Process "https://www.oracle.com/java/technologies/downloads/#jdk21-windows"
        Write-Info "请在浏览器中下载安装 JDK 21+，完成后按回车继续..."
        Read-Host
        # 重新检测
        $javaVer = try { & java -version 2>&1 | Select-Object -First 1 } catch { $null }
        if ($javaVer -match '(\d+)\.\d+\.\d+' -and [int]$matches[1] -ge 21) {
            Write-OK "Java $($matches[0]) 已安装"
            $STATUS.java = "fixed"
        } else {
            Write-Fail "Java 仍未就绪，跳过"
            $STATUS.java = "failed"
        }
    } else {
        $STATUS.java = "skipped"
    }
}

# ═══════════════════════════════════════════════════════════════════
# 2：Node.js
# ═══════════════════════════════════════════════════════════════════
Write-Step "2/8  Node.js 18+"

$nodeVer = try { & node -v 2>&1 } catch { $null }
if ($nodeVer -match 'v(\d+)' -and [int]$matches[1] -ge 18) {
    Write-OK "Node.js $nodeVer"
    $STATUS.node = "ok"
} else {
    Write-Warn "Node.js 18+ 未找到"
    if (Prompt-YesNo "是否打开 Node.js 下载页？") {
        Start-Process "https://nodejs.org/en/download"
        Write-Info "安装完成后按回车继续..."
        Read-Host
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
        $nodeVer = try { & node -v 2>&1 } catch { $null }
        if ($nodeVer -match 'v(\d+)' -and [int]$matches[1] -ge 18) {
            Write-OK "Node.js $nodeVer"
            $STATUS.node = "fixed"
        } else {
            Write-Fail "Node.js 仍未就绪"
            $STATUS.node = "failed"
        }
    } else { $STATUS.node = "skipped" }
}

# ═══════════════════════════════════════════════════════════════════
# 3：PostgreSQL
# ═══════════════════════════════════════════════════════════════════
Write-Step "3/8  PostgreSQL"

$pgOk = try { & psql -U postgres -c "SELECT 1;" 2>&1 | Out-Null; $true } catch { $false }
if ($pgOk) {
    Write-OK "PostgreSQL 运行中"
    $STATUS.pg = "ok"
} else {
    Write-Warn "PostgreSQL 不可达（psql -U postgres 失败）"
    Write-Info "请确认 PostgreSQL 服务已启动，且超级用户为 postgres"
    $STATUS.pg = "failed"
}

# ═══════════════════════════════════════════════════════════════════
# 4：Redis
# ═══════════════════════════════════════════════════════════════════
Write-Step "4/8  Redis"

if (Test-Port 6379) {
    Write-OK "Redis 端口 6379 在线"
    $STATUS.redis = "ok"
} else {
    # 检查 redis-server.exe 是否在 tools 下
    $redisExe = Join-Path $REDIS_DIR "redis-server.exe"
    if (Test-Path $redisExe) {
        Write-Warn "Redis 未运行，但 redis-server.exe 已存在"
        if (Prompt-YesNo "是否启动本地 Redis？") {
            Start-Process -FilePath $redisExe -WindowStyle Hidden
            Start-Sleep -Seconds 3
            if (Test-Port 6379) {
                Write-OK "Redis 已启动"
                $STATUS.redis = "fixed"
            } else {
                Write-Fail "Redis 启动失败"
                $STATUS.redis = "failed"
            }
        } else { $STATUS.redis = "skipped" }
    } else {
        Write-Warn "Redis 未安装"
        if (Prompt-YesNo "是否下载 Redis for Windows？") {
            $url = "https://github.com/tporadowski/redis/releases/download/v5.0.14.1/Redis-x64-5.0.14.1.zip"
            $zip = "$env:TEMP\Redis-x64.zip"
            Write-Info "下载中..."
            Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
            Expand-Archive -Path $zip -DestinationPath $REDIS_DIR -Force
            Write-OK "Redis 已解压到 $REDIS_DIR"
            Start-Process -FilePath (Join-Path $REDIS_DIR "redis-server.exe") -WindowStyle Hidden
            Start-Sleep -Seconds 3
            if (Test-Port 6379) {
                Write-OK "Redis 已启动"
                $STATUS.redis = "fixed"
            } else {
                Write-Fail "Redis 启动失败，请手动运行 redis-server.exe"
                $STATUS.redis = "failed"
            }
        } else { $STATUS.redis = "skipped" }
    }
}

# ═══════════════════════════════════════════════════════════════════
# 5：MinIO
# ═══════════════════════════════════════════════════════════════════
Write-Step "5/8  MinIO"

if (Test-Port 9000) {
    Write-OK "MinIO 端口 9000 在线"
    $STATUS.minio = "ok"
} else {
    $minioExe = Join-Path $MINIO_DIR "minio.exe"
    if (Test-Path $minioExe) {
        Write-Warn "MinIO 未运行"
        if (Prompt-YesNo "是否启动 MinIO？") {
            New-Item -ItemType Directory -Force -Path $MINIO_DATA | Out-Null
            Start-Process -FilePath $minioExe -ArgumentList "server",$MINIO_DATA,"--console-address",":9001" -WindowStyle Minimized
            Start-Sleep -Seconds 4
            if (Test-Port 9000) {
                Write-OK "MinIO 已启动"
                $STATUS.minio = "fixed"
            } else {
                Write-Fail "MinIO 启动失败"
                $STATUS.minio = "failed"
            }
        } else { $STATUS.minio = "skipped" }
    } else {
        Write-Warn "MinIO 未安装"
        if (Prompt-YesNo "是否下载 MinIO？") {
            New-Item -ItemType Directory -Force -Path $MINIO_DIR | Out-Null
            $url = "https://dl.min.io/server/minio/release/windows-amd64/minio.exe"
            Write-Info "下载中..."
            Invoke-WebRequest -Uri $url -OutFile $minioExe -UseBasicParsing
            Write-OK "MinIO 已下载到 $MINIO_DIR"
            New-Item -ItemType Directory -Force -Path $MINIO_DATA | Out-Null
            Start-Process -FilePath $minioExe -ArgumentList "server",$MINIO_DATA,"--console-address",":9001" -WindowStyle Minimized
            Start-Sleep -Seconds 4
            if (Test-Port 9000) {
                Write-OK "MinIO 已启动"
                $STATUS.minio = "fixed"
            } else {
                Write-Fail "MinIO 启动失败"
                $STATUS.minio = "failed"
            }
        } else { $STATUS.minio = "skipped" }
    }
}

# ═══════════════════════════════════════════════════════════════════
# 6：Ollama + bge-m3
# ═══════════════════════════════════════════════════════════════════
Write-Step "6/8  Ollama + bge-m3"

if (Test-Port 11434) {
    Write-OK "Ollama 服务在线"
    $STATUS.ollama = "ok"
} else {
    if (Test-Path $OLLAMA_DIR) {
        Write-Warn "Ollama 未运行"
        if (Prompt-YesNo "是否启动 Ollama？") {
            Start-Process -FilePath $OLLAMA_DIR -ArgumentList "serve" -WindowStyle Hidden
            Start-Sleep -Seconds 5
            if (Test-Port 11434) {
                Write-OK "Ollama 已启动"
                $STATUS.ollama = "fixed"
            } else {
                Write-Fail "Ollama 启动失败"
                $STATUS.ollama = "failed"
            }
        } else { $STATUS.ollama = "skipped" }
    } else {
        Write-Warn "Ollama 未安装"
        if (Prompt-YesNo "是否下载 Ollama？") {
            $url = "https://ollama.com/download/OllamaSetup.exe"
            $setup = "$env:TEMP\OllamaSetup.exe"
            Write-Info "下载中（约 1 GB）..."
            Invoke-WebRequest -Uri $url -OutFile $setup -UseBasicParsing
            Write-Info "安装 Ollama（静默安装）..."
            Start-Process -FilePath $setup -ArgumentList "/S" -Wait
            Start-Sleep -Seconds 3
            Start-Process -FilePath $OLLAMA_DIR -ArgumentList "serve" -WindowStyle Hidden
            Start-Sleep -Seconds 5
            if (Test-Port 11434) {
                Write-OK "Ollama 已安装并启动"
                $STATUS.ollama = "fixed"
            } else {
                Write-Fail "Ollama 安装或启动失败"
                $STATUS.ollama = "failed"
            }
        } else { $STATUS.ollama = "skipped" }
    }
}

# bge-m3 模型检查
if ($STATUS.ollama -in @("ok","fixed")) {
    $models = & $OLLAMA_DIR list 2>&1 | Out-String
    if ($models -match "bge-m3") {
        Write-OK "bge-m3 模型已就绪"
    } else {
        Write-Warn "bge-m3 模型未下载（约 1.2 GB）"
        if (Prompt-YesNo "是否下载 bge-m3？") {
            Write-Info "下载中（需要几分钟）..."
            & $OLLAMA_DIR pull bge-m3 2>&1
            Write-OK "bge-m3 下载完成"
        } else {
            Write-Warn "跳过 bge-m3（RAG 检索将降级返回空结果）"
        }
    }
}

# ═══════════════════════════════════════════════════════════════════
# 7：LLM API Key
# ═══════════════════════════════════════════════════════════════════
Write-Step "7/8  DeepSeek API Key"

if ($LLM_KEY -and $LLM_KEY.StartsWith("sk-")) {
    Write-OK "LLM_API_KEY 已设置"
    $STATUS.llm = "ok"
} else {
    $LLM_KEY = Read-Host "  请输入 DeepSeek API Key（https://platform.deepseek.com）"
    if ($LLM_KEY -and $LLM_KEY.StartsWith("sk-")) {
        $env:LLM_API_KEY = $LLM_KEY
        Write-OK "LLM_API_KEY 已设置"
        $STATUS.llm = "fixed"
    } else {
        Write-Fail "LLM_API_KEY 无效，AI 导入功能将不可用"
        $STATUS.llm = "failed"
    }
}

# ═══════════════════════════════════════════════════════════════════
# 8：npm 依赖 + Flyway 迁移
# ═══════════════════════════════════════════════════════════════════
Write-Step "8/8  npm 依赖 & 数据库迁移"

# npm install
if (-not (Test-Path (Join-Path $FRONTEND_DIR "node_modules"))) {
    Write-Warn "前端 node_modules 缺失，npm install..."
    Push-Location $FRONTEND_DIR
    npm install 2>&1 | Out-Null
    Pop-Location
    Write-OK "npm install 完成"
} else {
    Write-OK "前端依赖已就绪"
}

# Flyway migration
Write-Info "检查数据库迁移..."
Push-Location $BACKEND_DIR
$env:DB_USERNAME = "postgres"
$migrateResult = ./mvnw flyway:info -Dflyway.user=postgres -Dflyway.password=postgres -q 2>&1 | Out-String
if ($migrateResult -match "Success|up to date") {
    Write-OK "数据库迁移已是最新"
} else {
    Write-Warn "数据库迁移未执行或版本落后，执行 migrate..."
    ./mvnw flyway:migrate -Dflyway.user=postgres -Dflyway.password=postgres 2>&1 | Out-Null
    Write-OK "Flyway 迁移完成"
}
Pop-Location

# ═══════════════════════════════════════════════════════════════════
# 状态汇总
# ═══════════════════════════════════════════════════════════════════
Write-Host "`n" -NoNewline
Write-Host "════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  状态汇总" -ForegroundColor Cyan
Write-Host "════════════════════════════════════════════" -ForegroundColor Cyan

$allGood = $true
$components = @{
    "Java 21+"     = $STATUS.java
    "Node.js 18+"  = $STATUS.node
    "PostgreSQL"   = $STATUS.pg
    "Redis"        = $STATUS.redis
    "MinIO"        = $STATUS.minio
    "Ollama+bge-m3"= $STATUS.ollama
    "LLM API Key"  = $STATUS.llm
    "DB + npm"     = "ok"
}

foreach ($c in $components.GetEnumerator()) {
    $icon = switch ($c.Value) {
        "ok"    { $allGood = $allGood -and $true;  "✔" }
        "fixed" { $allGood = $allGood -and $true;  "✔" }
        default { $allGood = $false;                "✘" }
    }
    $color = if ($icon -eq "✔") { "Green" } else { "Red" }
    Write-Host "  $icon $($c.Key)" -ForegroundColor $color
}

if (-not $allGood) {
    Write-Host "`n⚠ 部分依赖未就绪，相关问题功能将不可用" -ForegroundColor Yellow
    Write-Host "  按回车继续启动（跳过不可用组件）..."
    Read-Host
}

# ═══════════════════════════════════════════════════════════════════
# 启动前后端
# ═══════════════════════════════════════════════════════════════════
Write-Step "启动后端 Spring Boot"

$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"
$env:LLM_API_KEY = $LLM_KEY
$env:LLM_BASE_URL = "https://api.deepseek.com"

$backendJob = Start-Process -FilePath "pwsh" -ArgumentList "-Command", @"
`$env:LLM_API_KEY = '$LLM_KEY'
`$env:LLM_BASE_URL = 'https://api.deepseek.com'
`$env:JAVA_TOOL_OPTIONS = '-Dfile.encoding=UTF-8'
Set-Location '$BACKEND_DIR'
./mvnw spring-boot:run 2>&1 | Tee-Object -FilePath '$PROJECT_ROOT\backend.log'
"@ -PassThru -WindowStyle Minimized

Write-Info "后端启动中（PID: $($backendJob.Id)）... 等待端口 8080..."
$timeout = 60; $elapsed = 0
while (-not (Test-Port 8080) -and $elapsed -lt $timeout) {
    Start-Sleep -Seconds 2; $elapsed += 2
    Write-Host -NoNewline "."
}
if (Test-Port 8080) {
    Write-OK "后端已在 http://localhost:8080 启动"
} else {
    Write-Fail "后端启动超时，请检查 backend.log"
}

Write-Step "启动前端 Vite"

$frontendJob = Start-Process -FilePath "pwsh" -ArgumentList "-Command", @"
Set-Location '$FRONTEND_DIR'
npm run dev
"@ -PassThru

Start-Sleep -Seconds 5
Write-OK "前端应在 http://localhost:3000 启动"
Write-Host "`n════════════════════════════════════════════" -ForegroundColor Green
Write-Host "  AI-FMS 已启动！" -ForegroundColor Green
Write-Host "  前端: http://localhost:3000" -ForegroundColor Green
Write-Host "  后端: http://localhost:8080" -ForegroundColor Green
Write-Host "  Swagger: http://localhost:8080/swagger-ui.html" -ForegroundColor Green
Write-Host "  MinIO Console: http://localhost:9001" -ForegroundColor Green
Write-Host "════════════════════════════════════════════" -ForegroundColor Green
Write-Host "  按任意键停止所有服务..."
$null = $host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
Write-Host "正在停止..."
if ($backendJob)  { Stop-Process -Id $backendJob.Id -Force -ErrorAction SilentlyContinue }
if ($frontendJob) { Stop-Process -Id $frontendJob.Id -Force -ErrorAction SilentlyContinue }
Write-Host "已停止，再见！"
