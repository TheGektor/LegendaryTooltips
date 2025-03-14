package com.anthonyhilyard.legendarytooltips.mixin;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.anthonyhilyard.iceberg.util.ITooltipAccess;
import com.anthonyhilyard.iceberg.util.StringRecomposer;
import com.anthonyhilyard.iceberg.util.Tooltips;
import com.anthonyhilyard.iceberg.util.Tooltips.TooltipInfo;
import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig;
import com.anthonyhilyard.legendarytooltips.tooltip.PaddingComponent;
import com.anthonyhilyard.legendarytooltips.tooltip.TooltipScroll;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.util.StringDecomposer;

@Debug(export = true)
@Mixin(value = GuiGraphics.class, priority = 1001)
public class GuiGraphicsMixin
{
	@Unique
	private int numTitleLines = 0;

	@Unique
	private int titleStart = 0;

	@Unique
	private TooltipInfo currentTooltipInfo = null;

	@Unique
	private Rect2i tooltipRect = null;

	@Unique
	private int tooltipX = 0;

	@Unique
	private int tooltipY = 0;

	@Unique
	private int startScrollIndex = 0;

	@Shadow
	private boolean managed;

	@ModifyVariable(method = "renderTooltipInternal", ordinal = 0, at = @At(value = "LOAD", ordinal = 0), argsOnly = true)
	private List<ClientTooltipComponent> mutableComponents(List<ClientTooltipComponent> components)
	{
		numTitleLines = Tooltips.calculateTitleLines(components);
		titleStart = Tooltips.calculateTitleStart(components);
		startScrollIndex = titleStart + numTitleLines;

		// If there is a padding component, add one to the scroll start index.
		if (components.stream().anyMatch(component -> component instanceof PaddingComponent))
		{
			startScrollIndex++;
		}

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
		GuiGraphics self = (GuiGraphics)(Object)this;
		currentTooltipInfo = new TooltipInfo(components, font, numTitleLines);
		tooltipX = x;
		tooltipY = y;

		if (!components.isEmpty() && font != null && LegendaryTooltipsConfig.getInstance().centeredTitle.get())
		{
			// Calculate tooltip width first.
			int tooltipWidth = 0;
			if (LegendaryTooltipsConfig.getInstance().enforceMinimumWidth.get())
			{
				tooltipWidth = 48;
			}

			Minecraft minecraft = Minecraft.getInstance();

			int screenWidth = minecraft.getWindow().getGuiScaledWidth();
			int screenHeight = minecraft.getWindow().getGuiScaledHeight();

			tooltipRect = Tooltips.calculateRect(((ITooltipAccess)this).getIcebergTooltipStack(), self, positioner, components, x, y, screenWidth, screenHeight, 0, font, tooltipWidth, LegendaryTooltipsConfig.getInstance().centeredTitle.get());

			tooltipWidth = currentTooltipInfo.getMaxLineWidth(tooltipWidth);

			// Replace the first components with the newly-centered versions.
			List<ClientTooltipComponent> centeredComponents = Tooltips.centerTitle(components, font, tooltipWidth, numTitleLines);
			components.clear();
			components.addAll(centeredComponents);
		}
	}

	@Unique
	private boolean enableScissor = false;

	@Unique
	private boolean scissorEnabled = false;

	@Unique
	private void startScissor(int x, int y, int width, int height)
	{
		GuiGraphics self = (GuiGraphics)(Object)this;
		self.flush();
		managed = true;
		self.enableScissor(x - 1, y - 1, x + width + 1, y + height - (y - tooltipY) + 1);

		self.pose().pushPose();
		self.pose().translate(0.0f, -TooltipScroll.currentScroll(), 0.0f);

		scissorEnabled = true;
		enableScissor = false;
	}

	@Unique
	private void stopScissor()
	{
		GuiGraphics self = (GuiGraphics)(Object)this;
		self.pose().popPose();

		self.disableScissor();
		managed = false;
		self.flush();
		scissorEnabled = false;
	}

	@ModifyArg(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;"))
	private int handleTooltipScroll(int index)
	{
		if (index <= startScrollIndex && scissorEnabled)
		{
			stopScissor();
		}
		// Enable scissor test.
		else if (index == startScrollIndex)
		{
			enableScissor = true;
		}
		return index;
	}

	@ModifyArg(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;renderText(Lnet/minecraft/client/gui/Font;IILorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V"), index = 0)
	private Font scrollText(Font font, int currentX, int currentY, Matrix4f matrix4f, MultiBufferSource.BufferSource bufferSource)
	{
		if (enableScissor)
		{
			startScissor(currentX, currentY, tooltipRect.getWidth(), tooltipRect.getHeight());
		}
		return font;
	}

	@ModifyArg(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;renderImage(Lnet/minecraft/client/gui/Font;IILnet/minecraft/client/gui/GuiGraphics;)V"), index = 0)
	private Font scrollImages(Font font, int currentX, int currentY, GuiGraphics guiGraphics)
	{
		if (enableScissor)
		{
			startScissor(currentX, currentY, tooltipRect.getWidth(), tooltipRect.getHeight());
		}
		return font;
	}

	@Inject(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"))
	private void turnOffScissor(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info)
	{
		if (scissorEnabled)
		{
			stopScissor();
		}

		numTitleLines = 0;
		titleStart = 0;
		currentTooltipInfo = null;
		tooltipX = 0;
		tooltipY = 0;
		startScrollIndex = 0;
		tooltipRect = null;
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