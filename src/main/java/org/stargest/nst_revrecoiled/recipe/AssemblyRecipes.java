package org.stargest.nst_revrecoiled.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.stargest.nst_revrecoiled.NstRevRecoiled;
import org.stargest.nst_revrecoiled.util.ModItems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Static registry of all available Assembly Table recipes.
 * Initialized during mod setup and frozen once complete.
 *
 * Contains pre-defined recipes for the base mod's revolvers and bullets.
 * Addon mods can register their own recipes via addRevolver() and addBullet()
 * during initialization, before freeze() is called.
 *
 * Recipes are stored in declaration order, which dictates their display order
 * in the Assembly Table GUI.
 */
public class AssemblyRecipes {

    private static final List<AssemblyRecipe> RECIPES = new ArrayList<>();
    private static boolean frozen = false;

    public static final AssemblyRecipe COBBLESTONE_REVOLVER = add(new AssemblyRecipe(
           new ResourceLocation(NstRevRecoiled.MOD_ID, "cobblestone_revolver"),
            ModItems.COBBLESTONE_REVOLVER.get(),
            "item.nst_revrecoiled.cobblestone_revolver",
            List.of(
                    new AssemblyRecipe.Ingredient(Items.COBBLESTONE, 16),
                    new AssemblyRecipe.Ingredient(Items.SPRUCE_PLANKS,   2),
                    new AssemblyRecipe.Ingredient(Items.GUNPOWDER,    3)
            )
    ));

    public static final AssemblyRecipe IRON_REVOLVER = add(new AssemblyRecipe(
            new ResourceLocation(NstRevRecoiled.MOD_ID, "iron_revolver"),
            ModItems.IRON_REVOLVER.get(),
            "item.nst_revrecoiled.iron_revolver",
            List.of(
                    new AssemblyRecipe.Ingredient(Items.IRON_INGOT,  10),
                    new AssemblyRecipe.Ingredient(Items.SPRUCE_PLANKS,   2),
                    new AssemblyRecipe.Ingredient(Items.GUNPOWDER,    6)
            )
    ));

    public static final AssemblyRecipe GOLDEN_REVOLVER = add(new AssemblyRecipe(
            new ResourceLocation(NstRevRecoiled.MOD_ID, "golden_revolver"),
            ModItems.GOLDEN_REVOLVER.get(),
            "item.nst_revrecoiled.golden_revolver",
            List.of(
                    new AssemblyRecipe.Ingredient(Items.GOLD_INGOT,  14),
                    new AssemblyRecipe.Ingredient(Items.SPRUCE_PLANKS,   2),
                    new AssemblyRecipe.Ingredient(Items.GUNPOWDER,    8)
            )
    ));

    public static final AssemblyRecipe DIAMOND_REVOLVER = add(new AssemblyRecipe(
            new ResourceLocation(NstRevRecoiled.MOD_ID, "diamond_revolver"),
            ModItems.DIAMOND_REVOLVER.get(),
            "item.nst_revrecoiled.diamond_revolver",
            List.of(
                    new AssemblyRecipe.Ingredient(Items.DIAMOND,     5),
                    new AssemblyRecipe.Ingredient(Items.SPRUCE_PLANKS,  2),
                    new AssemblyRecipe.Ingredient(Items.GUNPOWDER,   12)
            )
    ));

    public static final BulletAssemblyRecipe STONE_BULLET = addBullet(new BulletAssemblyRecipe(
            new ResourceLocation(NstRevRecoiled.MOD_ID, "stone_bullet"),
            ModItems.STONE_BULLET.get(),
            "item.nst_revrecoiled.stone_bullet",
            List.of(
                    new AssemblyRecipe.Ingredient(Items.COBBLESTONE, 1),
                    new AssemblyRecipe.Ingredient(Items.GUNPOWDER,   1)
            ),
            PickaxeTier.ANY
    ));

    public static final BulletAssemblyRecipe IRON_BULLET = addBullet(new BulletAssemblyRecipe(
            new ResourceLocation(NstRevRecoiled.MOD_ID, "iron_bullet"),
            ModItems.IRON_BULLET.get(),
            "item.nst_revrecoiled.iron_bullet",
            List.of(
                    new AssemblyRecipe.Ingredient(Items.IRON_INGOT, 1),
                    new AssemblyRecipe.Ingredient(Items.GUNPOWDER,  1)
            ),
            PickaxeTier.STONE_PLUS
    ));

    public static final BulletAssemblyRecipe GOLDEN_BULLET = addBullet(new BulletAssemblyRecipe(
            new ResourceLocation(NstRevRecoiled.MOD_ID, "golden_bullet"),
            ModItems.GOLDEN_BULLET.get(),
            "item.nst_revrecoiled.golden_bullet",
            List.of(
                    new AssemblyRecipe.Ingredient(Items.GOLD_INGOT, 1),
                    new AssemblyRecipe.Ingredient(Items.GUNPOWDER,  1)
            ),
            PickaxeTier.IRON_PLUS
    ));

    public static final BulletAssemblyRecipe DIAMOND_BULLET = addBullet(new BulletAssemblyRecipe(
            new ResourceLocation(NstRevRecoiled.MOD_ID, "diamond_bullet"),
            ModItems.DIAMOND_BULLET.get(),
            "item.nst_revrecoiled.diamond_bullet",
            List.of(
                    new AssemblyRecipe.Ingredient(Items.DIAMOND,   1),
                    new AssemblyRecipe.Ingredient(Items.GUNPOWDER, 1)
            ),
            PickaxeTier.IRON_PLUS
    ));

    private static AssemblyRecipe add(AssemblyRecipe recipe) {
        RECIPES.add(recipe);
        return recipe;
    }

    private static BulletAssemblyRecipe addBullet(BulletAssemblyRecipe recipe) {
        RECIPES.add(recipe);
        return recipe;
    }

    public static void register(AssemblyRecipe recipe) {
        if (frozen) throw new IllegalStateException(
                "Cannot register recipes after AssemblyRecipes is frozen"
        );
        RECIPES.add(recipe);
    }

    public static void freeze() {
        if (frozen) return;
        frozen = true;
    }

    public static boolean isFrozen() { return frozen; }

    public static List<AssemblyRecipe> getAll() {
        return Collections.unmodifiableList(RECIPES);
    }

    public static Optional<AssemblyRecipe> getById(ResourceLocation id) {
        return RECIPES.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst();
    }
}
