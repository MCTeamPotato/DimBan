# DimBan
This is a very simple server-side mod that allows you to ban dimensions by config.

## Config

See config/dimban-toml. Support in-game reloading.

```toml
[DimBan]
DimensionsToBan = ["minecraft:the_nether"]
#Where will the player go if their current dimension is banned.
WhereToGo = "minecraft:overworld"
```

## Details
- Player will be teleported to WhereToGo dim when they log in in the banned dimensions.
- Banned dimensions will stop to tick.
- If there are players in the banned dimensions when they're ticking, they will also be teleported to WhereToGo dim before we cancel the tick.