# Build: docker build -t ghcr.io/<your-org>/nuvioweb:latest -f docker/Dockerfile .
# Push:  docker push ghcr.io/<your-org>/nuvioweb:latest

FROM nginx:stable-alpine
RUN apk add --no-cache brotli gettext
RUN mkdir -p /etc/nginx/templates

# Build context must be the project root (.)
# Web artifacts must be pre-collected into web-dist/ by the caller
COPY docker/default.conf.template /etc/nginx/templates/default.conf.template
COPY web-dist/ /usr/share/nginx/html/
RUN find /usr/share/nginx/html -type f \( -name "*.js" -o -name "*.wasm" -o -name "*.css" \) \
    -exec sh -c 'gzip -kf "$1"; brotli -f "$1"' _ {} \;

LABEL org.opencontainers.image.source="https://github.com/albyalex96/NuvioMobile"
LABEL org.opencontainers.image.description="Nuvio Enhanced - Web WASM build"
LABEL org.opencontainers.image.licenses="MIT"

ENV PORT=7860
ENV NGINX_CONFIG=""
EXPOSE 7860
CMD if [ -n "$NGINX_CONFIG" ]; then \
      printf '%s\n' "$NGINX_CONFIG" > /etc/nginx/templates/default.conf.template && \
      echo "Using custom NGINX_CONFIG from env var"; \
    fi \
    && envsubst '${PORT}' < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf \
    && nginx -g 'daemon off;'
