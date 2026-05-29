# 锁单压测脚本（切片 3）

## 前置

1. 中间件 + 营销服务 `8091` 已启动（`application-dev`）
2. 可选：Prometheus/Grafana 已起，压测时在 Grafana「拼团业务监控」看 QPS / 耗时

## 方式一：Docker（无需本机安装 k6）

```powershell
cd f:\实习\s-pay-mall-vs-group-buy\scripts\load

# 基线（优化前请先不要改 application-dev.yml 里 tomcat 段）
docker run --rm -i `
  -v "${PWD}/lock_order.js:/scripts/lock_order.js" `
  -e BASE_URL=http://host.docker.internal:8091 `
  -e VUS=50 -e DURATION=60s -e RAMP=20s `
  grafana/k6 run /scripts/lock_order.js

# 优化后：重启 8091 再跑同样命令，对比终端输出的 p99
```

## 方式二：本机 k6

```powershell
winget install k6 --source winget
k6 run lock_order.js
```

环境变量：`BASE_URL`、`VUS`（默认 50）、`DURATION`（默认 60s）、`RAMP`（默认 20s）、`ACTIVITY_ID`、`GOODS_ID`。

## 结果归档

将两次运行的终端输出填入 `docs/perf/perf-report.md` §4.2。

**无 k6 快速对比（已用于报告基线）：**

若提示「禁止运行脚本」，用下面任一方式（**不要**直接 `& .\run-lock-parallel.ps1`）：

```cmd
cd /d f:\实习\s-pay-mall-vs-group-buy\scripts\load
run-lock-parallel.cmd -Label before-tomcat
run-lock-parallel.cmd -Label after-tomcat
```

或 PowerShell 单次绕过策略：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File "f:\实习\s-pay-mall-vs-group-buy\scripts\load\run-lock-parallel.ps1" -Label after-tomcat
```
