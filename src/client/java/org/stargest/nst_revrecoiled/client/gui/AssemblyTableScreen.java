package org.stargest.nst_revrecoiled.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;
import org.stargest.nst_revrecoiled.Items.BaseBulletItem;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import org.stargest.nst_revrecoiled.handlers.AssemblyTableScreenHandler;
import org.stargest.nst_revrecoiled.network.AssemblyCraftC2SPacket;
import org.stargest.nst_revrecoiled.recipe.AssemblyRecipe;
import org.stargest.nst_revrecoiled.recipe.AssemblyRecipes;
import org.stargest.nst_revrecoiled.recipe.BulletAssemblyRecipe;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.renderer.GeckolibSpecialRenderer;
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
 *   that doubles as a pickaxe-durability indicator
 * - Per-frame inventory cache (refreshInventoryCache) avoids redundant ingredient
 *   counts, canCraft checks, and pickaxe searches during the render pass
 * - Recipe-change cache (onRecipeSelected) avoids rebuilding ingredient ItemStacks
 *   and scale factors every frame
 */
@Environment(EnvType.CLIENT)
public class AssemblyTableScreen extends HandledScreen<AssemblyTableScreenHandler> {

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

    private static final int REV_SCROLLBAR_X    = BTN_X + BTN_SIZE;                    // 30
    private static final int CENTER_SEP_X       = REV_SCROLLBAR_X + SCROLLBAR_W + 1;   // 37
    private static final int BULLET_ZONE_X      = CENTER_SEP_X + 2;                    // 39
    private static final int BULLET_ZONE_W      = BTN_SIZE;                            // 24
    private static final int BULLET_SCROLLBAR_X = BULLET_ZONE_X + BULLET_ZONE_W;       // 63

    // ── Detail panel ──────────────────────────────────────────────────────────
    private static final int DETAIL_X   = BULLET_SCROLLBAR_X + SCROLLBAR_W + 2;        // 71
    private static final int DETAIL_PAD = 4;

    private static final int   MODEL_SIZE        = 60;
    private static final int   MODEL_W           = 96;
    private static final float MODEL_SCALE       = 23f;

    private static final float FIXED_YAW          = 100f;
    private static final float FIXED_PITCH        =  10f;
    private static final long  ROTATION_PERIOD_MS = 3000L;

    // GeckoLib model pivot offsets — tuned to center the revolver in the preview box
    private static final float MODEL_PIVOT_X = 1.4f;
    private static final float MODEL_PIVOT_Y = 0.8f;
    private static final float MODEL_PIVOT_Z = 1.1f;

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
    private static final int QTY_ROW_Y   = RECIPE_ZONE_BOTTOM - 17;              // 114
    private static final int QTY_MINUS_X = DETAIL_X + DETAIL_PAD;                // 75
    private static final int QTY_MINUS_W = 14;
    private static final int QTY_MINUS_H = 13;
    private static final int QTY_BOX_X   = QTY_MINUS_X + QTY_MINUS_W;           // 89
    private static final int QTY_BOX_W   = 36;
    private static final int QTY_PLUS_X  = QTY_BOX_X + QTY_BOX_W;               // 125
    private static final int QTY_PLUS_W  = 14;
    private static final int QTY_PICK_X  = QTY_PLUS_X + QTY_PLUS_W + 4;         // 143
    private static final int QTY_PICK_W  = 26;
    private static final int CRAFT_BTN_X = DETAIL_X + DETAIL_PAD;                // 75
    private static final int CRAFT_BTN_Y = RECIPE_ZONE_BOTTOM - 19;              // 112
    private static final int CRAFT_BTN_W = BG_W - (DETAIL_X + DETAIL_PAD) - 4;  //  97
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
    private List<ItemStack>                 cachedIngStacks; // ItemStack per ingredient slot
    private float                           cachedIngScale;  // scale factor for the ingredient row

    /**
     * Per-frame inventory cache. Rebuilt once per frame in refreshInventoryCache()
     * to avoid redundant inventory scans during the render pass.
     */
    private boolean   cachedCanCraft;
    private int[]     cachedIngCounts;           // how many of each ingredient the player has
    private int       cachedPickaxeSlot;         // -1 if no suitable pickaxe is present
    private Item      cachedDisplayPickaxe;      // pickaxe item for the craft button icon (null for revolvers)
    private ItemStack cachedDisplayPickaxeStack; // ItemStack for the pickaxe icon; recreated only on type change

    /** Frame timestamp in milliseconds, captured once per frame for animations and cursor blinking. */
    private long frameTimeMs;

    // ─────────────────────────────────────────────────────────────────────────

    public AssemblyTableScreen(AssemblyTableScreenHandler handler,
                               PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth  = BG_W;
        this.backgroundHeight = BG_H;
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

        // Create recipe icon ItemStacks once; reused for all rendering
        recipeIconStacks = new ItemStack[recipes.size()];
        for (int i = 0; i < recipes.size(); i++)
            recipeIconStacks[i] = new ItemStack(recipes.get(i).getResult());

        titleY = 4;
        playerInventoryTitleY = 134;
    }

    /**
     * Called whenever selectedRecipe changes.
     * Rebuilds recipe-dependent cache (ingredient list, ItemStacks, and row scale)
     * that does not depend on current inventory state.
     * Avoids rebuilding this data on every frame — only on actual selection changes.
     */
    private void onRecipeSelected(int newIndex) {
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

        // Build ingredient ItemStacks here, not in the render path
        cachedIngStacks = new ArrayList<>(cachedIngredients.size());
        for (AssemblyRecipe.Ingredient ing : cachedIngredients)
            cachedIngStacks.add(new ItemStack(ing.item()));

        // Scale depends only on ingredient count; recalculate here rather than per frame
        cachedIngScale = Math.min(1f, (float) MODEL_W / Math.max(1, cachedIngredients.size() * 48));

        // Reset inventory counts; recalculated on the next refreshInventoryCache()
        cachedIngCounts = null;
    }

    /**
     * Rebuilds the per-frame inventory cache. Called exactly once at the start of
     * each frame, consolidating all calls to countInInventory, canCraft,
     * findBestPickaxeSlot, and getMaxCraftable into a single pass.
     * Results are stored in cachedCanCraft, cachedIngCounts, cachedPickaxeSlot,
     * and cachedDisplayPickaxeStack for use during the render pass.
     */
    private void refreshInventoryCache() {
        if (selectedRecipe < 0 || selectedRecipe >= recipes.size()
                || cachedIngredients == null) return;

        AssemblyRecipe recipe = recipes.get(selectedRecipe);
        var inv = handler.getPlayerInventory();

        // Ingredient counts
        if (cachedIngCounts == null || cachedIngCounts.length != cachedIngredients.size())
            cachedIngCounts = new int[cachedIngredients.size()];
        for (int i = 0; i < cachedIngredients.size(); i++)
            cachedIngCounts[i] = recipe.countInInventory(inv, cachedIngredients.get(i).item());

        if (recipe instanceof BulletAssemblyRecipe br) {
            cachedCanCraft    = br.canCraftBullets(inv, bulletQuantity);
            cachedPickaxeSlot = br.findBestPickaxeSlot(inv);

            // Resolve pickaxe icon once per frame rather than redundantly in
            // drawPickaxeStatus() and drawQuantityControls()
            Item newPickaxe = cachedPickaxeSlot >= 0
                    ? inv.main.get(cachedPickaxeSlot).getItem()
                    : switch (br.getRequiredTier()) {
                case ANY        -> Items.WOODEN_PICKAXE;
                case STONE_PLUS -> Items.STONE_PICKAXE;
                case IRON_PLUS  -> Items.IRON_PICKAXE;
            };
            // Recreate ItemStack only when the pickaxe type actually changes
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
            int rx = (int) mouseX - this.x;
            int ry = (int) mouseY - this.y;

            // Scrollbars take priority over recipe buttons
            if (handleScrollbarClick(rx, ry, (int) mouseY)) return true;

            // Recipe buttons — account for current scroll offset
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
                    else onRecipeSelected(-1); // clicking selected recipe deselects it
                    return true;
                }
            }

            // Detail panel interaction
            if (selectedRecipe >= 0 && selectedRecipe < recipes.size()) {
                AssemblyRecipe recipe = recipes.get(selectedRecipe);
                if (recipe instanceof BulletAssemblyRecipe br) {
                    if (handleBulletInputClick(rx, ry, br)) return true;
                } else {
                    if (rx >= CRAFT_BTN_X && rx < CRAFT_BTN_X + CRAFT_BTN_W
                            && ry >= CRAFT_BTN_Y && ry < CRAFT_BTN_Y + CRAFT_BTN_H) {
                        if (cachedCanCraft)
                            ClientPlayNetworking.send(new AssemblyCraftC2SPacket.Payload(selectedRecipe));
                        return true;
                    }
                }
            }

            // Confirm quantity input when clicking outside the quantity box
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
    private boolean handleScrollbarClick(int rx, int ry, int absMouseY) {
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
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double deltaX, double deltaY) {
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
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
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
            if (cachedCanCraft)
                ClientPlayNetworking.send(
                        new AssemblyCraftC2SPacket.BulletPayload(selectedRecipe, bulletQuantity));
            return true;
        }
        return false;
    }

    /**
     * Commits the current quantity text input, clamping to [1, BULLET_QTY_MAX].
     * Resets the editing state regardless of whether parsing succeeded.
     */
    private void confirmQuantityInput(BulletAssemblyRecipe recipe) {
        try { bulletQuantity = Math.max(1, Math.min(BULLET_QTY_MAX, Integer.parseInt(quantityInputStr))); }
        catch (NumberFormatException ignored) { }
        editingQuantity = false; quantityInputStr = "";
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (editingQuantity) {
            // Only accept digits; limit to 5 characters to stay within BULLET_QTY_MAX (9999)
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
    public boolean mouseScrolled(double mouseX, double mouseY,
                                 double horizontalAmount, double verticalAmount) {
        int rx = (int) mouseX - this.x;
        int ry = (int) mouseY - this.y;

        // Scroll recipe lists when hovering over the recipe zone
        if (ry >= BTN_Y_START && ry < RECIPE_ZONE_BOTTOM) {
            int delta = verticalAmount > 0 ? -1 : 1;
            if (rx >= BTN_X && rx < REV_SCROLLBAR_X + SCROLLBAR_W && revolverCount > MAX_VISIBLE_BTNS) {
                revolverScroll = clampScroll(revolverScroll + delta, revolverCount - MAX_VISIBLE_BTNS);
                return true;
            }
            if (rx >= BULLET_ZONE_X && rx < BULLET_SCROLLBAR_X + SCROLLBAR_W && bulletCount > MAX_VISIBLE_BTNS) {
                bulletScroll = clampScroll(bulletScroll + delta, bulletCount - MAX_VISIBLE_BTNS);
                return true;
            }
        }

        // Scroll adjusts bullet quantity when a bullet recipe is selected
        if (selectedRecipe >= 0 && selectedRecipe < recipes.size()
                && recipes.get(selectedRecipe) instanceof BulletAssemblyRecipe) {
            bulletQuantity = Math.max(1, Math.min(BULLET_QTY_MAX,
                    bulletQuantity + (verticalAmount > 0 ? 1 : -1)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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
    public void render(DrawContext ctx, int mx, int my, float delta) {
        frameTimeMs = System.currentTimeMillis();
        refreshInventoryCache();

        renderBackground(ctx, mx, my, delta);
        super.render(ctx, mx, my, delta);
        drawMouseoverTooltip(ctx, mx, my);
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mx, int my) {
        drawPanel(ctx, x, y, BG_W, BG_H, C_BG);

        // Horizontal separators: below title, below recipe zone, above hotbar
        ctx.fill(x + 2, y + 13,                 x + BG_W - 2, y + 14,                     C_SEPARATOR);
        ctx.fill(x + 2, y + RECIPE_ZONE_BOTTOM, x + BG_W - 2, y + RECIPE_ZONE_BOTTOM + 1, C_SEPARATOR);
        ctx.fill(x + 2, y + HOTBAR_Y - 4,       x + BG_W - 2, y + HOTBAR_Y - 3,           C_SEPARATOR);

        // Vertical separator between revolver and bullet columns
        ctx.fill(x + CENTER_SEP_X, y + RECIPE_ZONE_TOP + 2,
                x + CENTER_SEP_X + 1, y + RECIPE_ZONE_BOTTOM - 2, C_SEPARATOR);

        drawInventorySlots(ctx);
        drawRecipeSelector(ctx, mx, my);

        if (selectedRecipe >= 0 && selectedRecipe < recipes.size()) {
            // Vertical separator between recipe list and detail panel
            ctx.fill(x + DETAIL_X - 2, y + RECIPE_ZONE_TOP + 2,
                    x + DETAIL_X - 1, y + RECIPE_ZONE_BOTTOM - 2, C_SEPARATOR);
            AssemblyRecipe recipe = recipes.get(selectedRecipe);
            if (recipe instanceof BulletAssemblyRecipe br) drawBulletDetail(ctx, mx, my, br);
            else drawRecipeDetail(ctx, mx, my, recipe);
        }
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mx, int my) {
        int titleW    = textRenderer.getWidth(title);
        int maxTitleW = BG_W - 8;
        drawTextFitted(ctx, title, (BG_W - Math.min(titleW, maxTitleW)) / 2, titleY, maxTitleW, C_TEXT);
        drawTextFitted(ctx, playerInventoryTitle, INV_X, playerInventoryTitleY, BG_W - INV_X - 4, C_TEXT);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inventory
    // ─────────────────────────────────────────────────────────────────────────

    private void drawInventorySlots(DrawContext ctx) {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                drawSlotCell(ctx, x + INV_X + col * 18, y + MAIN_INV_Y + row * 18);
        for (int col = 0; col < 9; col++)
            drawSlotCell(ctx, x + INV_X + col * 18, y + HOTBAR_Y);
    }

    /** Draws a single inventory slot cell with a recessed bevel border. */
    private void drawSlotCell(DrawContext ctx, int sx, int sy) {
        ctx.fill(sx,     sy,     sx + 16, sy + 16, C_SLOT_INNER);
        ctx.fill(sx - 1, sy - 1, sx + 17, sy,      C_SLOT_SHADOW);
        ctx.fill(sx - 1, sy - 1, sx,      sy + 17, C_SLOT_SHADOW);
        ctx.fill(sx - 1, sy + 16, sx + 17, sy + 17, C_LIGHT);
        ctx.fill(sx + 16, sy - 1, sx + 17, sy + 17, C_LIGHT);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Recipe buttons + scrollbars
    // ─────────────────────────────────────────────────────────────────────────

    private void drawRecipeSelector(DrawContext ctx, int mx, int my) {
        // Scissor clips the button area to the recipe zone.
        // Scrollbars are drawn outside the scissor region (to the right of clipX2)
        // so their thumbs are never accidentally clipped.
        ctx.enableScissor(x + BTN_X, y + BTN_Y_START,
                x + BULLET_SCROLLBAR_X + SCROLLBAR_W, y + RECIPE_ZONE_BOTTOM);

        int revolverIdx = 0, bulletIdx = 0;
        for (int i = 0; i < recipes.size(); i++) {
            boolean isBullet = recipes.get(i) instanceof BulletAssemblyRecipe;
            int virtualRow = (isBullet ? bulletIdx : revolverIdx)
                    - (isBullet ? bulletScroll : revolverScroll);
            if (isBullet) bulletIdx++; else revolverIdx++;
            if (virtualRow < 0 || virtualRow >= MAX_VISIBLE_BTNS) continue;

            int bx = x + (isBullet ? BULLET_ZONE_X : BTN_X);
            int by = y + BTN_Y_START + virtualRow * BTN_STEP;
            boolean hovered  = mx >= bx && mx < bx + BTN_SIZE && my >= by && my < by + BTN_SIZE;
            boolean selected = (selectedRecipe == i);
            ctx.fill(bx, by, bx + BTN_SIZE, by + BTN_SIZE,
                    selected ? C_BTN_SELECTED : hovered ? C_BTN_HOVER : C_BTN_NORMAL);
            drawBevelBorder(ctx, bx, by, BTN_SIZE, BTN_SIZE);
            ctx.drawItem(recipeIconStacks[i], bx + 4, by + 4);
        }

        ctx.disableScissor();

        if (revolverCount > MAX_VISIBLE_BTNS)
            drawVanillaScrollbar(ctx, mx, my,
                    x + REV_SCROLLBAR_X, y + BTN_Y_START,
                    revolverScroll, revolverCount, draggingRevScroll);

        if (bulletCount > MAX_VISIBLE_BTNS)
            drawVanillaScrollbar(ctx, mx, my,
                    x + BULLET_SCROLLBAR_X, y + BTN_Y_START,
                    bulletScroll, bulletCount, draggingBullScroll);
    }

    /**
     * Draws a vanilla-style scrollbar matching the creative inventory aesthetic.
     * Track: recessed (dark top/left border, light bottom/right border, dark fill).
     * Thumb: raised (light top/left border, dark bottom/right border, medium fill).
     * Thumb brightens on hover or while dragging.
     */
    private void drawVanillaScrollbar(DrawContext ctx, int mx, int my,
                                      int absX, int absY,
                                      int offset, int total, boolean dragging) {
        int maxOffset = total - MAX_VISIBLE_BTNS;
        int thumbH    = calcThumbHeight(total);
        int thumbTop  = absY + calcThumbY(offset, maxOffset, thumbH);
        int w = SCROLLBAR_W;
        int h = SCROLL_ZONE_H;

        // Track (recessed)
        ctx.fill(absX,         absY,         absX + w, absY + 1,          C_SLOT_SHADOW);
        ctx.fill(absX,         absY,         absX + 1, absY + h,          C_SLOT_SHADOW);
        ctx.fill(absX,         absY + h - 1, absX + w, absY + h,          C_LIGHT);
        ctx.fill(absX + w - 1, absY,         absX + w, absY + h,          C_LIGHT);
        ctx.fill(absX + 1,     absY + 1,     absX + w - 1, absY + h - 1, C_TRACK_FILL);

        // Thumb (raised)
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

    private void drawBulletDetail(DrawContext ctx, int mx, int my, BulletAssemblyRecipe recipe) {
        int cx       = x + DETAIL_X + DETAIL_PAD;
        int mY       = y + RECIPE_ZONE_TOP + MODEL_OFFSET;
        int nameMaxW = BG_W - (DETAIL_X + DETAIL_PAD) - 4;

        drawTextFitted(ctx, Text.translatable(recipe.getTranslationKey()),
                cx, y + RECIPE_ZONE_TOP + NAME_OFFSET, nameMaxW, C_TEXT);

        // Preview box
        int previewW = MODEL_W, previewH = BULLET_PREVIEW_SIZE;
        ctx.fill(cx, mY, cx + previewW, mY + previewH, C_MODEL_BG);
        int bc = C_MODEL_BORDER_NORMAL;
        ctx.fill(cx,                mY,                cx + previewW, mY + 1,        bc);
        ctx.fill(cx,                mY,                cx + 1,        mY + previewH, bc);
        ctx.fill(cx,                mY + previewH - 1, cx + previewW, mY + previewH, bc);
        ctx.fill(cx + previewW - 1, mY,                cx + previewW, mY + previewH, bc);

        drawSpinningBulletIcon(ctx, cx + previewW / 2, mY + previewH / 2);

        // Damage stat overlay
        if (recipe.getResult() instanceof BaseBulletItem bullet) {
            Text dmgText = Text.translatable("gui.nst_revrecoiled.damage", bullet.getDamage());
            int dmgW = Math.min(textRenderer.getWidth(dmgText), previewW - 4);
            int dmgX = cx + (previewW - dmgW) / 2, dmgY = mY + previewH - 10;
            ctx.fill(dmgX - 1, dmgY - 1, dmgX + dmgW + 1, dmgY + 9, 0x88000000);
            drawTextFitted(ctx, dmgText, dmgX, dmgY, previewW - 4, 0xFFFFAA00);
        }

        // Ingredient row with have/need counts
        int ingBaseY = mY + previewH + 4;
        var m = ctx.getMatrices(); m.push();
        try {
            m.translate(cx, ingBaseY, 0);
            m.scale(cachedIngScale, cachedIngScale, 1f);
            int ingOffX = 0;
            for (int i = 0; i < cachedIngredients.size(); i++) {
                AssemblyRecipe.Ingredient ing = cachedIngredients.get(i);
                int have = cachedIngCounts[i];
                int need = ing.count() * bulletQuantity;
                ctx.drawItem(cachedIngStacks.get(i), ingOffX, 0);
                drawTextFitted(ctx, Text.literal(have + "/" + need), ingOffX + 18, 4, 28,
                        have >= need ? C_TEXT_OK : C_TEXT_MISSING);
                ingOffX += 48;
            }
        } finally {
            m.pop();
        }

        drawPickaxeStatus(ctx, y + RECIPE_ZONE_BOTTOM - 27, recipe, nameMaxW);
        drawQuantityControls(ctx, mx, my);
    }

    /**
     * Draws the bullet icon with a horizontal squash animation simulating rotation.
     * scaleX oscillates between 0.08 (edge-on) and 1.0 (face-on) using a cosine curve
     * driven by frameTimeMs and ROTATION_PERIOD_MS.
     */
    private void drawSpinningBulletIcon(DrawContext ctx, int cx, int cy) {
        float yaw    = (float)((frameTimeMs % ROTATION_PERIOD_MS) * 360.0 / ROTATION_PERIOD_MS);
        float scaleX = Math.max(0.08f, Math.abs((float) Math.cos(Math.toRadians(yaw))));
        var m = ctx.getMatrices(); m.push();
        try {
            m.translate(cx, cy, 200);
            m.scale(BULLET_ICON_SCALE * scaleX, BULLET_ICON_SCALE, 1f);
            ctx.drawItem(recipeIconStacks[selectedRecipe], -8, -8);
        } finally {
            m.pop();
        }
    }

    /**
     * Draws the pickaxe status line below the ingredient row.
     * Shows the required tier in red if no valid pickaxe is present,
     * or remaining durability in green/red depending on whether it covers the current quantity.
     */
    private void drawPickaxeStatus(DrawContext ctx, int statusY,
                                   BulletAssemblyRecipe recipe, int maxW) {
        Text statusText; int color;
        if (cachedPickaxeSlot < 0) {
            statusText = Text.translatable(recipe.getTierTranslationKey());
            color = C_TEXT_MISSING;
        } else {
            ItemStack pick = handler.getPlayerInventory().main.get(cachedPickaxeSlot);
            int remaining  = pick.getMaxDamage() - pick.getDamage();
            statusText = Text.translatable("gui.nst_revrecoiled.pickaxe_durability",
                    remaining, pick.getMaxDamage());
            color = remaining >= bulletQuantity ? C_TEXT_OK : C_TEXT_MISSING;
        }
        drawTextFitted(ctx, statusText, x + DETAIL_X + DETAIL_PAD, statusY, maxW, color);
    }

    /**
     * Draws the quantity row: − button, quantity text box, + button, and pickaxe craft button.
     * The quantity box shows a blinking cursor while editing.
     * The pickaxe button is greyed out when cachedCanCraft is false.
     */
    private void drawQuantityControls(DrawContext ctx, int mx, int my) {
        int aMx = x + QTY_MINUS_X, aMy = y + QTY_ROW_Y;
        int aPx = x + QTY_PLUS_X,  aPy = y + QTY_ROW_Y;
        int aKx = x + QTY_PICK_X,  aKy = y + QTY_ROW_Y;

        // − button
        boolean mH = mx >= aMx && mx < aMx + QTY_MINUS_W && my >= aMy && my < aMy + QTY_MINUS_H;
        boolean mE = bulletQuantity > 1;
        ctx.fill(aMx, aMy, aMx + QTY_MINUS_W, aMy + QTY_MINUS_H, mH && mE ? C_QTY_BTN_HOVER : C_QTY_BTN);
        drawBevelBorder(ctx, aMx, aMy, QTY_MINUS_W, QTY_MINUS_H);
        ctx.fill(aMx + 3, aMy + 6, aMx + QTY_MINUS_W - 3, aMy + 7, mE ? C_DARK : C_SEPARATOR);

        // Quantity text box (black background; blue border while editing)
        int qX = x + QTY_BOX_X, qY = y + QTY_ROW_Y;
        boolean qH = mx >= qX && mx < qX + QTY_BOX_W && my >= qY && my < qY + QTY_MINUS_H;
        ctx.fill(qX, qY, qX + QTY_BOX_W, qY + QTY_MINUS_H, editingQuantity ? 0xFF000000 : 0xFF222222);
        int qBC = editingQuantity ? 0xFF8888FF : (qH ? 0xFF666688 : C_SLOT_SHADOW);
        ctx.fill(qX,                  qY,                  qX + QTY_BOX_W, qY + 1,               qBC);
        ctx.fill(qX,                  qY,                  qX + 1,         qY + QTY_MINUS_H,     qBC);
        ctx.fill(qX,                  qY + QTY_MINUS_H - 1, qX + QTY_BOX_W, qY + QTY_MINUS_H,   qBC);
        ctx.fill(qX + QTY_BOX_W - 1, qY,                  qX + QTY_BOX_W, qY + QTY_MINUS_H,     qBC);
        String ds = editingQuantity
                ? quantityInputStr + ((frameTimeMs / 500) % 2 == 0 ? "|" : " ")
                : String.valueOf(bulletQuantity);
        int   dsW = textRenderer.getWidth(ds);
        float dsS = dsW > QTY_BOX_W - 4 ? (float)(QTY_BOX_W - 4) / dsW : 1f;
        var qm = ctx.getMatrices(); qm.push();
        try {
            qm.translate(qX + (QTY_BOX_W - Math.min(dsW, QTY_BOX_W - 4)) / 2f,
                    qY + (QTY_MINUS_H - 8) / 2f, 0);
            qm.scale(dsS, dsS, 1f);
            ctx.drawText(textRenderer, ds, 0, 0, 0xFFFFFFFF, false);
        } finally {
            qm.pop();
        }

        // + button
        boolean pH = mx >= aPx && mx < aPx + QTY_PLUS_W && my >= aPy && my < aPy + QTY_MINUS_H;
        boolean pE = bulletQuantity < BULLET_QTY_MAX;
        ctx.fill(aPx, aPy, aPx + QTY_PLUS_W, aPy + QTY_MINUS_H, pH && pE ? C_QTY_BTN_HOVER : C_QTY_BTN);
        drawBevelBorder(ctx, aPx, aPy, QTY_PLUS_W, QTY_MINUS_H);
        ctx.fill(aPx + 3,                  aPy + 6, aPx + QTY_PLUS_W - 3, aPy + 7, pE ? C_DARK : C_SEPARATOR);
        ctx.fill(aPx + QTY_PLUS_W / 2 - 1, aPy + 3, aPx + QTY_PLUS_W / 2, aPy + QTY_MINUS_H - 3, pE ? C_DARK : C_SEPARATOR);

        // Pickaxe craft button — colour indicates ready/hover/disabled state
        boolean kH = mx >= aKx && mx < aKx + QTY_PICK_W && my >= aKy && my < aKy + QTY_MINUS_H;
        ctx.fill(aKx, aKy, aKx + QTY_PICK_W, aKy + QTY_MINUS_H,
                !cachedCanCraft ? C_PICK_OFF : kH ? C_PICK_HOVER : C_PICK_READY);
        drawBevelBorder(ctx, aKx, aKy, QTY_PICK_W, QTY_MINUS_H);
        var pm = ctx.getMatrices(); pm.push();
        try {
            pm.translate(aKx + QTY_PICK_W / 2f, aKy + QTY_MINUS_H / 2f, 0);
            pm.scale(0.75f, 0.75f, 1f);
            ctx.drawItem(cachedDisplayPickaxeStack, -8, -8);
        } finally {
            pm.pop();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Detail panel — REVOLVERS
    // ─────────────────────────────────────────────────────────────────────────

    private void drawRecipeDetail(DrawContext ctx, int mx, int my, AssemblyRecipe recipe) {
        int cx       = x + DETAIL_X + DETAIL_PAD;
        int mY       = y + RECIPE_ZONE_TOP + MODEL_OFFSET;
        int nameMaxW = BG_W - (DETAIL_X + DETAIL_PAD) - 4;

        drawTextFitted(ctx, Text.translatable(recipe.getTranslationKey()),
                cx, y + RECIPE_ZONE_TOP + NAME_OFFSET, nameMaxW, C_TEXT);

        // 3D model preview box
        int bc = C_MODEL_BORDER_NORMAL;
        ctx.fill(cx, mY, cx + MODEL_W, mY + MODEL_SIZE, C_MODEL_BG);
        ctx.fill(cx,               mY,                cx + MODEL_W, mY + 1,              bc);
        ctx.fill(cx,               mY,                cx + 1,       mY + MODEL_SIZE,     bc);
        ctx.fill(cx,               mY + MODEL_SIZE - 1, cx + MODEL_W, mY + MODEL_SIZE,   bc);
        ctx.fill(cx + MODEL_W - 1, mY,                cx + MODEL_W, mY + MODEL_SIZE,     bc);

        float autoYaw = FIXED_YAW - (float)((frameTimeMs % ROTATION_PERIOD_MS) * 360.0 / ROTATION_PERIOD_MS);
        renderGeckoModel(ctx, recipeIconStacks[selectedRecipe],
                cx + MODEL_W / 2, mY + MODEL_SIZE / 2, MODEL_SCALE, autoYaw, FIXED_PITCH);

        // Damage stat overlay
        if (recipe.getResult() instanceof BaseRevolverItem revolver) {
            Text dmgText = Text.translatable("gui.nst_revrecoiled.damage", revolver.getDamage());
            int dmgW = Math.min(textRenderer.getWidth(dmgText), MODEL_W - 4);
            int dmgX = cx + (MODEL_W - dmgW) / 2, dmgY = mY + MODEL_SIZE - 10;
            ctx.fill(dmgX - 1, dmgY - 1, dmgX + dmgW + 1, dmgY + 9, 0x88000000);
            drawTextFitted(ctx, dmgText, dmgX, dmgY, MODEL_W - 4, 0xFFFFAA00);
        }

        // Ingredient row with have/need counts
        int ingBaseY = mY + MODEL_SIZE + 4;
        var m = ctx.getMatrices(); m.push();
        try {
            m.translate(cx, ingBaseY, 0);
            m.scale(cachedIngScale, cachedIngScale, 1f);
            int ingOffX = 0;
            for (int i = 0; i < cachedIngredients.size(); i++) {
                AssemblyRecipe.Ingredient ing = cachedIngredients.get(i);
                int have = cachedIngCounts[i];
                int need = ing.count();
                ctx.drawItem(cachedIngStacks.get(i), ingOffX, 0);
                drawTextFitted(ctx, Text.literal(have + "/" + need), ingOffX + 18, 4, 28,
                        have >= need ? C_TEXT_OK : C_TEXT_MISSING);
                ingOffX += 48;
            }
        } finally {
            m.pop();
        }

        // Craft button
        int bx = x + CRAFT_BTN_X, by = y + CRAFT_BTN_Y;
        boolean hovered = mx >= bx && mx < bx + CRAFT_BTN_W && my >= by && my < by + CRAFT_BTN_H;
        ctx.fill(bx, by, bx + CRAFT_BTN_W, by + CRAFT_BTN_H,
                !cachedCanCraft ? C_CRAFT_OFF : hovered ? C_CRAFT_HOVER : C_CRAFT_READY);
        drawBevelBorder(ctx, bx, by, CRAFT_BTN_W, CRAFT_BTN_H);
        Text label    = Text.translatable("gui.nst_revrecoiled.assemble");
        int labelMaxW = CRAFT_BTN_W - 6;
        int labelW    = Math.min(textRenderer.getWidth(label), labelMaxW);
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
    private void renderGeckoModel(DrawContext ctx, ItemStack stack, int centerX, int centerY,
                                  float scale, float yawDeg, float pitchDeg) {
        if (!(stack.getItem() instanceof BaseRevolverItem revolver)) {
            ctx.drawItem(stack, centerX - 8, centerY - 8); return;
        }
        GeoRenderProvider provider = revolver.renderProvider.getValue();
        if (provider == null) return;
        GeoItemRenderer renderer = (GeoItemRenderer) provider.getGeoItemRenderer();
        if (renderer == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        VertexConsumerProvider.Immediate buffers = client.getBufferBuilders().getEntityVertexConsumers();
        ctx.enableScissor(centerX - MODEL_SIZE / 2, centerY - MODEL_SIZE / 2,
                centerX - MODEL_SIZE / 2 + MODEL_W, centerY - MODEL_SIZE / 2 + MODEL_SIZE);

        var m = ctx.getMatrices(); m.push();
        try {
            m.translate(centerX, centerY, 200.0);
            m.scale(scale, -scale, scale); // Y-flip to match GUI coordinate system
            m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitchDeg));
            m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawDeg));
            m.translate(-MODEL_PIVOT_X, -MODEL_PIVOT_Y, -MODEL_PIVOT_Z);

            DiffuseLighting.disableGuiDepthLighting();
            renderer.render(
                    new GeckolibSpecialRenderer.RenderData(stack.getItem(), stack),
                    ModelTransformationMode.NONE, m, buffers,
                    LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, false);
            buffers.draw();
            DiffuseLighting.enableGuiDepthLighting();
        } finally {
            m.pop();
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
    private void drawTextFitted(DrawContext ctx, Text text, int tx, int ty, int maxWidth, int color) {
        int w = textRenderer.getWidth(text);
        if (w <= maxWidth) { ctx.drawText(textRenderer, text, tx, ty, color, false); return; }
        var m = ctx.getMatrices(); m.push();
        try {
            m.translate(tx, ty, 0f);
            m.scale((float) maxWidth / w, (float) maxWidth / w, 1f);
            ctx.drawText(textRenderer, text, 0, 0, color, false);
        } finally {
            m.pop();
        }
    }

    /**
     * Draws a raised panel with a 1-pixel dark outer border, 1-pixel light highlight
     * on the bottom/right, and a 1-pixel bright inner highlight on top/left for depth.
     */
    private void drawPanel(DrawContext ctx, int px, int py, int pw, int ph, int bg) {
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
    private void drawBevelBorder(DrawContext ctx, int sx, int sy, int sw, int sh) {
        ctx.fill(sx,          sy,          sx + sw, sy + 1,  C_SLOT_SHADOW);
        ctx.fill(sx,          sy,          sx + 1,  sy + sh, C_SLOT_SHADOW);
        ctx.fill(sx,          sy + sh - 1, sx + sw, sy + sh, C_LIGHT);
        ctx.fill(sx + sw - 1, sy,          sx + sw, sy + sh, C_LIGHT);
    }
}
