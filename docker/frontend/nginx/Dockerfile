# Build context is repo root (see docker-compose.yml).
FROM nginx:1.27-alpine

RUN rm -rf /usr/share/nginx/html/* /etc/nginx/conf.d/default.conf

# Esto ya no es necesario porque en el docker-compose.yml se definen volúmenes en estos archivos
# COPY recetas-frontend/ /usr/share/nginx/html/
# COPY docker/frontend/nginx/conf/nginx.conf /etc/nginx/nginx.conf
# COPY docker/frontend/nginx/conf/recetas.conf /etc/nginx/conf.d/recetas.conf

EXPOSE 80 443
