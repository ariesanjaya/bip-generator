#!/bin/bash
echo "=== Testing BIP32/BIP38 Generator API ==="
echo ""

echo "1. Health Check:"
curl -s http://localhost:8080/api/health | jq '.'
echo ""

echo "2. Generate BIP32 Wallet:"
WALLET=$(curl -s -X POST http://localhost:8080/api/bip32/generate \
  -H "Content-Type: application/json" \
  -d '{"wordCount":12}')
echo "$WALLET" | jq '.'
echo ""

echo "3. Extract Master Key for Derivation:"
MASTER_KEY=$(echo "$WALLET" | jq -r '.masterPrivateKey')
echo "Master Key: $MASTER_KEY"
echo ""

echo "4. Derive Child Key (m/44'/0'/0'/0/0):"
curl -s -X POST http://localhost:8080/api/bip32/derive \
  -H "Content-Type: application/json" \
  -d "{\"masterKey\":\"$MASTER_KEY\",\"path\":\"m/44'/0'/0'/0/0\"}" | jq '.'
echo ""

echo "5. Derive Multiple Addresses:"
curl -s -X POST http://localhost:8080/api/bip32/derive-multiple \
  -H "Content-Type: application/json" \
  -d "{\"masterKey\":\"$MASTER_KEY\",\"pathPattern\":\"m/44'/0'/0'/0/*\",\"count\":3}" | jq '.addresses[] | {index, address, path}'
echo ""

echo "=== All tests completed ==="
