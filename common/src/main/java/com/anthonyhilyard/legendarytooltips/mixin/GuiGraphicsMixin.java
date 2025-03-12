package com.anthonyhilyard.legendarytooltips.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.anthonyhilyard.iceberg.util.ITooltipAccess;
import com.anthonyhilyard.iceberg.util.StringRecomposer;
import com.anthonyhilyard.iceberg.util.Tooltips;
import com.anthonyhilyard.iceberg.util.Tooltips.TooltipInfo;
import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.util.StringDecomposer;

@Mixin(value = GuiGraphics.class, priority = 1001)
public class GuiGraphicsMixin
{
	@ModifyVariable(method = "renderTooltipInternal", ordinal = 0, at = @At(value = "LOAD", ordinal = 0), argsOnly = true)
	private List<ClientTooltipComponent> mutableComponents(List<ClientTooltipComponent> components)
	{
		if (LegendaryTooltipsConfig.getInstance().centeredTitle.get())
		{
			return new ArrayList<>(components);
		}
		else
		{
			return components;
		}
	}

	@ModifyVariable(method = "renderTooltipInternal", ordinal = 2, at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 0))
	private int setMinimumWidth(int width)
	{
		if (LegendaryTooltipsConfig.getInstance().enforceMinimumWidth.get())
		{
			return Math.max(width, 48);
		}
		else
		{
			return width;
		}
	}

	@Inject(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 0))
	private void centerTitle(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info)
	{
		if (!components.isEmpty() && font != null && LegendaryTooltipsConfig.getInstance().centeredTitle.get())
		{
			// Calculate tooltip width first.
			int tooltipWidth = 0;
			if (LegendaryTooltipsConfig.getInstance().enforceMinimumWidth.get())
			{
				tooltipWidth = 48;
			}

			tooltipWidth = new TooltipInfo(components, font, 1).getMaxLineWidth(tooltipWidth);

			// Replace the first component with the newly-centered version.
			List<ClientTooltipComponent> centeredComponents = Tooltips.centerTitle(components, font, tooltipWidth, Tooltips.calculateTitleLines(components));
			components.clear();
			components.addAll(centeredComponents);
		}
	}

	@Unique
	private static int arsNouveauOffsetX = 0;

	@Unique
	private static int arsNouveauOffsetY = 0;

	@Redirect(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;getWidth(Lnet/minecraft/client/gui/Font;)I"))
	private int arsNouveauCompatGetWidthProxy(ClientTooltipComponent instance, Font font, Font font2, List<ClientTooltipComponent> list, int i, int j, ClientTooltipPositioner clientTooltipPositioner)
	{
		// If this is an Ars Nouveau School tooltip component, nudge it over if the title is being centered.
		if (instance.getClass().getName().contentEquals("com.hollingsworth.arsnouveau.client.gui.SchoolTooltip$SchoolTooltipRenderer"))
		{
			// Find the title, which is the first text component.
			for (ClientTooltipComponent component : list)
			{
				if (component instanceof ClientTextTooltip title)
				{
					boolean showModel = LegendaryTooltipsConfig.showModelForItem(((ITooltipAccess)(Object)(this)).getIcebergTooltipStack());
					if (LegendaryTooltipsConfig.getInstance().centeredTitle.get() || showModel)
					{
						String titleText = StringDecomposer.getPlainText(StringRecomposer.recompose(List.of(title)).get(0));

						// Get the width of the initial spaces before the title.
						String initialSpaces = titleText.substring(0, titleText.indexOf(titleText.strip()));
						arsNouveauOffsetX = font.width(initialSpaces);
					}
					else
					{
						arsNouveauOffsetX = 0;
					}

					if (showModel)
					{
						arsNouveauOffsetY = -title.getHeight();
					}
					else
					{
						arsNouveauOffsetY = 0;
					}

					return instance.getWidth(font) + arsNouveauOffsetX;
				}
			}
		}

		return instance.getWidth(font);
	}

	@Redirect(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;renderImage(Lnet/minecraft/client/gui/Font;IILnet/minecraft/client/gui/GuiGraphics;)V"))
	private void arsNouveauCompatRenderImageProxy(ClientTooltipComponent instance, Font font, int x, int y, GuiGraphics guiGraphics)
	{
		if (instance.getClass().getName().contentEquals("com.hollingsworth.arsnouveau.client.gui.SchoolTooltip$SchoolTooltipRenderer"))
		{
			instance.renderImage(font, x + arsNouveauOffsetX, y + arsNouveauOffsetY, guiGraphics);
		}
		else
		{
			instance.renderImage(font, x, y, guiGraphics);
		}
	}
}