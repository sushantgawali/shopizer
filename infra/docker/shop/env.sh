#!/bin/bash
# Runs as part of nginx's docker-entrypoint.d — regenerates env-config.js
# with current container env vars before nginx starts.
# The React app loads /env-config.js at runtime via public/index.html.

cat > /usr/share/nginx/html/env-config.js <<EOF
window._env_ = {
  APP_BASE_URL:           "${APP_BASE_URL:-http://localhost:8080}",
  APP_API_VERSION:        "${APP_API_VERSION:-/api/v1/}",
  APP_MERCHANT:           "${APP_MERCHANT:-DEFAULT}",
  APP_PRODUCT_GRID_LIMIT: "${APP_PRODUCT_GRID_LIMIT:-15}",
  APP_PAYMENT_TYPE:       "${APP_PAYMENT_TYPE:-STRIPE}",
  APP_STRIPE_KEY:         "${APP_STRIPE_KEY:-}",
  APP_THEME_COLOR:        "${APP_THEME_COLOR:-#D1D1D1}",
  APP_MAP_API_KEY:        "${APP_MAP_API_KEY:-}"
};
EOF

echo "env.sh: wrote env-config.js (APP_BASE_URL=${APP_BASE_URL:-http://localhost:8080})"
