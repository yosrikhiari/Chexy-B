# PostgreSQL Dockerfile
FROM postgres:16.2

# Set environment variables
ENV POSTGRES_DB=keycloak_db
ENV POSTGRES_USER=keycloak_user
ENV POSTGRES_PASSWORD=keycloak_password

# Create init script directory
RUN mkdir -p /docker-entrypoint-initdb.d

# Create initialization script - fix SQL syntax
RUN echo "-- Database initialization script\n\
-- The database is already created by POSTGRES_DB environment variable\n\
-- Just ensure the user has proper privileges\n\
GRANT ALL PRIVILEGES ON DATABASE keycloak_db TO keycloak_user;" > /docker-entrypoint-initdb.d/init-db.sql

# Expose PostgreSQL port
EXPOSE 5432

# Health check
HEALTHCHECK --interval=10s --timeout=5s --retries=5 --start-period=30s \
  CMD pg_isready -U keycloak_user -d keycloak_db || exit 1

# Use default PostgreSQL entrypoint
CMD ["postgres"]