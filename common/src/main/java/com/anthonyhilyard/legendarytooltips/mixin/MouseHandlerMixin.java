package com.anthonyhilyard.legendarytooltips.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.anthonyhilyard.legendarytooltips.tooltip.TooltipScroll;

import net.minecraft.client.MouseHandler;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin
{
	@ModifyArg(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"), index = 0)
	private double scrollTooltips(double mouseX, double mouseY, double scrollX, double scrollY)
	{
		TooltipScroll.scroll(-(float)scrollY);
		return mouseX;
	}
}
