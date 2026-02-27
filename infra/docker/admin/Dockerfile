FROM nginx:alpine
RUN apk add --no-cache bash
# dist.zip extracts to dist/dist; copy actual app files to nginx doc root
COPY dist/dist/ /usr/share/nginx/html/
# Injects APP_BASE_URL at container startup before nginx starts
COPY env.sh /docker-entrypoint.d/40-env.sh
RUN chmod +x /docker-entrypoint.d/40-env.sh
EXPOSE 80
