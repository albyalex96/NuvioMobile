FROM gradle:8.13-jdk17 AS builder
WORKDIR /app
COPY . .
RUN ./gradlew :composeApp:wasmJsBrowserDistribution --no-daemon

FROM nginx:alpine AS runtime
COPY --from=builder /app/composeApp/build/dist/wasmJs/productionExecutable /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]