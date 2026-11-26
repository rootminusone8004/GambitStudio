# GambitStudio Usage Guide 🏰

## Overview
**GambitStudio** is an offline opening studio for building and training chess openings and gambits. Each opening is stored as a plain-text file that you can export and load into **Chessy**, where Stockfish plays your recorded lines from its opening book.

---

## Creating an Opening File

1. On the home screen, tap **+ New File** (or **Create First Opening File**).
2. Enter a name and choose the side the opening is for (**White** or **Black**).
3. Play moves for both sides on the board.
4. Tap **Save Line** when the line is done.

To try the app quickly, tap **Add Sample Scotch Gambit** on the empty home screen.

---

## Branching Variations

1. Tap **Undo** to step back to the move where you want to branch. The status shows **BRANCH POINT**.
2. Play a different move and continue the new variation.
3. Tap **Save Line**. The new line is added as a branch, and your earlier lines are kept.

Tap **New Line** to go back to the starting position.

---

## Training

Open an opening from the home screen to practise it.

- The app auto-plays the recorded moves for the opening's side. You play the other side.
- When more than one recorded move is available, each one is shown as a chip. Tap a chip to play it.
- Tap the **Auto-Play** button to pause automatic moves and play both sides yourself.
- Tap **Flip Board** to change the board orientation.

### Status badges

| Badge | Meaning |
| ----- | ------- |
| **IN FILE** | The moves so far are recorded in the file. |
| **TRANSPOSITION** | The position is recorded in the file, but you reached it by a different move order. |
| **BRANCH POINT** | You stepped back with Undo. Play a new move to branch, or tap a chip. |
| **LINE COMPLETE** | You reached the end of a recorded line. |
| **NEW LINE** | The position is not in the file. Play moves and tap **Save Line** to record it. |

### Transpositions

GambitStudio remembers every board position in the opening file. If you reach a recorded position through a different move order, for example `1. Nf3 Nc6 2. e4 e5` instead of `1. e4 e5 2. Nf3 Nc6`:

- The status shows **TRANSPOSITION**, and training continues from the recorded moves, so you don't have to play the rest of the line again.
- Tapping **Save Line** saves your move order together with every recorded continuation from that position. The exported file then contains those lines in full, so Chessy can play them.

---

## Importing and Exporting

- **Import:** tap **Import** on the home screen, pick an opening `.txt` file, then choose which side the opening is for.
- **View and export:** while training, tap **View Repertoire File** to see the file. From there you can copy its contents or export it as a `.txt` file.

### Loading a file into Chessy

In Chessy, open **My Gambits**, tap **+ Create Gambit**, then tap **Load File** and pick the exported file. Files with more than one line starting from move 1 need Chessy 1.3.1 or later.

---

## Opening File Format

- Line 1 holds the base moves, e.g. `1. e4 e5 2. Nf3 Nc6 3. Bc4`.
- An indented line branches from the line above without repeating its moves.
- A line starting with `1.` starts again from the initial position.
- Lines starting with `#` or `//` are comments.

```text
1. e4 e5 2. Nf3 Nc6 3. Bc4
   Bc5 4. c3 Nf6 5. d4
   Nf6 4. Ng5 d5
1. Nf3 Nc6 2. e4 e5 3. Bc4
   Bc5 4. c3 Nf6 5. d4
   Nf6 4. Ng5 d5
```
