@echo off
REM 场景1：重复支付回调（需已 PAY_SUCCESS 的 orderId）
REM 用法：duplicate-pay-notify.cmd <outTradeNo>
set ORDER_ID=%~1
if "%ORDER_ID%"=="" (
  echo Usage: duplicate-pay-notify.cmd ^<outTradeNo^>
  exit /b 1
)
set URL=http://127.0.0.1:8070/api/v1/alipay/active_pay_notify?outTradeNo=%ORDER_ID%
echo === 1st notify ===
curl -s -X POST "%URL%"
echo.
echo === 2nd notify (expect idempotent) ===
curl -s -X POST "%URL%"
echo.
echo Check Prometheus: mall_alipay_notify_total{result="duplicate"}
pause
