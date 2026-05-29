/**
 * 拼团锁单轻量压测（方案 A · 切片 3）
 * 只压营销服务：POST /api/v1/gbm/trade/lock_market_pay_order
 *
 * 用法（本机已启动 8091）：
 *   docker run --rm -i -v "%cd%/lock_order.js:/scripts/lock_order.js" ^
 *     -e BASE_URL=http://host.docker.internal:8091 grafana/k6 run /scripts/lock_order.js
 *
 * 或安装 k6 后：k6 run lock_order.js
 */
import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';

const lockDuration = new Trend('lock_order_duration', true);

const BASE = __ENV.BASE_URL || 'http://127.0.0.1:8091';
const VUS = parseInt(__ENV.VUS || '50', 10);
const DURATION = __ENV.DURATION || '60s';
const RAMP = __ENV.RAMP || '20s';

export const options = {
  scenarios: {
    lock_order: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: RAMP, target: VUS },
        { duration: DURATION, target: VUS },
        { duration: '10s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.5'],
    http_req_duration: ['p(99)<10000'],
  },
};

export default function () {
  const vu = __VU;
  const iter = __ITER;
  const userId = `load_u${vu}_${iter % 1000}`;
  const outTradeNo = `L${Date.now()}${vu}${iter}`;
  const payload = JSON.stringify({
    userId,
    teamId: null,
    activityId: parseInt(__ENV.ACTIVITY_ID || '100123', 10),
    goodsId: __ENV.GOODS_ID || '9890001',
    source: 's01',
    channel: 'c01',
    notifyConfigVO: { notifyType: 'MQ' },
    outTradeNo,
  });

  const res = http.post(
    `${BASE}/api/v1/gbm/trade/lock_market_pay_order`,
    payload,
    { headers: { 'Content-Type': 'application/json' }, timeout: '30s' }
  );

  lockDuration.add(res.timings.duration);

  check(res, {
    'http 200': (r) => r.status === 200,
    'body has code': (r) => r.body && r.body.includes('code'),
  });
}

export function handleSummary(data) {
  const m = data.metrics;
  const line = (name, key) => {
    const v = m[key];
    if (!v || !v.values) return `${name}: n/a`;
    return `${name}: avg=${v.values.avg?.toFixed(1)}ms p95=${v.values['p(95)']?.toFixed(1)}ms p99=${v.values['p(99)']?.toFixed(1)}ms`;
  };
  const summary = [
    '=== lock_market_pay_order load test ===',
    `vus=${VUS} ramp=${RAMP} duration=${DURATION}`,
    line('http_req_duration', 'http_req_duration'),
    `http_reqs: rate=${m.http_reqs?.values?.rate?.toFixed(2)}/s total=${m.http_reqs?.values?.count}`,
    `http_req_failed: ${((m.http_req_failed?.values?.rate || 0) * 100).toFixed(2)}%`,
    line('lock_order_duration', 'lock_order_duration'),
  ].join('\n');
  console.log(summary);
  return {
    stdout: summary + '\n',
    'summary.json': JSON.stringify(data, null, 2),
  };
}
