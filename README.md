# BIP Generator

A BIP32 (Hierarchical Deterministic Wallets) and BIP38 (Password-Protected Private Keys) generator with both a web interface and a JavaFX desktop application.

## Features

### BIP32 - Hierarchical Deterministic Wallets
- Generate master keys with 12 or 24-word mnemonic phrases
- Custom mnemonic input support
- Optional BIP39 passphrase (25th word)
- Derive child keys using standard derivation paths (BIP44, BIP49, BIP84)
- Batch-derive multiple addresses
- Mnemonic validation

### BIP38 - Password-Protected Private Keys
- Encrypt WIF private keys with password protection
- Decrypt BIP38 encrypted keys
- Compressed and uncompressed key support
- Scrypt-based encryption (keys start with "6P")

### Paper Wallet
- Generate printable PDF paper wallets with QR codes

## Project Structure

This is a multi-module Maven project:

```
bip-generator/
├── pom.xml                      # Parent POM
├── bip-generator-core/          # Shared library (services, models)
│   └── src/main/java/com/bipgen/
│       ├── model/               # Data records (MasterKeyResult, etc.)
│       └── service/             # BIP32Service, BIP38Service, PaperWalletService
├── bip-generator-web/           # Vert.x web application
│   └── src/main/java/com/bipgen/
│       ├── MainVerticle.java
│       └── handler/             # HTTP request handlers
└── bip-generator-desktop/       # JavaFX desktop application
    ├── build-native.sh          # GraalVM native image build script
    └── src/main/java/com/bipgen/desktop/
        ├── BipGeneratorApp.java # JavaFX application
        └── Launcher.java        # Main entry point
```

## Technology Stack

- **Core**: BitcoinJ 0.17, Bouncy Castle, PDFBox, ZXing
- **Web**: Vert.x 4.5.1, HTML/CSS/JS
- **Desktop**: JavaFX 21
- **Build**: Maven, GraalVM Native Image (optional)
- **Java**: 17+

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- For the desktop module: JavaFX SDK (bundled via Maven)
- For native builds: GraalVM with `native-image`

## Building

Build all modules:
```bash
mvn clean package
```

Build a specific module:
```bash
mvn clean package -pl bip-generator-core,bip-generator-desktop -am
```

## Running

### Web Application

```bash
java -jar bip-generator-web/target/bip-generator-web-1.0.0-fat.jar
```

Then open `http://localhost:8080` in your browser.

### Desktop Application

```bash
mvn -f bip-generator-desktop/pom.xml javafx:run
```

Or run the shaded JAR:
```bash
java -jar bip-generator-desktop/target/bip-generator-desktop-1.0.0.jar
```

### Native Image (Desktop)

Build a standalone native executable using GraalVM:

```bash
cd bip-generator-desktop
./build-native.sh
```

Prerequisites for native build:
- GraalVM as `JAVA_HOME`
- `native-image` component installed
- GTK3 dev libraries on Linux (`sudo apt install libgtk-3-dev libglib2.0-dev`)

The resulting binary will be at `bip-generator-desktop/target/bip-generator`.

## API Endpoints (Web Module)

### BIP32

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/bip32/generate` | Generate master key from mnemonic |
| POST | `/api/bip32/derive` | Derive a child key |
| POST | `/api/bip32/derive-multiple` | Derive multiple addresses |

### BIP38

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/bip38/encrypt` | Encrypt a private key |
| POST | `/api/bip38/decrypt` | Decrypt a BIP38 key |

## Common Derivation Paths

- **BIP44 (Legacy)**: `m/44'/0'/0'/0/0`
- **BIP49 (SegWit Compatible)**: `m/49'/0'/0'/0/0`
- **BIP84 (Native SegWit)**: `m/84'/0'/0'/0/0`

## Security Considerations

1. This tool is for **educational and testing purposes only**
2. Never use generated keys for real funds without proper security measures
3. Run on a secure, offline computer for production key generation
4. Never share private keys or mnemonic phrases
5. Use strong passwords for BIP38 encryption
6. The application does not persist any keys

## Testing

```bash
mvn test
```

## Resources

- [BIP32 Specification](https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki)
- [BIP38 Specification](https://github.com/bitcoin/bips/blob/master/bip-0038.mediawiki)
- [BIP39 Mnemonic](https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki)
- [BIP44 Multi-Account Hierarchy](https://github.com/bitcoin/bips/blob/master/bip-0044.mediawiki)

## License

This project is for educational purposes. Use at your own risk.
