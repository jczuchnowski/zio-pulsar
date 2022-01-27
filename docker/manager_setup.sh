#!/bin/sh

PM_HOST=manager

>&2 echo "Setting up Pulsar Manager"

until curl http://$PM_HOST:7750/pulsar-manager/csrf-token
do
  >&2 echo "Pulsar Manager is unavailable - sleeping"
  sleep 1
done
  
>&2 echo "Pulsar Manager is up - executing command"

CSRF_TOKEN=$(curl http://$PM_HOST:7750/pulsar-manager/csrf-token)

curl \
  -H 'X-XSRF-TOKEN: $CSRF_TOKEN' \
  -H 'Cookie: XSRF-TOKEN=$CSRF_TOKEN;' \
  -H "Content-Type: application/json" \
  -X PUT http://$PM_HOST:7750/pulsar-manager/users/superuser \
  -d '{"name": "admin", "password": "apachepulsar", "description": "test", "email": "username@test.org"}'

