# 🃏 Blackjack Game

A feature-rich Blackjack game implementation in Java with both GUI and console interfaces. Play against an intelligent dealer with multiple difficulty levels and enjoy a polished gaming experience.

![Java](https://img.shields.io/badge/Java-17+-orange.svg)
![LibGDX](https://img.shields.io/badge/LibGDX-1.12.1-red.svg)
![Maven](https://img.shields.io/badge/Maven-3.8+-blue.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

## ✨ Features

### 🎮 Dual Interface
- **GUI Mode**: Modern graphical interface built with LibGDX
- **Console Mode**: Classic terminal-based gameplay with colored output

### 🎯 Dealer Difficulty Levels
Choose from three AI difficulty modes:
- **Basic**: Standard casino rules (dealer hits until 17)
- **Conservative**: Easier difficulty (dealer stands on 16+)
- **Aggressive**: Challenging mode (dealer hits until 18)

### 🎨 Enhanced Graphics
- Beautiful playing cards with proper suit symbols (♥ ♦ ♣ ♠)
- Color-coded suits (red for hearts/diamonds, black for clubs/spades)
- Smooth rounded corners and professional styling
- Decorative card backs

### 💰 Complete Blackjack Rules
- Betting system with bankroll management
- Blackjack pays 3:2
- Dealer reveals one card during player turn
- Proper bust and win/loss detection
- Push (tie) handling

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.8 or higher

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/KIRAZINA/java-games-collection.git
cd java-games-collection/Black-Jack-game
```

2. **Build the project**
```bash
mvn clean compile
```

## 🎲 How to Play

### GUI Mode (Recommended)

Run the graphical version:
```bash
mvn exec:java -Dexec.mainClass="com.KIRA_ZINA.app.gdx.BlackjackDesktopLauncher"
```

**Gameplay:**
1. Click **PLAY** on the main menu
2. Select your preferred **difficulty level**
3. Adjust your bet using **BET +** and **BET -** buttons
4. Click **DEAL** to start the round
5. Choose **HIT** to take another card or **STAND** to end your turn
6. Try to get closer to 21 than the dealer without going over!

### Console Mode

Run the terminal version:
```bash
mvn exec:java -Dexec.mainClass="com.KIRA_ZINA.app.Main"
```

**Gameplay:**
1. Select difficulty level (1-3)
2. Enter your bet amount
3. Type `h` to hit or `s` to stand
4. Type `y` to play another round or `n` to quit

## 🎯 Game Rules

- **Objective**: Get a hand value closer to 21 than the dealer without exceeding 21
- **Card Values**:
  - Number cards (2-10): Face value
  - Face cards (J, Q, K): 10 points
  - Ace: 1 or 11 points (automatically optimized)
- **Blackjack**: Ace + 10-value card on initial deal (pays 3:2)
- **Bust**: Hand value exceeds 21 (automatic loss)
- **Push**: Tie with dealer (bet returned)

## 🏗️ Project Structure

```
Black-Jack-game/
├── src/main/java/com/KIRA_ZINA/app/
│   ├── core/              # Game logic and controllers
│   │   ├── GameController.java
│   │   └── GameModel.java
│   ├── entity/            # Player and dealer entities
│   ├── gdx/               # LibGDX GUI components
│   │   ├── BlackjackGdxGame.java
│   │   ├── MenuScreen.java
│   │   ├── DifficultySelectionScreen.java
│   │   ├── GameScreen.java
│   │   └── CardRenderer.java
│   ├── model/             # Card, deck, hand models
│   ├── strategy/          # Dealer AI strategies
│   │   ├── BasicDealerStrategy.java
│   │   ├── ConservativeDealerStrategy.java
│   │   └── AggressiveDealerStrategy.java
│   ├── view/              # Console view components
│   └── Main.java          # Console entry point
└── pom.xml                # Maven configuration
```

## 🧪 Testing

Run the test suite:
```bash
mvn test
```

The project includes comprehensive unit tests for:
- Game logic and rules
- Dealer strategies
- Card and deck operations
- Bankroll management
- Input validation

## 🛠️ Technologies Used

- **Java 17**: Core programming language
- **LibGDX 1.12.1**: Game development framework for GUI
- **Maven**: Build automation and dependency management
- **JUnit 5**: Unit testing framework

## 🎨 Customization

### Adding Custom Card Graphics

The game supports custom card images. Place PNG files in `assets/cards/` with the naming convention:
```
assets/cards/hearts_ace.png
assets/cards/diamonds_2.png
assets/cards/clubs_king.png
...
```

If custom images are not found, the game automatically generates cards programmatically.

### Modifying Dealer Strategies

Create custom dealer strategies by implementing the `DealerStrategy` interface:

```java
public class CustomStrategy implements DealerStrategy {
    @Override
    public boolean shouldHit(int handValue, Deck deck) {
        // Your custom logic here
        return handValue < 16;
    }
    
    @Override
    public String getStrategyName() {
        return "Custom";
    }
}
```

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👤 Author

**KIRA_ZINA**
- GitHub: [@KIRAZINA](https://github.com/KIRAZINA)

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](https://github.com/KIRAZINA/java-games-collection/issues).

## ⭐ Show Your Support

Give a ⭐️ if you enjoyed this project!

## 📸 Screenshots

### GUI Mode
- Main menu with play and exit options
- Difficulty selection screen with three modes
- Game screen with betting controls and card display
- Visual card rendering with proper suit symbols

### Console Mode
- Colored text output for better readability
- Interactive difficulty selection
- Real-time game state display
- Formatted card and hand information

## 🔄 Recent Updates

### Version 1.0 (Latest)
- ✅ Added dealer difficulty selection (Basic, Conservative, Aggressive)
- ✅ Implemented improved card graphics with proper suit symbols
- ✅ Created dedicated difficulty selection screen in GUI
- ✅ Enhanced console interface with colored difficulty menu
- ✅ Added texture caching for better performance
- ✅ Comprehensive test coverage

## 🐛 Known Issues

None at this time. Please report any bugs in the [issues section](https://github.com/KIRAZINA/java-games-collection/issues).

## 📚 Additional Resources

- [Blackjack Rules](https://en.wikipedia.org/wiki/Blackjack)
- [LibGDX Documentation](https://libgdx.com/wiki/)
- [Maven Guide](https://maven.apache.org/guides/)

---

**Enjoy the game! 🎰**
