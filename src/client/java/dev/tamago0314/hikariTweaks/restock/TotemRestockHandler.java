package dev.tamago0314.hikariTweaks.restock;

import dev.tamago0314.hikariTweaks.config.TweaksOptions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

public final class TotemRestockHandler {
    private static final int OFFHAND_SLOT = 40;
    private static final int NO_PENDING_SLOT = -1;
    private static final int MAX_PENDING_RETRIES = 100;

    private static boolean snapshotMainHandHadTotem;
    private static boolean snapshotOffHandHadTotem;
    private static int snapshotSelectedHotbarSlot;

    private static int pendingTargetInventorySlot = NO_PENDING_SLOT;
    private static int pendingRetries;
    private static int cooldownTicks;

    private TotemRestockHandler() {}

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            reset();
            return;
        }

        if (!TweaksOptions.TOTEM_RESTOCK.getBooleanValue()) {
            refreshSnapshot(player);
            clearPending();
            cooldownTicks = 0;
            return;
        }

        if (pendingTargetInventorySlot != NO_PENDING_SLOT) {
            if (cooldownTicks > 0) {
                cooldownTicks--;
            } else {
                if (tryRestockToSlot(client, pendingTargetInventorySlot)) {
                    cooldownTicks = 5;
                    clearPending();
                } else if (--pendingRetries <= 0) {
                    clearPending();
                }
            }
        }

        refreshSnapshot(player);
    }

    public static void onLocalTotemPopped(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return;
        }
        if (!TweaksOptions.TOTEM_RESTOCK.getBooleanValue()) {
            return;
        }

        int targetSlot = resolveTargetSlot(player);
        if (targetSlot == NO_PENDING_SLOT) {
            return;
        }

        pendingTargetInventorySlot = targetSlot;
        pendingRetries = MAX_PENDING_RETRIES;
        // Wait a couple of ticks for inventory sync after totem-pop status packet.
        cooldownTicks = 2;
    }

    private static boolean tryRestockToSlot(MinecraftClient client, int targetInventorySlot) {
        ClientPlayerEntity player = client.player;
        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        if (player == null || interactionManager == null) {
            return false;
        }

        if (!(player.currentScreenHandler instanceof PlayerScreenHandler)) {
            return false;
        }

        if (isTotem(getInventoryStack(player.getInventory(), targetInventorySlot))) {
            // Totem may still appear here before server inventory sync arrives.
            // Keep pending and retry instead of completing early.
            return false;
        }

        int sourceInventorySlot = findTotemSlot(player.getInventory(), targetInventorySlot);
        if (sourceInventorySlot < 0) {
            return false;
        }

        int sourceSlotId = toScreenHandlerSlotId(sourceInventorySlot);
        int targetSlotId = toScreenHandlerSlotId(targetInventorySlot);
        if (sourceSlotId < 0 || targetSlotId < 0) {
            return false;
        }

        int syncId = player.currentScreenHandler.syncId;
        interactionManager.clickSlot(syncId, sourceSlotId, 0, SlotActionType.PICKUP, player);
        interactionManager.clickSlot(syncId, targetSlotId, 0, SlotActionType.PICKUP, player);
        interactionManager.clickSlot(syncId, sourceSlotId, 0, SlotActionType.PICKUP, player);
        return true;
    }

    private static int resolveTargetSlot(ClientPlayerEntity player) {
        boolean hadMain = snapshotMainHandHadTotem;
        boolean hadOff = snapshotOffHandHadTotem;

        if (!hadMain && !hadOff) {
            return NO_PENDING_SLOT;
        }

        boolean currentMainHasTotem = isTotem(player.getMainHandStack());
        boolean currentOffHasTotem = isTotem(player.getOffHandStack());

        if (hadOff && !currentOffHasTotem && (!hadMain || currentMainHasTotem)) {
            return OFFHAND_SLOT;
        }

        if (hadMain && !currentMainHasTotem && (!hadOff || currentOffHasTotem)) {
            return snapshotSelectedHotbarSlot;
        }

        if (hadOff && !hadMain) {
            return OFFHAND_SLOT;
        }

        if (hadMain) {
            return snapshotSelectedHotbarSlot;
        }

        return NO_PENDING_SLOT;
    }

    private static int findTotemSlot(PlayerInventory inventory, int excludedSlot) {
        for (int i = 9; i < 36; i++) {
            if (i != excludedSlot && isTotem(inventory.getStack(i))) {
                return i;
            }
        }

        for (int i = 0; i < 9; i++) {
            if (i != excludedSlot && isTotem(inventory.getStack(i))) {
                return i;
            }
        }

        return -1;
    }

    private static ItemStack getInventoryStack(PlayerInventory inventory, int inventorySlot) {
        if (inventorySlot >= 0 && inventorySlot < inventory.main.size()) {
            return inventory.getStack(inventorySlot);
        }

        if (inventorySlot == OFFHAND_SLOT) {
            return inventory.offHand.get(0);
        }

        return ItemStack.EMPTY;
    }

    private static void refreshSnapshot(ClientPlayerEntity player) {
        snapshotMainHandHadTotem = isTotem(player.getMainHandStack());
        snapshotOffHandHadTotem = isTotem(player.getOffHandStack());
        snapshotSelectedHotbarSlot = player.getInventory().selectedSlot;
    }

    private static void clearPending() {
        pendingTargetInventorySlot = NO_PENDING_SLOT;
        pendingRetries = 0;
    }

    private static int toScreenHandlerSlotId(int inventorySlot) {
        if (inventorySlot >= 9 && inventorySlot <= 35) {
            return inventorySlot;
        }

        if (inventorySlot >= 0 && inventorySlot <= 8) {
            return 36 + inventorySlot;
        }

        if (inventorySlot == OFFHAND_SLOT) {
            return 45;
        }

        return -1;
    }

    private static boolean isTotem(ItemStack stack) {
        Item item = stack.getItem();
        return !stack.isEmpty() && item == Items.TOTEM_OF_UNDYING;
    }

    private static void reset() {
        snapshotMainHandHadTotem = false;
        snapshotOffHandHadTotem = false;
        snapshotSelectedHotbarSlot = 0;
        clearPending();
        cooldownTicks = 0;
    }
}
