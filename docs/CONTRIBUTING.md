# 🤝 Contributing to Drink It!

Thank you for your interest in contributing to **Drink It!**. We welcome contributions from developers of all skill levels.

---

## 🛠️ Development Setup

### Prerequisites
- **Android Studio**: Android Studio Ladybug (2024.2+) or newer.
- **JDK**: Java Development Kit 17 (bundled with Android Studio JBR).
- **Android SDK**: Android API 36 with Build-Tools 36.0.0.
- **Wear OS / Android Emulators**: Or physical Android devices paired via Bluetooth with Wear OS companion app.

### Getting the Code
```bash
git clone https://github.com/ParvShah2001/DrinkIt.git
cd DrinkIt
```

### Building the Project
```bash
# Build phone debug APK
./gradlew :mobile:assembleDebug

# Build watch debug APK
./gradlew :app:assembleDebug

# Run unit and instrumented tests
./gradlew test
```

---

## 📐 Project Conventions

- **Language**: Kotlin 2.0+ targeting JVM 17.
- **UI Toolkit**:
  - `:mobile`: Jetpack Compose Material 3.
  - `:app`: Compose for Wear OS (`ScalingLazyColumn`, `Chip`, tonal ambient surfaces).
- **Code Formatting**: Follow official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) and Android Compose guidelines.
- **Independence Directive**: Code changes in one module must not introduce hard runtime dependencies on the other. Both modules must continue to function independently.

---

## 🚀 Submitting Changes

1. **Fork** the repository and create your feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```
2. **Commit** your changes with descriptive, conventional commit messages:
   ```bash
   git commit -m "feat(mobile): add hydration history chart"
   ```
3. **Verify** that both modules compile and tests pass:
   ```bash
   ./gradlew assembleDebug test
   ```
4. **Push** to your fork and submit a Pull Request against `main`.

---

## 📄 License
By contributing to Drink It!, you agree that your contributions will be licensed under the [MIT License](../LICENSE).
