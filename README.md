# Tidings Vanilla

A [Tidings](https://github.com/tidingsmc/tidingsmc) add-on that adds a **Stats** tab using what vanilla Minecraft already records:

- **Your stats**: deaths, play time, distance walked and travelled, mob and player kills, and advancements done.
- **Leaderboards** by deaths, play time, distance walked, mob kills or advancements.
- **Advancements**, one category at a time with the date each was made.
- **Notifications** when someone dies or completes a challenge. Tapping one opens that player's stats.

It is the reference example for [writing an add-on](https://github.com/tidingsmc/tidingsmc/blob/main/docs/addons.md).

## Server setup

Put the jar in `mods/` next to Tidings and restart the server. Players don't need to install anything.

It follows the server's own rules and players' choices:

- Players hidden from phones with `/tidings hide` are left out of leaderboards, and other players can't open their stats or advancements. Nobody is notified about them. They still see their own stats.
- With the `showDeathMessages` game rule off, deaths aren't notified. With `announceAdvancements` off, challenges aren't either.

Leaderboards include everyone who has played on the world, even before the mod was installed. On startup the mod reads everyone's saved stats on a background thread, and the leaderboard says so while it's still reading.

## Development

Requirements: a JDK 17+ (Gradle downloads Java 17 for Forge automatically).

The mod compiles against the Tidings jar without bundling it. Download `tidings-forge-1.20.1-<version>.jar` from the [Tidings releases](https://github.com/tidingsmc/tidingsmc/releases) into `libs/`, and set `tidings_version` in `gradle.properties` to its version.

```sh
./gradlew build        # mod jar in build/libs/
./gradlew test         # unit tests
./gradlew runServer    # dev server with Tidings and this mod (accept the EULA in run/eula.txt,
                       # and set app-url in run/config/tidings-common.toml)
```

## License

AGPL-3.0-or-later. See [LICENSE](LICENSE).
