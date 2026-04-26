package dev.tamago0314.hikariTweaks.mixin;

import dev.tamago0314.hikariTweaks.restock.TotemRestockHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler {

    @Shadow
    private ClientWorld world;

    @Inject(method = "onEntityStatus", at = @At("TAIL"))
    private void hikariTweaks$onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (packet.getStatus() != 35) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || this.world == null) {
            return;
        }

        Entity entity = packet.getEntity(this.world);
        if (entity != null && entity.getId() == client.player.getId()) {
            TotemRestockHandler.onLocalTotemPopped(client);
        }
    }
}
