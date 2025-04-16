package com.anthonyhilyard.legendarytooltips.mixin.emi;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig;

import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.runtime.EmiDrawContext;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;


@Mixin(value = EmiRenderHelper.class, remap = false)
public class EmiRenderHelperMixin
{
	@Unique
	private static List<ClientTooltipComponent> storedComponents;

	@Unique
	private static int storedMaxWidth;

	@Inject(method = "drawTooltip(Lnet/minecraft/client/gui/screens/Screen;Ldev/emi/emi/runtime/EmiDrawContext;Ljava/util/List;IIILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V",
		at = @At("HEAD"), require = 0)
	private static void storeComponents(Screen screen, EmiDrawContext context, List<ClientTooltipComponent> components, int x, int y, int maxWidth, ClientTooltipPositioner positioner, CallbackInfo info)
	{
		storedComponents = components;
		storedMaxWidth = maxWidth;
	}

	@ModifyArg(method = "drawTooltip(Lnet/minecraft/client/gui/screens/Screen;Ldev/emi/emi/runtime/EmiDrawContext;Ljava/util/List;IIILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V",
		at = @At(value = "INVOKE", target = "Ldev/emi/emi/mixin/accessor/DrawContextAccessor;invokeDrawTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V"),
		index = 1, require = 0)
	private static List<ClientTooltipComponent> cancelWrapping(List<ClientTooltipComponent> mutable)
	{
		if (LegendaryTooltipsConfig.getMaxTooltipWidth() < storedMaxWidth)
		{
			return new ArrayList<>(storedComponents);
		}

		return mutable;
	}
}
