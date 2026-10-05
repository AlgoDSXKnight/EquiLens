FROM node:24-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ARG VITE_RESEARCH_API_URL=http://localhost:8080/api
ARG VITE_PORTFOLIO_API_URL=http://localhost:8081/api
ENV VITE_RESEARCH_API_URL=$VITE_RESEARCH_API_URL
ENV VITE_PORTFOLIO_API_URL=$VITE_PORTFOLIO_API_URL
RUN npm run build

FROM nginx:1.29-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
