/**
 * k6 multi-tenant smoke / baseline load script for Product Costing platform.
 *
 * Target capacity guidance (to be proven, not claimed):
 *   - 1,000 customers
 *   - 2,000–5,000 concurrent users at peak
 *
 * This script is a starting harness. Replace BASE_URL, tokens, and customer IDs
 * with environment-specific values. Run against staging that mirrors production
 * topology (N backends, real MySQL, Redis when available).
 *
 * Usage:
 *   k6 run -e BASE_URL=https://staging.example.com \
 *          -e TOKEN_A=... -e TOKEN_B=... \
 *          -e CUSTOMER_A=... -e CUSTOMER_B=... \
 *          load-tests/k6-multi-tenant-smoke.js
 *
 * Acceptance (example — measure, do not invent):
 *   - p95 latency for normal APIs < 500 ms under target concurrency
 *   - error rate < 1% excluding intentional 4xx
 *   - zero cross-tenant 200 responses when Customer A calls with Customer B IDs
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN_A = __ENV.TOKEN_A || '';
const TOKEN_B = __ENV.TOKEN_B || '';
const CUSTOMER_A = __ENV.CUSTOMER_A || '';
const CUSTOMER_B = __ENV.CUSTOMER_B || '';

const errorRate = new Rate('errors');
const latency = new Trend('api_latency', true);

export const options = {
  scenarios: {
    smoke: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 50 },
        { duration: '3m', target: 200 },
        { duration: '2m', target: 500 },
        { duration: '2m', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<800'], // tighten to 500 ms after optimization
    errors: ['rate<0.05'],
  },
};

function authHeaders(token, customerId) {
  const h = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  };
  if (customerId) {
    h['X-Customer-Id'] = customerId;
  }
  return h;
}

export default function () {
  const useA = Math.random() < 0.5;
  const token = useA ? TOKEN_A : TOKEN_B;
  const customer = useA ? CUSTOMER_A : CUSTOMER_B;

  if (!token || !customer) {
    console.error('TOKEN_A/B and CUSTOMER_A/B must be set');
    return;
  }

  group('health', () => {
    const res = http.get(`${BASE_URL}/actuator/health`);
    check(res, { 'health UP': (r) => r.status === 200 });
    latency.add(res.timings.duration);
  });

  group('authenticated_dashboard_like', () => {
    // Replace with real lightweight list endpoints present in the deployment
    const res = http.get(`${BASE_URL}/api/v1/notifications?page=0&size=20`, {
      headers: authHeaders(token, customer),
    });
    const ok = check(res, {
      'notifications 2xx/404': (r) => r.status === 200 || r.status === 404 || r.status === 403,
    });
    errorRate.add(!ok);
    latency.add(res.timings.duration);
  });

  group('cross_tenant_probe', () => {
    // Customer A token must not succeed reading Customer B context with elevated data
    if (TOKEN_A && CUSTOMER_B) {
      const res = http.get(`${BASE_URL}/api/v1/notifications?page=0&size=5`, {
        headers: authHeaders(TOKEN_A, CUSTOMER_B),
      });
      const blocked = check(res, {
        'cross-tenant blocked (403/401)': (r) => r.status === 403 || r.status === 401,
      });
      errorRate.add(!blocked);
    }
  });

  sleep(0.5 + Math.random());
}
