#!/usr/bin/env node
/* 冒烟：侧栏「智能体」入口 → /assistant 全屏页（方案A），含建议卡发送、FAB 抽屉、/assistant 隐藏 FAB */
const { chromium } = require('playwright');

const FRONTEND = process.env.QBLOG_TEST_FRONTEND || 'http://localhost:3001/blog';
const BACKEND = process.env.QBLOG_TEST_BACKEND || 'http://localhost:8081';
const USERNAME = process.env.QBLOG_TEST_USERNAME || 'admin';
const PASSWORD = process.env.QBLOG_TEST_PASSWORD || 'admin123';
const ExecutablePath = process.env.QBLOG_TEST_CHROME || '/Users/duanluyao/Library/Caches/ms-playwright/chromium-1228/chrome-mac-arm64/Google Chrome for Testing.app/Contents/MacOS/Google Chrome for Testing';

(async () => {
  const browser = await chromium.launch({ headless: true, executablePath: ExecutablePath });
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  const errors = [];
  page.on('pageerror', e => errors.push('pageerror: ' + e.message));

  const res = await page.request.post(BACKEND + '/api/auth/login', { data: { username: USERNAME, password: PASSWORD } });
  const body = await res.json();
  if (body.code !== 200) throw new Error('登录失败: ' + JSON.stringify(body));
  const token = body.data;
  const me = await (await page.request.get(BACKEND + '/api/auth/me', { headers: { Authorization: 'Bearer ' + token } })).json();

  await page.goto(FRONTEND + '/login');
  await page.evaluate(([t, u]) => {
    localStorage.setItem('token', t);
    localStorage.setItem('userInfo', JSON.stringify(u));
    localStorage.removeItem('qblog_agent_conversations');
  }, [token, me.data]);

  let ok = 0, fail = 0;
  const check = (name, cond) => { console.log(`${cond ? '✅' : '❌'} ${name}`); cond ? ok++ : fail++; };

  // 1. 侧栏入口 → /assistant 全屏页
  await page.goto(FRONTEND + '/', { waitUntil: 'networkidle' });
  const entry = page.locator('[data-testid="kb-agent-entry"]');
  check('知识库侧栏存在智能体入口', await entry.count() === 1);
  await entry.click();
  await page.waitForURL('**/assistant', { timeout: 8000 });
  check('点击后跳转 /assistant', page.url().includes('/assistant'));

  // 2. 方案A hero + 建议卡（空腔 localStorage 已清，应为新会话）
  await page.waitForSelector('.ac-welcome', { timeout: 5000 });
  check('Hero 欢迎区渲染', await page.locator('.ac-welcome h2').count() === 1);
  check('4 张建议卡', await page.locator('.ac-sug').count() === 4);
  check('汉堡输入卡渲染', await page.locator('.composer .box').count() === 1);
  check('FAB 在 /assistant 隐藏', await page.locator('.agent-fab').count() === 0);

  // 3. 截图 hello（浅色）
  await page.screenshot({ path: 'frontend/tests/browser/_assistant-hero.png' });

  // 4. 点击「站点问答」建议卡（轻量任务）→ 真实发送（走 LLM）
  await page.locator('.ac-sug').nth(2).click();
  await page.waitForSelector('.msg-user', { timeout: 5000 });
  check('用户消息出现', (await page.locator('.msg-user').first().textContent()).includes('RAG'));
  // 等流结束：停止按钮消失（含 24 次工具上限内的大量调用，放宽等待）
  await page.waitForSelector('.composer .send.stop', { state: 'detached', timeout: 300000 });
  await page.waitForTimeout(500);
  const toolCount = await page.locator('.msg-ai .tool').count();
  const bodyLen = ((await page.locator('.msg-ai .body').last().textContent()) || '').trim().length;
  check(`AI 回答渲染（正文 ${bodyLen} 字）`, bodyLen > 0);
  console.log(`ℹ️  工具 chip 数量: ${toolCount}`);
  await page.screenshot({ path: 'frontend/tests/browser/_assistant-chat.png' });

  // 5. 顶栏按钮：历史会话 popover
  await page.locator('.agent-topbar .tb-ghost', { hasText: '历史会话' }).click();
  await page.waitForTimeout(400);
  check('历史 popover 打开', await page.locator('.history-item').count() >= 1);
  await page.keyboard.press('Escape');

  // 6. FAB 抽屉：回到知识库，FAB 应恢复且可用
  await page.goto(FRONTEND + '/', { waitUntil: 'networkidle' });
  check('知识库页 FAB 恢复', await page.locator('.agent-fab').count() === 1);
  await page.locator('.agent-fab').click();
  await page.waitForTimeout(500);
  check('抽屉打开且弹层内含 agent-chat', await page.locator('.agent-drawer .agent-chat').count() === 1);
  // 抽屉内应显示 mini 欢迎态而非 hero（新会话）
  await page.locator('.agent-drawer button:has-text("新会话")').first().click().catch(() => {});
  await page.waitForTimeout(400);
  check('抽屉 mini 欢迎态（无 hero 大标题）', await page.locator('.agent-drawer .ac-welcome h2').count() === 0);
  await page.screenshot({ path: 'frontend/tests/browser/_agent-drawer.png' });

  console.log(`\n结果: ${ok} 通过 / ${fail} 失败`);
  if (errors.length) console.log('页面错误:\n' + errors.join('\n'));
  await browser.close();
  process.exit(fail || errors.length ? 1 : 0);
})().catch(e => { console.error('冒烟失败:', e); process.exit(1); });
