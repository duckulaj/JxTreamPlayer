#!/bin/bash
# Script to generate PKCS12 keystore from Let's Encrypt PEM files

CERT_DIR="/etc/letsencrypt/live/shazjon.hopto.org-0001"
OUTPUT_KEYSTORE="src/main/resources/keystore.p12"
ALIAS="xtreamjson"
PASSWORD="password"

echo "Generating keystore from certificates in $CERT_DIR..."

if [ ! -d "$CERT_DIR" ]; then
    echo "Error: Certificate directory $CERT_DIR does not exist."
    exit 1
fi

sudo openssl pkcs12 -export \
    -in "$CERT_DIR/fullchain.pem" \
    -inkey "$CERT_DIR/privkey.pem" \
    -out "$OUTPUT_KEYSTORE" \
    -name "$ALIAS" \
    -passout pass:"$PASSWORD"

if [ $? -eq 0 ]; then
    echo "Successfully generated $OUTPUT_KEYSTORE"
    # Ensure the file is owned by the current user
    sudo chown $(id -u):$(id -g) "$OUTPUT_KEYSTORE"
    ls -l "$OUTPUT_KEYSTORE"
else
    echo "Failed to generate keystore."
    exit 1
fi
