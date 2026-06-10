FROM node:20-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
ENV NODE_OPTIONS=--max-old-space-size=1024
RUN npm install --maxsockets 1 --no-optional --no-audit --no-fund && \
    test -d node_modules/typescript || (echo "ERROR: typescript not installed — npm may have been OOM-killed" && exit 1)

COPY . .
ENV PATH="/app/node_modules/.bin:${PATH}"
RUN node_modules/.bin/tsc -b && node_modules/.bin/vite build

FROM nginx:1.27-alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
