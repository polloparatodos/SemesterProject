import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 20 },
    { duration: '1m', target: 20 },
    { duration: '20s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.05'],
  },
};

const BASE_URL = 'http://localhost:8080';

export function setup() {
  const jsonHeaders = { headers: { 'Content-Type': 'application/json' } };

  // Step 1: create a customer (customer-service permits unauthenticated POSTs)
  const customerRes = http.post(`${BASE_URL}/api/customers`, JSON.stringify({
    organizationName: 'K6 Load Test Org',
    contactName: 'K6 Runner',
    email: 'k6@agenthub.test',
    subscriptionPlan: 'STANDARD',
    status: 'ACTIVE',
  }), jsonHeaders);

  const customerId = customerRes.json('id');
  if (!customerId) {
    console.error('Failed to create customer: ' + customerRes.body);
    return { token: null };
  }

  // Step 2: register using the new customer ID
  const registerRes = http.post(`${BASE_URL}/api/auth/register`, JSON.stringify({
    customerId: customerId,
    username: 'k6_seed_user',
    email: 'k6_seed@agenthub.test',
    password: 'K6Stress@123',
  }), jsonHeaders);

  if (registerRes.status !== 200 && registerRes.status !== 201) {
    console.error('Register failed (' + registerRes.status + '): ' + registerRes.body);
  }

  // Step 3: login to get JWT
  const loginRes = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
    username: 'k6_seed_user',
    password: 'K6Stress@123',
  }), jsonHeaders);

  const token = loginRes.json('token');
  if (!token) {
    console.error('Login failed (' + loginRes.status + '): ' + loginRes.body);
  }
  return { token };
}

export default function (data) {
  const token = data.token;

  const authHeaders = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  };

  // GET agents
  const agentsRes = http.get(`${BASE_URL}/api/agents`, authHeaders);
  check(agentsRes, {
    'GET /api/agents status 200': (r) => r.status === 200,
    'GET /api/agents responds fast': (r) => r.timings.duration < 2000,
  });

  sleep(0.5);

  // GET customers
  const customersRes = http.get(`${BASE_URL}/api/customers`, authHeaders);
  check(customersRes, {
    'GET /api/customers status 200': (r) => r.status === 200,
    'GET /api/customers responds fast': (r) => r.timings.duration < 2000,
  });

  sleep(0.5);

  // GET deployments
  const deploymentsRes = http.get(`${BASE_URL}/api/deployments`, authHeaders);
  check(deploymentsRes, {
    'GET /api/deployments status 200': (r) => r.status === 200,
    'GET /api/deployments responds fast': (r) => r.timings.duration < 2000,
  });

  sleep(1);
}
