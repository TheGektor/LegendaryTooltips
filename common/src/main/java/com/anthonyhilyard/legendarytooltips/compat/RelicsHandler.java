package com.anthonyhilyard.legendarytooltips.compat;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

import it.hurts.sskirillss.relics.items.relics.base.IRelicItem;

@SuppressWarnings("deprecation")
public final class RelicsHandler
{
	public static boolean hasTooltipDecor(ItemStack itemStack, LocalPlayer player)
	{
		if (itemStack.getItem() instanceof IRelicItem relic &&
			relic.getStyleData().getTooltip().apply(player, itemStack).isTextured())
		{
			return true;
		}

		return false;
	}
}
