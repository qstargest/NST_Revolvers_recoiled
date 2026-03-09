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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * Base GeckoLib renderer for all revolver items.
 * Tracks the current holder entity before each render so that
 * RevolverParticleHandler can resolve it, since DataTickets.ENTITY
 * is not populated for item animatables in GeckoLib 4.8.5.
 *
 * Key features:
 * - Holder tracking for particle spawn positioning
 * - Thread-safe single-threaded rendering with stack-based state
 * - Efficient holder lookup (checks local player first)
 * - Type-safe comparison using item identity
 * - Exception-safe cleanup using try-finally pattern
 * - NPE-safe handling when item is rendered without holder (e.g., in inventory)
 */
public abstract class BaseRevolverItemRenderer<T extends BaseRevolverItem> extends GeoItemRenderer<T> {

    /**
     * Stack-based holder tracking for nested render calls.
     * Uses Deque, Optional & LivingEntity to support null holders safely.
     * Wrapped in Optional to prevent NullPointerException when items are rendered
     * without a holder (e.g., in inventory UI, item frames, ground).
     * Rendering is single-threaded, so static storage is safe.
     */
    private static final Deque<Optional<LivingEntity>> holderStack = new ArrayDeque<>();

    /**
     * Gets the current holder entity being rendered.
     * Used by RevolverParticleHandler to determine particle spawn position.
     *
     * @return The living entity holding the revolver, or null if stack is empty or holder not found
     */
    public static @Nullable LivingEntity getCurrentHolder() {
        Optional<LivingEntity> top = holderStack.peek();
        return top != null ? top.orElse(null) : null;
    }

    public BaseRevolverItemRenderer(GeoModel<T> model) {
        super(model);
    }

    /**
     * Renders the revolver and tracks its holder for particle effects.
     * Searches for the holder entity before rendering to support particle keyframe events.
     * Uses try-finally to ensure stack cleanup even if rendering throws an exception.
     * Wraps holder in Optional to safely handle cases where no holder exists.
     */
    @Override
    public void render(GeckolibSpecialRenderer.RenderData renderData, ModelTransformationMode transformType,
                       MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight,
                       int packedOverlay, boolean hasGlint) {

        MinecraftClient client = MinecraftClient.getInstance();
        ItemStack stack = renderData.itemstack();

        LivingEntity holder = null;
        if (client.world != null) {
            // Check local player first (optimization for common case)
            if (client.player != null && isHolding(client.player, stack)) {
                holder = client.player;
            } else {
                // Search among other players in the world
                for (PlayerEntity player : client.world.getPlayers()) {
                    if (isHolding(player, stack)) {
                        holder = player;
                        break;
                    }
                }
            }
        }

        // Push holder onto stack wrapped in Optional (prevents NPE when holder is null)
        holderStack.push(Optional.ofNullable(holder));
        try {
            super.render(renderData, transformType, poseStack, bufferSource, packedLight, packedOverlay, hasGlint);
        } finally {
            // Guaranteed cleanup even if rendering throws exception
            holderStack.pop();
        }
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
