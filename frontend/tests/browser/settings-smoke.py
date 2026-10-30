"""Read-only settings smoke test. Requires Python Playwright and Chromium.

QBlog admin credentials must be supplied via QBLOG_TEST_USERNAME/PASSWORD.
Run a frontend dev/preview server first; defaults to http://localhost:3017/.
The browser API is forwarded to a local backend for preview-server checks.
No password, token, backup or existing note mutations are performed.
"""
import os
from urllib.parse import urlparse
from playwright.sync_api import sync_playwright

FRONTEND = os.environ.get('QBLOG_TEST_FRONTEND', 'http://localhost:3017/').rstrip('/')
BACKEND = os.environ.get('QBLOG_TEST_BACKEND', 'http://localhost:8081').rstrip('/')
USERNAME = os.environ['QBLOG_TEST_USERNAME']
PASSWORD = os.environ['QBLOG_TEST_PASSWORD']
for address in [FRONTEND, BACKEND]:
    if urlparse(address).hostname not in ['localhost', '127.0.0.1', '::1']:
        raise ValueError('This smoke script is restricted to local services')

with sync_playwright() as playwright:
    options = {'headless': True}
    executable = os.environ.get('QBLOG_TEST_CHROMIUM')
    if executable:
        options['executable_path'] = executable
    browser = playwright.chromium.launch(**options)
    try:
        page = browser.new_page(viewport={'width': 1440, 'height': 900})
        errors = []
        page.on('pageerror', lambda error: errors.append(error.stack))
        response = page.request.post(BACKEND + '/api/auth/login', data={'username': USERNAME, 'password': PASSWORD}).json()
        assert response['code'] == 200, 'Login failed'
        token = response['data']
        assert isinstance(token, str), 'Unexpected login response contract'
        identity = page.request.get(BACKEND + '/api/auth/me', headers={'Authorization': 'Bearer ' + token})
        user = identity.json()
        assert user['code'] == 200 and user['data']['role'] == 1, 'Admin identity verification failed'

        def forward_readonly_api(route):
            request = route.request
            assert request.method == 'GET', 'Unexpected mutation during read-only smoke test'
            api_path = urlparse(request.url).path
            assert api_path.startswith('/api/'), 'Unexpected API path'
            query = urlparse(request.url).query
            result = page.request.get(BACKEND + api_path + ('?' + query if query else ''), headers={'Authorization': 'Bearer ' + token})
            route.fulfill(response=result)

        page.route('**/api/**', forward_readonly_api)
        page.goto(FRONTEND + '/login')
        page.evaluate('([token,user]) => { localStorage.setItem("token",token); localStorage.setItem("userInfo",JSON.stringify(user)); }', [token, user['data']])
        page.goto(FRONTEND + '/settings/appearance')
        page.get_by_role('button', name='深色', exact=True).click()
        assert page.locator('html').get_attribute('data-theme') == 'dark'
        page.get_by_role('button', name='紧凑 · 仅标题', exact=True).click()
        page.reload()
        assert page.get_by_role('button', name='紧凑 · 仅标题', exact=True).get_attribute('aria-pressed') == 'true'
        page.get_by_role('button', name='浅色', exact=True).click()
        page.get_by_role('button', name='标准 · 标题与摘要', exact=True).click()
        for width in [1440, 390]:
            page.set_viewport_size({'width': width, 'height': 900})
            for path, title in [('appearance', '外观与布局'), ('backup', '备份与导出'), ('tokens', 'API 与 MCP')]:
                page.goto(FRONTEND + '/settings/' + path)
                page.get_by_role('heading', name=title, exact=True).wait_for()
                assert page.get_by_role('navigation', name='工作区设置').is_visible()
                assert page.locator('html').evaluate('(element) => element.scrollWidth <= innerWidth')
            page.goto(FRONTEND + '/user/profile')
            page.get_by_role('heading', name='账号与密码', exact=True).wait_for()
            page.get_by_role('button', name='修改密码', exact=True).click()
            page.get_by_role('button', name='确认修改', exact=True).click()
            page.get_by_text('请输入旧密码', exact=True).wait_for(state='visible')
            assert page.locator('html').evaluate('(element) => element.scrollWidth <= innerWidth')
        assert not errors, errors
        print('PASS settings routes, mobile layout, preference persistence, password validation; no browser errors')
    finally:
        browser.close()
