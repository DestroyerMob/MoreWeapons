package org.destroyermob.mobsmoreweapons.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public class MacheteItem extends SwordItem {
    public MacheteItem(Tier tier, Properties properties) {
        super(tier, properties, ShearsItem.createToolProperties());
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        return itemAbility == ItemAbilities.SHEARS_DIG || super.canPerformAction(stack, itemAbility);
    }
}
