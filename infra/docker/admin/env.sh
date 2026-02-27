#!/bin/bash
# Runs as part of nginx's docker-entrypoint.d — writes runtime env vars into
# the Angular app's env.js before nginx starts.

ENV_FILE=/usr/share/nginx/html/assets/env.js

cat > "$ENV_FILE" <<EOF
(function(window) {
    window["env"] = window["env"] || {};
    window["env"]["APP_BASE_URL"] = "${APP_BASE_URL:-http://localhost:8080/api}";
    window["env"]["APP_SHIPPING_URL"] = "${APP_SHIPPING_URL:-}";
    window["env"]["APP_MAP_API_KEY"] = "${APP_MAP_API_KEY:-}";
    window["env"]["APP_DEFAULT_LANGUAGE"] = "${APP_DEFAULT_LANGUAGE:-en}";
})(this);
EOF

echo "env.sh: wrote APP_BASE_URL=${APP_BASE_URL:-http://localhost:8080/api} to $ENV_FILE"
