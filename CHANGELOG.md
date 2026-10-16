# Changelog

All notable changes to the **GambitStudio** project will be documented in this file.

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
