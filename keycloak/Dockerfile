FROM quay.io/keycloak/keycloak:26.1.4

# Switch to root for setup
USER root

# Create directory for realm import
RUN mkdir -p /opt/keycloak/data/import && chown -R keycloak:keycloak /opt/keycloak/data

# Switch back to keycloak user
USER keycloak

# Copy realm file
COPY --chown=keycloak:keycloak realm-export.json /opt/keycloak/data/import/

# Database configuration
ENV KC_DB=postgres
ENV KC_DB_URL=jdbc:postgresql://postgres:5432/keycloak_db
ENV KC_DB_USERNAME=keycloak_user
ENV KC_DB_PASSWORD=keycloak_password

# Admin credentials
ENV KC_BOOTSTRAP_ADMIN_USERNAME=chessnewadmin
ENV KC_BOOTSTRAP_ADMIN_PASSWORD=admin

# Hostname configuration (updated for new version)
ENV KC_HOSTNAME=localhost
ENV KC_HOSTNAME_STRICT=false
ENV KC_HTTP_ENABLED=true
ENV KC_HEALTH_ENABLED=true

# Disable HTTPS requirements for development
ENV KC_HOSTNAME_STRICT_HTTPS=false

# Performance and caching
ENV KC_CACHE=ispn
ENV KC_CACHE_STACK=tcp

# Logging configuration
ENV KC_LOG_LEVEL=INFO

# Transaction timeout (increase for better stability)
ENV KC_TRANSACTION_XA_ENABLED=false

# Build configuration for optimized startup
RUN /opt/keycloak/bin/kc.sh build --db=postgres --health-enabled=true

# Expose ports
EXPOSE 8080 9000

# Health check (commented out since curl is not installed)
# HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=5 \
#   CMD curl -f http://localhost:8080/health/ready || exit 1

# Start command with proper configuration
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start-dev", "--import-realm", "--optimized"]

