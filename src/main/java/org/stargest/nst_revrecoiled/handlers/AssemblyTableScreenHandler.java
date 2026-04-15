package org.stargest.nst_revrecoiled.handlers;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.stargest.nst_revrecoiled.util.ModBlocks;
import org.stargest.nst_revrecoiled.util.ModScreenHandlers;

/**
 * Screen handler for the Assembly Table GUI.
 * Manages the 36 player inventory slots displayed inside the Assembly Table screen.
 *
 * Instantiated on both the server (when a player opens the block) and on the client
 * (by internal screen factories when the open-screen packet arrives).
 * Actual crafting logic is handled separately via AssemblyCraftC2SPacket —
 * this handler only owns slot registration, shift-click movement, and proximity validation.
 *
 * Slot layout:
 *   0–26  — main inventory (PlayerInventory.main indices 9–35)
 *   27–35 — hotbar (PlayerInventory.main indices 0–8)
 *
 * Adapted for Forge 1.20.1.
 */
public class AssemblyTableScreenHandler extends AbstractContainerMenu {

    private final Inventory playerInventory;
    private final ContainerLevelAccess context;

    /**
     * Client-side constructor. Called when the open-screen packet arrives.
     * Passes ContainerLevelAccess.NULL — proximity validation is server-only.
     *
     * @param syncId          synchronisation ID assigned by the server
     * @param playerInventory the inventory of the player who opened the screen
     */
    public AssemblyTableScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, ContainerLevelAccess.NULL);
    }

    /**
     * Full constructor used on the server when the player opens the block.
     * The context carries the block position so stillValid() can enforce proximity.
     *
     * @param syncId          synchronisation ID assigned by the server
     * @param playerInventory the inventory of the player who opened the screen
     * @param ctx             block context created via ContainerLevelAccess.create(world, pos)
     */
    public AssemblyTableScreenHandler(int syncId, Inventory playerInventory,
                                      ContainerLevelAccess ctx) {
        super(ModScreenHandlers.ASSEMBLY_TABLE_HANDLER.get(), syncId);
        this.playerInventory = playerInventory;
        this.context = ctx;

        // Main inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        8 + col * 18,
                        148 + row * 18
                ));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(
                    playerInventory,
                    col,
                    8 + col * 18,
                    206
            ));
        }
    }

    /**
     * Handles shift-click: moves a stack between main inventory and hotbar.
     * Slots 0–26 move into the hotbar (27–35); slots 27–35 move into the main inventory (0–26).
     *
     * @param player    the player performing the action
     * @param slotIndex index of the clicked slot
     * @return the original stack before the transfer, or ItemStack.EMPTY if nothing moved
     */
    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack    = slot.getItem();
        ItemStack original = stack.copy();

        boolean fromMain   = slotIndex < 27;

        if (fromMain) {
            if (!moveItemStackTo(stack, 27, 36, false)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 0, 27, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();

        return original;
    }

    /**
     * Returns true if the player is close enough to the Assembly Table block to keep
     * the screen open. Delegates to the vanilla distance check via ContainerLevelAccess.
     * Always returns true on the client (context is NULL there).
     */
    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(context, player, ModBlocks.ASSEMBLY_TABLE.get());
    }

    /** Returns the player inventory bound to this handler. */
    public Inventory getPlayerInventory() {
        return playerInventory;
    }
}
