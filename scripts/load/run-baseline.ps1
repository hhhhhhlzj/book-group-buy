# 切片3：Docker k6 压测封装（Windows）
param(
    [string]$Label = "run",
    [string]$BaseUrl = "http://host.docker.internal:8091",
    [int]$Vus = 50,
    [string]$Duration = "60s",
    [string]$Ramp = "20s"
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$outFile = Join-Path $scriptDir "results-$Label.txt"

Write-Host ">>> k6 $Label VUS=$Vus -> $outFile"

docker run --rm -i `
    -v "${scriptDir}/lock_order.js:/scripts/lock_order.js" `
    -e "BASE_URL=$BaseUrl" `
    -e "VUS=$Vus" `
    -e "DURATION=$Duration" `
    -e "RAMP=$Ramp" `
    grafana/k6 run /scripts/lock_order.js 2>&1 | Tee-Object -FilePath $outFile

Write-Host ">>> done: $outFile"
