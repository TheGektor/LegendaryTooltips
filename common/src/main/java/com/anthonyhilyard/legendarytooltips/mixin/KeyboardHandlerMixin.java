package com.anthonyhilyard.legendarytooltips.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.anthonyhilyard.legendarytooltips.LegendaryTooltips;

import net.minecraft.client.KeyboardHandler;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin
{
	@ModifyArg(method = "method_1454", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyReleased(III)Z"), index = 0)
	private static int releaseTooltipScroll2(int i, int j, int k)
	{
		if (LegendaryTooltips.scrollTooltips.matches(i, j))
		{
			LegendaryTooltips.scrollTooltipsKeyDown = false;
		}
		return i;
	}
}
