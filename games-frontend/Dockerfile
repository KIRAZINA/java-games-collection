# Stage 1: Build the React app
FROM node:20-alpine AS build

ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

WORKDIR /app

# Install dependencies (layer cached as long as lockfile is unchanged)
COPY games-frontend/package.json games-frontend/package-lock.json* ./
RUN npm ci

# Copy source and build
COPY games-frontend/ .
RUN npm run build

# Stage 2: Serve with Nginx
FROM nginx:stable-alpine

COPY games-frontend/nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
