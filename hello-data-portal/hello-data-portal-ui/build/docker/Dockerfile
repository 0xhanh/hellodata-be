FROM nginx:1.25.3

LABEL MAINTAINER="HelloDATA Team"

COPY hello-data-portal-ui /app
COPY 500.html /app/500.html
RUN rm -rf /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/nginx.conf
COPY csp.conf /etc/nginx/conf.d/csp.conf.template

RUN chown -R nginx: /app

EXPOSE 80

CMD ["sh", "-c", "envsubst < /etc/nginx/conf.d/csp.conf.template > /etc/nginx/conf.d/csp.conf && nginx -g 'daemon off;'"]


# FROM nginx:1.25.3

# LABEL MAINTAINER="HelloDATA Team"

# # Sao chép các tệp ứng dụng
# COPY hello-data-portal-ui /app
# COPY 500.html /app/500.html

# # Xóa cấu hình mặc định của Nginx
# RUN rm -rf /etc/nginx/conf.d/default.conf

# # Sao chép các tệp cấu hình Nginx
# COPY new_nginx.conf /etc/nginx/nginx.conf.template
# COPY csp.conf /etc/nginx/conf.d/csp.conf.template

# # Thiết lập quyền
# RUN chown -R nginx: /app

# # Thiết lập các biến môi trường mặc định
# ENV PORTAL_API_URL="http://localhost:8081"
# ENV DOCS_API_URL="http://localhost:8086"
# ENV AIRFLOW_URL="http://localhost:28080"
# ENV CLOUDBEAVER_URL="http://localhost:8087"
# ENV JUPYTER_URL="http://localhost:8088"
# ENV FILEBROWSER_URL="http://localhost:8090"
# ENV MONITORING_URL="http://localhost:8001"
# ENV MAILBOX_URL="http://localhost:5099"
# ENV DATAGOV_URL="http://192.168.51.165:8585"
# ENV AUTH_SERVER_URL="http://localhost:38080"

# # Create a script to replace variables and start Nginx
# COPY start-nginx.sh /start-nginx.sh
# RUN chmod +x /start-nginx.sh

# EXPOSE 80

# CMD ["/start-nginx.sh"]
