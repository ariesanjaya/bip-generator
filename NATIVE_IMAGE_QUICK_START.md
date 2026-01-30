# Native Image Quick Start

**Build a standalone native executable with instant startup!**

---

## Prerequisites Checklist

- [ ] GraalVM installed (Java 17+)
- [ ] `JAVA_HOME` pointing to GraalVM
- [ ] `native-image` component installed (`gu install native-image`)
- [ ] Maven 3.6+ installed

---

## Quick Build

### Windows
```batch
build-native.bat
```

### Linux/Mac
```bash
chmod +x build-native.sh
./build-native.sh
```

---

## Run

### Windows
```batch
target\bip-generator.exe run com.bipgen.MainVerticle
```

### Linux/Mac
```bash
./target/bip-generator run com.bipgen.MainVerticle
```

**Server starts in < 100ms!** 🚀

---

## Benefits

| Feature | JAR | Native Image |
|---------|-----|--------------|
| Startup | 2-3s | <0.1s |
| Memory | 150 MB | 50 MB |
| Java Required | ✅ Yes | ❌ No |

---

## Verification

```bash
# Check file size
ls -lh target/bip-generator*

# Test startup time
time ./target/bip-generator run com.bipgen.MainVerticle

# Should start in milliseconds!
```

---

## Troubleshooting

**GraalVM not detected?**
```bash
java -version  # Should show "GraalVM"
```

**Build failed?**
- Check `JAVA_HOME` points to GraalVM
- Ensure `native-image` is installed
- Review build output for specific errors

**Runtime errors?**
- Check reflection configs
- Ensure all resources are included
- See full guide: `NATIVE_IMAGE_GUIDE.md`

---

For detailed documentation, see **[NATIVE_IMAGE_GUIDE.md](NATIVE_IMAGE_GUIDE.md)**
