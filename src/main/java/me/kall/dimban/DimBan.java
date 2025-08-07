package me.kall.dimban;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import me.kall.dimban.api.IServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Mod(DimBan.MOD_ID)
public final class DimBan {
    public static final String MOD_ID = "dimban";
    public static final String MOD_NAME = "DimBan";

    public DimBan(@NotNull FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, CONFIG);
        context.getModEventBus().addListener(this::onConfigReload);
        MinecraftForge.EVENT_BUS.addListener(this::onChangeDim);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        MinecraftForge.EVENT_BUS.addListener(this::onJoin);
    }

    public void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level && ((IServerLevel) level).dimBan$isBlacklisted()) {
            updatePlayer(level.getServer(), player);
        }
    }

    public void onChangeDim(@NotNull EntityTravelToDimensionEvent event) {
        MinecraftServer server = event.getEntity().getServer();
        if (server != null) {
            ServerLevel level = server.getLevel(event.getDimension());
            if (level == null) return;
            if (((IServerLevel) level).dimBan$isBlacklisted()) event.setCanceled(true);
        }
    }

    public void onServerStarting(@NotNull ServerStartingEvent event) {
        event.getServer().getAllLevels().forEach(serverLevel -> ((IServerLevel) serverLevel).dimBan$setBlacklisted(DIMENSIONS.get().contains(serverLevel.dimension().location().toString())));
    }

    public void onConfigReload(ModConfigEvent.@NotNull Reloading event) {
        if (event.getConfig().getModId().equals(MOD_ID)) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.getAllLevels().forEach(serverLevel -> ((IServerLevel) serverLevel).dimBan$setBlacklisted(DIMENSIONS.get().contains(serverLevel.dimension().location().toString())));
            }
        }
    }

    public static final ForgeConfigSpec CONFIG;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DIMENSIONS;
    public static final ForgeConfigSpec.ConfigValue<? extends String> WHERE_TO_GO;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push(MOD_NAME);
        DIMENSIONS = builder.defineList("DimensionsToBan", Lists.newArrayList(), Predicates.alwaysTrue());
        WHERE_TO_GO = builder.comment("Where will the player go if their current dimension is banned.").define("WhereToGo", "minecraft:overworld");
        builder.pop();
        CONFIG = builder.build();
    }

    private static @Nullable BlockPos findSafeTeleportPos(ServerLevel level, @NotNull LevelChunk chunk) {
        final int startX = chunk.getPos().getMinBlockX();
        final int startZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = startX + dx;
                int z = startZ + dz;

                int topY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                for (int y = topY; y >= level.getMinBuildHeight(); y--) {
                    pos.set(x, y, z);
                    if (isSafeForPlayer(level, pos)) return pos.immutable();
                }
            }
        }

        return null;
    }

    private static boolean isSafeForPlayer(@NotNull LevelAccessor level, @NotNull BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        BlockState at = level.getBlockState(pos);
        BlockState above = level.getBlockState(pos.above());

        boolean solidGround = below.isCollisionShapeFullBlock(level, pos.below());
        boolean spaceClear = at.isAir() && above.isAir();

        return solidGround && spaceClear;
    }

    private static @NotNull ServerLevel findDestination(@NotNull MinecraftServer server) {
        ResourceLocation toGo = ResourceLocation.parse(DimBan.WHERE_TO_GO.get());
        ServerLevel destination = null;
        for (ServerLevel level : server.getAllLevels()) {
            if (!level.dimension().location().equals(toGo)) continue;
            destination = level;
            break;

        }
        if (destination == null) throw new NullPointerException("Invalid dimension: " + toGo);
        return destination;
    }

    private static BlockPos findSafePos(ServerLevel destination) {
        BlockPos safePos = null;
        int chunkX = 0;
        while (safePos == null) {
            safePos = DimBan.findSafeTeleportPos(destination, destination.getChunk(chunkX, 0));
            chunkX++;
        }
        return safePos;
    }

    public static void updatePlayer(@NotNull MinecraftServer server, @NotNull List<ServerPlayer> players) {
        ServerLevel destination = findDestination(server);
        BlockPos safePos = findSafePos(destination);
        for (ServerPlayer player : players) {
            player.teleportTo(destination, safePos.getX(), safePos.getY(), safePos.getZ(), player.getYRot(), player.getXRot());
        }
    }

    public static void updatePlayer(@NotNull MinecraftServer server, @NotNull ServerPlayer player) {
        ServerLevel destination = findDestination(server);
        BlockPos safePos = findSafePos(destination);
        player.teleportTo(destination, safePos.getX(), safePos.getY(), safePos.getZ(), player.getYRot(), player.getXRot());
    }
}
