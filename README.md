# Aurora Commands

<div align="center">

**The most advanced command API for Spigot/Paper plugins**

[![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://www.oracle.com/java/)
[![Spigot](https://img.shields.io/badge/Spigot-1.8.8--1.21.1-yellow.svg)](https://www.spigotmc.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Documentation](https://img.shields.io/badge/Docs-commands.auroradev.cc-brightgreen.svg)](https://commands.auroradev.cc)

[Documentation](https://commands.auroradev.cc) | [Features](#-features) | [Quick Start](#-quick-start) | [Examples](#-examples)

</div>

---

## 🚀 Overview

Aurora Commands is an enterprise-grade command framework that transforms how you build Minecraft plugins. Say goodbye to boilerplate code and hello to intuitive, powerful command creation.

```java
// Simple yet powerful
new AuroraCommand("heal")
    .setDescription("Restore health and hunger")
    .setPermission("aurora.heal")
    .addArgument("target", new PlayerArgumentType())
    .setExecutor((sender, args) -> {
        Player target = args.get("target");
        target.setHealth(20.0);
        target.setFoodLevel(20);
        sender.sendMessage("§aHealed " + target.getName());
    })
    .register(plugin);
```

## ✨ Features

### 🎯 Core Features
- **22+ Argument Types** - Player, World, Enchantment, Time ("5m30s"), RangedInteger, Color (hex/RGB), and more
- **Smart Tab Completion** - Fuzzy matching, async completions, intelligent caching
- **Command Flags & Options** - Parse `-f`, `--verbose`, `--amount=5` automatically
- **Fluent Builder API** - Validators, defaults, conditions, dependencies in one chain
- **Permission System** - ANY/ALL groups, dynamic permissions (lambdas)
- **Cooldown System** - Per-player/global cooldowns with automatic cleanup

### 🔥 Advanced Features
- **Command Middleware** - Before/after/error hooks with priority chains
- **Async Execution** - Thread-safe async command processing
- **Confirmation System** - Require confirmation for destructive commands
- **Smart Suggestions** - "Did you mean?" with Levenshtein distance
- **Rate Limiting** - Per-player/per-IP/global limits with sliding window algorithm
- **Smart Pagination** - JSON-based clickable navigation with caching

### 🎨 Innovative Features
- **Natural Language Processing** - "aurora give me 64 diamonds" → parsed and executed
- **Auto-Generated GUIs** - Convert any command to an inventory menu
- **Command Pipelines** - Chain commands with `|` operator
- **Conditional Execution** - Time/world/weather-based command conditions
- **Transaction System** - Auto-rollback on errors (database-like ACID)
- **Scripting Integration** - Execute JavaScript code in commands
- **A/B Testing Framework** - Statistical testing for command variants
- **YAML/JSON Commands** - Load commands from configuration files
- **Command Templates** - Reusable configs ("admin", "player", "dangerous")
- **Metrics & Analytics** - Track executions, timing, error rates

## 📦 Installation

### Maven
```xml
<repository>
    <id>aurora-repo</id>
    <url>https://repo.auroradev.cc/releases</url>
</repository>

<dependency>
    <groupId>dev.aurora</groupId>
    <artifactId>aurora-commands</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Gradle
```gradle
repositories {
    maven { url 'https://repo.auroradev.cc/releases' }
}

dependencies {
    implementation 'dev.aurora:aurora-commands:2.0.0'
}
```

### Manual Installation
1. Download the latest JAR from [Releases](../../releases)
2. Add to your plugin's `libs/` folder
3. Shade into your plugin JAR

## 🏃 Quick Start

### 1. Basic Command
```java
public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        new AuroraCommand("greet")
            .setDescription("Greet a player")
            .addArgument("player", new PlayerArgumentType())
            .addArgument("message", new GreedyStringArgumentType())
            .setExecutor((sender, args) -> {
                Player target = args.get("player");
                String message = args.get("message");
                target.sendMessage("§e" + sender.getName() + " says: " + message);
            })
            .register(this);
    }
}
```

### 2. With Cooldowns & Permissions
```java
new AuroraCommand("kit")
    .setDescription("Claim your starter kit")
    .setPermission("myplugin.kit")
    .setCooldown(3600) // 1 hour in seconds
    .setExecutor((sender, args) -> {
        Player player = (Player) sender;
        player.getInventory().addItem(
            new ItemStack(Material.DIAMOND_SWORD),
            new ItemStack(Material.DIAMOND_PICKAXE)
        );
        player.sendMessage("§aKit claimed!");
    })
    .register(plugin);
```

### 3. Advanced Validation & Defaults
```java
new AuroraCommand("teleport")
    .setDescription("Teleport to coordinates")
    .addArgument("x", ArgumentBuilder.create(new IntegerArgumentType())
        .withDefault(() -> 0)
        .withValidator(x -> x >= -30000000 && x <= 30000000)
        .build())
    .addArgument("y", ArgumentBuilder.create(new IntegerArgumentType())
        .withDefault(() -> 64)
        .withValidator(y -> y >= 0 && y <= 256)
        .build())
    .addArgument("z", new IntegerArgumentType())
    .setExecutor((sender, args) -> {
        Player player = (Player) sender;
        int x = args.get("x");
        int y = args.get("y");
        int z = args.get("z");

        Location loc = new Location(player.getWorld(), x, y, z);
        player.teleport(loc);
        player.sendMessage("§aTeleported to " + x + ", " + y + ", " + z);
    })
    .register(plugin);
```

## 📖 Examples

### Confirmation System
```java
new AuroraCommand("deleteworld")
    .requireConfirmation("This will permanently delete the world. Type the command again to confirm.")
    .setExecutor((sender, args) -> {
        // Destructive operation here
    })
    .register(plugin);
```

### Command Flags & Options
```java
new AuroraCommand("backup")
    .enableFlags() // Enable flag parsing
    .setExecutor((sender, args) -> {
        boolean verbose = args.hasFlag("v") || args.hasFlag("verbose");
        String format = args.getOption("format", "zip"); // Default: zip
        int compression = Integer.parseInt(args.getOption("compression", "9"));

        if (verbose) sender.sendMessage("§eStarting backup...");
        // Perform backup
    })
    .register(plugin);

// Usage: /backup -v --format=tar --compression=6
```

### Natural Language Processing
```java
new AuroraCommand("aurora")
    .setDescription("Natural language command interface")
    .enableNaturalLanguage()
    .addNLPPattern("give {player} {amount} {item}", (sender, entities) -> {
        Player target = entities.getPlayer("player");
        int amount = entities.getInt("amount");
        Material item = entities.getMaterial("item");

        target.getInventory().addItem(new ItemStack(item, amount));
        sender.sendMessage("§aGave " + amount + " " + item + " to " + target.getName());
    })
    .register(plugin);

// Usage: /aurora give Steve 64 diamonds
```

### Command Pipelines
```java
// Enable pipeline support globally
CommandManager.enablePipelines();

// Commands can now be chained
// /gamemode creative | fly | speed 3
```

### Auto-Generated GUI
```java
new AuroraCommand("kit")
    .setDescription("Claim a kit")
    .addArgument("type", new EnumArgumentType<>(KitType.class))
    .enableAutoGUI() // Automatically creates inventory menu
    .setExecutor((sender, args) -> {
        // Give kit
    })
    .register(plugin);
```

### Async Execution
```java
new AuroraCommand("lookup")
    .setDescription("Lookup player stats from database")
    .addArgument("player", new OfflinePlayerArgumentType())
    .setAsyncExecutor((sender, args) -> {
        OfflinePlayer target = args.get("player");

        // Heavy database query (runs async)
        PlayerStats stats = database.getStats(target.getUniqueId());

        // Return to main thread for Bukkit API
        Bukkit.getScheduler().runTask(plugin, () -> {
            sender.sendMessage("§eStats for " + target.getName());
            sender.sendMessage("§7Kills: " + stats.getKills());
            sender.sendMessage("§7Deaths: " + stats.getDeaths());
        });
    })
    .register(plugin);
```

### Metrics & Analytics
```java
CommandMetrics metrics = CommandManager.getMetrics();

// Get stats
long executions = metrics.getExecutionCount("heal");
double avgTime = metrics.getAverageExecutionTime("heal");
int errors = metrics.getErrorCount("heal");

System.out.println("Heal command: " + executions + " uses, " + avgTime + "ms avg");
```

## 📚 Documentation

For complete documentation, advanced features, and best practices, visit:

### 🌐 [commands.auroradev.cc](https://commands.auroradev.cc)

**Topics covered:**
- Complete API reference
- All 22 argument types
- Middleware system guide
- Rate limiting configuration
- Transaction system usage
- Scripting integration
- A/B testing setup
- Performance optimization
- Cross-version compatibility
- Migration guides

## 🎯 Why Aurora Commands?

| Feature | Aurora Commands | Brigadier | ACF | Cloud |
|---------|----------------|-----------|-----|-------|
| Ease of Use | ✅ Excellent | ❌ Complex | ✅ Good | ✅ Good |
| Version Support | ✅ 1.8.8+ | ⚠️ 1.13+ | ✅ 1.8+ | ✅ 1.8+ |
| Async Support | ✅ Built-in | ❌ No | ❌ No | ⚠️ Limited |
| NLP | ✅ Yes | ❌ No | ❌ No | ❌ No |
| Transactions | ✅ Yes | ❌ No | ❌ No | ❌ No |
| Auto GUI | ✅ Yes | ❌ No | ❌ No | ❌ No |
| Pipelines | ✅ Yes | ❌ No | ❌ No | ❌ No |
| A/B Testing | ✅ Yes | ❌ No | ❌ No | ❌ No |
| Scripting | ✅ Yes | ❌ No | ❌ No | ❌ No |

## 🔧 Requirements

- **Java:** 8 or higher
- **Server:** Spigot 1.8.8 - 1.21.1 (Paper recommended)
- **Memory:** ~5MB overhead
- **Performance:** <1ms command parsing (typical 3 arguments)

## 📈 Performance

- **Zero GC pressure** from cooldown management
- **Thread-safe** concurrent data structures
- **Intelligent caching** for tab completions
- **Lazy initialization** for optional features
- **Sub-millisecond** command resolution

## 🤝 Contributing

Contributions are welcome! Please read our [Contributing Guidelines](CONTRIBUTING.md) first.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🌟 Support

- **Documentation:** [commands.auroradev.cc](https://commands.auroradev.cc)
- **Discord:** [Join our community](#) (Coming soon)
- **Issues:** [GitHub Issues](../../issues)
- **Email:** support@auroradev.cc

## 🙏 Acknowledgments

Built with ❤️ for the Minecraft plugin development community.

Special thanks to all contributors and users who help make Aurora Commands better every day.

---

<div align="center">

**⭐ Star this repository if Aurora Commands helps your project! ⭐**

Made with ☕ by the Aurora Team

</div>
