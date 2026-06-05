# Custom Daytime Plugin

[![Download on Modrinth](https://img.shields.io/badge/Modrinth-Download-brightgreen?style=for-the-badge&logo=modrinth)](https://modrinth.com/plugin/custom-daytime)
[![Latest Release](https://img.shields.io/github/v/release/SeedimV/CustomDaytime?logo=github&logoColor=white&style=for-the-badge)](https://github.com/SeedimV/CustomDaytime/releases/latest)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=for-the-badge)](https://github.com/SeedimV/CustomDaytime?tab=GPL-3.0-1-ov-file#readme)

Customize Minecraft's day/night cycle on **Folia 1.21.11** and experience smoother nights with this lightweight plugin.

> [!IMPORTANT]
> Since **v2.0.0**, the configuration file was changed from `config.yml` to `config.conf`.
> **Old `config.yml` files are not compatible and will not work.**
> The configuration format has also changed, so the old structure/syntax must be migrated manually.

## Requirements and support

- **Runtime:** Folia `1.21.11`.
- **Java:** Java `21` or newer.
- **Loader metadata:** the published Modrinth artifact is marked for the `folia` loader only.
- **Artifact:** `CustomDaytimeFolia-<version>.jar`.

Ordinary Paper is **not** a supported runtime target for this artifact. It may only work if the selected runtime provides the Folia API and scheduler behavior used by the plugin; otherwise, use Folia 1.21.11.

## Folia notes

Custom Daytime uses Folia scheduler contexts explicitly:

- world time, gamerules, weather-related state, and sleep-skip handling run on the global scheduler;
- player/sleeping counts are maintained as snapshots from player events instead of scanning entities from arbitrary region threads;
- async work, such as update checks, runs on the async scheduler.

Time configuration is applied **per world**. It does not create per-region day/night settings inside the same world; every region in a configured world follows that world's configured cycle.

## Features

- **Configurable Day & Night Lengths**
  Set the duration of Minecraft's day and night in **minutes** via the config. Decimal values are supported (for example, `0.5` for 30 seconds).

- **Accelerate Night While Sleeping**
  Instead of instantly skipping the night, time smoothly accelerates when enough players sleep. This feature can be disabled in config.

- **Per-world Config**
  Values are configured separately for each world. Copy the `"minecraft:overworld"` block, paste it at the end of the config, rename it to the target world key, and edit the values.

```hocon
"minecraft:overworld" {
  # Length of the daytime cycle in minutes
  # Default: 10.0
  dayLength=10.0

  # Length of the nighttime cycle in minutes
  # Default: 10.0
  nightLength=10.0

  # Enable or disable night acceleration when enough players sleep
  # false: use vanilla sleep-to-skip-night mechanic
  # Default: true
  accelerationEnabled=true

  # Multiplier for night acceleration during sleep
  # Example: nightLength=10.0 (= 600 seconds) and AccelerationMultiplier=100.0 -> acceleration lasts 6 seconds
  # Default: 100.0
  AccelerationMultiplier=100.0
}
```

## Development build

For Folia-layer work, Sponge can be skipped during Gradle configuration so SpongeVanilla does not block the Folia module build:

```bash
./gradlew --no-daemon -PskipSponge=true :folia:build
```

Useful checks:

```bash
./gradlew --no-daemon -PskipSponge=true :common:test
./gradlew --no-daemon -PskipSponge=true :folia:build
```

Use the full multi-module build without `-PskipSponge=true` only when Sponge configuration is required.

## License

This project is licensed under the [GNU General Public License v3.0](https://github.com/SeedimV/CustomDaytime/blob/master/LICENSE). You are free to use, modify, and distribute the project, but any derivative works must also be licensed under the same terms.
