#!/bin/sh

# Create a copy of the template
cp /etc/nginx/nginx.conf.template /etc/nginx/nginx.conf
# cp /etc/nginx/conf.d/csp.conf.template /etc/nginx/conf.d/csp.conf

# Replace environment variables in the Nginx configuration using sed
sed -i "s|\${PORTAL_API_URL}|$PORTAL_API_URL|g" /etc/nginx/nginx.conf
sed -i "s|\${DOCS_API_URL}|$DOCS_API_URL|g" /etc/nginx/nginx.conf
sed -i "s|\${AIRFLOW_URL}|$AIRFLOW_URL|g" /etc/nginx/nginx.conf
sed -i "s|\${CLOUDBEAVER_URL}|$CLOUDBEAVER_URL|g" /etc/nginx/nginx.conf
sed -i "s|\${JUPYTER_URL}|$JUPYTER_URL|g" /etc/nginx/nginx.conf
sed -i "s|\${FILEBROWSER_URL}|$FILEBROWSER_URL|g" /etc/nginx/nginx.conf
sed -i "s|\${MONITORING_URL}|$MONITORING_URL|g" /etc/nginx/nginx.conf
sed -i "s|\${MAILBOX_URL}|$MAILBOX_URL|g" /etc/nginx/nginx.conf
sed -i "s|\${DATAGOV_URL}|$DATAGOV_URL|g" /etc/nginx/nginx.conf
sed -i "s|\${AUTH_SERVER_URL}|$AUTH_SERVER_URL|g" /etc/nginx/nginx.conf

# Also process the CSP configuration if needed
# sed -i "s|\${CSP_HEADER_VALUE}|$CSP_HEADER_VALUE|g" /etc/nginx/conf.d/csp.conf
# Add other variables as needed for CSP configuration

# Start Nginx
nginx -g 'daemon off;'
