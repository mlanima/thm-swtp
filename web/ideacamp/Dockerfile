# Stage 1: Build
FROM node:24-alpine AS builder
WORKDIR /app
ARG API_URL=https://api.swtp-ss26.de/api
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN sed -i "s|__API_URL__|${API_URL}|g" src/app/enviroments/enviroment.prod.ts
RUN npm run build -- --configuration=production

# Stage 2: Runtime
FROM node:24-alpine
RUN apk add --no-cache wget && addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=builder /app/dist/ideacamp ./dist/ideacamp
USER app
EXPOSE 4000
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s \
  CMD wget -qO- http://localhost:4000/health || exit 1
ENV PORT=4000
CMD ["node", "dist/ideacamp/server/server.mjs"]
