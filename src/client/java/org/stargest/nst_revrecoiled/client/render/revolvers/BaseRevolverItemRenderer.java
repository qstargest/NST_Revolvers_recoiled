package org.stargest.nst_revrecoiled.client.render.revolvers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ModelTransformationMode;
import org.jetbrains.annotations.Nullable;
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
 * - Stack-based state supporting nested render calls (e.g. GUI preview inside world render)
 * - Efficient holder lookup — only the local player is ever rendered in first/third person;
 *   other entities' items are rendered without a living holder context
 * - Optional wrapping prevents NPE when items are rendered without a holder
 *   (inventory UI, item frames, dropped items on the ground)
 * - Exception-safe cleanup via try-finally guarantees stack balance even on render errors
 *
 * Rendering is single-threaded on the client, so static Deque storage is safe.
 */
public abstract class BaseRevolverItemRenderer<T extends BaseRevolverItem> extends GeoItemRenderer<T> {

    /**
     * Stack of current holder entities, one entry per active render call.
     * A Deque is used instead of a single field to correctly handle nested renders
     * (e.g. the Assembly Table GUI renders a revolver model inside a world frame).
     * Each entry is wrapped in Optional to distinguish "no holder" from an absent frame.
     */
    private static final Deque<Optional<LivingEntity>> holderStack = new ArrayDeque<>();

    /**
     * Returns the living entity currently being rendered holding this revolver.
     * Used by RevolverParticleHandler to calculate the correct particle spawn position.
     * Returns null when the stack is empty (no render in progress) or when the item
     * is being rendered without a holder (inventory, item frames, ground).
     *
     * @return the current holder, or null if unavailable
     */
    public static @Nullable LivingEntity getCurrentHolder() {
        Optional<LivingEntity> top = holderStack.peek();
        return top != null ? top.orElse(null) : null;
    }

    public BaseRevolverItemRenderer(GeoModel<T> model) {
        super(model);
    }

    /**
     * Renders the revolver and tracks its holder for particle keyframe events.
     * Pushes the resolved holder onto the stack before delegating to GeckoLib,
     * and pops it in a finally block to guarantee stack balance on exceptions.
     *
     * The holder is only non-null for first/third-person transform modes, where
     * the local player is the implied holder. All other modes (GUI, ground, fixed)
     * push Optional.empty() so getCurrentHolder() returns null safely.
     */
    @Override
    public void render(GeckolibSpecialRenderer.RenderData renderData, ModelTransformationMode transformType,
                       MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight,
                       int packedOverlay, boolean hasGlint) {

        LivingEntity holder = getLivingEntity(transformType);

        holderStack.push(Optional.ofNullable(holder));
        try {
            super.render(renderData, transformType, poseStack, bufferSource, packedLight, packedOverlay, hasGlint);
        } finally {
            holderStack.pop();
        }
    }

    /**
     * Resolves the holder entity for the given transform mode.
     * First/third-person modes imply the local player as holder.
     * All other modes (GUI, ground, fixed, none) return null — no holder is available.
     *
     * @param transformType the current item transform mode
     * @return the local player if rendering in hand, null otherwise
     */
    private static @Nullable LivingEntity getLivingEntity(ModelTransformationMode transformType) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (transformType == ModelTransformationMode.FIRST_PERSON_RIGHT_HAND
                || transformType == ModelTransformationMode.FIRST_PERSON_LEFT_HAND
                || transformType == ModelTransformationMode.THIRD_PERSON_RIGHT_HAND
                || transformType == ModelTransformationMode.THIRD_PERSON_LEFT_HAND) {
            return client.player;
        }
        return null;
    }
}
