# Keycloak Dockerfile
FROM quay.io/keycloak/keycloak:26.1.4

# Set environment variables
ENV KC_DB=postgres
ENV KC_DB_URL=jdbc:postgresql://postgres:5432/keycloak_db
ENV KC_DB_USERNAME=keycloak_user
ENV KC_DB_PASSWORD=keycloak_password
ENV KEYCLOAK_ADMIN=chessnewadmin
ENV KEYCLOAK_ADMIN_PASSWORD=admin
# Remove hostname restriction for development
# ENV KC_HOSTNAME=keycloak
ENV KC_PROXY=edge
ENV KC_HTTP_ENABLED=true
ENV KC_HEALTH_ENABLED=true
# Allow access from any hostname in dev mode
ENV KC_HOSTNAME_STRICT=false

# Create directory for realm import
USER root
RUN mkdir -p /opt/keycloak/data/import
USER keycloak

# Expose Keycloak port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=10s --timeout=5s --retries=30 --start-period=120s \
  CMD curl -f http://localhost:8080/realms/chess/.well-known/openid-configuration || exit 1

COPY realm-export.json /opt/keycloak/data/import/

# Start Keycloak in development mode with realm import
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start-dev", "--import-realm"]
