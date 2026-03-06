package org.stargest.nst_revrecoiled.client.render.revolvers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeckolibSpecialRenderer;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Base GeckoLib renderer for all revolver items.
 * Tracks the current holder entity before each render so that
 * RevolverParticleHandler can resolve it, since DataTickets.ENTITY
 * is not populated for item animatables in GeckoLib 4.8.5.
 *
 * Key features:
 * - Holder tracking for particle spawn positioning
 * - Thread-safe single-threaded rendering
 * - Efficient holder lookup (checks local player first)
 * - Type-safe comparison using item identity
 */
public abstract class BaseRevolverItemRenderer<T extends BaseRevolverItem> extends GeoItemRenderer<T> {

    /**
     * Currently rendering holder entity.
     * Static is safe because rendering happens on a single thread.
     */
    private static LivingEntity currentHolder = null;

    /**
     * Gets the current holder entity being rendered.
     * Used by RevolverParticleHandler to determine particle spawn position.
     *
     * @return The living entity holding the revolver, or null if not found
     */
    public static @Nullable LivingEntity getCurrentHolder() {
        return currentHolder;
    }

    public BaseRevolverItemRenderer(GeoModel<T> model) {
        super(model);
    }

    /**
     * Renders the revolver and tracks its holder for particle effects.
     * Searches for the holder entity before rendering to support particle keyframe events.
     */
    @Override
    public void render(GeckolibSpecialRenderer.RenderData renderData, ModelTransformationMode transformType,
                       MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight,
                       int packedOverlay, boolean hasGlint) {

        MinecraftClient client = MinecraftClient.getInstance();
        ItemStack stack = renderData.itemstack();

        // Find the item holder
        currentHolder = null;
        if (client.world != null) {
            // Check local player first (optimization for common case)
            if (client.player != null && isHolding(client.player, stack)) {
                currentHolder = client.player;
            } else {
                // Search among other players in the world
                for (PlayerEntity player : client.world.getPlayers()) {
                    if (isHolding(player, stack)) {
                        currentHolder = player;
                        break;
                    }
                }
            }
        }

        super.render(renderData, transformType, poseStack, bufferSource, packedLight, packedOverlay, hasGlint);

        // Note: We don't clear currentHolder here because particle keyframe events
        // may need it during the same render frame. It will be overwritten on next render.
    }

    /**
     * Checks if an entity is holding this item stack.
     * Compares item types rather than stack instances since ItemStack may be a copy.
     *
     * @param entity Entity to check
     * @param stack Item stack to look for
     * @return true if entity is holding this item in either hand
     */
    private boolean isHolding(LivingEntity entity, ItemStack stack) {
        return entity.getMainHandStack().getItem() == stack.getItem() ||
                entity.getOffHandStack().getItem() == stack.getItem();
    }
}
