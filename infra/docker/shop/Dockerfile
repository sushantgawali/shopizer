FROM nginx:stable-alpine
RUN apk add --no-cache bash
# build/ is extracted from build.zip (GitHub Release artifact) by the Makefile
COPY build/ /usr/share/nginx/html/
# Regenerates env-config.js with runtime values before nginx starts
COPY env.sh /docker-entrypoint.d/40-env.sh
RUN chmod +x /docker-entrypoint.d/40-env.sh
EXPOSE 80
