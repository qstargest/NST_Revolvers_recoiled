package org.stargest.nst_revrecoiled.recipe;

import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.stargest.nst_revrecoiled.Main;
import org.stargest.nst_revrecoiled.util.ModItems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Static registry of all Assembly Table recipes.
 *
 * Every AssemblyRecipe and BulletAssemblyRecipe is declared as a public static final
 * constant so other systems can reference individual recipes by name when needed.
 * All constants are automatically added to the shared RECIPES list via the private
 * add() and addBullet() helpers at class load time.
 *
 * Recipes are ordered: revolvers first, then bullets.
 * Each recipe is identified by a namespaced Identifier, allowing addon mods to
 * register their own recipes under a separate namespace via register().
 *
 * The registry supports a freeze mechanism: the base mod calls freeze() after
 * registering all built-in recipes, and addon mods must register their recipes
 * before freeze() is called (i.e. during their own ModInitializer). Attempting
 * to register after freezing throws IllegalStateException.
 *
 * Recipes can be looked up by Identifier via getById(), which addon mods and
 * the network layer use to resolve craft requests without relying on list indices.
 */
public class AssemblyRecipes {

    /** Master list of all registered recipes, in declaration order. */
    private static final List<AssemblyRecipe> RECIPES = new ArrayList<>();

    /**
     * When true, register() throws IllegalStateException.
     * Set by freeze() after all built-in recipes are added.
     */
    private static boolean frozen = false;

    // -------------------------------------------------------------------------
    // Revolvers
    // -------------------------------------------------------------------------

    public static final AssemblyRecipe COBBLESTONE_REVOLVER = add(new AssemblyRecipe(
            Identifier.of(Main.MOD_ID, "cobblestone_revolver"), ModItems.COBBLESTONE_REVOLVER,
            "item.nst_revrecoiled.cobblestone_revolver",
            List.of(
                    new AssemblyRecipe.Ingredient(Items.COBBLESTONE, 16),
                    new AssemblyRecipe.Ingredient(Items.SPRUCE_PLANKS,   2),
                    new AssemblyRecipe.Ingredient(Items.GUNPOWDER,    3)
            )
    ));

    public static final AssemblyRecipe IRON_REVOLVER = add(new AssemblyRecipe(
            Identifier.of(Main.MOD_ID, "iron_revolver"), ModItems.IRON_REVOLVER,
            "item.nst_revrecoiled.iron_revolver",
            List.of(
                    new AssemblyRecipe.Ingredient(Items.IRON_INGOT,  10),
                    new AssemblyRecipe.Ingredient(Items.SPRUCE_PLANKS,   2),
                    new AssemblyRecipe.Ingredient(Items.GUNPOWDER,    6)
            )
    ));

    public static final AssemblyRecipe GOLDEN_REVOLVER = add(new AssemblyRecipe(
            Identifier.of(Main.MOD_ID, "golden_revolver"), ModItems.GOLDEN_REVOLVER,
            "item.nst_revrecoiled.golden_revolver",
            List.of(
                    new AssemblyRecipe.Ingredient(Items.GOLD_INGOT,  14),
                    new AssemblyRecipe.Ingredient(Items.SPRUCE_PLANKS,   2),
                    new AssemblyRecipe.Ingredient(Items.GUNPOWDER,    8)
            )
    ));

    public static final AssemblyRecipe DIAMOND_REVOLVER = add(new AssemblyRecipe(
            Identifier.of(Main.MOD_ID, "diamond_revolver"), ModItems.DIAMOND_REVOLVER,
            "item.nst_revrecoiled.diamond_revolver",
            List.of(
                    new AssemblyRecipe.Ingredient(Items.DIAMOND,     5),
                    new AssemblyRecipe.Ingredient(Items.SPRUCE_PLANKS,  2),
                    new AssemblyRecipe.Ingredient(Items.GUNPOWDER,   12)
            )
    ));

    // -------------------------------------------------------------------------
    // Bullets
    // -------------------------------------------------------------------------

    public static final BulletAssemblyRecipe STONE_BULLET = addBullet(new BulletAssemblyRecipe(
            Identifier.of(Main.MOD_ID, "stone_bullet"), ModItems.STONE_BULLET,
            "item.nst_revrecoiled.stone_bullet",
            List.of(
                    new AssemblyRecipe.Ingredient(Items.COBBLESTONE, 1),
                    new AssemblyRecipe.Ingredient(Items.GUNPOWDER,   1)
            ),
            PickaxeTier.ANY
    ));

    public static final BulletAssemblyRecipe IRON_BULLET = addBullet(new BulletAssemblyRecipe(
            Identifier.of(Main.MOD_ID, "iron_bullet"), ModItems.IRON_BULLET,
            "item.nst_revrecoiled.iron_bullet",
            List.of(
                    new AssemblyRecipe.Ingredient(Items.IRON_INGOT, 1),
                    new AssemblyRecipe.Ingredient(Items.GUNPOWDER,  1)
            ),
            PickaxeTier.STONE_PLUS
    ));

    public static final BulletAssemblyRecipe GOLDEN_BULLET = addBullet(new BulletAssemblyRecipe(
            Identifier.of(Main.MOD_ID, "golden_bullet"), ModItems.GOLDEN_BULLET,
            "item.nst_revrecoiled.golden_bullet",
            List.of(
                    new AssemblyRecipe.Ingredient(Items.GOLD_INGOT, 1),
                    new AssemblyRecipe.Ingredient(Items.GUNPOWDER,  1)
            ),
            PickaxeTier.IRON_PLUS
    ));

    public static final BulletAssemblyRecipe DIAMOND_BULLET = addBullet(new BulletAssemblyRecipe(
            Identifier.of(Main.MOD_ID, "diamond_bullet"), ModItems.DIAMOND_BULLET,
            "item.nst_revrecoiled.diamond_bullet",
            List.of(
                    new AssemblyRecipe.Ingredient(Items.DIAMOND,   1),
                    new AssemblyRecipe.Ingredient(Items.GUNPOWDER, 1)
            ),
            PickaxeTier.IRON_PLUS
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
     * Registers a custom recipe from an addon mod.
     * Must be called during mod initialization, before the base mod calls freeze().
     * Addon mods should register recipes from their own ModInitializer.
     *
     * @param recipe the recipe to register
     * @throws IllegalStateException if called after the registry has been frozen
     */
    public static void register(AssemblyRecipe recipe) {
        if (frozen) throw new IllegalStateException(
                "Cannot register recipes after AssemblyRecipes is frozen"
        );
        RECIPES.add(recipe);
    }

    /**
     * Freezes the registry, preventing further registration.
     * Called once by the base mod's main initializer after all built-in recipes are added.
     * Addon mods must register their recipes before this point.
     * Calling freeze() when already frozen is a no-op.
     */
    public static void freeze() {
        if (frozen) return;
        frozen = true;
    }

    /** @return true if the registry has been frozen and no further registration is allowed */
    public static boolean isFrozen() { return frozen; }

    /**
     * Returns an unmodifiable view of all registered recipes in declaration order.
     * Used by AssemblyTableScreen to populate the recipe list and by
     * AssemblyCraftC2SPacket for index-based bullet craft requests.
     *
     * @return unmodifiable list of all recipes
     */
    public static List<AssemblyRecipe> getAll() {
        return Collections.unmodifiableList(RECIPES);
    }

    /**
     * Looks up a recipe by its namespaced Identifier.
     * Used by AssemblyCraftC2SPacket to resolve revolver craft requests server-side,
     * decoupling the packet from list indices so recipe order changes don't break
     * in-flight packets across versions.
     *
     * @param id the recipe identifier to look up
     * @return the matching recipe, or empty if not found
     */
    public static Optional<AssemblyRecipe> getById(Identifier id) {
        return RECIPES.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst();
    }
}