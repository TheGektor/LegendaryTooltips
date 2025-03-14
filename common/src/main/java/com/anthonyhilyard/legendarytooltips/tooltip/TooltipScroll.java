package com.anthonyhilyard.legendarytooltips.tooltip;

import org.spongepowered.asm.mixin.Unique;

public final class TooltipScroll
{
	@Unique
	private static float scrollOffset = 0.0f;

	private TooltipScroll() {}

	public static void scroll(float amount)
	{
		scrollOffset = scrollOffset + amount * 5.0f;
		scrollOffset = Math.clamp(scrollOffset, 0.0f, 100.0f);
	}

	public static float currentScroll()
	{
		return scrollOffset;
	}
}
