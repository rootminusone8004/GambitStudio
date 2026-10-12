# GambitStudio 🏰

<p align="center">
  <img src="app/src/main/res/drawable/ic_launcher.png" alt="GambitStudio Icon" width="96" />
</p>

<p align="center">
  <strong>The dedicated opening studio and gambit trainer designed to craft, branch, explore, and export <code>opening.txt</code> files for Chessy.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%207.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Companion%20App-Chessy-E64A19?style=for-the-badge" alt="Chessy Companion" />
  <img src="https://img.shields.io/badge/Theme-Vibrant%20Orange-E64A19?style=for-the-badge" alt="Theme" />
  <img src="https://img.shields.io/badge/Pieces-Lichess%20cburnett-B58863?style=for-the-badge" alt="Lichess Theme" />
  <img src="https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white" alt="GitHub Actions" />
</p>

---

## 🎯 Purpose & Overview

**GambitStudio** is the opening preparation workshop and companion app for **Chessy**. Its mission is focused and simple:
- **Build**: Play through moves and explore opening theories, sharp gambits, and complex lines.
- **Branch**: Undo moves to create alternate branching variations without losing previously recorded history.
- **Train**: Rehearse and memorize your opening repertoire with the interactive training engine.
- **Export & Import**: Generate clean, standardized `opening.txt` files to immediately power the predefined gambit book in Chessy.

---

## ✨ Features & Highlights

- 🏰 **Interactive Board & Line Recording**:
  - Full chess rules with castling, en passant, promotion, and check detection.
  - Interactive move candidate dots and capture rings.
  - Real-time Standard Algebraic Notation (SAN) generation.
- 🌿 **Branching & Multi-Line Exploration**:
  - Undo any move to branch into alternate continuations.
  - Save multiple variations under the same opening family.
- 📁 **`opening.txt` Ecosystem**:
  - **Export**: One-tap export to formatted `opening.txt` for easy file sharing and Chessy integration.
  - **Import**: Load existing `opening.txt` files directly into GambitStudio to inspect, edit, or extend.
- 🎓 **Active Training Mode**:
  - Practice lines from White or Black perspectives.
  - Immediate feedback on correct vs. deviation moves.
- 🎨 **Lichess Visual Theme**:
  - Classic Lichess brown board (`#F0D9B5` / `#B58863`).
  - Authentic Colin M.L. Burnett (**cburnett**) vector piece artwork.
  - Vivid violet check highlight (`#8E24AA`, alpha 230) for high visual clarity on both light and dark squares.
- 🟧 **Vibrant Orange Branding**:
  - Clean top app bar with prominent top-left title.
  - Signature Vibrant Orange (`#E64A19`) theme and custom Rook emblem icon.
- 🚀 **Automated CI/CD Release Pipeline**:
  - Tag-driven semantic versioning (`vX.Y.Z` automatically generates `versionCode`).
  - GitHub Actions runs unit tests, compiles signed release APKs, and publishes releases.

---

## 📄 `opening.txt` Format Specification

GambitStudio exports files formatted specifically for seamless parsing by both GambitStudio and Chessy:

```text
[Opening Name 1]
1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5

[Opening Name 2 - Variation]
1. d4 d5 2. c4 dxc4 3. Nf3 Nf6
```

---

## 🧱 Project Architecture

```
GambitStudio/
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/mhrahmankwoshik/gambitstudio/
│       │   │   ├── data/
│       │   │   │   ├── model/ (Move, Piece, Square, GameState, Opening)
│       │   │   │   ├── OpeningDatabase.kt
│       │   │   │   ├── OpeningFileManager.kt
│       │   │   │   └── OpeningVariationParser.kt
│       │   │   ├── game/
│       │   │   │   ├── ChessBoard.kt
│       │   │   │   ├── ChessGame.kt
│       │   │   │   ├── FenUtil.kt
│       │   │   │   └── MoveGenerator.kt
│       │   │   ├── ui/
│       │   │   │   ├── ChessBoardView.kt
│       │   │   │   ├── MainActivity.kt
│       │   │   │   ├── TrainingActivity.kt
│       │   │   │   ├── MoveHistoryAdapter.kt
│       │   │   │   ├── OpeningsAdapter.kt
│       │   │   │   └── dialog/PawnPromotionDialog.kt
│       │   │   └── util/SoundPlayer.kt
│       │   └── res/ (drawable, layout, values, mipmap)
│       └── test/ (Unit tests for game rules, FEN, SAN, and line parsing)
├── .github/
│   ├── workflows/release.yml
│   └── scripts/
│       ├── generate_release_notes.py
│       └── generate_summary.py
├── CHANGELOG.md
└── README.md
```

---

## 🛠️ Building & Developing

### Prerequisites
- Android Studio Ladybug / Koala or newer
- JDK 17 (Eclipse Temurin recommended)
- Android SDK 34 (compileSdk / targetSdk)
- Min SDK: 24 (Android 7.0 Nougat)

### Build Commands
```bash
# Clone the repository
git clone https://github.com/<your-username>/GambitStudio.git
cd GambitStudio

# Run all unit tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# Build optimized release APK
./gradlew assembleRelease
```
The output APK will be placed in `app/build/outputs/apk/release/app-release.apk`.

---

## 🚀 Automated CI/CD & Versioning

GambitStudio uses **Tag-Driven Automated Semantic Versioning** (Option C):

- Every pushed Git tag (e.g., `git tag v1.0.1 && git push origin v1.0.1`) triggers `.github/workflows/release.yml`.
- The workflow automatically calculates `versionCode = (major * 10000) + (minor * 100) + patch` and applies it to the build.
- The pipeline runs unit tests, builds the signed release APK, and attaches it directly to a new GitHub Release.
