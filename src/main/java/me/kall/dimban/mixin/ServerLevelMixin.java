package me.kall.dimban.mixin;

import me.kall.dimban.DimBan;
import me.kall.dimban.api.IServerLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Supplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level implements IServerLevel {
    @Shadow @Final private MinecraftServer server;
    @Shadow @Final List<ServerPlayer> players;
    @Unique private boolean dimBan$isBlacklistedDim;

    protected ServerLevelMixin(WritableLevelData levelData, ResourceKey<Level> dimension, RegistryAccess registryAccess, Holder<DimensionType> dimensionTypeRegistration, Supplier<ProfilerFiller> profiler, boolean isClientSide, boolean isDebug, long biomeZoomSeed, int maxChainedNeighborUpdates) {
        super(levelData, dimension, registryAccess, dimensionTypeRegistration, profiler, isClientSide, isDebug, biomeZoomSeed, maxChainedNeighborUpdates);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        if (this.dimBan$isBlacklisted()) {
            if (!this.players.isEmpty()) {
                DimBan.updatePlayer(this.server, this.players);
                this.players.clear();
            }
            ci.cancel();
        }
    }

    @Override
    public boolean dimBan$isBlacklisted() {
        return this.dimBan$isBlacklistedDim;
    }

    @Override
    public void dimBan$setBlacklisted(boolean blacklisted) {
        this.dimBan$isBlacklistedDim = blacklisted;
    }
}
