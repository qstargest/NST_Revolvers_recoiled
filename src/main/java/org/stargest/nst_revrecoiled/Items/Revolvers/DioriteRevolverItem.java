package org.stargest.nst_revrecoiled.Items.Revolvers;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Diorite-tier revolver implementation.
 * Deals 4.0 base damage plus bullet damage.
 */
public class DioriteRevolverItem extends BaseRevolverItem implements GeoItem {

    private static final float DIORITE_REVOLVER_DAMAGE = 4.0f;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Client-only renderer container (populated in client initializer)
    public final MutableObject<GeoRenderProvider> renderProvider = new MutableObject<>();

    public DioriteRevolverItem(Settings settings) {
        super(settings, DIORITE_REVOLVER_DAMAGE);
    }

    @Override
    public Predicate<ItemStack> getProjectiles() {
        return super.getProjectiles();
    }

    @Override
    protected void shoot(LivingEntity shooter, ProjectileEntity projectile, int index,
                         float speed, float divergence, float yaw, @Nullable LivingEntity target) {
        // Not used - shooting logic is handled in BaseRevolverItem
    }

    /**
     * GeckoLib calls this on both client and server.
     * Only the client-side renderer is set, so server-side this is a no-op.
     */
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        GeoRenderProvider provider = renderProvider.getValue();
        if (provider != null) {
            consumer.accept(provider);
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}