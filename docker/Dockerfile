FROM nginx:stable-alpine
RUN apk add --no-cache brotli gettext
RUN mkdir -p /etc/nginx/templates
RUN printf '%s\n' \
  'server {' \
  '  listen ${PORT};' \
  '  server_name _;' \
  '  root /usr/share/nginx/html;' \
  '  index index.html;' \
  '  gzip on;' \
  '  gzip_types text/plain text/css application/javascript application/wasm image/svg+xml;' \
  '  gzip_min_length 1024;' \
  '  gzip_vary on;' \
  '  gzip_static on;' \
  '  add_header X-Frame-Options "SAMEORIGIN" always;' \
  '  add_header X-Content-Type-Options "nosniff" always;' \
  '  add_header Referrer-Policy "strict-origin-when-cross-origin" always;' \
  '  location ~* \.(?:js|wasm)$ { expires 1y; add_header Cache-Control "public, immutable"; add_header Cross-Origin-Opener-Policy "same-origin"; add_header Cross-Origin-Embedder-Policy "require-corp"; }' \
  '  location /composeResources/ { expires 1y; add_header Cache-Control "public, immutable"; }' \
  '  location ~* \.map$ { expires -1; add_header Cache-Control "no-cache"; }' \
  '  location = /index.html { expires -1; add_header Cache-Control "no-cache, no-store, must-revalidate"; }' \
  '  location = /styles.css { expires 1y; add_header Cache-Control "public, immutable"; }' \
  '  location / { try_files $uri /index.html; }' \
  '}' \
  > /etc/nginx/templates/default.conf.template
COPY ./ /usr/share/nginx/html/
RUN find /usr/share/nginx/html -type f \( -name "*.js" -o -name "*.wasm" -o -name "*.css" \) \
    -exec sh -c 'gzip -kf "$1"; brotli -f "$1"' _ {} \;
ENV PORT=7860
ENV NGINX_CONFIG=""
EXPOSE 7860
CMD if [ -n "$NGINX_CONFIG" ]; then \
      printf '%s\n' "$NGINX_CONFIG" > /etc/nginx/templates/default.conf.template; \
    fi \
    && envsubst '${PORT}' < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf \
    && nginx -g 'daemon off;'
