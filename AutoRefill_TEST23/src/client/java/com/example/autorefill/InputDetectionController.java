package com.example.autorefill;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class InputDetectionController {
    private static final int HOTBAR_SIZE = 9;

    private static ItemStack lastOffhandStack = ItemStack.EMPTY;
    private static final ItemStack[] lastHotbarStacks = new ItemStack[HOTBAR_SIZE];
    private static long lastSignalTick = Long.MIN_VALUE;
    private static long cooldownUntilTick = Long.MIN_VALUE;

    static {
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            lastHotbarStacks[i] = ItemStack.EMPTY;
        }
    }

    private InputDetectionController() {
    }

    public static void onPlayerTick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            resetSnapshots();
            return;
        }

        if (client.screen != null || player.isCreative() || !player.isAlive()) {
            snapshotCurrent(player);
            return;
        }

        ItemStack currentOffhand = player.getItemInHand(InteractionHand.OFF_HAND).copy();
        detectOffhandTransition(client, player, lastOffhandStack, currentOffhand);
        lastOffhandStack = currentOffhand;

        for (int hotbarIndex = 0; hotbarIndex < HOTBAR_SIZE; hotbarIndex++) {
            ItemStack currentHotbar = player.getInventory().getItem(hotbarIndex).copy();
            detectHotbarTransition(client, player, hotbarIndex, lastHotbarStacks[hotbarIndex], currentHotbar);
            lastHotbarStacks[hotbarIndex] = currentHotbar;
        }
    }

    private static void detectOffhandTransition(Minecraft client, LocalPlayer player, ItemStack previous, ItemStack current) {
        if (previous.isEmpty() || !current.isEmpty()) {
            return;
        }

        if (!beginRefillWindow(client)) {
            return;
        }

        String itemName = previous.getHoverName().getString();
        SlotMatch match = findMatchingInventorySlot(player.inventoryMenu, previous, 45);
        if (match == null) {
            notify(client, "[TEST23] OFFHAND EMPTY / NO CANDIDATE <= " + itemName, false);
            AutoRefillClient.LOGGER.info("[TEST23] no candidate found for offhand item='{}'", itemName);
            return;
        }

        AutoRefillClient.LOGGER.info(
                "[TEST23] offhand candidate found item='{}' menuSlot={} inventoryIndex={} stackCount={}",
                itemName,
                match.menuSlot,
                match.inventoryIndex,
                match.stack.getCount()
        );

        MoveResult result = tryMoveStackToSlot(client, player, match, 45, 40, "OFFHAND");
        ItemStack after = player.getItemInHand(InteractionHand.OFF_HAND).copy();
        boolean sameItemInOffhand = !after.isEmpty() && ItemStack.isSameItemSameComponents(after, previous);

        AutoRefillClient.LOGGER.info(
                "[TEST23] offhand result item='{}' success={} offhandNow='{}' carriedCount={} note={}",
                itemName,
                result.success,
                after.isEmpty() ? "<empty>" : after.getHoverName().getString() + " x" + after.getCount(),
                player.inventoryMenu.getCarried().getCount(),
                result.note
        );

        if (result.success && sameItemInOffhand) {
            notify(client, "[TEST23] OFFHAND REFILL SUCCESS <= " + itemName + " [menu=" + match.menuSlot + "/inv=" + match.inventoryIndex + "]", true);
        } else {
            notify(client, "[TEST23] OFFHAND REFILL ATTEMPTED BUT FAILED <= " + itemName + " [menu=" + match.menuSlot + "/inv=" + match.inventoryIndex + "]", false);
        }
    }

    private static void detectHotbarTransition(Minecraft client, LocalPlayer player, int hotbarIndex, ItemStack previous, ItemStack current) {
        if (previous.isEmpty() || !current.isEmpty()) {
            return;
        }

        if (!beginRefillWindow(client)) {
            return;
        }

        String itemName = previous.getHoverName().getString();
        int targetMenuSlot = toMenuSlot(hotbarIndex);
        SlotMatch match = findMatchingInventorySlot(player.inventoryMenu, previous, targetMenuSlot);
        if (match == null) {
            notify(client, "[TEST23] HOTBAR " + (hotbarIndex + 1) + " EMPTY / NO CANDIDATE <= " + itemName, false);
            AutoRefillClient.LOGGER.info("[TEST23] no candidate found for hotbarIndex={} item='{}'", hotbarIndex, itemName);
            return;
        }

        AutoRefillClient.LOGGER.info(
                "[TEST23] hotbar candidate found slot={} item='{}' menuSlot={} inventoryIndex={} stackCount={}",
                hotbarIndex,
                itemName,
                match.menuSlot,
                match.inventoryIndex,
                match.stack.getCount()
        );

        MoveResult result = tryMoveStackToSlot(client, player, match, targetMenuSlot, hotbarIndex, "HOTBAR" + hotbarIndex);
        ItemStack after = player.getInventory().getItem(hotbarIndex).copy();
        boolean sameItemInHotbar = !after.isEmpty() && ItemStack.isSameItemSameComponents(after, previous);

        AutoRefillClient.LOGGER.info(
                "[TEST23] hotbar result slot={} item='{}' success={} slotNow='{}' carriedCount={} note={}",
                hotbarIndex,
                itemName,
                result.success,
                after.isEmpty() ? "<empty>" : after.getHoverName().getString() + " x" + after.getCount(),
                player.inventoryMenu.getCarried().getCount(),
                result.note
        );

        if (result.success && sameItemInHotbar) {
            notify(client, "[TEST23] HOTBAR " + (hotbarIndex + 1) + " REFILL SUCCESS <= " + itemName + " [menu=" + match.menuSlot + "/inv=" + match.inventoryIndex + "]", true);
        } else {
            notify(client, "[TEST23] HOTBAR " + (hotbarIndex + 1) + " REFILL ATTEMPTED BUT FAILED <= " + itemName + " [menu=" + match.menuSlot + "/inv=" + match.inventoryIndex + "]", false);
        }
    }

    private static boolean beginRefillWindow(Minecraft client) {
        long gameTime = client.level != null ? client.level.getGameTime() : 0L;
        if (gameTime == lastSignalTick || gameTime < cooldownUntilTick) {
            return false;
        }
        lastSignalTick = gameTime;
        cooldownUntilTick = gameTime + 6L;
        return true;
    }

    private static SlotMatch findMatchingInventorySlot(InventoryMenu menu, ItemStack target, int targetMenuSlotToSkip) {
        for (int menuSlot = 0; menuSlot < menu.slots.size(); menuSlot++) {
            Slot slot = menu.slots.get(menuSlot);
            if (!(slot.container instanceof net.minecraft.world.entity.player.Inventory)) {
                continue;
            }
            if (slot.index == 40 || menuSlot == 45 || menuSlot == targetMenuSlotToSkip) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            if (!ItemStack.isSameItemSameComponents(stack, target)) {
                continue;
            }
            return new SlotMatch(menuSlot, slot.index, stack.copy());
        }
        return null;
    }

    private static MoveResult tryMoveStackToSlot(Minecraft client, LocalPlayer player, SlotMatch match, int targetMenuSlot, int swapButton, String label) {
        MultiPlayerGameMode gameMode = client.gameMode;
        if (gameMode == null) {
            return new MoveResult(false, "gameMode=null");
        }

        int containerId = player.inventoryMenu.containerId;
        int[] attempts = buildAttemptSlots(match);

        try {
            boolean screenOpen = client.screen != null;
            AutoRefillClient.LOGGER.info(
                    "[TEST23] refill start label={} item='{}' screenOpen={} containerId={} targetMenuSlot={} currentTarget={} carriedBefore={}",
                    label,
                    match.stack.getHoverName().getString(),
                    screenOpen,
                    containerId,
                    targetMenuSlot,
                    describe(player.inventoryMenu.getSlot(targetMenuSlot).getItem()),
                    describe(player.inventoryMenu.getCarried())
            );

            for (int sourceMenuSlot : attempts) {
                if (sourceMenuSlot < 0 || sourceMenuSlot >= player.inventoryMenu.slots.size() || sourceMenuSlot == targetMenuSlot) {
                    continue;
                }

                AutoRefillClient.LOGGER.info(
                        "[TEST23] strategy=pickup label={} sourceMenuSlot={} targetMenuSlot={} carriedBefore={} targetBefore={}",
                        label,
                        sourceMenuSlot,
                        targetMenuSlot,
                        describe(player.inventoryMenu.getCarried()),
                        describe(player.inventoryMenu.getSlot(targetMenuSlot).getItem())
                );

                gameMode.handleContainerInput(containerId, sourceMenuSlot, 0, ContainerInput.PICKUP, player);
                ItemStack carriedAfterPickup = player.inventoryMenu.getCarried().copy();
                AutoRefillClient.LOGGER.info(
                        "[TEST23] after pickup label={} sourceMenuSlot={} carried={} sourceNow={}",
                        label,
                        sourceMenuSlot,
                        describe(carriedAfterPickup),
                        describe(player.inventoryMenu.getSlot(sourceMenuSlot).getItem())
                );

                if (!carriedAfterPickup.isEmpty()) {
                    gameMode.handleContainerInput(containerId, targetMenuSlot, 0, ContainerInput.PICKUP, player);
                    ItemStack targetAfterPlace = player.inventoryMenu.getSlot(targetMenuSlot).getItem().copy();
                    ItemStack carriedAfterPlace = player.inventoryMenu.getCarried().copy();
                    AutoRefillClient.LOGGER.info(
                            "[TEST23] after pickup-place label={} sourceMenuSlot={} targetNow={} carried={}",
                            label,
                            sourceMenuSlot,
                            describe(targetAfterPlace),
                            describe(carriedAfterPlace)
                    );

                    if (!player.inventoryMenu.getCarried().isEmpty()) {
                        gameMode.handleContainerInput(containerId, sourceMenuSlot, 0, ContainerInput.PICKUP, player);
                        AutoRefillClient.LOGGER.info(
                                "[TEST23] returned carried stack label={} sourceMenuSlot={} carriedNow={}",
                                label,
                                sourceMenuSlot,
                                describe(player.inventoryMenu.getCarried())
                        );
                    }

                    if (!targetAfterPlace.isEmpty() && ItemStack.isSameItemSameComponents(targetAfterPlace, match.stack)) {
                        return new MoveResult(true, "pickup sequence success sourceMenuSlot=" + sourceMenuSlot);
                    }
                }

                AutoRefillClient.LOGGER.info(
                        "[TEST23] strategy=swap label={} sourceMenuSlot={} swapButton={} targetBefore={} sourceBefore={}",
                        label,
                        sourceMenuSlot,
                        swapButton,
                        describe(player.inventoryMenu.getSlot(targetMenuSlot).getItem()),
                        describe(player.inventoryMenu.getSlot(sourceMenuSlot).getItem())
                );

                gameMode.handleContainerInput(containerId, sourceMenuSlot, swapButton, ContainerInput.SWAP, player);
                ItemStack targetAfterSwap = player.inventoryMenu.getSlot(targetMenuSlot).getItem().copy();
                ItemStack sourceAfterSwap = player.inventoryMenu.getSlot(sourceMenuSlot).getItem().copy();
                AutoRefillClient.LOGGER.info(
                        "[TEST23] after swap label={} sourceMenuSlot={} targetNow={} sourceNow={} carried={}",
                        label,
                        sourceMenuSlot,
                        describe(targetAfterSwap),
                        describe(sourceAfterSwap),
                        describe(player.inventoryMenu.getCarried())
                );

                if (!targetAfterSwap.isEmpty() && ItemStack.isSameItemSameComponents(targetAfterSwap, match.stack)) {
                    return new MoveResult(true, "swap sequence success sourceMenuSlot=" + sourceMenuSlot);
                }
            }

            return new MoveResult(false, "pickup+swap attempts exhausted");
        } catch (Exception ex) {
            AutoRefillClient.LOGGER.error("[TEST23] refill click sequence failed label={}", label, ex);
            return new MoveResult(false, "exception=" + ex.getClass().getSimpleName());
        }
    }

    private static int[] buildAttemptSlots(SlotMatch match) {
        int mappedMenuSlot = toMenuSlot(match.inventoryIndex);
        if (mappedMenuSlot == match.menuSlot) {
            return new int[] { match.menuSlot };
        }
        return new int[] { match.menuSlot, mappedMenuSlot };
    }

    private static int toMenuSlot(int inventoryIndex) {
        if (inventoryIndex >= 0 && inventoryIndex <= 8) {
            return 36 + inventoryIndex;
        }
        if (inventoryIndex >= 9 && inventoryIndex <= 35) {
            return inventoryIndex;
        }
        if (inventoryIndex == 40) {
            return 45;
        }
        return inventoryIndex;
    }

    private static void notify(Minecraft client, String message, boolean success) {
        if (client.gui != null) {
            client.gui.setOverlayMessage(Component.literal(message), false);
        }
        if (client.player != null) {
            client.player.playSound(success ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.NOTE_BLOCK_BASS.value(), 0.55F, success ? 1.10F : 0.90F);
        }

        AutoRefillClient.LOGGER.info("{}", message);
    }

    private static void snapshotCurrent(LocalPlayer player) {
        lastOffhandStack = player.getItemInHand(InteractionHand.OFF_HAND).copy();
        for (int hotbarIndex = 0; hotbarIndex < HOTBAR_SIZE; hotbarIndex++) {
            lastHotbarStacks[hotbarIndex] = player.getInventory().getItem(hotbarIndex).copy();
        }
    }

    private static void resetSnapshots() {
        lastOffhandStack = ItemStack.EMPTY;
        for (int hotbarIndex = 0; hotbarIndex < HOTBAR_SIZE; hotbarIndex++) {
            lastHotbarStacks[hotbarIndex] = ItemStack.EMPTY;
        }
        lastSignalTick = Long.MIN_VALUE;
        cooldownUntilTick = Long.MIN_VALUE;
    }

    private static String describe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "<empty>";
        }
        return stack.getHoverName().getString() + " x" + stack.getCount();
    }

    private record SlotMatch(int menuSlot, int inventoryIndex, ItemStack stack) {
    }

    private record MoveResult(boolean success, String note) {
    }
}
