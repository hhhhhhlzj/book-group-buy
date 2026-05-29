# 无 k6 时的轻量压测（Runspace 并行 POST）
param(
    [string]$BaseUrl = "http://127.0.0.1:8091",
    [int]$Concurrency = 50,
    [int]$Requests = 150,
    [int]$ActivityId = 100123,
    [string]$GoodsId = "9890001",
    [string]$Label = "run"
)

$uri = "$BaseUrl/api/v1/gbm/trade/lock_market_pay_order"
$perWorker = [int][math]::Ceiling($Requests / [double]$Concurrency)
$sw = [System.Diagnostics.Stopwatch]::StartNew()

$pool = [runspacefactory]::CreateRunspacePool(1, $Concurrency)
$pool.Open()
$jobs = @()

for ($w = 1; $w -le $Concurrency; $w++) {
    $workerId = $w
    $ps = [powershell]::Create().AddScript({
        param($uri, $workerId, $perWorker, $activityId, $goodsId)
        $out = @()
        for ($i = 0; $i -lt $perWorker; $i++) {
            $body = @{
                userId = "pw_u${workerId}_$i"
                teamId = $null
                activityId = $activityId
                goodsId = $goodsId
                source = "s01"
                channel = "c01"
                notifyConfigVO = @{ notifyType = "MQ" }
                outTradeNo = "PW$(Get-Random -Maximum 999999999999)${workerId}$i"
            } | ConvertTo-Json -Depth 5
            $t = [System.Diagnostics.Stopwatch]::StartNew()
            try {
                $r = Invoke-WebRequest -Method POST -Uri $uri -Body $body -ContentType "application/json" -UseBasicParsing -TimeoutSec 30
                $t.Stop()
                $out += [PSCustomObject]@{ ms = $t.ElapsedMilliseconds; ok = ($r.StatusCode -eq 200) }
            } catch {
                $t.Stop()
                $out += [PSCustomObject]@{ ms = $t.ElapsedMilliseconds; ok = $false }
            }
        }
        return $out
    }).AddArgument($uri).AddArgument($workerId).AddArgument($perWorker).AddArgument($ActivityId).AddArgument($GoodsId)
    $ps.RunspacePool = $pool
    $jobs += [PSCustomObject]@{ Pipe = $ps; Handle = $ps.BeginInvoke() }
}

$flat = foreach ($j in $jobs) {
    $j.Pipe.EndInvoke($j.Handle)
    $j.Pipe.Dispose()
}
$pool.Close()
$pool.Dispose()
$sw.Stop()

$sorted = @($flat.ms | Sort-Object)
$n = $sorted.Count
if ($n -eq 0) { Write-Error "no samples"; exit 1 }

$idx95 = [Math]::Max(0, [int][Math]::Ceiling(0.95 * $n) - 1)
$idx99 = [Math]::Max(0, [int][Math]::Ceiling(0.99 * $n) - 1)
$report = [ordered]@{
    label = $Label
    total = $n
    ok = (@($flat | Where-Object { $_.ok })).Count
    wallSec = [math]::Round($sw.Elapsed.TotalSeconds, 2)
    qps = [math]::Round($n / $sw.Elapsed.TotalSeconds, 2)
    avgMs = [math]::Round(($sorted | Measure-Object -Average).Average, 1)
    p95Ms = $sorted[$idx95]
    p99Ms = $sorted[$idx99]
    maxMs = $sorted[-1]
}

Write-Host "=== lock load [$Label] concurrency=$Concurrency requests~$Requests ==="
$report.GetEnumerator() | ForEach-Object { Write-Host "$($_.Key): $($_.Value)" }

$out = Join-Path $PSScriptRoot "results-$Label.txt"
$report.GetEnumerator() | ForEach-Object { "$($_.Key)=$($_.Value)" } | Set-Content -Encoding utf8 $out
Write-Host "saved: $out"
