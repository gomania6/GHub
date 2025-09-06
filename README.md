Конечно! Вот переведённая на английский версия вашего README/Modrinth описания:

---

# GHub — Hub Utilities Plugin for Minecraft

**Version:** 1.0
**Compatible with:** Paper 1.21.8+
**Dependencies:** PlaceholderAPI (optional)

GHub is a minimalist yet powerful plugin for managing spawn points, hub items, and player teleportation on a Minecraft server. It allows you to easily set up spawn, give special items, and supports placeholders via PlaceholderAPI.

---

## 🌟 Main Features

### 1. Spawn Setup

* Players are automatically teleported to spawn when joining the server.
* Supports spawn messages (`welcome-spawn`) with Minecraft colors.

**Example command:**

```
/ghub spawn
```

---

### 2. Hub Item

* A configurable item given to players on join.
* Supports:

  * Material (e.g., COMPASS)
  * Name and lore
  * Action on use (`console` or `bungee`)
* Players cannot drop the item.

**Example `config.yml` setup:**

```yaml
item:
  material: COMPASS
  slot: 4
  name: "&6Server Selector"
  lore:
    - "&7Click to choose a server"
  function: "[console] deluxemenus open selector %player_name%"
```

---

### 3. Item Functions

1. **Console Commands**

   * Executed as console commands.
   * Supports placeholders (e.g., `%player_name%`).
   * If run from console without a player — a warning is shown.

2. **BungeeCord Teleport**

   * Teleports the player to another server via BungeeCord.
   * Example: `[bungee] lobby`

---

### 4. Void Teleportation

* Players who fall below a set height (`VOID_HEIGHT = -60`) are automatically teleported to spawn.
* Cooldown for repeated teleportation: 3 seconds.

---

### 5. PlaceholderAPI Support

* All standard PlaceholderAPI placeholders work in player messages.

**Example:**

```yaml
messages:
  welcome-spawn: "Welcome, %player_name%!"
```

* Console commands that rely on player placeholders are disabled to avoid errors.

---

### 6. Plugin Commands

| Command        | Description                 | Permission    |
| -------------- | --------------------------- | ------------- |
| `/ghub reload` | Reload plugin configuration | `ghub.reload` |
| `/ghub spawn`  | Set the spawn point         | `ghub.spawn`  |

---

### 7. Configuration

* All settings are in `config.yml` and `spawn.yml`.
* Configurable options include:

  * Material, slot, and name of hub item
  * Player messages
  * Item functions

**Example `config.yml`:**

```yaml
item:
  material: COMPASS
  slot: 4
  name: "&6Server Selector"
  lore:
    - "&7Click to choose a server"
  function: "[console] deluxemenus open selector %player_name%"

messages:
  welcome-spawn: "Welcome, %player_name%!"
  cannot-drop: "&cYou cannot drop this item!"
  reload-success: "&aConfiguration reloaded successfully!"
  no-permission: "&cYou do not have permission to use this command!"
  usage: "&eUsage: /ghub [reload|spawn]"
```

---

### 8. Safety & Convenience

* Hub item cannot be dropped or moved.
* Cooldowns prevent teleport spam when falling into the void.
* Messages and commands safely handle missing players in console.

---

### 9. Installation

1. Download the latest `GHub.jar` plugin.
2. Place the file in your Paper server's `plugins` folder.
3. Restart the server to generate `config.yml` and `spawn.yml`.
4. Configure `config.yml` and set the spawn point using `/ghub spawn`.
5. Done! Players will now receive the hub item on join.

---

If you want, I can also polish it to be **even more Modrinth/GitHub friendly** with badges, headings, and concise examples. Do you want me to do that?
