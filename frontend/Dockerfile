FROM node:20-bookworm-slim AS build

WORKDIR /app

ENV CI=true

COPY package.json package-lock.json ./

RUN npm ci --include=dev --no-audit --no-fund && \
    test -x node_modules/.bin/tsc && \
    test -x node_modules/.bin/vite

COPY . .

RUN npm run build


FROM nginx:1.27-alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]