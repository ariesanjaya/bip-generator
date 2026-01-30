#!/bin/bash

echo "Building BIP32/BIP38 Generator..."
mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo ""
    echo "Build successful! Starting server..."
    echo ""
    java -jar target/bip-generator-1.0.0-fat.jar
else
    echo ""
    echo "Build failed! Please check the errors above."
    exit 1
fi
