# ResistIQ ⚡ Resistor & Ohm's Law Utility

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![Material3](https://img.shields.io/badge/Design-Material%203-7B1FA2.svg)](https://m3.material.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**ResistIQ** is a modern, fully offline Android utility designed for electronics engineers, technicians, makers, students, and hobbyists. It provides precise, bidirectional resistor color code calculations, EIA E-Series preferred value verification, Ohm's Law calculations, and persistent offline calculation history.

---

## ✨ Features

### 🎨 Resistor Color Code Calculator
* **Multi-Band Support**: Handles **3-band**, **4-band**, **5-band**, and **6-band** resistors seamlessly.
* **Bidirectional Lookup**:
  * **Color → Resistance**: Select color bands from interactive dropdowns to calculate nominal resistance, tolerance range (Min–Max Ω), and temperature coefficient (PPM/K for 6-band).
  * **Value → Color**: Enter any resistance value, tolerance, and temp coefficient to determine the exact standard color code bands.
* **Interactive Visual Representation**: Real-time graphical SVG/Canvas rendering of the resistor body with accurate color bands.
* **Band Breakdown**: View individual digit values, multipliers, tolerance percentages, and temperature coefficients step-by-step.

### 📐 Preferred Value Checker (EIA E-Series)
* Validates calculated or target resistance against standard **E12 (10%)**, **E24 (5%)**, and **E96 (1%)** preferred series according to international standards (IEC 60062 / EIA).
* Calculates percentage error and suggests the closest standard nominal preferred resistor value.

### ⚡ Ohm's Law Calculator
* Solves for any two unknown electrical parameters when provided with two known inputs:
  * **Voltage ($V$)**
  * **Current ($I$)**
  * **Resistance ($R$)**
  * **Power ($P$)**
* Supports metric unit prefixes (e.g., $\mu A$, $mA$, $A$, $mV$, $V$, $kV$, $\Omega$, $k\Omega$, $M\Omega$, $mW$, $W$, $kW$).
* Optional step-by-step formula breakdown display.

### 📜 Calculation History
* Logs calculation results with timestamps.
* Retains up to **50 recent calculations** locally.
* Re-load previous calculations directly back into the calculator with a single tap.

### ⚙️ Customization & Display Settings
* **Theme Customization**: Light, Dark, or System default modes, with Material You **Dynamic Color** support (Android 12+).
* **Configurable Defaults**: Set default startup tool, band count, default Ohm's law inputs, and precision (1–3 decimal places).
* **Haptic Feedback**: Tactile feedback on band and tab selections.

### 🔒 Privacy & Offline First
* **100% Offline**: Requires zero network permissions. No user tracking, ads, telemetry, or account sign-ins.

---

## 🛠️ Technology Stack & Architecture

* **Language**: [Kotlin](https://kotlinlang.org/)
* **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3
* **Architecture**: MVVM (Model-View-ViewModel) + Unidirectional Data Flow (UDF)
* **State Management**: Kotlin `StateFlow` & `State`
* **Navigation**: Jetpack Navigation Compose
* **Data Persistence**: Jetpack DataStore Preferences + `kotlinx.serialization`
* **Build System**: Gradle Version Catalogs (Kotlin DSL)
* **SDK Compatibility**: `minSdk 24` (Android 7.0), `targetSdk 37`

---

## 📐 Standards & Compliance

ResistIQ follows recognized international electrical standards:
* **IEC 60062**: Marking codes for resistors and capacitors (defining color coding, multipliers, tolerances, and temperature coefficients).
* **EIA E-Series**: Standard preferred resistor value systems (**E12**, **E24**, **E96**) dividing logarithmic decades for standard nominal tolerances.

---

## 🚀 Getting Started

### Prerequisites
* Android Studio Ladybug | 2024.2.1 or newer (recommended)
* JDK 11 or higher
* Android SDK 37

### Building from Source

1. **Clone the repository**:
   ```bash
   git clone https://github.com/abdullah-ateeq/ResistIQ.git
   cd ResistIQ
   ```

2. **Open in Android Studio**:
   * Launch Android Studio and open the `ResistIQ` folder.
   * Allow Gradle to sync dependencies.

3. **Build & Run**:
   * Select the `app` run configuration.
   * Connect an Android device or emulator running Android 7.0 (API 24) or higher.
   * Click **Run** (`Shift + F10`).

Alternatively, compile from command line:
```bash
./gradlew assembleDebug
```

---

## 📱 Screenshots & Previews

| Color Code Calculator | Value → Color | Ohm's Law Calculator | Settings |
|:---:|:---:|:---:|:---:|
| Resistor band selection with visual visualizer | Reverse lookup from resistance value | Multi-parameter electrical calculations | Customizable themes & default options |

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
