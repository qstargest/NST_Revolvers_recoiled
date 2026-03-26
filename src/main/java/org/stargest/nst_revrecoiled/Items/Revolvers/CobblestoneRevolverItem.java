package org.stargest.nst_revrecoiled.Items.Revolvers;

import org.stargest.nst_revrecoiled.Items.BaseRevolverItem;

/**
 * Cobblestone-tier revolver implementation.
 * Deals 4.0 base damage plus bullet damage.
 */
public class CobblestoneRevolverItem extends BaseRevolverItem {

    private static final float COBBLESTONE_REVOLVER_DAMAGE = 4.0f;
    private static final int MAX_DURABILITY = 131;

    public CobblestoneRevolverItem(Settings settings) {
        super(settings, COBBLESTONE_REVOLVER_DAMAGE, MAX_DURABILITY);
    }
}