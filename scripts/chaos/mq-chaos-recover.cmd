@echo off
REM 场景2：MQ 停服 -> notify_task 积压 -> 恢复后手动补偿
echo 1) Stop RabbitMQ:
echo    docker stop rabbitmq
echo.
echo 2) Trigger group complete flow (pay/settlement) while MQ down
echo    observe: gbm_notify_task_pending rises in Grafana
echo.
echo 3) Start RabbitMQ:
echo    docker start rabbitmq
echo.
echo 4) Run compensation on marketing 8091:
curl -s -X POST http://127.0.0.1:8091/api/v1/dev/chaos/run-notify-job
echo.
echo 5) SQL check:
echo    mysql -h127.0.0.1 -P13306 -uroot -p123456 -e "select notify_status,count(*) from group_buy_market.notify_task group by notify_status;"
pause
