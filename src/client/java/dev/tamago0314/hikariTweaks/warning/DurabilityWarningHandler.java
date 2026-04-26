package dev.tamago0314.hikariTweaks.warning;

import dev.tamago0314.hikariTweaks.config.TweaksOptions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;

import java.util.HashMap;
import java.util.Map;

/**
 * 耐久値 1% 警告ハンドラ。
 * Mixin を使わず ClientTickEvents から呼ぶことで refMap 問題を回避。
 * スロット+署名ごとに1回だけ警告を出す。
 */
public final class DurabilityWarningHandler {

    private static final Map<Integer, String> warnedSignatures = new HashMap<>();

    private DurabilityWarningHandler() {}

    public static void tick(MinecraftClient mc) {
        if (!TweaksOptions.DURABILITY_WARNING_ENABLED.getBooleanValue()) {
            warnedSignatures.clear();
            return;
        }
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null) return;

        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);

            if (stack.isEmpty() || !stack.isDamageable()) {
                warnedSignatures.remove(slot);
                continue;
            }

            int maxDamage = stack.getMaxDamage();
            int remaining  = maxDamage - stack.getDamage();
            int threshold  = Math.max(1, (int) Math.ceil(maxDamage * 0.01));

            if (remaining > threshold) {
                warnedSignatures.remove(slot);
                continue;
            }

            String sig = stack.getItem().toString() + "|" + stack.getDamage();
            if (sig.equals(warnedSignatures.get(slot))) continue;

            warnedSignatures.put(slot, sig);
            int percent = Math.max(0, (int) Math.ceil((remaining * 100.0) / maxDamage));
            player.sendMessage(
                    new LiteralText(
                            "\u00A7c[HikariTweaks]\u00A7f 耐久値警告: \u00A7e"
                                    + stack.getName().getString()
                                    + "\u00A7f 残り \u00A7c" + remaining
                                    + "\u00A7f (" + percent + "%)"
                    ),
                    false
            );
            // FIX⑤: ClientPlayerEntity.playSound() は MC 1.18.2 では SoundCategory 引数を取らない。
            //        world.playSound() を使ってプレイヤー位置でサウンドを再生する。
            mc.world.playSound(
                    player,
                    player.getBlockPos(),
                    SoundEvents.BLOCK_NOTE_BLOCK_PLING,
                    SoundCategory.MASTER,
                    1.0F,
                    1.2F
            );
        }
    }
}