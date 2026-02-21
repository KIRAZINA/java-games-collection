# Minesweeper - Java Swing

A classic Minesweeper game implementation in Java 17 using Swing GUI framework.

![Minesweeper Screenshot](https://img.shields.io/badge/Java-17-blue) ![Swing](https://img.shields.io/badge/GUI-Swing-green) ![Maven](https://img.shields.io/badge/Build-Maven-orange)

## Features

- 🎮 Classic Minesweeper gameplay
- 🎯 Three difficulty levels: Beginner, Intermediate, Expert
- ⏱️ Timer and mine counter
- 🚩 Flag placement with right-click
- 🖱️ Middle-click chord functionality
- 💣 Safe first click (3x3 safe zone)
- 🎨 Classic Minesweeper visual style
- 📱 Start screen with game menu

## Requirements

- Java 17 or higher
- Maven 3.6+ (for building)

## Project Structure

```
src/main/java/com/KIRA_ZINA/app/
├── MinesweeperApp.java          # Application entry point
├── model/
│   ├── Cell.java                # Cell data model
│   └── MinesweeperModel.java    # Game logic and state
└── ui/
    ├── StartScreen.java         # Start menu screen
    ├── MainFrame.java           # Main game window
    ├── GamePanel.java           # Game grid panel
    ├── TopPanel.java            # Timer and counter panel
    └── CellButton.java          # Cell button component
```

## Build & Run

### Using Maven

```bash
# Clone the repository
git clone <repository-url>
cd Minesweeper-game

# Build the project
mvn clean compile

# Run the application
mvn exec:java -Dexec.mainClass="com.KIRA_ZINA.app.MinesweeperApp"
```

### Using JAR

```bash
# Build JAR
mvn clean package

# Run JAR
java -jar target/Minesweeper-game-1.0-SNAPSHOT.jar
```

### From IDE

1. Open the project in your IDE (IntelliJ IDEA, Eclipse, VS Code)
2. Configure JDK 17
3. Run `MinesweeperApp.java`

## How to Play

| Action | Control |
|--------|---------|
| Open cell | Left click |
| Place/Remove flag | Right click |
| Chord (open neighbors) | Middle click (on opened number) |
| New game | F2 or click smiley |
| Exit | Alt+F4 |

## Difficulty Levels

| Level | Grid Size | Mines |
|-------|-----------|-------|
| Beginner | 9 × 9 | 10 |
| Intermediate | 16 × 16 | 40 |
| Expert | 16 × 30 | 99 |

## Game Rules

1. The objective is to uncover all cells that do not contain mines
2. The first click is always safe (3×3 safe zone)
3. Numbers indicate how many mines are in adjacent cells
4. Flag suspected mines with right-click
5. Win by opening all non-mine cells

## Technical Details

- **Language**: Java 17
- **GUI Framework**: Swing
- **Build Tool**: Maven
- **Architecture**: Model-View separation
- **Design Patterns**: Observer (GameListener), MVC

## License

This project is created as a demo/educational project.

## Author

KIRA_ZINA

---

*Classic Minesweeper - Java Swing Edition*
