# Native Image Build Guide

This guide explains how to build a GraalVM native image of the BIP32/BIP38 Generator for faster startup and lower memory usage.

---

## What is Native Image?

GraalVM Native Image compiles Java applications ahead-of-time into standalone native executables that:

- ✅ Start **instantly** (milliseconds vs seconds)
- ✅ Use **less memory** (~50 MB vs ~150 MB)
- ✅ Have **smaller footprint** (executable only, no JVM needed)
- ✅ Are **self-contained** (no Java installation required)

---

## Prerequisites

### 1. Install GraalVM

**Download GraalVM:**
- Visit: https://www.graalvm.org/downloads/
- Download GraalVM for Java 17 or later
- Choose the appropriate version for your OS

**Windows:**
```powershell
# Download GraalVM
# Extract to C:\graalvm

# Set environment variables
setx JAVA_HOME "C:\graalvm"
setx PATH "%PATH%;C:\graalvm\bin"
```

**Linux/Mac:**
```bash
# Download GraalVM
# Extract to ~/graalvm

# Add to ~/.bashrc or ~/.zshrc
export JAVA_HOME=~/graalvm
export PATH=$JAVA_HOME/bin:$PATH

# Reload shell
source ~/.bashrc
```

### 2. Install Native Image Component

```bash
# Install native-image tool
gu install native-image
```

### 3. Verify Installation

```bash
# Check GraalVM
java -version
# Should show: "GraalVM ..."

# Check native-image
native-image --version
# Should show version info
```

---

## Building Native Image

### Quick Build (Recommended)

**Windows:**
```batch
build-native.bat
```

**Linux/Mac:**
```bash
chmod +x build-native.sh
./build-native.sh
```

### Manual Build

```bash
# Step 1: Build the JAR
mvn clean package -DskipTests

# Step 2: Build native image
mvn -Pnative native:compile
```

**Build Time:**
- First build: 5-10 minutes (depending on hardware)
- Subsequent builds: 3-5 minutes

---

## Running Native Image

### Windows

```batch
# Run the native executable
target\bip-generator.exe run com.bipgen.MainVerticle

# Or with custom port
target\bip-generator.exe run com.bipgen.MainVerticle -Dhttp.port=8080
```

### Linux/Mac

```bash
# Run the native executable
./target/bip-generator run com.bipgen.MainVerticle

# Or with custom port
./target/bip-generator run com.bipgen.MainVerticle -Dhttp.port=8080
```

---

## Configuration Files

Native Image requires explicit configuration for features like reflection, resources, and JNI.

### Configuration Files Location

```
src/main/resources/META-INF/native-image/
├── reflect-config.json          # Reflection configuration
├── resource-config.json         # Resources to include
├── jni-config.json              # JNI configuration
├── proxy-config.json            # Dynamic proxy configuration
└── native-image.properties      # Build arguments
```

### reflect-config.json

Specifies classes that use reflection:
- Application classes (Verticle, Handlers, Services)
- Vert.x classes (JsonObject, JsonArray)
- BitcoinJ classes (DeterministicKey, ECKey)
- Bouncy Castle provider

### resource-config.json

Includes resources in native image:
- Web files (HTML, CSS, JS)
- Configuration files (properties, JSON)
- BitcoinJ resources (mnemonic wordlists)

### native-image.properties

Build-time configurations:
- Initialize classes at build time
- Initialize classes at runtime
- Enable security services
- Enable HTTP/HTTPS

---

## Build Configuration (pom.xml)

The native image plugin is configured in `pom.xml`:

```xml
<plugin>
    <groupId>org.graalvm.buildtools</groupId>
    <artifactId>native-maven-plugin</artifactId>
    <version>0.10.1</version>
    <configuration>
        <imageName>bip-generator</imageName>
        <mainClass>io.vertx.core.Launcher</mainClass>
        <buildArgs>
            <buildArg>--no-fallback</buildArg>
            <buildArg>--report-unsupported-elements-at-runtime</buildArg>
            <buildArg>--allow-incomplete-classpath</buildArg>
        </buildArgs>
    </configuration>
</plugin>
```

---

## Performance Comparison

### JAR vs Native Image

| Metric | JAR (JVM) | Native Image |
|--------|-----------|--------------|
| Startup Time | ~2-3 seconds | ~0.1 seconds |
| Memory Usage | ~150 MB | ~50 MB |
| File Size | ~15 MB | ~80 MB |
| First Request | Slower | Fast |
| Steady State | Fast | Fast |

### When to Use Native Image

✅ **Good For:**
- Production deployments
- Serverless/FaaS environments
- Resource-constrained systems
- Fast startup requirements
- Container deployments

❌ **Not Ideal For:**
- Development (longer build times)
- Frequent code changes
- Heavy runtime reflection
- Dynamic class loading

---

## Troubleshooting

### Issue: "GraalVM not detected"

**Solution:**
```bash
# Verify Java is GraalVM
java -version

# Should output something like:
# openjdk version "17.x.x" ...
# OpenJDK Runtime Environment GraalVM CE ...
```

### Issue: "native-image: command not found"

**Solution:**
```bash
# Install native-image component
gu install native-image
```

### Issue: Build fails with reflection errors

**Solution:**
Add missing classes to `reflect-config.json`:
```json
{
  "name": "com.example.MissingClass",
  "allDeclaredConstructors": true,
  "allPublicMethods": true
}
```

### Issue: Resources not found at runtime

**Solution:**
Add missing patterns to `resource-config.json`:
```json
{
  "pattern": "path/to/resource/.*"
}
```

### Issue: ClassNotFoundException at runtime

**Solution:**
1. Check if class is in `reflect-config.json`
2. Ensure class is included in the classpath
3. Check initialization configuration in `native-image.properties`

---

## Advanced Configuration

### Custom Build Arguments

Edit `pom.xml` to add custom build arguments:

```xml
<buildArgs>
    <buildArg>--verbose</buildArg>
    <buildArg>-H:+PrintClassInitialization</buildArg>
    <buildArg>-H:+ReportExceptionStackTraces</buildArg>
    <buildArg>-H:IncludeResources=custom/.*</buildArg>
</buildArgs>
```

### Generate Configuration with Agent

Run application with native-image-agent to auto-generate configs:

```bash
# Run with agent
java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image \
     -jar target/bip-generator-1.0-SNAPSHOT-fat.jar

# Test all features
# Then stop the application
# Config files will be generated
```

### Optimize for Size

```xml
<buildArg>-Ob</buildArg>  <!-- Optimize for binary size -->
<buildArg>--gc=serial</buildArg>  <!-- Use serial GC -->
```

### Optimize for Speed

```xml
<buildArg>-O3</buildArg>  <!-- Maximum optimization -->
<buildArg>--gc=G1</buildArg>  <!-- Use G1 GC -->
```

---

## Deployment

### Container Deployment (Docker)

```dockerfile
# Multi-stage build
FROM ghcr.io/graalvm/graalvm-ce:latest AS builder

WORKDIR /app
COPY . .

RUN ./mvnw clean package -Pnative -DskipTests

FROM ubuntu:22.04

COPY --from=builder /app/target/bip-generator /app/bip-generator

EXPOSE 8080

CMD ["/app/bip-generator", "run", "com.bipgen.MainVerticle"]
```

### Systemd Service (Linux)

```ini
[Unit]
Description=BIP32/BIP38 Generator
After=network.target

[Service]
Type=simple
User=bipgen
ExecStart=/opt/bip-generator/bip-generator run com.bipgen.MainVerticle
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

---

## Known Limitations

### Current Limitations

1. **Build Time**: Native image compilation takes 5-10 minutes
2. **Large Binary**: Executable is ~80 MB (vs 15 MB JAR)
3. **Platform Specific**: Must build on target platform
4. **Limited Dynamic Features**: Reflection requires configuration

### Vert.x Specific

- Some Vert.x features require additional configuration
- Codegen features need explicit registration
- Clustering requires special setup

### BitcoinJ Specific

- Mnemonic wordlists must be explicitly included
- Crypto providers need security services enabled
- Some operations may need JNI configuration

---

## Best Practices

### Development Workflow

1. **Development**: Use regular JAR (`mvn package`)
2. **Testing**: Test with JAR first
3. **Production**: Build native image for deployment

### Configuration Management

1. **Start Simple**: Use minimal configuration
2. **Test Thoroughly**: Test all features
3. **Add As Needed**: Add reflection configs as errors appear
4. **Use Agent**: Use native-image-agent for auto-discovery

### Performance Tuning

1. **Profile First**: Identify bottlenecks
2. **Optimize Critical Paths**: Focus on hot paths
3. **Test Both**: Compare JAR vs native image
4. **Monitor**: Track startup time and memory usage

---

## Additional Resources

- [GraalVM Documentation](https://www.graalvm.org/latest/docs/)
- [Native Image Reference](https://www.graalvm.org/latest/reference-manual/native-image/)
- [Vert.x Native Image Guide](https://vertx.io/docs/vertx-core/java/#_native_image)
- [Maven Plugin Documentation](https://graalvm.github.io/native-build-tools/latest/maven-plugin.html)

---

## FAQ

**Q: Do I need GraalVM installed on the deployment server?**
A: No, the native executable is self-contained.

**Q: Can I cross-compile for different platforms?**
A: No, you must build on the target platform (Windows → Windows, Linux → Linux).

**Q: Is the native image slower than the JVM?**
A: Peak performance is similar, but native image has faster startup and lower memory usage.

**Q: Can I update the application without rebuilding?**
A: No, any code change requires a full rebuild.

**Q: Does it work with all Java libraries?**
A: Most libraries work, but some require additional configuration for reflection/resources.

---

**Status**: ✅ **Native Image Configuration Complete**
**Build Scripts**: ✅ **Ready**
**Documentation**: ✅ **Complete**
