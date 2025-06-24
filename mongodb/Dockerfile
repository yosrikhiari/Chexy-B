# MongoDB Dockerfile
FROM mongo:6.0

# Set environment variables
ENV MONGO_INITDB_ROOT_USERNAME=mongo_user
ENV MONGO_INITDB_ROOT_PASSWORD=mongo_password
ENV MONGO_INITDB_DATABASE=chessmystic

# Create init script directory
RUN mkdir -p /docker-entrypoint-initdb.d

# Create initialization script
RUN echo 'db = db.getSiblingDB("chessmystic"); \
db.createUser({ \
  user: "mongo_user", \
  pwd: "mongo_password", \
  roles: [{ role: "readWrite", db: "chessmystic" }] \
});' > /docker-entrypoint-initdb.d/init-mongo.js

# Expose MongoDB port
EXPOSE 27017

# Health check
HEALTHCHECK --interval=10s --timeout=5s --retries=30 --start-period=30s \
  CMD mongosh --eval "db.adminCommand('ping')" || exit 1

# Use default MongoDB entrypoint
CMD ["mongod"]
