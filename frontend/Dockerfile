# ---- 构建阶段 ----
FROM node:22-alpine AS build
WORKDIR /build
COPY .npmrc package.json package-lock.json* ./
RUN npm install --no-audit --no-fund
COPY index.html vite.config.js ./
COPY src ./src
RUN npm run build

# ---- 运行阶段：nginx 托管静态资源并反代后端 ----
FROM nginx:alpine
RUN mkdir -p /tmp/nginx-cache && chown -R nginx:nginx /tmp/nginx-cache
COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=build /build/dist /usr/share/nginx/html
USER nginx
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --retries=10 \
  CMD wget -qO- http://127.0.0.1:8080/ >/dev/null 2>&1 || exit 1
CMD ["nginx", "-g", "daemon off;"]
