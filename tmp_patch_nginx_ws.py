from pathlib import Path

conf = Path('/etc/nginx/conf.d/mallfei.conf')
text = conf.read_text()

start_marker = '    # Backend API proxy. Only /api/ requests are forwarded to local Java backend.\n'
end_marker = '    # Upload/static backend resources, kept under the same HTTPS domain.\n'

start = text.find(start_marker)
end = text.find(end_marker)

if start == -1 or end == -1 or end <= start:
    raise SystemExit(f'Could not find nginx API block markers: start={start}, end={end}')

replacement = '''    # Backend API proxy. Only /api/ requests are forwarded to local Java backend.
    location /api/ {
        proxy_pass http://127.0.0.1:9090;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-Scheme https;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port 443;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_connect_timeout 30s;
        proxy_send_timeout 120s;
        proxy_read_timeout 120s;
    }

    # Admin force-logout websocket proxy.
    location /ws/ {
        proxy_pass http://127.0.0.1:9090;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port 443;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 3600s;
    }

'''

new_text = text[:start] + replacement + text[end:]
conf.write_text(new_text)
print('patched /etc/nginx/conf.d/mallfei.conf')
