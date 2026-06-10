# Stage 1: Build the frontend React app
FROM node:20-alpine AS build
WORKDIR /app

# Install dependencies
COPY games-frontend/package.json games-frontend/package-lock.json* ./
RUN npm install

# Copy source and build assets
COPY games-frontend/ .
RUN npm run build

# Stage 2: Serve built assets with Nginx
FROM nginx:stable-alpine

# Copy nginx config
COPY games-frontend/nginx.conf /etc/nginx/conf.d/default.conf

# Copy build artifacts to nginx public folder
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
