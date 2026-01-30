#!/bin/bash

echo "============================================"
echo "Building Native Image for BIP32/BIP38 Generator"
echo "============================================"
echo ""

echo "Checking for GraalVM..."
if ! java -version 2>&1 | grep -q "GraalVM"; then
    echo "ERROR: GraalVM not detected!"
    echo "Please ensure GraalVM is installed and JAVA_HOME points to GraalVM."
    echo "Download from: https://www.graalvm.org/downloads/"
    echo ""
    exit 1
fi

echo "GraalVM detected!"
echo ""

echo "Step 1: Building JAR..."
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "ERROR: Maven build failed!"
    exit 1
fi

echo ""
echo "Step 2: Building Native Image..."
echo "This may take 5-10 minutes..."
echo ""

mvn -Pnative native:compile
if [ $? -ne 0 ]; then
    echo "ERROR: Native image build failed!"
    exit 1
fi

echo ""
echo "============================================"
echo "Native Image Build Complete!"
echo "============================================"
echo ""
echo "Executable location: target/bip-generator"
echo ""
echo "To run: ./target/bip-generator run com.bipgen.MainVerticle"
echo ""
