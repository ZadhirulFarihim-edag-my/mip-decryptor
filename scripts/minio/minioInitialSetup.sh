#!/bin/bash

# Define MinIO alias
MINIO_ALIAS="myminio"

# Define users and their access/secret keys
USER="rnr"
ACCESS_KEY="rnr"
SECRET_KEY="rnr-secret-key"

# Set alias
mc alias set 'myminio' 'http://localhost:9000' 'minioadmin' 'minioadmin'

# Create users
mc admin user add $MINIO_ALIAS $USER $SECRET_KEY

# Create buckets
mc mb $MINIO_ALIAS/rnr-dev-aip

# Define policies to grant users access to the buckets
mc admin policy create $MINIO_ALIAS rnr-dev-aip-policy allAccessPolicy.json

# Assign policies to users
mc admin policy attach $MINIO_ALIAS rnr-dev-aip-policy --user $USER

echo "MinIO users created, folders created, and policies set successfully."
