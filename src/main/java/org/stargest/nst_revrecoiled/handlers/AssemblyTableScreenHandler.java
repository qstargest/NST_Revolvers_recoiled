package org.stargest.nst_revrecoiled.handlers;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import org.stargest.nst_revrecoiled.util.ModBlocks;
import org.stargest.nst_revrecoiled.util.ModScreenHandlers;

/**
 * Screen handler for the Assembly Table GUI.
 * Manages the 36 player inventory slots displayed inside the Assembly Table screen.
 *
 * Instantiated on both the server (when a player opens the block) and on the client
 * (by HandledScreens when the open-screen packet arrives).
 * Actual crafting logic is handled separately via AssemblyCraftC2SPacket —
 * this handler only owns slot registration, shift-click movement, and proximity validation.
 *
 * The ScreenHandlerContext carries the block's world position so canUse() can enforce
 * a block-proximity check on the server. The client-side constructor passes
 * ScreenHandlerContext.EMPTY, which skips the check — required by the HandledScreens
 * factory signature which only receives syncId and PlayerInventory.
 *
 * Slot layout:
 *   0–26  — main inventory (PlayerInventory.main indices 9–35)
 *   27–35 — hotbar (PlayerInventory.main indices 0–8)
 */
public class AssemblyTableScreenHandler extends ScreenHandler {

    private final PlayerInventory playerInventory;
    private final ScreenHandlerContext context;

    /**
     * Client-side constructor. Called by HandledScreens when the open-screen packet arrives.
     * Passes ScreenHandlerContext.EMPTY — proximity validation is server-only.
     *
     * @param syncId          synchronisation ID assigned by the server
     * @param playerInventory the inventory of the player who opened the screen
     */
    public AssemblyTableScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
    }

    /**
     * Full constructor used on the server when the player opens the block.
     * The context carries the block position so canUse() can enforce proximity.
     *
     * @param syncId          synchronisation ID assigned by the server
     * @param playerInventory the inventory of the player who opened the screen
     * @param ctx             block context created via ScreenHandlerContext.create(world, pos)
     */
    public AssemblyTableScreenHandler(int syncId, PlayerInventory playerInventory,
                                      ScreenHandlerContext ctx) {
        super(ModScreenHandlers.ASSEMBLY_TABLE_HANDLER, syncId);
        this.playerInventory = playerInventory;
        this.context = ctx;

        // Main inventory — rows 0–2, PlayerInventory.main indices 9–35
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

        // Hotbar — PlayerInventory.main indices 0–8
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
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasStack()) return ItemStack.EMPTY;

        ItemStack stack    = slot.getStack();
        ItemStack original = stack.copy();

        boolean fromMain   = slotIndex < 27;
        boolean fromHotbar = slotIndex >= 27;

        if (fromMain) {
            if (!insertItem(stack, 27, 36, false)) return ItemStack.EMPTY;
        } else {
            if (!insertItem(stack, 0, 27, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY);
        else slot.markDirty();

        return original;
    }

    /**
     * Returns true if the player is close enough to the Assembly Table block to keep
     * the screen open. Delegates to the vanilla distance check via ScreenHandlerContext.
     * Always returns true on the client (context is EMPTY there).
     */
    @Override
    public boolean canUse(PlayerEntity player) {
        return canUse(context, player, ModBlocks.ASSEMBLY_TABLE);
    }

    /** Returns the player inventory bound to this handler. */
    public PlayerInventory getPlayerInventory() {
        return playerInventory;
    }
}
