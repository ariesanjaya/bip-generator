# BIP32 & BIP38 Generator

A web-based generator for BIP32 (Hierarchical Deterministic Wallets) and BIP38 (Password-Protected Private Keys) built with Vert.x Java and modern web technologies.

**NEW: ✨ GraalVM Native Image Support** - Build standalone executables with instant startup!

## Features

### BIP32 - Hierarchical Deterministic Wallets
- Generate new master keys with 12 or 24-word mnemonic phrases
- Support for custom mnemonic input
- **Optional BIP39 passphrase** (25th word) for additional security
- Derive child keys using BIP32 derivation paths
- Generate multiple addresses at once
- Support for standard derivation paths (BIP44, BIP49, BIP84)

### BIP38 - Password-Protected Private Keys
- Encrypt WIF private keys with password protection
- Decrypt BIP38 encrypted keys
- Support for both compressed and uncompressed keys
- Secure password-based encryption using Scrypt
- **Compliant BIP38 format** (keys start with "6P")

## Technology Stack

- **Backend**: Vert.x 4.5.1 (Java)
- **Crypto Libraries**: BitcoinJ 0.16.3, Bouncy Castle
- **Frontend**: Vanilla JavaScript, HTML5, CSS3
- **Build Tool**: Maven

## Prerequisites

- Java 17 or higher
- Maven 3.6+

## Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd bip-generator
```

2. Build the project:
```bash
mvn clean package
```

## Running the Application

1. Start the server:
```bash
java -jar target/bip-generator-1.0-SNAPSHOT-fat.jar
```

2. Open your browser and navigate to:
```
http://localhost:8080
```

## API Endpoints

### BIP32 Endpoints

#### Generate Master Key
```
POST /api/bip32/generate
Content-Type: application/json

{
  "mnemonic": "optional custom mnemonic",
  "wordCount": 12
}
```

Response:
```json
{
  "mnemonic": "word1 word2 ... word12",
  "masterPrivateKey": "xprv...",
  "masterPublicKey": "xpub...",
  "seed": "hex seed"
}
```

#### Derive Child Key
```
POST /api/bip32/derive
Content-Type: application/json

{
  "masterKey": "xprv...",
  "path": "m/44'/0'/0'/0/0"
}
```

Response:
```json
{
  "path": "m/44'/0'/0'/0/0",
  "privateKey": "5K... (WIF)",
  "publicKey": "hex public key",
  "address": "Bitcoin address"
}
```

#### Derive Multiple Addresses
```
POST /api/bip32/derive-multiple
Content-Type: application/json

{
  "masterKey": "xprv...",
  "pathPattern": "m/44'/0'/0'/0/*",
  "count": 10
}
```

### BIP38 Endpoints

#### Encrypt Private Key
```
POST /api/bip38/encrypt
Content-Type: application/json

{
  "privateKey": "5K... (WIF)",
  "password": "your password"
}
```

Response:
```json
{
  "encryptedKey": "6PR... (BIP38)",
  "compressed": true
}
```

#### Decrypt BIP38 Key
```
POST /api/bip38/decrypt
Content-Type: application/json

{
  "encryptedKey": "6PR...",
  "password": "your password"
}
```

Response:
```json
{
  "privateKey": "5K... (WIF)",
  "address": "Bitcoin address",
  "compressed": true
}
```

## Common Derivation Paths

- **BIP44 (Legacy)**: `m/44'/0'/0'/0/0`
- **BIP49 (SegWit Compatible)**: `m/49'/0'/0'/0/0`
- **BIP84 (Native SegWit)**: `m/84'/0'/0'/0/0`

## Security Considerations

**⚠️ IMPORTANT SECURITY WARNINGS:**

1. This tool is for **educational and testing purposes only**
2. Never use generated keys for real funds without proper security measures
3. Always run on a secure, offline computer for production key generation
4. Never share your private keys or mnemonic phrases with anyone
5. Use strong passwords (minimum 4 characters, recommend 12+) for BIP38 encryption
6. The application does not store any keys on the server or in browser storage

## Development

### Project Structure
```
bip-generator/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/bipgen/
│   │   │   ├── MainVerticle.java
│   │   │   ├── handler/
│   │   │   │   ├── BIP32Handler.java
│   │   │   │   └── BIP38Handler.java
│   │   │   └── service/
│   │   │       ├── BIP32Service.java
│   │   │       └── BIP38Service.java
│   │   └── resources/webroot/
│   │       ├── index.html
│   │       ├── css/style.css
│   │       └── js/app.js
│   └── test/
│       └── java/com/bipgen/
│           └── BIP32ServiceTest.java
```

### Running Tests
```bash
mvn test
```

### Building

**Regular JAR:**
```bash
mvn clean package
```

**Native Image (requires GraalVM):**
```bash
# Windows
build-native.bat

# Linux/Mac
chmod +x build-native.sh
./build-native.sh
```

See **[NATIVE_IMAGE_GUIDE.md](NATIVE_IMAGE_GUIDE.md)** for detailed native image documentation.

## License

This project is for educational purposes. Use at your own risk.

## Resources

- [BIP32 Specification](https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki)
- [BIP38 Specification](https://github.com/bitcoin/bips/blob/master/bip-0038.mediawiki)
- [BIP39 Mnemonic](https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki)
- [BIP44 Multi-Account Hierarchy](https://github.com/bitcoin/bips/blob/master/bip-0044.mediawiki)

## Contributing

Contributions are welcome! Please ensure all tests pass before submitting a pull request.

## Support

For issues and questions, please open an issue on the GitHub repository.
