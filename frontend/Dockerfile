# syntax=docker/dockerfile:1.7
# Multi-stage: Node 20 build → nginx static
# Cache strategy:
#   1) package.json + lock + .npmrc → npm ci (stable)
#   2) app sources only → vite build
#   3) BuildKit cache mount for npm

FROM node:20-alpine AS build
WORKDIR /app

# Layer 1: dependency install (invalidates only when lock/package changes)
COPY package.json package-lock.json .npmrc ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci --prefer-offline --no-audit --no-fund

# Layer 2: sources required for production build only
COPY index.html vite.config.ts tsconfig.json tsconfig.app.json tsconfig.node.json ./
COPY public ./public
COPY src ./src
ARG VITE_API_BASE_URL=
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
