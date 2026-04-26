package dev.tamago0314.hikariTweaks.restock;

import dev.tamago0314.hikariTweaks.config.TweaksOptions;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AutoRestockHotbarHandler {
    private static Screen lastProcessedScreen;

    private AutoRestockHotbarHandler() {}

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            lastProcessedScreen = null;
            return;
        }

        if (!TweaksOptions.AUTO_RESTOCK_HOTBAR.getBooleanValue()) {
            lastProcessedScreen = null;
            return;
        }

        Screen screen = client.currentScreen;
        if (!(screen instanceof HandledScreen<?> handledScreen)) {
            lastProcessedScreen = null;
            return;
        }

        if (screen == lastProcessedScreen) {
            return;
        }

        if (screen instanceof InventoryScreen || handledScreen.getScreenHandler() instanceof PlayerScreenHandler) {
            lastProcessedScreen = screen;
            return;
        }

        process(client, handledScreen);
        lastProcessedScreen = screen;
    }

    private static void process(MinecraftClient client, HandledScreen<?> handledScreen) {
        if (shouldSkipForEnderChest(client)) {
            return;
        }

        ScreenHandler handler = handledScreen.getScreenHandler();
        List<Slot> containerSlots = new ArrayList<>();
        List<Slot> hotbarSlots = new ArrayList<>();

        for (Slot slot : handler.slots) {
            if (slot.inventory instanceof PlayerInventory && slot.getIndex() >= 0 && slot.getIndex() < 9) {
                hotbarSlots.add(slot);
            } else if (!(slot.inventory instanceof PlayerInventory)) {
                containerSlots.add(slot);
            }
        }

        if (containerSlots.isEmpty() || hotbarSlots.isEmpty()) {
            return;
        }

        Map<String, Integer> movedAmounts = new LinkedHashMap<>();
        for (Slot hotbarSlot : hotbarSlots) {
            ItemStack hotbarStack = hotbarSlot.getStack();
            if (hotbarStack.isEmpty() || hotbarStack.getCount() >= hotbarStack.getMaxCount()) {
                continue;
            }

            String itemId = Registry.ITEM.getId(hotbarStack.getItem()).toString();
            if (!TweaksOptions.HOTBAR_RESTOCK_LIST.getStrings().contains(itemId)) {
                continue;
            }

            int moved = restockSlot(client, handledScreen, hotbarSlot, containerSlots);
            if (moved <= 0) {
                continue;
            }

            String displayName = hotbarStack.getName().copy().formatted(getItemFormatting(hotbarStack)).getString();
            movedAmounts.merge(displayName, moved, Integer::sum);
        }

        if (!movedAmounts.isEmpty()) {
            client.player.sendMessage(Text.of(buildActionbarMessage(movedAmounts)), true);
            client.player.closeHandledScreen();
        }
    }

    private static int restockSlot(MinecraftClient client, HandledScreen<?> handledScreen, Slot hotbarSlot, List<Slot> containerSlots) {
        int movedTotal = 0;

        for (int i = containerSlots.size() - 1; i >= 0; i--) {
            Slot containerSlot = containerSlots.get(i);
            ItemStack containerStack = containerSlot.getStack().copy();
            ItemStack hotbarStack = hotbarSlot.getStack().copy();

            if (containerStack.isEmpty() || hotbarStack.isEmpty()) {
                continue;
            }

            if (!ItemStack.canCombine(containerStack, hotbarStack)) {
                continue;
            }

            int remaining = hotbarStack.getMaxCount() - hotbarStack.getCount();
            if (remaining <= 0) {
                break;
            }

            int moveAmount = Math.min(remaining, containerStack.getCount());
            moveItems(client, handledScreen, containerSlot, hotbarSlot, moveAmount);
            movedTotal += moveAmount;
        }

        return movedTotal;
    }

    private static void moveItems(MinecraftClient client, HandledScreen<?> handledScreen, Slot containerSlot, Slot hotbarSlot, int moveAmount) {
        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        if (interactionManager == null || client.player == null || moveAmount <= 0) {
            return;
        }

        int syncId = handledScreen.getScreenHandler().syncId;
        interactionManager.clickSlot(syncId, containerSlot.id, 0, SlotActionType.PICKUP, client.player);
        for (int i = 0; i < moveAmount; i++) {
            interactionManager.clickSlot(syncId, hotbarSlot.id, 1, SlotActionType.PICKUP, client.player);
        }
        interactionManager.clickSlot(syncId, containerSlot.id, 0, SlotActionType.PICKUP, client.player);
    }

    private static boolean shouldSkipForEnderChest(MinecraftClient client) {
        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof BlockHitResult blockHitResult)) {
            return false;
        }

        BlockEntity blockEntity = client.world.getBlockEntity(blockHitResult.getBlockPos());
        return blockEntity instanceof EnderChestBlockEntity;
    }

    private static Formatting getItemFormatting(ItemStack stack) {
        return stack.getRarity().formatting;
    }

    private static String buildActionbarMessage(Map<String, Integer> movedAmounts) {
        List<String> contents = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : movedAmounts.entrySet()) {
            contents.add(entry.getKey() + " +" + entry.getValue());
        }
        return "ホットバー自動補充: " + String.join(", ", contents);
    }
}
