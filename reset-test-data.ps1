[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[Console]::OutputEncoding = $utf8NoBom
$OutputEncoding = $utf8NoBom

function Get-DotEnvValue {
    param(
        [string]$Path,
        [string]$Name,
        [string]$DefaultValue = ''
    )

    if (-not (Test-Path $Path)) {
        return $DefaultValue
    }

    $match = Get-Content -Path $Path | Where-Object { $_ -match "^\s*$Name=(.*)$" } | Select-Object -First 1
    if (-not $match) {
        return $DefaultValue
    }

    return ($match -replace "^\s*$Name=", '').Trim()
}

$root = $PSScriptRoot
$envPath = Join-Path $root '.env'
$sqlPath = Join-Path $root 'lifeos-backend\db\seeds\reset_test_data.sql'

if (-not (Test-Path $sqlPath)) {
    throw "Seed SQL not found: $sqlPath"
}

$mysqlHost = Get-DotEnvValue -Path $envPath -Name 'MYSQL_HOST' -DefaultValue '127.0.0.1'
$mysqlPort = Get-DotEnvValue -Path $envPath -Name 'MYSQL_PORT' -DefaultValue '3306'
$mysqlDatabase = Get-DotEnvValue -Path $envPath -Name 'MYSQL_DATABASE' -DefaultValue 'lifeos'
$mysqlUser = Get-DotEnvValue -Path $envPath -Name 'MYSQL_USERNAME' -DefaultValue 'root'
$mysqlPassword = Get-DotEnvValue -Path $envPath -Name 'MYSQL_PASSWORD' -DefaultValue ''
$redisHost = Get-DotEnvValue -Path $envPath -Name 'REDIS_HOST' -DefaultValue '127.0.0.1'
$redisPort = Get-DotEnvValue -Path $envPath -Name 'REDIS_PORT' -DefaultValue '6379'

$mysql = (Get-Command mysql -ErrorAction Stop).Source

Write-Host "Resetting database '$mysqlDatabase' on $mysqlHost`:$mysqlPort ..."
Get-Content -Path $sqlPath -Raw -Encoding UTF8 | & $mysql --default-character-set=utf8mb4 -h $mysqlHost -P $mysqlPort -u $mysqlUser "-p$mysqlPassword" $mysqlDatabase

$redisCli = Get-Command redis-cli -ErrorAction SilentlyContinue
if ($redisCli) {
    $tokenKeys = @(& $redisCli.Source -h $redisHost -p $redisPort --scan --pattern 'token:*')
    $limitKeys = @(& $redisCli.Source -h $redisHost -p $redisPort --scan --pattern 'login:limit:*')
    $keysToDelete = @($tokenKeys + $limitKeys | Where-Object { $_ })
    if ($keysToDelete.Count -gt 0) {
        Write-Host "Removing stale Redis auth keys ..."
        foreach ($key in $keysToDelete) {
            & $redisCli.Source -h $redisHost -p $redisPort DEL $key | Out-Null
        }
    }
}

Write-Host ''
Write-Host 'Seeded users (password: Pass123456):'
Write-Host '  liwen_pm      产品经理，关注发布、访谈和首页工作台'
Write-Host '  zhouyi_dev    后端开发，关注异步链路、Redis 和联调'
Write-Host '  heqing_fit    健身习惯用户，关注训练、备餐和睡眠'
Write-Host '  susu_creator  内容创作者，关注选题、脚本和合作跟进'
Write-Host '  chenyu_grad   研究生，关注论文阅读、实验设计和导师反馈'
