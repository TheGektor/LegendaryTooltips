package com.anthonyhilyard.legendarytooltips.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.anthonyhilyard.iceberg.util.Tooltips;
import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig;
import com.anthonyhilyard.legendarytooltips.tooltip.TooltipScroll;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;

@Mixin(value = MouseHandler.class, priority = 999)
public class MouseHandlerMixin
{
	@Shadow
	@Final
	Minecraft minecraft;

	@Inject(method = "onScroll", cancellable = true,
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"))
	private void scrollTooltips(long windowHandle, double dx, double dy, CallbackInfo info)
	{
		if (Tooltips.anyTooltipsVisible() && LegendaryTooltipsConfig.shouldScrollTooltip())
		{
			if (windowHandle == minecraft.getWindow().getWindow())
			{
				boolean discrete = this.minecraft.options.discreteMouseScroll().get();
				double scrollSensitivity = this.minecraft.options.mouseWheelSensitivity().get();
				double scrollY = (discrete ? Math.signum(dy) : dy) * scrollSensitivity;
				TooltipScroll.scroll(-(float)scrollY);
				info.cancel();
			}
		}
	}
}
