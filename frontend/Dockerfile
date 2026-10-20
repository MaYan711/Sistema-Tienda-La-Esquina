FROM node:24-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./

RUN npm ci

COPY . .

RUN npm run build && \
    INDEX_FILE="$(find dist -type f -name 'index.html' | head -n 1)" && \
    test -n "$INDEX_FILE" && \
    OUTPUT_DIR="$(dirname "$INDEX_FILE")" && \
    mkdir -p /app/output && \
    cp -a "$OUTPUT_DIR"/. /app/output/

FROM nginx:1.31-alpine

COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template

COPY --from=build /app/output/ /usr/share/nginx/html/

ENV PORT=8080
ENV BACKEND_URL=http://127.0.0.1:8090
ENV NGINX_ENVSUBST_FILTER="^(PORT|BACKEND_URL)$"

EXPOSE 8080
