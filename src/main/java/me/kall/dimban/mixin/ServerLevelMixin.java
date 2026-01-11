package me.kall.dimban.mixin;

import me.kall.dimban.DimBan;
import me.kall.dimban.api.IServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin implements IServerLevel {
    @Shadow @Final private MinecraftServer server;
    @Shadow @Final private List<ServerPlayer> players;
    @Unique private boolean dimBan$isBlacklistedDim;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        if (this.dimBan$isBlacklisted()) {
            if (!this.players.isEmpty()) DimBan.updatePlayer(this.server, this.players.stream().map(ServerPlayer::getUUID).collect(Collectors.toList()));
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
