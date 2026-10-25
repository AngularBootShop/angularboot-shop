# Hardened Unprivileged Nginx Runtime Container
FROM nginxinc/nginx-unprivileged:alpine AS runner

COPY nginx.conf /etc/nginx/nginx.conf
COPY dist/shop-ui/browser /usr/share/nginx/html

EXPOSE 4200

HEALTHCHECK --interval=10s --timeout=3s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:4200/healthz || exit 1

CMD ["nginx", "-g", "daemon off;"]
