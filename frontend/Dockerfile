# Interface LT Expertise : construite avec Vite, servie par nginx qui relaie aussi /api vers le backend
# (même origine : le cookie de rafraîchissement SameSite=Strict accompagne les appels).
FROM node:22-alpine AS construction
WORKDIR /construction
RUN corepack enable && corepack prepare pnpm@10.30.0 --activate
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile
COPY . .
RUN pnpm build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=construction /construction/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=10s --timeout=3s --retries=3 CMD wget -q -O /dev/null http://127.0.0.1/ || exit 1
