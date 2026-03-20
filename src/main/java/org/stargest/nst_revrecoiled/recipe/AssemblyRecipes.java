package org.stargest.nst_revrecoiled.recipe;

import net.minecraft.item.Items;
import org.stargest.nst_revrecoiled.util.ModItems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Static registry of all Assembly Table recipes.
 *
 * Every AssemblyRecipe and BulletAssemblyRecipe is declared as a public static final
 * constant so other systems can reference individual recipes by name when needed.
 * All constants are automatically added to the shared RECIPES list via the private
 * add() and addBullet() helpers at class load time.
 *
 * Recipes are ordered: revolvers first, then bullets.
 * Declaration order determines the indices used by AssemblyCraftC2SPacket —
 * do not reorder existing entries.
 */
public class AssemblyRecipes {

    /** Master list of all registered recipes, in declaration order. */
    private static final List<AssemblyRecipe> RECIPES = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Revolvers
    // -------------------------------------------------------------------------

    public static final AssemblyRecipe COBBLESTONE_REVOLVER = add(new AssemblyRecipe(
            "cobblestone_revolver", ModItems.COBBLESTONE_REVOLVER,
            "item.nst_revrecoiled.cobblestone_revolver",
            List.of(
                    new AssemblyRecipe.Ingredient(Items.COBBLESTONE, 10),
                    new AssemblyRecipe.Ingredient(Items.OAK_PLANKS,   3),
                    new AssemblyRecipe.Ingredient(Items.GUNPOWDER,    5)
            )
    ));

    public static final AssemblyRecipe IRON_REVOLVER = add(new AssemblyRecipe(
            "iron_revolver", ModItems.IRON_REVOLVER,
            "item.nst_revrecoiled.iron_revolver",
            List.of(
                    new AssemblyRecipe.Ingredient(Items.IRON_INGOT,  12),
                    new AssemblyRecipe.Ingredient(Items.OAK_PLANKS,   3),
                    new AssemblyRecipe.Ingredient(Items.GUNPOWDER,    6)
            )
    ));

    public static final AssemblyRecipe GOLDEN_REVOLVER = add(new AssemblyRecipe(
            "golden_revolver", ModItems.GOLDEN_REVOLVER,
            "item.nst_revrecoiled.golden_revolver",
            List.of(
                    new AssemblyRecipe.Ingredient(Items.GOLD_INGOT,  12),
                    new AssemblyRecipe.Ingredient(Items.OAK_PLANKS,   3),
                    new AssemblyRecipe.Ingredient(Items.GUNPOWDER,    7)
            )
    ));

    public static final AssemblyRecipe DIAMOND_REVOLVER = add(new AssemblyRecipe(
            "diamond_revolver", ModItems.DIAMOND_REVOLVER,
            "item.nst_revrecoiled.diamond_revolver",
            List.of(
                    new AssemblyRecipe.Ingredient(Items.DIAMOND,     8),
                    new AssemblyRecipe.Ingredient(Items.OAK_PLANKS,  3),
                    new AssemblyRecipe.Ingredient(Items.GUNPOWDER,   7)
            )
    ));

    // -------------------------------------------------------------------------
    // Bullets
    // -------------------------------------------------------------------------

    public static final BulletAssemblyRecipe STONE_BULLET = addBullet(new BulletAssemblyRecipe(
            "stone_bullet", ModItems.STONE_BULLET,
            "item.nst_revrecoiled.stone_bullet",
            List.of(
                    new AssemblyRecipe.Ingredient(Items.COBBLESTONE, 1),
                    new AssemblyRecipe.Ingredient(Items.GUNPOWDER,   1)
            ),
            BulletAssemblyRecipe.PickaxeTier.ANY
    ));

    public static final BulletAssemblyRecipe IRON_BULLET = addBullet(new BulletAssemblyRecipe(
            "iron_bullet", ModItems.IRON_BULLET,
            "item.nst_revrecoiled.iron_bullet",
            List.of(
                    new AssemblyRecipe.Ingredient(Items.IRON_INGOT, 1),
                    new AssemblyRecipe.Ingredient(Items.GUNPOWDER,  1)
            ),
            BulletAssemblyRecipe.PickaxeTier.STONE_PLUS
    ));

    public static final BulletAssemblyRecipe GOLDEN_BULLET = addBullet(new BulletAssemblyRecipe(
            "golden_bullet", ModItems.GOLDEN_BULLET,
            "item.nst_revrecoiled.golden_bullet",
            List.of(
                    new AssemblyRecipe.Ingredient(Items.GOLD_INGOT, 1),
                    new AssemblyRecipe.Ingredient(Items.GUNPOWDER,  1)
            ),
            BulletAssemblyRecipe.PickaxeTier.IRON_PLUS
    ));

    public static final BulletAssemblyRecipe DIAMOND_BULLET = addBullet(new BulletAssemblyRecipe(
            "diamond_bullet", ModItems.DIAMOND_BULLET,
            "item.nst_revrecoiled.diamond_bullet",
            List.of(
                    new AssemblyRecipe.Ingredient(Items.DIAMOND,   1),
                    new AssemblyRecipe.Ingredient(Items.GUNPOWDER, 1)
            ),
            BulletAssemblyRecipe.PickaxeTier.IRON_PLUS
    ));

    // -------------------------------------------------------------------------

    /**
     * Registers a revolver recipe and returns it for field assignment.
     *
     * @param recipe the recipe to register
     * @return the same recipe instance
     */
    private static AssemblyRecipe add(AssemblyRecipe recipe) {
        RECIPES.add(recipe);
        return recipe;
    }

    /**
     * Registers a bullet recipe and returns it for field assignment.
     *
     * @param recipe the bullet recipe to register
     * @return the same recipe instance
     */
    private static BulletAssemblyRecipe addBullet(BulletAssemblyRecipe recipe) {
        RECIPES.add(recipe);
        return recipe;
    }

    /**
     * Returns an unmodifiable view of all registered recipes in declaration order.
     * The index of each recipe corresponds to the recipeIndex field in
     * AssemblyCraftC2SPacket payloads — do not rely on this order being stable
     * across versions if packets can be replayed.
     *
     * @return unmodifiable list of all recipes
     */
    public static List<AssemblyRecipe> getAll() {
        return Collections.unmodifiableList(RECIPES);
    }
}