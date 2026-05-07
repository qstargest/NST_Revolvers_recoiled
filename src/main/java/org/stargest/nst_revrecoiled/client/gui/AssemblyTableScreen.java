package org.stargest.nst_revrecoiled.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.stargest.nst_revrecoiled.Items.BaseBulletItem;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import org.stargest.nst_revrecoiled.handlers.AssemblyTableScreenHandler;
import org.stargest.nst_revrecoiled.network.AssemblyCraftC2SPacket;
import org.stargest.nst_revrecoiled.recipe.AssemblyRecipe;
import org.stargest.nst_revrecoiled.recipe.AssemblyRecipes;
import org.stargest.nst_revrecoiled.recipe.BulletAssemblyRecipe;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side GUI for the Assembly Table block.
 * Renders a custom crafting interface supporting two recipe categories:
 * revolvers and bullets.
 *
 * Layout:
 *   [ Revolver buttons | Scrollbar | Bullet buttons | Scrollbar | Detail panel ]
 *   [                   Player inventory (3 rows + hotbar)                      ]
 *
 * Craft requests are sent as AssemblyCraftC2SPacket packets. The server validates
 * and executes the craft; the resulting inventory update is synced back to the
 * client through the normal slot-sync mechanism.
 *
 * Key features:
 * - Independent scrollable lists for revolver and bullet recipes (up to 4 visible at once)
 * - Detail panel shows a rotating 3D GeckoLib model for revolvers and a spinning
 *   2D icon for bullets, with damage stat and ingredient counts
 * - Bullet detail includes quantity controls (−, text input, +) and a craft button
 *   that doubles as a pickaxe-durability indicator; the icon shows the weakest
 *   valid pickaxe for the tier when none is present in the inventory
 * - Per-frame inventory cache (refreshInventoryCache) avoids redundant ingredient
 *   counts, canCraft checks, and pickaxe searches during the render pass
 * - Recipe-change cache (onRecipeSelected) avoids rebuilding ingredient ItemStacks
 *   and scale factors every frame
 *
 * Key render and input methods are protected to allow addon mods to subclass this
 * screen and override recipe display, detail panels, or input handling for custom
 * recipe types registered via AssemblyRecipes.register().
 */
public class AssemblyTableScreen extends AbstractContainerScreen<AssemblyTableScreenHandler> {

    // ── Background dimensions ─────────────────────────────────────────────────
    private static final int BG_W = 176;
    private static final int BG_H = 230;

    // ── Recipe zone ───────────────────────────────────────────────────────────
    private static final int RECIPE_ZONE_TOP    = 14;
    private static final int RECIPE_ZONE_BOTTOM = 131;

    // ── Recipe selection buttons ──────────────────────────────────────────────
    private static final int BTN_X       = 6;
    private static final int BTN_SIZE    = 24;
    private static final int BTN_Y_START = 18;
    private static final int BTN_STEP    = 28;

    // ── Scrollbar ─────────────────────────────────────────────────────────────
    private static final int SCROLLBAR_W           = 6;
    private static final int SCROLL_ZONE_H         = RECIPE_ZONE_BOTTOM - BTN_Y_START; // 113
    private static final int MAX_VISIBLE_BTNS       = SCROLL_ZONE_H / BTN_STEP;         // 4
    private static final int SCROLLBAR_THUMB_MIN_H = 10;

    private static final int REV_SCROLLBAR_X    = BTN_X + BTN_SIZE;                  // 30
    private static final int CENTER_SEP_X       = REV_SCROLLBAR_X + SCROLLBAR_W + 1; // 37
    private static final int BULLET_ZONE_X      = CENTER_SEP_X + 2;                  // 39
    private static final int BULLET_ZONE_W      = BTN_SIZE;                          // 24
    private static final int BULLET_SCROLLBAR_X = BULLET_ZONE_X + BULLET_ZONE_W;     // 63

    // ── Detail panel ──────────────────────────────────────────────────────────
    private static final int DETAIL_X   = BULLET_SCROLLBAR_X + SCROLLBAR_W + 2;      // 71
    private static final int DETAIL_PAD = 4;

    private static final int   MODEL_SIZE        = 60;
    private static final int   MODEL_W           = 96;
    private static final float MODEL_SCALE       = 28f;

    private static final float FIXED_YAW          = 100f;
    private static final float FIXED_PITCH        =  10f;
    private static final long  ROTATION_PERIOD_MS = 3000L;

    // GeckoLib model pivot offsets — tuned to center the revolver in the preview box
    private static final float MODEL_PIVOT_X = 0;
    private static final float MODEL_PIVOT_Y = 0.6f;
    private static final float MODEL_PIVOT_Z = 0f;

    private static final int NAME_OFFSET  = 3;
    private static final int MODEL_OFFSET = 13;

    // ── Bullet preview ────────────────────────────────────────────────────────
    private static final int   BULLET_PREVIEW_SIZE = 48;
    private static final float BULLET_ICON_SCALE   = 2.5f;
    private static final int   BULLET_QTY_MAX      = 9999;

    // ── Inventory ─────────────────────────────────────────────────────────────
    private static final int INV_X      = 8;
    private static final int MAIN_INV_Y = 148;
    private static final int HOTBAR_Y   = 206;

    // ── Quantity control button bounds (static to avoid per-frame allocation) ─
    private static final int QTY_ROW_Y   = RECIPE_ZONE_BOTTOM - 17;             // 114
    private static final int QTY_MINUS_X = DETAIL_X + DETAIL_PAD;               // 75
    private static final int QTY_MINUS_W = 14;
    private static final int QTY_MINUS_H = 13;
    private static final int QTY_BOX_X   = QTY_MINUS_X + QTY_MINUS_W;          // 89
    private static final int QTY_BOX_W   = 36;
    private static final int QTY_PLUS_X  = QTY_BOX_X + QTY_BOX_W;              // 125
    private static final int QTY_PLUS_W  = 14;
    private static final int QTY_PICK_X  = QTY_PLUS_X + QTY_PLUS_W + 4;        // 143
    private static final int QTY_PICK_W  = 26;
    private static final int CRAFT_BTN_X = DETAIL_X + DETAIL_PAD;               // 75
    private static final int CRAFT_BTN_Y = RECIPE_ZONE_BOTTOM - 19;             // 112
    private static final int CRAFT_BTN_W = BG_W - (DETAIL_X + DETAIL_PAD) - 4; //  97
    private static final int CRAFT_BTN_H = 16;

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final int C_BG                  = 0xFFC6C6C6;
    private static final int C_LIGHT               = 0xFFFFFFFF;
    private static final int C_DARK                = 0xFF555555;
    private static final int C_SLOT_SHADOW         = 0xFF373737;
    private static final int C_SLOT_INNER          = 0xFF8B8B8B;
    private static final int C_SEPARATOR           = 0xFF888888;
    private static final int C_BTN_NORMAL          = 0xFFBBBBBB;
    private static final int C_BTN_HOVER           = 0xFFCCCCFF;
    private static final int C_BTN_SELECTED        = 0xFF9999EE;
    private static final int C_MODEL_BG            = 0xFF111111;
    private static final int C_MODEL_BORDER_NORMAL = 0xFF333333;
    private static final int C_TEXT                = 0xFF333333;
    private static final int C_TEXT_OK             = 0xFF226622;
    private static final int C_TEXT_MISSING        = 0xFFCC1111;
    private static final int C_CRAFT_READY         = 0xFF44AA44;
    private static final int C_CRAFT_HOVER         = 0xFF55CC55;
    private static final int C_CRAFT_OFF           = 0xFF888888;
    private static final int C_QTY_BTN             = 0xFFAAAAAA;
    private static final int C_QTY_BTN_HOVER       = 0xFFCCCCEE;
    private static final int C_PICK_READY          = 0xFF4488CC;
    private static final int C_PICK_HOVER          = 0xFF55AAEE;
    private static final int C_PICK_OFF            = 0xFF666666;
    private static final int C_TRACK_FILL          = 0xFF373737;
    private static final int C_THUMB_NORMAL        = 0xFFAAAAAA;
    private static final int C_THUMB_HOVER         = 0xFFCCCCCC;

    // ── State ─────────────────────────────────────────────────────────────────
    private List<AssemblyRecipe> recipes;
    private int selectedRecipe = -1;
    private int revolverCount  = 0;
    private int bulletCount    = 0;

    private int     bulletQuantity   = 1;
    private boolean editingQuantity  = false;
    private String  quantityInputStr = "";

    // ── Scroll offsets ────────────────────────────────────────────────────────
    private int revolverScroll = 0;
    private int bulletScroll   = 0;

    // ── Scrollbar drag state ──────────────────────────────────────────────────
    private boolean draggingRevScroll   = false;
    private boolean draggingBullScroll  = false;
    private float   dragScrollStartNorm = 0f;
    private int     dragMouseYStart     = 0;


    /** Recipe icon stacks, created once in init() and reused for all rendering. */
    private ItemStack[] recipeIconStacks;

    /**
     * Selected-recipe cache: ingredients, their ItemStacks, and ingredient row scale.
     * Rebuilt in onRecipeSelected(); not updated per frame.
     */
    private List<AssemblyRecipe.Ingredient> cachedIngredients;
    private List<ItemStack>                 cachedIngStacks;
    private float                           cachedIngScale;

    /**
     * Per-frame inventory cache. Rebuilt once per frame in refreshInventoryCache()
     * to avoid redundant inventory scans during the render pass.
     */
    private boolean   cachedCanCraft;
    private int[]     cachedIngCounts;
    private int       cachedPickaxeSlot;
    private net.minecraft.world.item.Item cachedDisplayPickaxe;
    private ItemStack cachedDisplayPickaxeStack;

    /** Frame timestamp in milliseconds, captured once per frame for animations and cursor blinking. */
    private long frameTimeMs;

    // ─────────────────────────────────────────────────────────────────────────

    public AssemblyTableScreen(AssemblyTableScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth  = BG_W;
        this.imageHeight = BG_H;
    }

    @Override
    protected void init() {
        super.init();
        recipes = AssemblyRecipes.getAll();
        revolverCount = 0;
        bulletCount   = 0;
        for (AssemblyRecipe r : recipes) {
            if (r instanceof BulletAssemblyRecipe) bulletCount++;
            else revolverCount++;
        }

        recipeIconStacks = new ItemStack[recipes.size()];
        for (int i = 0; i < recipes.size(); i++)
            recipeIconStacks[i] = new ItemStack(recipes.get(i).getResult());

        titleLabelY     = 4;
        inventoryLabelY = 134;
    }

    protected void playUiClick() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f)
        );
    }


    /**
     * Public wrapper to refresh the currently selected recipe's data.
     * Useful when recipes are updated via configuration sync while the screen is open.
     */
    public void refreshSelectedRecipe() {
        if (selectedRecipe >= 0) {
            onRecipeSelected(selectedRecipe);
        }
    }

    /**
     * Called whenever selectedRecipe changes.
     * Rebuilds recipe-dependent cache (ingredient list, ItemStacks, and row scale)
     * that does not depend on current inventory state.
     * Avoids rebuilding this data on every frame — only on actual selection changes.
     */
    protected void onRecipeSelected(int newIndex) {
        selectedRecipe = newIndex;
        if (newIndex >= 0) bulletQuantity = 1;

        if (newIndex < 0 || newIndex >= recipes.size()) {
            cachedIngredients = null;
            cachedIngStacks   = null;
            cachedIngScale    = 1f;
            return;
        }

        AssemblyRecipe recipe = recipes.get(newIndex);
        cachedIngredients = recipe.getIngredients();

        cachedIngStacks = new ArrayList<>(cachedIngredients.size());
        for (AssemblyRecipe.Ingredient ing : cachedIngredients)
            cachedIngStacks.add(new ItemStack(ing.item()));

        cachedIngScale = Math.min(1f, (float) MODEL_W / Math.max(1, cachedIngredients.size() * 48));

        // Reset inventory counts; recalculated on next refreshInventoryCache()
        cachedIngCounts = null;
    }

    /**
     * Rebuilds the per-frame inventory cache. Called exactly once at the start of
     * each frame, consolidating all calls to countInInventory, canCraft,
     * findBestPickaxeSlot, and getMaxCraftable into a single pass.
     * Results are stored in cachedCanCraft, cachedIngCounts, cachedPickaxeSlot,
     * and cachedDisplayPickaxeStack for use during the render pass.
     *
     * When no valid pickaxe is present, the GUI icon falls back to the weakest
     * pickaxe in the tier's valid list (PickaxeTier.getValidPickaxes().get(0)),
     * giving the player a visual hint of the minimum tool required.
     * The ItemStack is only recreated when the displayed pickaxe type actually changes.
     */
    protected void refreshInventoryCache() {
        if (selectedRecipe < 0 || selectedRecipe >= recipes.size()
                || cachedIngredients == null) return;

        AssemblyRecipe recipe = recipes.get(selectedRecipe);
        var inv = menu.getPlayerInventory();

        if (cachedIngCounts == null || cachedIngCounts.length != cachedIngredients.size())
            cachedIngCounts = new int[cachedIngredients.size()];
        for (int i = 0; i < cachedIngredients.size(); i++)
            cachedIngCounts[i] = recipe.countInInventory(inv, cachedIngredients.get(i).item());

        if (recipe instanceof BulletAssemblyRecipe br) {
            cachedCanCraft    = br.canCraftBullets(inv, bulletQuantity);
            cachedPickaxeSlot = br.findBestPickaxeSlot(inv);

            net.minecraft.world.item.Item newPickaxe = cachedPickaxeSlot >= 0
                    ? inv.items.get(cachedPickaxeSlot).getItem()
                    : br.getRequiredTier().getValidPickaxes().get(0);
            if (newPickaxe != cachedDisplayPickaxe) {
                cachedDisplayPickaxe      = newPickaxe;
                cachedDisplayPickaxeStack = new ItemStack(newPickaxe);
            }
        } else {
            cachedCanCraft    = recipe.canCraft(inv);
            cachedPickaxeSlot = -1;
        }
    }

    // =========================================================================
    // INPUT
    // =========================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int rx = (int) mouseX - this.leftPos;
            int ry = (int) mouseY - this.topPos;

            if (handleScrollbarClick(rx, ry, (int) mouseY)) return true;

            int revolverIdx = 0, bulletIdx = 0;
            for (int i = 0; i < recipes.size(); i++) {
                boolean isBullet = recipes.get(i) instanceof BulletAssemblyRecipe;
                int relIdx     = isBullet ? bulletIdx : revolverIdx;
                int scroll     = isBullet ? bulletScroll : revolverScroll;
                int virtualRow = relIdx - scroll;
                if (isBullet) bulletIdx++; else revolverIdx++;
                if (virtualRow < 0 || virtualRow >= MAX_VISIBLE_BTNS) continue;

                int bx = isBullet ? BULLET_ZONE_X : BTN_X;
                int by = BTN_Y_START + virtualRow * BTN_STEP;
                if (rx >= bx && rx < bx + BTN_SIZE && ry >= by && ry < by + BTN_SIZE) {
                    if (selectedRecipe != i) onRecipeSelected(i);
                    else onRecipeSelected(-1);
                    playUiClick();
                    return true;
                }
            }

            if (selectedRecipe >= 0 && selectedRecipe < recipes.size()) {
                AssemblyRecipe recipe = recipes.get(selectedRecipe);
                if (recipe instanceof BulletAssemblyRecipe br) {
                    if (handleBulletInputClick(rx, ry, br)) return true;
                } else {
                    if (rx >= CRAFT_BTN_X && rx < CRAFT_BTN_X + CRAFT_BTN_W
                            && ry >= CRAFT_BTN_Y && ry < CRAFT_BTN_Y + CRAFT_BTN_H) {
                        if (cachedCanCraft) {
                            PacketDistributor.sendToServer(
                                    new AssemblyCraftC2SPacket.Payload(
                                            recipes.get(selectedRecipe).getId().toString()));
                            playUiClick();
                        }
                        return true;
                    }
                }
            }

            if (editingQuantity && selectedRecipe >= 0 && selectedRecipe < recipes.size()
                    && recipes.get(selectedRecipe) instanceof BulletAssemblyRecipe br) {
                if (!(rx >= QTY_BOX_X && rx < QTY_BOX_X + QTY_BOX_W
                        && ry >= QTY_ROW_Y && ry < QTY_ROW_Y + QTY_MINUS_H))
                    confirmQuantityInput(br);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Handles a left-click on a scrollbar track or thumb.
     * Clicking the thumb starts a drag; clicking the track jumps the scroll position.
     */
    protected boolean handleScrollbarClick(int rx, int ry, int absMouseY) {
        if (revolverCount > MAX_VISIBLE_BTNS
                && rx >= REV_SCROLLBAR_X && rx < REV_SCROLLBAR_X + SCROLLBAR_W
                && ry >= BTN_Y_START && ry < BTN_Y_START + SCROLL_ZONE_H) {
            int max    = revolverCount - MAX_VISIBLE_BTNS;
            int thumbH = calcThumbHeight(revolverCount);
            int thumbY = BTN_Y_START + calcThumbY(revolverScroll, max, thumbH);
            if (ry >= thumbY && ry < thumbY + thumbH) {
                draggingRevScroll   = true;
                dragScrollStartNorm = (float) revolverScroll / max;
                dragMouseYStart     = absMouseY;
            } else {
                revolverScroll = clampScroll(
                        (int)((float)(ry - BTN_Y_START - thumbH / 2) / (SCROLL_ZONE_H - thumbH) * max), max);
            }
            return true;
        }

        if (bulletCount > MAX_VISIBLE_BTNS
                && rx >= BULLET_SCROLLBAR_X && rx < BULLET_SCROLLBAR_X + SCROLLBAR_W
                && ry >= BTN_Y_START && ry < BTN_Y_START + SCROLL_ZONE_H) {
            int max    = bulletCount - MAX_VISIBLE_BTNS;
            int thumbH = calcThumbHeight(bulletCount);
            int thumbY = BTN_Y_START + calcThumbY(bulletScroll, max, thumbH);
            if (ry >= thumbY && ry < thumbY + thumbH) {
                draggingBullScroll  = true;
                dragScrollStartNorm = (float) bulletScroll / max;
                dragMouseYStart     = absMouseY;
            } else {
                bulletScroll = clampScroll(
                        (int)((float)(ry - BTN_Y_START - thumbH / 2) / (SCROLL_ZONE_H - thumbH) * max), max);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
            if (draggingRevScroll && revolverCount > MAX_VISIBLE_BTNS) {
                revolverScroll = calcScrollFromDrag(revolverCount, (int) mouseY);
                return true;
            }
            if (draggingBullScroll && bulletCount > MAX_VISIBLE_BTNS) {
                bulletScroll = calcScrollFromDrag(bulletCount, (int) mouseY);
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /** Converts the current drag mouse position into a clamped scroll offset. */
    private int calcScrollFromDrag(int total, int absMouseY) {
        int max        = total - MAX_VISIBLE_BTNS;
        int thumbH     = calcThumbHeight(total);
        int trackRange = SCROLL_ZONE_H - thumbH;
        if (trackRange <= 0) return 0;
        float delta = (float)(absMouseY - dragMouseYStart) / trackRange;
        return clampScroll(Math.round((dragScrollStartNorm + delta) * max), max);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingRevScroll  = false;
        draggingBullScroll = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * Handles left-clicks on the bullet detail panel controls:
     * quantity box, − button, + button, and the pickaxe craft button.
     */
    private boolean handleBulletInputClick(int rx, int ry, BulletAssemblyRecipe recipe) {
        if (rx >= QTY_BOX_X && rx < QTY_BOX_X + QTY_BOX_W
                && ry >= QTY_ROW_Y && ry < QTY_ROW_Y + QTY_MINUS_H) {
            editingQuantity = true; quantityInputStr = String.valueOf(bulletQuantity); return true;
        }
        if (editingQuantity) confirmQuantityInput(recipe);

        if (rx >= QTY_MINUS_X && rx < QTY_MINUS_X + QTY_MINUS_W
                && ry >= QTY_ROW_Y && ry < QTY_ROW_Y + QTY_MINUS_H) {
            bulletQuantity = Math.max(1, bulletQuantity - 1); return true;
        }
        if (rx >= QTY_PLUS_X && rx < QTY_PLUS_X + QTY_PLUS_W
                && ry >= QTY_ROW_Y && ry < QTY_ROW_Y + QTY_MINUS_H) {
            bulletQuantity = Math.min(BULLET_QTY_MAX, bulletQuantity + 1); return true;
        }
        if (rx >= QTY_PICK_X && rx < QTY_PICK_X + QTY_PICK_W
                && ry >= QTY_ROW_Y && ry < QTY_ROW_Y + QTY_MINUS_H) {
            if (cachedCanCraft) {
                PacketDistributor.sendToServer(
                        new AssemblyCraftC2SPacket.BulletPayload(
                                recipes.get(selectedRecipe).getId().toString(),
                                bulletQuantity));
                playUiClick();
            }
            return true;
        }
        return false;
    }

    /**
     * Commits the current quantity text input, clamping to [1, BULLET_QTY_MAX].
     * Resets the editing state regardless of whether parsing succeeded.
     */
    private void confirmQuantityInput(BulletAssemblyRecipe recipe) {
        try { bulletQuantity = Math.clamp(Integer.parseInt(quantityInputStr), 1, BULLET_QTY_MAX); }
        catch (NumberFormatException ignored) { }
        editingQuantity = false; quantityInputStr = "";
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (editingQuantity) {
            if (Character.isDigit(chr) && quantityInputStr.length() < 5) quantityInputStr += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingQuantity) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (selectedRecipe >= 0 && recipes.get(selectedRecipe) instanceof BulletAssemblyRecipe br)
                    confirmQuantityInput(br);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { editingQuantity = false; quantityInputStr = ""; return true; }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !quantityInputStr.isEmpty()) {
                quantityInputStr = quantityInputStr.substring(0, quantityInputStr.length() - 1); return true;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int rx = (int) mouseX - this.leftPos;
        int ry = (int) mouseY - this.topPos;

        if (ry >= BTN_Y_START && ry < RECIPE_ZONE_BOTTOM) {
            int delta = scrollY > 0 ? -1 : 1;
            if (rx >= BTN_X && rx < REV_SCROLLBAR_X + SCROLLBAR_W && revolverCount > MAX_VISIBLE_BTNS) {
                revolverScroll = clampScroll(revolverScroll + delta, revolverCount - MAX_VISIBLE_BTNS);
                return true;
            }
            if (rx >= BULLET_ZONE_X && rx < BULLET_SCROLLBAR_X + SCROLLBAR_W && bulletCount > MAX_VISIBLE_BTNS) {
                bulletScroll = clampScroll(bulletScroll + delta, bulletCount - MAX_VISIBLE_BTNS);
                return true;
            }
        }

        if (selectedRecipe >= 0 && selectedRecipe < recipes.size()
                && recipes.get(selectedRecipe) instanceof BulletAssemblyRecipe) {
            boolean overQtyRow = rx >= QTY_MINUS_X && rx < QTY_PLUS_X + QTY_PLUS_W
                    && ry >= QTY_ROW_Y && ry < QTY_ROW_Y + QTY_MINUS_H;
            if (overQtyRow) {
                bulletQuantity = Math.clamp(
                        bulletQuantity + (scrollY > 0 ? 1 : -1), 1, BULLET_QTY_MAX);
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ── Scroll helpers ────────────────────────────────────────────────────────

    private static int clampScroll(int v, int max) { return Math.max(0, Math.min(max, v)); }

    /** Calculates thumb height proportional to visible fraction, with a minimum for usability. */
    private static int calcThumbHeight(int total) {
        return Math.max(SCROLLBAR_THUMB_MIN_H, SCROLL_ZONE_H * MAX_VISIBLE_BTNS / total);
    }

    /** Calculates thumb Y offset within the track based on current scroll position. */
    private static int calcThumbY(int offset, int maxOffset, int thumbH) {
        return maxOffset <= 0 ? 0 : (SCROLL_ZONE_H - thumbH) * offset / maxOffset;
    }

    // =========================================================================
    // RENDER
    // =========================================================================

    @Override
    public void render(@NotNull GuiGraphics ctx, int mx, int my, float delta) {
        // Refresh every frame so craft-button state reacts immediately to quantity
        // changes and inventory changes — mirrors the Fabric version's behaviour.
        frameTimeMs = System.currentTimeMillis();
        refreshInventoryCache();

        super.render(ctx, mx, my, delta);
        this.renderTooltip(ctx, mx, my);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics ctx, float delta, int mx, int my) {
        drawPanel(ctx, leftPos, topPos, imageWidth, imageHeight, C_BG);

        ctx.fill(leftPos + 2, topPos + 13,                 leftPos + BG_W - 2, topPos + 14,                     C_SEPARATOR);
        ctx.fill(leftPos + 2, topPos + RECIPE_ZONE_BOTTOM, leftPos + BG_W - 2, topPos + RECIPE_ZONE_BOTTOM + 1, C_SEPARATOR);
        ctx.fill(leftPos + 2, topPos + HOTBAR_Y - 4,       leftPos + BG_W - 2, topPos + HOTBAR_Y - 3,           C_SEPARATOR);

        ctx.fill(leftPos + CENTER_SEP_X,     topPos + RECIPE_ZONE_TOP + 2,
                leftPos + CENTER_SEP_X + 1, topPos + RECIPE_ZONE_BOTTOM - 2, C_SEPARATOR);

        drawInventorySlots(ctx);
        drawRecipeSelector(ctx, mx, my);

        if (selectedRecipe >= 0 && selectedRecipe < recipes.size()) {
            ctx.fill(leftPos + DETAIL_X - 2, topPos + RECIPE_ZONE_TOP + 2,
                    leftPos + DETAIL_X - 1, topPos + RECIPE_ZONE_BOTTOM - 2, C_SEPARATOR);
            AssemblyRecipe recipe = recipes.get(selectedRecipe);
            if (recipe instanceof BulletAssemblyRecipe br) drawBulletDetail(ctx, mx, my, br);
            else drawRecipeDetail(ctx, mx, my, recipe);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics ctx, int mx, int my) {
        int titleW    = font.width(title);
        int maxTitleW = imageWidth - 8;
        drawTextFitted(ctx, title, (imageWidth - Math.min(titleW, maxTitleW)) / 2, titleLabelY, maxTitleW, C_TEXT);
        drawTextFitted(ctx, playerInventoryTitle, INV_X, inventoryLabelY, imageWidth - INV_X - 4, C_TEXT);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inventory
    // ─────────────────────────────────────────────────────────────────────────

    private void drawInventorySlots(GuiGraphics ctx) {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                drawSlotCell(ctx, leftPos + INV_X + col * 18, topPos + MAIN_INV_Y + row * 18);
        for (int col = 0; col < 9; col++)
            drawSlotCell(ctx, leftPos + INV_X + col * 18, topPos + HOTBAR_Y);
    }

    /** Draws a single inventory slot cell with a recessed bevel border. */
    private void drawSlotCell(GuiGraphics ctx, int sx, int sy) {
        ctx.fill(sx,     sy,     sx + 16, sy + 16, C_SLOT_INNER);
        ctx.fill(sx - 1, sy - 1, sx + 17, sy,      C_SLOT_SHADOW);
        ctx.fill(sx - 1, sy - 1, sx,      sy + 17, C_SLOT_SHADOW);
        ctx.fill(sx - 1, sy + 16, sx + 17, sy + 17, C_LIGHT);
        ctx.fill(sx + 16, sy - 1, sx + 17, sy + 17, C_LIGHT);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Recipe buttons + scrollbars
    // ─────────────────────────────────────────────────────────────────────────

    protected void drawRecipeSelector(GuiGraphics ctx, int mx, int my) {
        ctx.enableScissor(leftPos + BTN_X, topPos + BTN_Y_START,
                leftPos + BULLET_SCROLLBAR_X + SCROLLBAR_W, topPos + RECIPE_ZONE_BOTTOM);

        int revolverIdx = 0, bulletIdx = 0;
        for (int i = 0; i < recipes.size(); i++) {
            boolean isBullet = recipes.get(i) instanceof BulletAssemblyRecipe;
            int virtualRow = (isBullet ? bulletIdx : revolverIdx)
                    - (isBullet ? bulletScroll : revolverScroll);
            if (isBullet) bulletIdx++; else revolverIdx++;
            if (virtualRow < 0 || virtualRow >= MAX_VISIBLE_BTNS) continue;

            int bx = leftPos + (isBullet ? BULLET_ZONE_X : BTN_X);
            int by = topPos + BTN_Y_START + virtualRow * BTN_STEP;
            boolean hovered  = mx >= bx && mx < bx + BTN_SIZE && my >= by && my < by + BTN_SIZE;
            boolean selected = (selectedRecipe == i);
            ctx.fill(bx, by, bx + BTN_SIZE, by + BTN_SIZE,
                    selected ? C_BTN_SELECTED : hovered ? C_BTN_HOVER : C_BTN_NORMAL);
            drawBevelBorder(ctx, bx, by, BTN_SIZE, BTN_SIZE);
            ctx.renderItem(recipeIconStacks[i], bx + 4, by + 4);
        }

        ctx.disableScissor();

        if (revolverCount > MAX_VISIBLE_BTNS)
            drawVanillaScrollbar(ctx, mx, my,
                    leftPos + REV_SCROLLBAR_X, topPos + BTN_Y_START,
                    revolverScroll, revolverCount, draggingRevScroll);

        if (bulletCount > MAX_VISIBLE_BTNS)
            drawVanillaScrollbar(ctx, mx, my,
                    leftPos + BULLET_SCROLLBAR_X, topPos + BTN_Y_START,
                    bulletScroll, bulletCount, draggingBullScroll);
    }

    protected void drawVanillaScrollbar(GuiGraphics ctx, int mx, int my,
                                        int absX, int absY,
                                        int offset, int total, boolean dragging) {
        int maxOffset = total - MAX_VISIBLE_BTNS;
        int thumbH    = calcThumbHeight(total);
        int thumbTop  = absY + calcThumbY(offset, maxOffset, thumbH);
        int w = SCROLLBAR_W, h = SCROLL_ZONE_H;

        ctx.fill(absX,         absY,         absX + w, absY + 1,          C_SLOT_SHADOW);
        ctx.fill(absX,         absY,         absX + 1, absY + h,          C_SLOT_SHADOW);
        ctx.fill(absX,         absY + h - 1, absX + w, absY + h,          C_LIGHT);
        ctx.fill(absX + w - 1, absY,         absX + w, absY + h,          C_LIGHT);
        ctx.fill(absX + 1,     absY + 1,     absX + w - 1, absY + h - 1, C_TRACK_FILL);

        boolean thumbHovered = !dragging
                && mx >= absX && mx < absX + w
                && my >= thumbTop && my < thumbTop + thumbH;
        int fill = (thumbHovered || dragging) ? C_THUMB_HOVER : C_THUMB_NORMAL;

        ctx.fill(absX,         thumbTop,              absX + w,     thumbTop + thumbH,      fill);
        ctx.fill(absX,         thumbTop,              absX + w - 1, thumbTop + 1,            C_LIGHT);
        ctx.fill(absX,         thumbTop,              absX + 1,     thumbTop + thumbH - 1,   C_LIGHT);
        ctx.fill(absX,         thumbTop + thumbH - 1, absX + w,     thumbTop + thumbH,       C_SLOT_SHADOW);
        ctx.fill(absX + w - 1, thumbTop,              absX + w,     thumbTop + thumbH,       C_SLOT_SHADOW);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Detail panel — BULLETS
    // ─────────────────────────────────────────────────────────────────────────

    protected void drawBulletDetail(GuiGraphics ctx, int mx, int my, BulletAssemblyRecipe recipe) {
        int cx       = leftPos + DETAIL_X + DETAIL_PAD;
        int mY       = topPos + RECIPE_ZONE_TOP + MODEL_OFFSET;
        int nameMaxW = BG_W - (DETAIL_X + DETAIL_PAD) - 4;

        drawTextFitted(ctx, Component.translatable(recipe.getTranslationKey()),
                cx, topPos + RECIPE_ZONE_TOP + NAME_OFFSET, nameMaxW, C_TEXT);

        int previewW = MODEL_W, previewH = BULLET_PREVIEW_SIZE;
        ctx.fill(cx, mY, cx + previewW, mY + previewH, C_MODEL_BG);
        int bc = C_MODEL_BORDER_NORMAL;
        ctx.fill(cx,                mY,                cx + previewW, mY + 1,        bc);
        ctx.fill(cx,                mY,                cx + 1,        mY + previewH, bc);
        ctx.fill(cx,                mY + previewH - 1, cx + previewW, mY + previewH, bc);
        ctx.fill(cx + previewW - 1, mY,                cx + previewW, mY + previewH, bc);

        drawSpinningBulletIcon(ctx, cx + previewW / 2, mY + previewH / 2);

        if (recipe.getResult() instanceof BaseBulletItem bullet) {
            Component dmgText = Component.translatable("gui.nst_revrecoiled.damage", bullet.getDamage());
            int dmgW = Math.min(font.width(dmgText), previewW - 4);
            int dmgX = cx + (previewW - dmgW) / 2, dmgY = mY + previewH - 10;
            ctx.fill(dmgX - 1, dmgY - 1, dmgX + dmgW + 1, dmgY + 9, 0x88000000);
            drawTextFitted(ctx, dmgText, dmgX, dmgY, previewW - 4, 0xFFFFAA00);
        }

        int ingBaseY = mY + previewH + 4;
        if (cachedIngCounts == null || cachedIngCounts.length < cachedIngredients.size()) return;

        var m = ctx.pose(); m.pushPose();
        try {
            m.translate(cx, ingBaseY, 0);
            m.scale(cachedIngScale, cachedIngScale, 1f);
            int ingOffX = 0;
            for (int i = 0; i < cachedIngredients.size(); i++) {
                AssemblyRecipe.Ingredient ing = cachedIngredients.get(i);
                int have = cachedIngCounts[i];
                int need = ing.count() * bulletQuantity;
                ctx.renderItem(cachedIngStacks.get(i), ingOffX, 0);
                drawTextFitted(ctx, Component.literal(have + "/" + need), ingOffX + 18, 4, 28,
                        have >= need ? C_TEXT_OK : C_TEXT_MISSING);
                ingOffX += 48;
            }
        } finally {
            m.popPose();
        }

        drawPickaxeStatus(ctx, topPos + RECIPE_ZONE_BOTTOM - 27, recipe, nameMaxW);
        drawQuantityControls(ctx, mx, my);
    }

    /**
     * Draws the bullet icon with a horizontal squash animation simulating rotation.
     * scaleX oscillates between 0.08 (edge-on) and 1.0 (face-on) using a cosine curve
     * driven by frameTimeMs and ROTATION_PERIOD_MS.
     */
    private void drawSpinningBulletIcon(GuiGraphics ctx, int cx, int cy) {
        float yaw    = (float)((frameTimeMs % ROTATION_PERIOD_MS) * 360.0 / ROTATION_PERIOD_MS);
        float scaleX = Math.max(0.08f, Math.abs((float) Math.cos(Math.toRadians(yaw))));
        var m = ctx.pose(); m.pushPose();
        try {
            m.translate(cx, cy, 200);
            m.scale(BULLET_ICON_SCALE * scaleX, BULLET_ICON_SCALE, 1f);
            ctx.renderItem(recipeIconStacks[selectedRecipe], -8, -8);
        } finally {
            m.popPose();
        }
    }

    /**
     * Draws the pickaxe status line below the ingredient row.
     * Shows the required tier in red if no valid pickaxe is present,
     * or remaining durability in green/red depending on whether it covers the current quantity.
     */
    private void drawPickaxeStatus(GuiGraphics ctx, int statusY,
                                   BulletAssemblyRecipe recipe, int maxW) {
        Component statusText; int color;
        if (cachedPickaxeSlot < 0) {
            statusText = Component.translatable(recipe.getTierTranslationKey());
            color = C_TEXT_MISSING;
        } else {
            ItemStack pick = menu.getPlayerInventory().items.get(cachedPickaxeSlot);
            int remaining  = pick.getMaxDamage() - pick.getDamageValue();
            statusText = Component.translatable("gui.nst_revrecoiled.pickaxe_durability",
                    remaining, pick.getMaxDamage());
            color = remaining >= bulletQuantity ? C_TEXT_OK : C_TEXT_MISSING;
        }
        drawTextFitted(ctx, statusText, leftPos + DETAIL_X + DETAIL_PAD, statusY, maxW, color);
    }

    /**
     * Draws the quantity row: − button, quantity text box, + button, and pickaxe craft button.
     * The quantity box shows a blinking cursor while editing.
     * The pickaxe button is greyed out when cachedCanCraft is false.
     */
    private void drawQuantityControls(GuiGraphics ctx, int mx, int my) {
        int aMx = leftPos + QTY_MINUS_X, aMy = topPos + QTY_ROW_Y;
        int aPx = leftPos + QTY_PLUS_X,  aPy = topPos + QTY_ROW_Y;
        int aKx = leftPos + QTY_PICK_X,  aKy = topPos + QTY_ROW_Y;

        // − button
        boolean mH = mx >= aMx && mx < aMx + QTY_MINUS_W && my >= aMy && my < aMy + QTY_MINUS_H;
        boolean mE = bulletQuantity > 1;
        ctx.fill(aMx, aMy, aMx + QTY_MINUS_W, aMy + QTY_MINUS_H, mH && mE ? C_QTY_BTN_HOVER : C_QTY_BTN);
        drawBevelBorder(ctx, aMx, aMy, QTY_MINUS_W, QTY_MINUS_H);
        ctx.fill(aMx + 3, aMy + 6, aMx + QTY_MINUS_W - 3, aMy + 7, mE ? C_DARK : C_SEPARATOR);

        // Quantity text box
        int qX = leftPos + QTY_BOX_X, qY = topPos + QTY_ROW_Y;
        boolean qH = mx >= qX && mx < qX + QTY_BOX_W && my >= qY && my < qY + QTY_MINUS_H;
        ctx.fill(qX, qY, qX + QTY_BOX_W, qY + QTY_MINUS_H, editingQuantity ? 0xFF000000 : 0xFF222222);
        int qBC = editingQuantity ? 0xFF8888FF : (qH ? 0xFF666688 : C_SLOT_SHADOW);
        ctx.fill(qX,                  qY,                    qX + QTY_BOX_W, qY + 1,             qBC);
        ctx.fill(qX,                  qY,                    qX + 1,         qY + QTY_MINUS_H,   qBC);
        ctx.fill(qX,                  qY + QTY_MINUS_H - 1,  qX + QTY_BOX_W, qY + QTY_MINUS_H,  qBC);
        ctx.fill(qX + QTY_BOX_W - 1, qY,                    qX + QTY_BOX_W, qY + QTY_MINUS_H,   qBC);
        String ds = editingQuantity
                ? quantityInputStr + ((frameTimeMs / 500) % 2 == 0 ? "|" : " ")
                : String.valueOf(bulletQuantity);
        int   dsW = font.width(ds);
        float dsS = dsW > QTY_BOX_W - 4 ? (float)(QTY_BOX_W - 4) / dsW : 1f;
        var qm = ctx.pose(); qm.pushPose();
        try {
            qm.translate(qX + (QTY_BOX_W - Math.min(dsW, QTY_BOX_W - 4)) / 2f,
                    qY + (QTY_MINUS_H - 8) / 2f, 0);
            qm.scale(dsS, dsS, 1f);
            ctx.drawString(font, ds, 0, 0, 0xFFFFFFFF, false);
        } finally {
            qm.popPose();
        }

        // + button
        boolean pH = mx >= aPx && mx < aPx + QTY_PLUS_W && my >= aPy && my < aPy + QTY_MINUS_H;
        boolean pE = bulletQuantity < BULLET_QTY_MAX;
        ctx.fill(aPx, aPy, aPx + QTY_PLUS_W, aPy + QTY_MINUS_H, pH && pE ? C_QTY_BTN_HOVER : C_QTY_BTN);
        drawBevelBorder(ctx, aPx, aPy, QTY_PLUS_W, QTY_MINUS_H);
        ctx.fill(aPx + 3,                   aPy + 6, aPx + QTY_PLUS_W - 3, aPy + 7, pE ? C_DARK : C_SEPARATOR);
        ctx.fill(aPx + QTY_PLUS_W / 2 - 1, aPy + 3, aPx + QTY_PLUS_W / 2, aPy + QTY_MINUS_H - 3, pE ? C_DARK : C_SEPARATOR);

        // Pickaxe craft button
        boolean kH = mx >= aKx && mx < aKx + QTY_PICK_W && my >= aKy && my < aKy + QTY_MINUS_H;
        ctx.fill(aKx, aKy, aKx + QTY_PICK_W, aKy + QTY_MINUS_H,
                !cachedCanCraft ? C_PICK_OFF : kH ? C_PICK_HOVER : C_PICK_READY);
        drawBevelBorder(ctx, aKx, aKy, QTY_PICK_W, QTY_MINUS_H);
        var pm = ctx.pose(); pm.pushPose();
        try {
            pm.translate(aKx + QTY_PICK_W / 2f, aKy + QTY_MINUS_H / 2f, 0);
            pm.scale(0.75f, 0.75f, 1f);
            ctx.renderItem(cachedDisplayPickaxeStack, -8, -8);
        } finally {
            pm.popPose();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Detail panel — REVOLVERS
    // ─────────────────────────────────────────────────────────────────────────

    protected void drawRecipeDetail(GuiGraphics ctx, int mx, int my, AssemblyRecipe recipe) {
        int cx       = leftPos + DETAIL_X + DETAIL_PAD;
        int mY       = topPos + RECIPE_ZONE_TOP + MODEL_OFFSET;
        int nameMaxW = BG_W - (DETAIL_X + DETAIL_PAD) - 4;

        drawTextFitted(ctx, Component.translatable(recipe.getTranslationKey()),
                cx, topPos + RECIPE_ZONE_TOP + NAME_OFFSET, nameMaxW, C_TEXT);

        int bc = C_MODEL_BORDER_NORMAL;
        ctx.fill(cx, mY, cx + MODEL_W, mY + MODEL_SIZE, C_MODEL_BG);
        ctx.fill(cx,               mY,                cx + MODEL_W, mY + 1,              bc);
        ctx.fill(cx,               mY,                cx + 1,       mY + MODEL_SIZE,     bc);
        ctx.fill(cx,               mY + MODEL_SIZE - 1, cx + MODEL_W, mY + MODEL_SIZE,   bc);
        ctx.fill(cx + MODEL_W - 1, mY,                cx + MODEL_W, mY + MODEL_SIZE,     bc);

        float autoYaw = FIXED_YAW + (float)((frameTimeMs % ROTATION_PERIOD_MS) * 360.0 / ROTATION_PERIOD_MS);
        renderGeckoModel(ctx, recipeIconStacks[selectedRecipe],
                cx + MODEL_W / 2, mY + MODEL_SIZE / 2, MODEL_SCALE, autoYaw, FIXED_PITCH);

        if (recipe.getResult() instanceof BaseRevolverItem revolver) {
            Component dmgText = Component.translatable("gui.nst_revrecoiled.damage", revolver.getDamage());
            int dmgW = Math.min(font.width(dmgText), MODEL_W - 4);
            int dmgX = cx + (MODEL_W - dmgW) / 2, dmgY = mY + MODEL_SIZE - 10;
            ctx.fill(dmgX - 1, dmgY - 1, dmgX + dmgW + 1, dmgY + 9, 0x88000000);
            drawTextFitted(ctx, dmgText, dmgX, dmgY, MODEL_W - 4, 0xFFFFAA00);
        }

        int ingBaseY = mY + MODEL_SIZE + 4;
        if (cachedIngCounts == null || cachedIngCounts.length < cachedIngredients.size()) return;

        var m = ctx.pose(); m.pushPose();
        try {
            m.translate(cx, ingBaseY, 0);
            m.scale(cachedIngScale, cachedIngScale, 1f);
            int ingOffX = 0;
            for (int i = 0; i < cachedIngredients.size(); i++) {
                AssemblyRecipe.Ingredient ing = cachedIngredients.get(i);
                int have = cachedIngCounts[i];
                int need = ing.count();
                ctx.renderItem(cachedIngStacks.get(i), ingOffX, 0);
                drawTextFitted(ctx, Component.literal(have + "/" + need), ingOffX + 18, 4, 28,
                        have >= need ? C_TEXT_OK : C_TEXT_MISSING);
                ingOffX += 48;
            }
        } finally {
            m.popPose();
        }

        int bx = leftPos + CRAFT_BTN_X, by = topPos + CRAFT_BTN_Y;
        boolean hovered = mx >= bx && mx < bx + CRAFT_BTN_W && my >= by && my < by + CRAFT_BTN_H;
        ctx.fill(bx, by, bx + CRAFT_BTN_W, by + CRAFT_BTN_H,
                !cachedCanCraft ? C_CRAFT_OFF : hovered ? C_CRAFT_HOVER : C_CRAFT_READY);
        drawBevelBorder(ctx, bx, by, CRAFT_BTN_W, CRAFT_BTN_H);
        Component label   = Component.translatable("gui.nst_revrecoiled.assemble");
        int labelMaxW     = CRAFT_BTN_W - 6;
        int labelW        = Math.min(font.width(label), labelMaxW);
        drawTextFitted(ctx, label, bx + (CRAFT_BTN_W - labelW) / 2,
                by + (CRAFT_BTN_H - 8) / 2, labelMaxW, 0xFFFFFFFF);
    }

    // =========================================================================
    // 3D GeckoLib rendering
    // =========================================================================

    /**
     * Renders the GeckoLib 3D model of a revolver item centered in the preview box.
     * Falls back to a flat item render if the revolver's GeoRenderProvider is not yet
     * initialized (e.g. during the first frame after screen open).
     *
     * Scissor is enabled to clip the model to the preview box bounds,
     * preventing it from bleeding into adjacent UI elements.
     * DiffuseLighting is toggled around the render call to match GUI lighting expectations.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void renderGeckoModel(GuiGraphics ctx, ItemStack stack, int centerX, int centerY,
                                    float scale, float yawDeg, float pitchDeg) {
        if (!(stack.getItem() instanceof BaseRevolverItem revolver)) {
            ctx.renderItem(stack, centerX - 8, centerY - 8);
            return;
        }

        Object providerObj = revolver.getRenderProvider();
        if (!(providerObj instanceof GeoRenderProvider provider)) return;

        GeoItemRenderer<?> renderer = (GeoItemRenderer<?>) provider.getGeoItemRenderer();
        if (renderer == null) return;

        Minecraft client = Minecraft.getInstance();
        MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();

        ctx.enableScissor(
                centerX - MODEL_SIZE / 2,
                centerY - MODEL_SIZE / 2,
                centerX - MODEL_SIZE / 2 + MODEL_W,
                centerY - MODEL_SIZE / 2 + MODEL_SIZE);

        PoseStack m = ctx.pose();
        m.pushPose();
        try {
            m.translate(centerX, centerY, 200.0);
            m.scale(scale, -scale, scale); // Y-flip to match GUI coordinate system
            m.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitchDeg));
            m.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yawDeg));
            m.translate(-MODEL_PIVOT_X, -MODEL_PIVOT_Y, -MODEL_PIVOT_Z);

            com.mojang.blaze3d.platform.Lighting.setupForEntityInInventory();
            renderer.renderByItem(
                    stack,
                    ItemDisplayContext.NONE, m, buffers,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            buffers.endBatch();
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
        } finally {
            m.popPose();
        }
        ctx.disableScissor();
    }

    // =========================================================================
    // Drawing primitives
    // =========================================================================

    /**
     * Draws text scaled down to fit within maxWidth if the natural width exceeds it.
     * Scaling is applied via matrix transform to preserve sub-pixel alignment.
     */
    protected void drawTextFitted(GuiGraphics ctx, Component text, int tx, int ty, int maxWidth, int color) {
        int w = font.width(text);
        if (w <= maxWidth) { ctx.drawString(font, text, tx, ty, color, false); return; }
        var m = ctx.pose(); m.pushPose();
        try {
            m.translate(tx, ty, 0f);
            m.scale((float) maxWidth / w, (float) maxWidth / w, 1f);
            ctx.drawString(font, text, 0, 0, color, false);
        } finally {
            m.popPose();
        }
    }

    /**
     * Draws a raised panel with a 1-pixel dark outer border, 1-pixel light highlight
     * on the bottom/right, and a 1-pixel bright inner highlight on top/left for depth.
     */
    protected void drawPanel(GuiGraphics ctx, int px, int py, int pw, int ph, int bg) {
        ctx.fill(px + 1,      py + 1,      px + pw - 1, py + ph - 1, bg);
        ctx.fill(px,          py,          px + pw,     py + 1,      C_DARK);
        ctx.fill(px,          py,          px + 1,      py + ph,     C_DARK);
        ctx.fill(px,          py + ph - 1, px + pw,     py + ph,     C_LIGHT);
        ctx.fill(px + pw - 1, py,          px + pw,     py + ph,     C_LIGHT);
        ctx.fill(px + 1,      py + 1,      px + pw - 1, py + 2,      0xFFEEEEEE);
        ctx.fill(px + 1,      py + 1,      px + 2,      py + ph - 1, 0xFFEEEEEE);
    }

    /**
     * Draws a recessed bevel border matching the vanilla inventory slot style.
     * Dark top/left edges, light bottom/right edges — gives the appearance of
     * a pressed-in surface.
     */
    protected void drawBevelBorder(GuiGraphics ctx, int sx, int sy, int sw, int sh) {
        ctx.fill(sx,          sy,          sx + sw, sy + 1,  C_SLOT_SHADOW);
        ctx.fill(sx,          sy,          sx + 1,  sy + sh, C_SLOT_SHADOW);
        ctx.fill(sx,          sy + sh - 1, sx + sw, sy + sh, C_LIGHT);
        ctx.fill(sx + sw - 1, sy,          sx + sw, sy + sh, C_LIGHT);
    }
}
