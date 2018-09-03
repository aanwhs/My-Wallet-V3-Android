#!/usr/bin/env bash

echo $FIREBASE_API_JSON  | base64 --decode > google-services.json
echo $SECRETS_PROPERTIES | base64 --decode > secrets.properties
