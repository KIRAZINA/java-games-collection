# 2048 Game - Java Swing Implementation (Green-Black Edition)

A complete implementation of the classic 2048 game using Java 17 and Swing with smooth animations, a stunning **green-black color theme**, and an animated welcome screen.

## Features

- **Welcome Screen**: Animated glowing title with Start Game and Exit buttons
- **Classic 2048 Gameplay**: 4×4 grid (easily configurable to other sizes)
- **Advanced Animations**: 
  - Tile sliding with ease-out interpolation (200ms)
  - **Rotation animation** for new tiles (720° spin)
  - **Fade-in effects** with opacity transitions
  - Pop/scale animation for merges
  - 60 FPS rendering for buttery smooth visuals
- **Dual Control Schemes**: Arrow keys OR WASD keys
- **Green-Black Color Scheme**: Modern high-contrast theme with neon green accents
- **Score Tracking**: Real-time score display
- **Game Over Detection**: Automatic detection with glowing overlay
- **Menu Navigation**: Return to welcome screen anytime

## Project Structure

```
2048-game/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── KIRA_ZINA/
│                   └── app/
│                       ├── Main.java                 # Entry point
│                       ├── model/
│                       │   ├── Direction.java        # Movement directions enum
│                       │   ├── Tile.java            # Tile with animation support
│                       │   └── Board.java           # Game logic and state
│                       └── gui/
│                           ├── ColorScheme.java     # Green color theme
│                           ├── GamePanel.java       # Main game rendering
│                           └── MainFrame.java       # Application window
└── README.md
```

## How to Run

### Option 1: Command Line (using javac and java)

1. Navigate to the project directory:
   ```bash
   cd 2048-game
   ```

2. Compile all Java files:
   ```bash
   javac -d bin src/main/java/com/KIRA_ZINA/app/*.java src/main/java/com/KIRA_ZINA/app/model/*.java src/main/java/com/KIRA_ZINA/app/gui/*.java
   ```

3. Run the game:
   ```bash
   java -cp bin com.KIRA_ZINA.app.Main
   ```

### Running Tests

To run the unit tests, verify you have Maven installed and run:

```bash
mvn test
```

### Option 2: Using an IDE (IntelliJ IDEA, Eclipse, VS Code)

1. Open the `2048-game` folder as a project
2. Make sure the JDK is set to Java 17 or higher
3. Run the `Main.java` class

## How to Play

- **Move tiles**: Use Arrow keys (↑ ↓ ← →) or WASD keys
- **Goal**: Combine tiles with the same number to create larger numbers
- **Win condition**: Reach the 2048 tile (you can continue playing after)
- **Game over**: When the board is full and no adjacent tiles can merge
- **New game**: Press F2 or click the "New Game" button

## Game Rules

1. Each move slides all tiles in the chosen direction
2. Tiles with the same value merge when they collide
3. Each merge adds the combined value to your score
4. After each successful move, a new tile (90% chance of 2, 10% chance of 4) appears
5. Tiles can only merge once per move

## Customization

To change the grid size (e.g., from 4×4 to 5×5):

1. Open `src/main/java/com/KIRA_ZINA/app/model/Board.java`
2. Change the `GRID_SIZE` constant:
   ```java
   public static final int GRID_SIZE = 5; // Change from 4 to 5
   ```
3. Recompile and run

To change the color scheme:

1. Open `src/main/java/com/KIRA_ZINA/app/gui/ColorScheme.java`
2. Modify the color values in the `TILE_COLORS` map

## Technical Highlights

- **Clean OOP Design**: Separation of model and view
- **Double Buffering**: Smooth rendering without flicker
- **Timer-based Animation**: 60 FPS animation loop
- **Extensible Architecture**: Easy to modify grid size, colors, or add features
- **No External Dependencies**: Uses only standard JDK libraries

## Requirements

- Java 17 or higher
- No external libraries required

## License

This is a learning project. Feel free to use and modify as needed.

---

Enjoy playing 2048! 🎮
