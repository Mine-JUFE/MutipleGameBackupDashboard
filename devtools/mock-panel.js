/*
 * 开发用假翼龙面板（Mock Panel）
 * ------------------------------------------------------------
 * 作用：不需要真实面板就能验证本项目的 API 调用是否正确。
 * 它按翼龙 Client API v1 的约定回应两个接口：
 *   GET  /api/client/servers/:id/backups   -> 返回备份列表
 *   POST /api/client/servers/:id/backups   -> 返回 202，并记入列表
 * 同时把收到的每一个请求（方法、路径、Authorization 头、body）打到日志里，
 * 方便确认"我们发出去的请求到底长什么样"。
 *
 * 运行：node devtools/mock-panel.js [端口]
 * 默认端口 9099；配合 src/main/resources/application-mock.yml 使用。
 */
const http = require('http');

const PORT = Number(process.argv[2] || 9099);
const SERVER_ID = 'mock-server-01';
const API_KEY = 'ptlc_mock_key_for_local_test';

/** 内存里的备份列表，POST 会往里追加 */
const backups = [
  {
    object: 'backup',
    attributes: {
      uuid: '11111111-1111-1111-1111-111111111111',
      name: '20260101-030000-backup.tar.gz',
      bytes: 524288000,
      created_at: '2026-01-01T03:00:00+00:00',
      completed_at: '2026-01-01T03:02:11+00:00',
      is_successful: true,
      checksum: 'sha256:aaaabbbbcccc'
    }
  },
  {
    object: 'backup',
    attributes: {
      uuid: '22222222-2222-2222-2222-222222222222',
      name: '20251225-030000-backup.tar.gz',
      bytes: 1048576,
      created_at: '2025-12-25T03:00:00+00:00',
      completed_at: null,
      is_successful: false,
      checksum: null
    }
  }
];

function json(res, status, payload) {
  const body = JSON.stringify(payload);
  res.writeHead(status, {
    'Content-Type': 'application/json',
    'Content-Length': Buffer.byteLength(body)
  });
  res.end(body);
}

const server = http.createServer((req, res) => {
  let raw = '';
  req.on('data', (chunk) => { raw += chunk; });
  req.on('end', () => {
    const auth = req.headers['authorization'] || '(无)';
    console.log(`[mock-panel] ${req.method} ${req.url} Authorization=${auth} body=${raw || '(空)'}`);

    const backupPath = `/api/client/servers/${SERVER_ID}/backups`;
    const isOurServer = req.url === backupPath || req.url.startsWith(backupPath + '?');

    if (!isOurServer) {
      return json(res, 404, { errors: [{ code: 'NotFoundHttpException', detail: 'The requested resource was not found.' }] });
    }
    if (auth !== `Bearer ${API_KEY}`) {
      return json(res, 401, { errors: [{ code: 'AuthenticationException', detail: 'Unauthenticated: 无效的 API Key。' }] });
    }

    if (req.method === 'GET') {
      return json(res, 200, {
        object: 'list',
        data: backups,
        meta: { pagination: { total: backups.length, count: backups.length, per_page: 50, current_page: 1, total_pages: 1 } }
      });
    }

    if (req.method === 'POST') {
      const stamp = new Date().toISOString().replace(/[-:T]/g, '').slice(0, 14);
      backups.push({
        object: 'backup',
        attributes: {
          uuid: `33333333-3333-3333-3333-${String(backups.length).padStart(12, '0')}`,
          name: `${stamp}-backup.tar.gz`,
          bytes: 0,
          created_at: new Date().toISOString(),
          completed_at: null,
          is_successful: false,
          checksum: null
        }
      });
      return json(res, 202, {
        object: 'backup',
        attributes: backups[backups.length - 1].attributes
      });
    }

    return json(res, 405, { errors: [{ code: 'MethodNotAllowed', detail: '方法不允许。' }] });
  });
});

server.listen(PORT, '127.0.0.1', () => {
  console.log(`[mock-panel] 假翼龙面板已启动：http://127.0.0.1:${PORT}  (serverId=${SERVER_ID}, apiKey=${API_KEY})`);
});
