"""CI-only smoke check: launches a packaged app against a disposable Compose DB.
Run from the repository root after compose up --wait and mvnw package.
Creates synthetic records; never point it at an existing database.
"""
import http.cookiejar
import json
import os
from pathlib import Path
import subprocess
import threading
import time
import urllib.error
import urllib.request
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

MODE = 'querypilot'
PORT = 8083
ARTIFACT = 'querypilot'
BASE = f'http://localhost:{PORT}'

class Client:
    def __init__(self):
        self.http = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

    def call(self, method, path, body=None, expected=200, csrf=False):
        headers = {}
        if csrf:
            token = self.call('GET', '/api/auth/csrf')
            headers[token['headerName']] = token['token']
        data = None if body is None else json.dumps(body).encode()
        if data is not None:
            headers['Content-Type'] = 'application/json'
        request = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
        try:
            response = self.http.open(request, timeout=10)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            raw = response.read().decode()
            assert response.code == expected, (path, response.code, raw[:500])
            if not raw:
                return None
            try:
                return json.loads(raw)
            except json.JSONDecodeError:
                return raw

provider_requests = []
class Provider(BaseHTTPRequestHandler):
    def do_POST(self):
        if self.headers.get('Transfer-Encoding', '').lower() == 'chunked':
            chunks = []
            while True:
                size = int(self.rfile.readline().split(b';', 1)[0], 16)
                if size == 0:
                    while self.rfile.readline().strip():
                        pass
                    break
                chunks.append(self.rfile.read(size))
                assert self.rfile.read(2) == b'\r\n'
            raw = b''.join(chunks)
        else:
            raw = self.rfile.read(int(self.headers['Content-Length']))
        payload = json.loads(raw)
        provider_requests.append(payload)
        body = json.dumps({'choices': [{'message': {'content': 'SELECT name, salary FROM employees WHERE salary > 50000'}}]}).encode()
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *args):
        pass


def querypilot():
    c = Client()
    collection = json.loads(Path('docs/api.postman_collection.json').read_text())
    body = json.loads(collection['item'][1]['request']['body']['raw'])
    result = c.call('POST', '/api/query', body)
    assert result['rowCount'] == 1, result
    assert result['data'][0]['name'] == 'Asha', result
    assert provider_requests and 'employees' in provider_requests[0]['messages'][0]['content']
    # Test permissions directly as the same account configured for the application.
    command = ['docker', 'compose', 'exec', '-T', 'mysql', 'sh', '-c',
               'MYSQL_PWD="$MYSQL_PASSWORD" mysql -h 127.0.0.1 -u"$MYSQL_USER" "$MYSQL_DATABASE" -e "INSERT INTO employees VALUES (99, \'Forbidden\', \'Test\', 1)"']
    denied = subprocess.run(command, capture_output=True, text=True)
    assert denied.returncode != 0 and 'INSERT command denied' in denied.stderr, denied.stderr


def main():
    env = os.environ.copy()
    env['SERVER_PORT'] = str(PORT)
    mock = None
    if MODE == 'hospital':
        env.update(APP_BOOTSTRAPADMIN_ENABLED='true', APP_BOOTSTRAPADMIN_EMAIL='ci-admin@example.com',
                   APP_BOOTSTRAPADMIN_PASSWORD='ci-demo-password-2026')
    if MODE == 'querypilot':
        mock = ThreadingHTTPServer(('127.0.0.1', 0), Provider)
        threading.Thread(target=mock.serve_forever, daemon=True).start()
        env.update(DEEPSEEK_API_KEY='ci-mock-only', DEEPSEEK_API_URL=f'http://127.0.0.1:{mock.server_port}/chat/completions')
    Path('.smoke').mkdir(exist_ok=True)
    with Path('.smoke/application.log').open('w') as log:
        app = subprocess.Popen(['java', '-jar', f'target/{ARTIFACT}-0.0.1-SNAPSHOT.jar', '--spring.profiles.active=local'], env=env, stdout=log, stderr=subprocess.STDOUT)
        try:
            path = '/api/materials' if MODE == 'procureflow' else '/'
            deadline = time.monotonic() + 90
            while True:
                if app.poll() is not None:
                    raise RuntimeError('Application exited; see .smoke/application.log')
                try:
                    Client().call('GET', path)
                    break
                except (OSError, AssertionError):
                    if time.monotonic() >= deadline:
                        raise RuntimeError('Application did not become ready; see .smoke/application.log')
                    time.sleep(1)
            globals()[MODE]()
            print(f'{MODE}: Compose database, local profile and HTTP smoke checks passed')
        finally:
            app.terminate()
            try:
                app.wait(timeout=15)
            except subprocess.TimeoutExpired:
                app.kill()
                app.wait()
            if mock:
                mock.shutdown()
                mock.server_close()

if __name__ == '__main__':
    main()
