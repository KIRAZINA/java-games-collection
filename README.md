# Java Games Collection

Web-based gaming platform featuring Blackjack, Minesweeper, and 2048 with a React frontend and Spring Boot backend.

## Games

| Game | Description |
|------|-------------|
| **Blackjack** | Casino card game with dealer AI (Basic/Conservative/Aggressive), dealer peek, 5-Card Charlie, and bankruptcy bailout |
| **Minesweeper** | Classic logic puzzle with first-click safety zone, flagging, BFS flood-fill, and next-board progression |
| **2048** | Sliding tile puzzle with keyboard/button controls, score tracking, and move counting |

## Project Structure

```
java-games-collection/
├── games-backend/          # Spring Boot 3.3.5 REST API (Java 17)
│   ├── src/main/java/.../backend/
│   │   ├── blackjack/      # Blackjack domain, service, controller
│   │   ├── minesweeper/    # Minesweeper domain, service, controller
│   │   ├── twentyfortyeight/ # 2048 domain, service, controller
│   │   ├── common/         # Room system, progress polling
│   │   └── config/         # Rate limiting, CORS, exception handling
│   └── src/test/
├── games-frontend/         # React 18 + Vite
│   ├── src/
│   │   ├── components/     # Blackjack, Minesweeper, Game2048, RoomLobby, etc.
│   │   ├── api/api.js      # Central fetch wrapper
│   │   ├── App.jsx         # Multi-view router (no react-router)
│   │   └── styles.css      # Animations, responsive layout
│   └── src/test/
├── docker-compose.yml
├── pom.xml
└── README.md
```

## Architecture

### Multiplayer Rooms ("Parallel Races")

Each player in a room has their own independent game session. Rooms aggregate progress via a lightweight polling endpoint (`GET /api/rooms/{roomId}/progress`). There is no shared table — all games run in parallel within the room context. This fits a clean server-authoritative model with REST short-polling (no WebSockets).

### Backend (games-backend)

Pure domain layer with no framework coupling — domain classes (`BlackjackSession`, `MinesweeperSession`, `Game2048Session`) contain no Spring annotations. Services wrap domain objects with HTTP lifecycle. All storage is in-memory (`ConcurrentHashMap`).

**Key design decisions:**
- Server-authoritative: all game logic runs on the backend; frontend sends intents only
- No database: all state lives in memory (suitable for ephemeral game sessions)
- Room auto-cleanup: empty rooms removed immediately; TTL-based eviction runs every 60s
- Rate limiting: bucket4j per-IP (60 tokens, 10 refill/10s); GET requests exempt
- Phase enforcement: Blackjack `placeBet`/`hit`/`stand` guarded by `IllegalStateException` → 409 Conflict

### Frontend (games-frontend)

Conditional rendering via `activeView` state (no `react-router-dom`). No heavy state management — `useState` + prop drilling only. API calls go through a single `api.js` fetch wrapper.

**UI features:**
- Dealer card masking (second card hidden during PLAYER_TURN, revealed on ROUND_OVER)
- Balance flash animations (green for wins, red for losses)
- Live countdown (5s auto-advance between rounds)
- Notification toast (blackjack, dealer peek, bailout messages)
- Bankruptcy banner (orange alert when balance reaches zero)
- Live opponent polling (room progress updates every 2s)
- Minesweeper "Next Board" button on win

## Requirements

- JDK 17+
- Maven 3.9+
- Node.js 18+ and npm
- Docker and Docker Compose (optional)

## Development

### Backend

```bash
# Build
mvn clean install

# Run tests (164 tests)
cd games-backend && mvn test

# Start server
cd games-backend && mvn spring-boot:run
```

API available at `http://localhost:8080`

### Frontend

```bash
cd games-frontend
npm install
cp .env.example .env
npm run dev
```

Frontend at `http://localhost:5173`

### Run All Tests

```bash
# Backend
cd games-backend && mvn test

# Frontend (44 tests)
cd games-frontend && npm test
```

## Docker

```bash
docker-compose up --build
```

Access at `http://localhost:80`

## In-Memory State (Deployment Note)

This is an in-memory demo application. All active games, rooms, and player balances are stored in `ConcurrentHashMap` and will be reset whenever the service restarts or goes to sleep after 15 minutes of inactivity (Render Free Tier behavior). No database is used.

## API Overview

### Game Sessions

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/blackjack/sessions` | Create Blackjack session |
| GET | `/api/blackjack/sessions/{id}` | Get state |
| DELETE | `/api/blackjack/sessions/{id}` | Close session |
| POST | `.../{id}/rounds` | Start new round |
| POST | `.../{id}/bets` | Place bet |
| POST | `.../{id}/hit` | Hit |
| POST | `.../{id}/stand` | Stand |

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/minesweeper/sessions` | Create Minesweeper session |
| GET/POST/DELETE | as above | Open, flag, reset, next-board |

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/2048/sessions` | Create 2048 session |
| GET/POST/DELETE | as above | Move, reset |

### Rooms

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/rooms` | List all rooms |
| GET | `/api/rooms?type=BLACKJACK` | List rooms by type |
| POST | `/api/rooms` | Create room |
| GET | `/api/rooms/{id}` | Get room details |
| POST | `/api/rooms/{id}/join` | Join room |
| DELETE | `/api/rooms/{id}/leave` | Leave room |
| GET | `/api/rooms/{id}/progress` | Lightweight player progress |
| GET | `/api/rooms/{id}/state` | Full room state |
| POST | `/api/rooms/{id}/sessions` | Register game session |

## Blackjack Rules

- **Dealer Peek**: If dealer has natural 21 after deal, round settles immediately (no player action)
- **5-Card Charlie**: Player with 5 cards without busting auto-wins (even money)
- **Bankruptcy Bailout**: When balance reaches 0, the next `startRound` refills to $100 and skips the hand
- **Blackjack Payout**: 3:2 (2.5× bet)
- **Win Payout**: 2× bet
- **Tie**: Bet returned (push)
- **Bet Range**: $1 – $1,000
- **Dealer Difficulty**: BASIC (hits ≤16), CONSERVATIVE (hits ≤15), AGGRESSIVE (hits ≤17)

## Testing

### Backend Tests (164 tests)

| Suite | Tests | Scope |
|-------|-------|-------|
| HandTest | 19 | value(), blackjack(), bust(), clear() |
| BlackjackSessionTest | 40 | State machine, payouts, dealer difficulty, deck reshuffle, bankruptcy bailout, dealer peek, 5-Card Charlie, notifications |
| BlackjackController IT | 10 | HTTP status, error handling, full lifecycle |
| MinesweeperSessionTest | 48 | First-click safety, win/loss, flag, BFS, reset |
| MinesweeperController IT | 8 | Create, open, flag, reset, lifecycle |
| Game2048SessionTest | 18 | Move directions, score, game-over, reset |
| Game2048Controller IT | 7 | Create, move, reset, lifecycle |
| GamesBackendIntegrationTest | 32 | All three games + room lifecycle + TTL eviction |
| GameRoomServiceTest | 7 | Create, join, leave, auto-cleanup |

### Frontend Tests (44 tests)

| Suite | Tests | Scope |
|-------|-------|-------|
| Blackjack.test.jsx | 23 | Rendering, button states, API calls, notifications, bankruptcy banner, countdown, dealer masking, exit button |
| Minesweeper.test.jsx | 10 | Board rendering, flag count, game over, win/loss |
| Game2048.test.jsx | 11 | Grid, score, arrow keys, move buttons, game over, reset |

## Build

```bash
# Backend JAR
mvn clean package           # target/games-backend-1.0-SNAPSHOT.jar

# Frontend bundle
cd games-frontend && npm run build   # dist/
```
