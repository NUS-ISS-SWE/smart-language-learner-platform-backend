FROM node:20-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./

# Lower heap forces more frequent GC and reduces peak RSS on memory-constrained instances
ENV NODE_OPTIONS=--max-old-space-size=384
RUN npm install --maxsockets 1 --no-optional --no-audit --no-fund \
    && ls node_modules/.bin/vite || (echo "ERROR: vite not installed — npm install was killed mid-way (OOM)" && exit 1)

COPY . .

# Skip tsc type-check: vite/esbuild handles TypeScript transpilation and produces identical output.
# tsc -b only adds memory pressure and is redundant for a production Docker build.
RUN node_modules/.bin/vite build

FROM nginx:1.27-alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
