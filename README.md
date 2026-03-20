# Login-Brute-Force & Unified Security Console

A highly robust, unified Java desktop application built to demonstrate advanced identity and access management features. It seamlessly integrates a secure login mechanism (with brute-force protection) and a Time-Based One-Time Password (TOTP) Authenticator module into a modern, easy-to-use interface.

## ✨ Key Features

- **Unified Console Dashboard:** A single window interface (powered by `CardLayout`) to manage logins, registrations, account setups, and 2FA token generation.
- **Advanced Authentication:** 
  - Standard User/Password login with strong hashing (`jBCrypt`).
  - Active brute-force protection to lock out malicious login attempts.
- **Built-in 2FA/Authenticator (TOTP):**
  - Generate standards-compliant TOTP 2FA tokens (compatible with Google Authenticator, Authy, etc.).
  - Automatic QR code visual generation for easily adding accounts to your mobile device (`ZXing`).
- **Modern UI/UX:** Built with Swing but enhanced by the `FlatLaf` (Flat Light and Dark) Look and Feel for a sleek, contemporary aesthetic.
- **Embedded Database:** Powered by a local embedded `SQLite` database requiring no external database servers or configuration.
- **Smart Launcher/Bootstrapper:** Automatically detects and downloads missing `.jar` dependencies on its first run to guarantee a smooth startup process. 

## 🛠️ Technology Stack

- **Language:** Java 11+
- **Build Tool:** Maven
- **UI Framework:** Java Swing + FlatLaf (Dark Mode)
- **Database:** SQLite JDBC
- **Cryptography & Security:** jBCrypt (Password Hashing), Apache Commons Codec (Base32 keys)
- **QR Code Generation:** ZXing Core

## 🚀 How to Run

### Option 1: Using the Pre-configured Scripts
Inside the `scripts` folder, you will find easy-to-use launcher scripts for both Windows and Linux/Mac environments.
These scripts automatically look for the compiled `.jar` file in the `target/` directory.

- **Windows:** Double-click or run `scripts/launcher.bat`
- **Linux / macOS:** Run `./scripts/launcher.sh`

### Option 2: Running the JAR directly
If you already have the Maven target compiled:
```bash
java -jar target/unified-console-1.0-SNAPSHOT.jar
```

### Option 3: Building from Source (Maven)
If you want to compile the project yourself into a "fat JAR":
```bash
mvn clean package
```
This will generate `unified-console-1.0-SNAPSHOT.jar` inside the `target/` directory, which you can then run using any of the methods above.

## 📂 Project Structure

```text
login-brute-force/
├── src/main/java/com/loginbruteforce/
│   ├── auth/         # Core authentication, brute-force validation & 2FA handlers
│   ├── db/           # SQLite connection and initialization handlers
│   ├── model/        # Data models (User, Account state)
│   ├── totp/         # TOTP generation tools, diagnostic utilities, and QR Code generation
│   ├── ui/           # Custom Java Swing UI components (FlatLaf)
│   ├── util/         # Logging and diagnostic wrappers
│   ├── Launcher.java       # Bootstrapper that downloads missing deps and launches the app
│   └── MainController.java # Main application JFrame and state router
├── scripts/          # Launcher shell and batch scripts (.bat / .sh)
└── pom.xml           # Maven configuration and dependencies definitions
```

## 🔧 Bootstrapping & First Run Behavior

On the very first run, `Launcher.java` will perform a system check. If any required dependencies (like SQLite, FlatLaf, ZXing) are absent from your local classpath, the bootstrapper will proactively display a progress bar, safely download the verified libraries from Maven Central into your user's `~/.unifiedconsole/libs` folder, and automatically restart the application. All local databases are safely initialized automatically.
