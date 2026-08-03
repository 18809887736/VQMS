# ---- build stage：npm run build:prod 产出静态资源 ----
FROM node:20-alpine AS build
WORKDIR /build

COPY package.json package-lock.json* ./
RUN npm install

COPY . .
RUN npm run build:prod

# ---- runtime stage：nginx 托管静态资源 + /prod-api 反代到后端 ----
FROM nginx:stable-alpine
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
