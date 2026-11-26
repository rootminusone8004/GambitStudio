# 🏰 GambitStudio

![License: MIT](https://img.shields.io/badge/License-MIT-800000.svg)
&nbsp;&nbsp;
![release: 1.2.1](https://img.shields.io/badge/release-1.2.1-blue.svg)

<img src="fastlane/metadata/android/en-US/images/icon.png" alt="GambitStudio" width="50">

GambitStudio is a **simple, offline opening studio** for building and training chess openings and gambits.
It is the companion app for [Chessy](https://github.com/rootminusone8004/Chessy): the opening files it exports power Chessy's opening book.

## ♞ What is it?

GambitStudio is a focused opening workshop designed to be:

- ✅ Simple — Play the moves on the board and save the line.
- 📶 Offline — Everything stays on your phone.
- 🔐 Private — No data collection, no accounts, no syncing.
- 🎯 Focused — Built solely for preparing opening and gambit lines.

With GambitStudio, you can:

- ♟️ Record lines by playing moves for both sides on the board.
- 🌿 Undo any move to branch into a new variation without losing saved lines.
- 🎓 Train an opening as White or Black, with your recorded replies played automatically.
- 🧠 Reuse what you already recorded: a known position reached by a different move order (a transposition) is recognised, so you don't have to play the rest of the line again.
- 📁 Import and export plain-text opening files, ready to load into Chessy.

## 📄 Opening file format

Line 1 holds the base moves. Indented lines branch from the line above without repeating moves, and a line starting with `1.` starts again from the initial position:

```text
1. e4 e5 2. Nf3 Nc6 3. Bc4
   Bc5 4. c3 Nf6 5. d4
   Nf6 4. Ng5 d5
1. Nf3 Nc6 2. e4 e5 3. Bc4
   Bc5 4. c3 Nf6 5. d4
   Nf6 4. Ng5 d5
```

Files with more than one line starting from move 1 need Chessy 1.3.1 or later.

## 📥 Download

Download the latest APK from the [Releases](https://github.com/rootminusone8004/GambitStudio/releases/).

## 🧰 Build the app from Source

Want to build GambitStudio yourself? See the [BUILD_INFO.md](BUILD_INFO.md) file for full instructions.

## 📖 How to Use

New to GambitStudio? Check out the [USAGE.md](USAGE.md) file for a quick guide.

## 🤝 Contribute

We welcome your contributions! You can:

- ✨ Add features or fix bugs via a <a href="https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request" target="_blank">pull request</a>.
- ♟️ Share opening and gambit files.
- 💡 Suggest ideas or improvements.

## 🔒 Security

If you find a security vulnerability or have a security concern, please see our [SECURITY.md](SECURITY.md) for guidance on how to report it responsibly.

## ⚖️ License

GambitStudio - simple offline opening studio and gambit trainer<br>
Copyright (c) 2026-present Md. Hashibur Rahman Kwoshik

This program is free software, released under the MIT License. You may use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of it, provided the copyright notice and permission notice are included.

This program is provided "as is", without warranty of any kind. See the [LICENSE](LICENSE.txt) file for the full text.
