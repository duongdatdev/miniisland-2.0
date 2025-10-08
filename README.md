# Mini Island 2D - Client

## 📖 Overview
This is the client application for Mini Island 2D, a real-time multiplayer 2D game. The client provides the game interface, handles user input, renders graphics, and communicates with the game server via WebSocket.

## 🎮 Features
- **2D Game Rendering** - Custom sprite-based graphics using Java AWT/Swing
- **Real-time Networking** - WebSocket client for server communication
- **User Interface**:
  - Login/Registration screens
  - Game scenes (Maze, PvP)
  - In-game chat system
  - Global leaderboard display
- **Game Mechanics**:
  - Player movement and collision detection
  - Maze navigation
  - PvP combat system
  - Bomb mechanics
- **Custom Font Rendering** - Pixel art fonts and custom text rendering
- **Map System** - CSV-based tile maps with multiple game modes

## 🛠️ Technology Stack
- **Java 17** - Modern Java LTS version
- **Java Swing/AWT** - GUI and graphics rendering
- **Java-WebSocket 1.5.3** - WebSocket client library
- **Maven** - Build and dependency management

## 📁 Project Structure
```
mini-island-2d/
├── src/                          # Source code
│   ├── main/                     # Main application entry point
│   │   ├── Main.java            # Application launcher
│   │   ├── MiniIsland.java      # Main game class
│   │   ├── GameScene.java       # Game scene manager
│   │   └── CustomButton.java    # UI button component
│   ├── collision/                # Collision detection
│   ├── font/                     # Custom font rendering
│   ├── imageRender/              # Image loading and handling
│   ├── input/                    # Keyboard input handling
│   ├── maps/                     # Map system and tiles
│   ├── network/                  # WebSocket client
│   │   ├── client/              # Client connection
│   │   ├── entitiesNet/         # Network entities
│   │   └── leaderBoard/         # Leaderboard sync
│   ├── objects/                  # Game objects
│   │   └── entities/            # Player, NPC entities
│   └── panes/                    # UI screens
│       ├── auth/                # Login/Register
│       ├── chat/                # Chat interface
│       └── loading/             # Loading screens
├── Resource/                     # Game assets
│   ├── Chat/                    # Chat UI images
│   ├── font/                    # Font files and sprites
│   ├── LeaderBoard/             # Leaderboard UI
│   ├── Login/                   # Login screen assets
│   ├── Maps/                    # Map tiles and CSV data
│   ├── NPC/                     # NPC sprites
│   ├── player/                  # Player sprite sheets
│   └── Ui/                      # UI elements
└── pom.xml                      # Maven configuration
```

## 🚀 Getting Started

### Prerequisites
- **Java 17 JDK** or higher ([Download](https://adoptium.net/temurin/releases/?version=17))
- **Maven** (optional, for command-line builds)
- **Game Server** running and accessible

### Installation

1. **Navigate to client directory:**
   ```bash
   cd mini-island-2d
   ```

2. **Build the project:**
   ```bash
   mvn clean compile
   ```

3. **Run the client:**
   ```bash
   mvn exec:java -Dexec.mainClass="main.Main"
   ```

   Or using the provided batch script (Windows):
   ```bash
   ..\build.bat
   ```

### Configuration

The client connects to the game server via WebSocket. Configure the server connection in the network client classes:

- Default server: `ws://localhost:8887`
- Modify connection settings in `src/network/client/` files

## 🎯 How to Play

1. **Launch the game** - Run the client application
2. **Login/Register** - Create an account or sign in
3. **Choose game mode:**
   - **Maze Mode** - Navigate mazes to earn points
   - **PvP Mode** - Battle other players
4. **Controls:**
   - Arrow keys or WASD - Move your character
   - Space - Place bomb (in PvP mode)
   - Enter - Send chat message
5. **View leaderboard** - Check top players and rankings

## 🔧 Development

### Building from Source
```bash
# Clean and compile
mvn clean compile

# Package as JAR
mvn package

# Run tests (if available)
mvn test
```

### IDE Setup
1. Open the `mini-island-2d` folder in your IDE
2. Ensure Java 17 JDK is configured
3. Let Maven import dependencies automatically
4. Run `main.Main` class to start the game

### Adding New Features
- **New game objects** - Add to `src/objects/entities/`
- **New UI screens** - Add to `src/panes/`
- **New maps** - Add CSV files to `Resource/Maps/`
- **New sprites** - Add PNG files to appropriate `Resource/` subdirectories

## 📦 Dependencies

```xml
<dependency>
    <groupId>org.java-websocket</groupId>
    <artifactId>Java-WebSocket</artifactId>
    <version>1.5.3</version>
</dependency>
```

## 🐛 Troubleshooting

### Client won't start
- Ensure Java 17+ is installed: `java -version`
- Check Maven build for errors: `mvn clean compile`

### Can't connect to server
- Verify server is running on the expected port (default: 8887)
- Check firewall settings
- Verify WebSocket URL in client configuration

### Graphics not rendering
- Ensure `Resource/` folder is in the correct location
- Check that PNG and TTF files are not corrupted
- Verify image paths in code match actual file locations

### Performance issues
- Reduce game resolution
- Close other resource-intensive applications
- Check for memory leaks in game loop

## 📝 Network Protocol

The client communicates with the server using WebSocket JSON messages:

- **Authentication**: `{type: "login", username: "...", password: "..."}`
- **Movement**: `{type: "move", direction: "...", x: 0, y: 0}`
- **Chat**: `{type: "chat", message: "..."}`
- **Game State**: Receives real-time updates from server

See `src/network/` for complete protocol implementation.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes in the client code
4. Test thoroughly with the server
5. Submit a pull request

## 📄 License

This project is part of the Mini Island 2D game system.

## 🔗 Related

- **Server**: See `../mini-island-2d-server/README.md`
- **Main README**: See `../README.md` for complete project overview

## 👥 Support

For issues, questions, or contributions, please refer to the main repository.

---

**Note**: This client requires the Mini Island 2D server to be running for multiplayer functionality.
