# Changelog

All notable changes to the **GambitStudio** project will be documented in this file.

## [1.2.1] - 2026-11-27
### Changed
- **New app icon**:
  - White pawn on orange, replacing the rook emblem.
  - Adaptive icon support for all launcher shapes, plus square and round icons for older Android versions.
  - The app now uses the same logo as the README and the store listing.

## [1.2.0] - 2026-11-27
### Added
- **Position memory (transpositions)**:
  - Training now remembers every board position in the opening file. When a recorded position is reached by a different move order, it is recognised instead of being treated as a new line.
  - A new **TRANSPOSITION** status badge shows when this happens. The recorded replies appear as move chips and auto-play continues from them, so the rest of the line does not need to be played again.
  - **Save Line** at a transposition also writes every recorded continuation from that position under the new move order, so exported files contain the full lines for Chessy.

### Changed
- **Store listing**: Added the fastlane title, short description and full description, and fixed the fastlane package name (it pointed to Chessy).
- **README**: Restyled like Chessy's, with build, usage and security details moved to `BUILD_INFO.md`, `USAGE.md` and `SECURITY.md`. The release badge is now updated automatically by a `post-commit` Git hook.

### Fixed
- **Opening files with several starting lines**:
  - Opening files can now hold more than one line starting from move 1 (e.g. `1. e4 e5 2. Nf3 Nc6` and `1. Nf3 Nc6 2. e4 e5`). Previously every extra line starting with `1.` was read as a continuation of the first line, rejected as illegal, and ignored.
  - Saving a line no longer risks dropping the first line of the file when another line starts with a different first move.

### Notes
- Files that contain several lines starting from move 1 need **Chessy 1.3.1** or later to load in full.

## [1.1.0] - 2026-10-16
### Added
- **Asking Side Prompt**:
  - A prompt will appear when a opening list is imported.
- **LICENSE**:
  - MIT License has been chosen for the project.

## [1.0.0] - 2026-10-12

### Added
- **Gambit & Opening Line Creation**:
  - Interactive chessboard to play through, record, and explore opening variations and gambits.
  - Branching support: undo moves, explore alternate continuations, and save multiple distinct opening lines without losing history.
  - Export and save lines directly to formatted `opening.txt` for training and seamless integration with Chessy.
  - Import existing `opening.txt` files to load, view, and expand gambit lines.
- **Visuals & Board Design**:
  - Vibrant Orange branding (`#E64A19`) with dedicated Rook emblem icon.
  - Standard Lichess board styling and cburnett vector chess pieces.
  - Distinct king-in-check violet indicator (`#8E24AA`) with high visibility.
  - Clean top app bar with prominent top-left title.
- **Engine Diagnostics & Unit Tests**:
  - Full move validation and algebraic notation (SAN) support.
  - Unit tests covering board state, SAN generation, FEN handling, and move validation.
