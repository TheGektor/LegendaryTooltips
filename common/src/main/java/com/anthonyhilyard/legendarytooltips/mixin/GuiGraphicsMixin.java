package com.anthonyhilyard.legendarytooltips.mixin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector2ic;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.anthonyhilyard.iceberg.component.IExtendedText;
import com.anthonyhilyard.iceberg.component.IExtendedText.TextAlignment;
import com.anthonyhilyard.iceberg.services.Services;
import com.anthonyhilyard.iceberg.util.ITooltipAccess;
import com.anthonyhilyard.iceberg.util.Tooltips;
import com.anthonyhilyard.iceberg.util.Tooltips.TooltipRenderContext;
import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig;
import com.anthonyhilyard.legendarytooltips.tooltip.ItemModelComponent;
import com.anthonyhilyard.legendarytooltips.tooltip.TooltipScroll;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin
{
	@Unique
	private int numTitleLines = 0;

	@Unique
	private int titleStart = 0;

	@Unique
	private int currentMouseX = 0;

	@Unique
	private int currentMouseY = 0;

	@Unique
	private int tooltipY = 0;

	@Unique
	private int startScrollIndex = 0;

	@Unique
	private int originalHeight = 0;

	@Unique
	private boolean hasItemModel = false;

	@Shadow
	private boolean managed;

	@ModifyVariable(method = "renderTooltipInternal", ordinal = 0, at = @At(value = "LOAD", ordinal = 0), argsOnly = true)
	private List<ClientTooltipComponent> mutableComponents(List<ClientTooltipComponent> components)
	{
		components = new ArrayList<>(components);

		numTitleLines = Tooltips.calculateTitleLines(components);
		titleStart = Tooltips.calculateTitleStart(components);
		startScrollIndex = titleStart + numTitleLines;
		hasItemModel = components.stream().anyMatch(component -> component instanceof ItemModelComponent);

		return components;
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
	private void adjustTitle(Font font, List<ClientTooltipComponent> components, int mouseX, int mouseY, ClientTooltipPositioner positioner, CallbackInfo info)
	{
		currentMouseX = mouseX;
		currentMouseY = mouseY;

		if (!components.isEmpty())
		{
			boolean shouldCenter = LegendaryTooltipsConfig.getInstance().centeredTitle.get();
			boolean has3DModel = components.stream().anyMatch(c -> c instanceof ItemModelComponent);
			int componentIndex = 0;

			// Center the title lines.
			for (ClientTooltipComponent component : components)
			{
				if (componentIndex >= titleStart && componentIndex < titleStart + numTitleLines &&
					component instanceof IExtendedText extendedComponent)
				{
					if (shouldCenter)
					{
						extendedComponent.setAlignment(TextAlignment.CENTER);
					}

					if (has3DModel)
					{
						extendedComponent.setPadding(ItemModelComponent.getRenderWidth() + ItemModelComponent.PADDING * 2, ItemModelComponent.PADDING);

						if (componentIndex == titleStart + numTitleLines - 1 && numTitleLines == 1)
						{
							extendedComponent.setPadding(extendedComponent.getLeftPadding(), extendedComponent.getRightPadding(), extendedComponent.getTopPadding(), extendedComponent.getBottomPadding() + 9);
						}
					}
				}
				componentIndex++;
			}
		}
	}

	@ModifyArg(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;positionTooltip(IIIIII)Lorg/joml/Vector2ic;"), index = 4)
	private int overrideTooltipWidthOnPosition(int width)
	{
		// Only apply max width to item tooltips.
		ItemStack currentStack = ((ITooltipAccess)(Object)this).getIcebergTooltipStack();

		if (!currentStack.isEmpty())
		{
			int maxTooltipWidth = LegendaryTooltipsConfig.getMaxTooltipWidth();
			if (maxTooltipWidth < width)
			{
				return maxTooltipWidth;
			}
		}

		return width;
	}

	@ModifyArg(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;positionTooltip(IIIIII)Lorg/joml/Vector2ic;"), index = 5)
	private int overrideTooltipHeightOnPosition(int height)
	{
		originalHeight = height;

		// Only apply max height to item tooltips.
		ItemStack currentStack = ((ITooltipAccess)(Object)this).getIcebergTooltipStack();

		if (!currentStack.isEmpty())
		{
			int maxTooltipHeight = LegendaryTooltipsConfig.getMaxTooltipHeight();
			if (maxTooltipHeight < height)
			{
				return maxTooltipHeight;
			}
		}

		return height;
	}

	@ModifyVariable(method = "renderTooltipInternal", at = @At(value = "STORE", ordinal = 0))
	private Vector2ic storeTooltipY(Vector2ic pos)
	{
		tooltipY = pos.y();
		return pos;
	}

	@Group(name = "tooltipWidth", max = 1)
	@ModifyArg(method = "lambda$renderTooltipInternal$3", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIIIIIII)V"), index = 3)
	private int overrideTooltipWidthOnDraw(int width)
	{
		// Only apply max width to item tooltips.
		ItemStack currentStack = ((ITooltipAccess)(Object)this).getIcebergTooltipStack();

		if (!currentStack.isEmpty())
		{
			int maxTooltipWidth = LegendaryTooltipsConfig.getMaxTooltipWidth();
			if (maxTooltipWidth < width)
			{
				return maxTooltipWidth;
			}
		}

		return width;
	}

	@Group(name = "tooltipWidth", max = 1)
	@ModifyArg(method = "method_51743", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIII)V"), index = 3)
	private int overrideTooltipWidthOnDraw2(int width)
	{
		return overrideTooltipWidthOnDraw(width);
	}

	@Group(name = "tooltipHeight", max = 1)
	@ModifyArg(method = "lambda$renderTooltipInternal$3", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIIIIIII)V"), index = 4)
	private int overrideTooltipHeightOnDraw(int height)
	{
		// Only apply max height to item tooltips.
		ItemStack currentStack = ((ITooltipAccess)(Object)this).getIcebergTooltipStack();

		if (!currentStack.isEmpty())
		{
			int maxTooltipHeight = LegendaryTooltipsConfig.getMaxTooltipHeight();
			if (maxTooltipHeight < height)
			{
				return maxTooltipHeight;
			}
		}

		return height;
	}

	@Group(name = "tooltipHeight", max = 1)
	@ModifyArg(method = "method_51743", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIII)V"), index = 4)
	private int overrideTooltipHeightOnDraw2(int height)
	{
		return overrideTooltipHeightOnDraw(height);
	}


	@Unique
	private boolean enableScissor = false;

	@Unique
	private boolean scissorEnabled = false;

	@Unique
	Method getXMethod = null;

	@Unique
	Method getYMethod = null;

	@Unique
	Method hasClickedOutsideMethod = null;

	@Unique
	private void startScissor(int x, int y, int width, int height)
	{
		int contentHeight = originalHeight - (y - tooltipY);
		int scrollHeight = (y + height - (y - tooltipY) + 1) - (y - 1);

		if (contentHeight > scrollHeight)
		{
			// Titanium library causes issues with tooltip scissoring.  Check if the currently-active screen
			// is a titanium screen, if so grab the x and y values and offset by that much.
			if (Services.getPlatformHelper().isModLoaded("titanium"))
			{
				try
				{
					Class<?> basicContainerScreenClass = Class.forName("com.hrznstudio.titanium.client.screen.container.BasicContainerScreen");
					if (getXMethod == null)
					{
						getXMethod = basicContainerScreenClass.getDeclaredMethod("getX");
						getYMethod = basicContainerScreenClass.getDeclaredMethod("getY");
						hasClickedOutsideMethod = AbstractContainerScreen.class.getDeclaredMethod("hasClickedOutside", double.class, double.class, int.class, int.class, int.class);
						hasClickedOutsideMethod.setAccessible(true);
					}

					if (getXMethod != null)
					{
						Minecraft minecraft = Minecraft.getInstance();
						Screen currentScreen = minecraft.screen;

						if (currentScreen != null && basicContainerScreenClass.isAssignableFrom(currentScreen.getClass()))
						{
							int leftPos = (int)getXMethod.invoke(currentScreen, (Object[])null);
							int topPos = (int)getYMethod.invoke(currentScreen, (Object[])null);

							if (!(boolean)hasClickedOutsideMethod.invoke(currentScreen, (double)(currentMouseX + leftPos), (double)(currentMouseY + topPos), leftPos, topPos, 0))
							{
								x += leftPos;
								y += topPos;
								height += topPos;
							}
						}
					}
				}
				catch (Exception e)
				{
					// Do nothing.
				}
			}

			TooltipRenderContext context = Tooltips.getCurrentRenderContext();
			GuiGraphics self = (GuiGraphics)(Object)this;

			managed = true;
			self.enableScissor(x - 1, y - 1, x + width + 1, y + height - (y - tooltipY) + 1);

			TooltipScroll.setTooltipVisible(context.index(), true);
			TooltipScroll.setScrollBounds(context.index(), y - 1, y + height - (y - tooltipY) + 1);
			TooltipScroll.setContentHeight(context.index(), contentHeight);

			self.pose().pushPose();
			self.pose().translate(0.0f, -TooltipScroll.currentScroll(context.index()), 0.0f);

			scissorEnabled = true;
			enableScissor = false;
		}
	}

	@Unique
	private void stopScissor()
	{
		TooltipRenderContext context = Tooltips.getCurrentRenderContext();
		GuiGraphics self = (GuiGraphics)(Object)this;
		self.pose().popPose();

		TooltipScroll.setTooltipVisible(context.index(), false);

		self.disableScissor();
		managed = false;

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

	@Unique
	private static int currentIndex = 0;
	@Unique
	private static int maxIndex = 0;

	@Inject(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
	private void resetComponentIteration(Font font, List<ClientTooltipComponent> list, int i, int j, ClientTooltipPositioner clientTooltipPositioner, CallbackInfo info)
	{
		currentIndex = 0;
		maxIndex = list.size();
	}

	@ModifyArg(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;renderText(Lnet/minecraft/client/gui/Font;IILorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V"), index = 2)
	private int scrollText(Font font, int currentX, int currentY, Matrix4f matrix4f, MultiBufferSource.BufferSource bufferSource)
	{
		// Adjust vertical position for multi-line titles to fix improper spacing.
		if (titleStart + numTitleLines > 1 && currentIndex > 0 && currentIndex < titleStart + numTitleLines)
		{
			currentY -= 2;
		}

		// If there is a 3D item model...
		if (titleStart > 0 && hasItemModel)
		{
			// If there is only a single title, move it down so it is vertically centered.
			if (numTitleLines == 1 && currentIndex == titleStart)
			{
				currentY += 2;
			}

			// If there are two or more title lines, move them up a bit.
			if (numTitleLines > 1 && currentIndex >= titleStart && currentIndex < titleStart + numTitleLines)
			{
				currentY -= 2;
			}
		}

		if (enableScissor)
		{
			Rect2i tooltipRect = Tooltips.getCurrentRect();
			startScissor(currentX, currentY, tooltipRect.getWidth(), tooltipRect.getHeight());
		}

		currentIndex++;
		if (currentIndex == maxIndex)
		{
			currentIndex = 0;
		}
		return currentY;
	}

	@ModifyArg(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;renderImage(Lnet/minecraft/client/gui/Font;IILnet/minecraft/client/gui/GuiGraphics;)V"), index = 2)
	private int scrollImages(Font font, int currentX, int currentY, GuiGraphics guiGraphics)
	{
		// Adjust vertical position for multi-line titles.
		if (titleStart + numTitleLines > 1 && currentIndex > 0 && currentIndex < titleStart + numTitleLines)
		{
			currentY -= 2;
		}

		// If there is a 3D item model...
		if (titleStart > 0 && hasItemModel)
		{
			// If there is only a single title, move it down so it is vertically centered.
			if (numTitleLines == 1 && currentIndex == titleStart)
			{
				currentY += 2;
			}

			// If there are two or more title lines, move them up a bit.
			if (numTitleLines > 1 && currentIndex >= titleStart && currentIndex < titleStart + numTitleLines)
			{
				currentY -= 2;
			}
		}

		if (enableScissor)
		{
			Rect2i tooltipRect = Tooltips.getCurrentRect();
			startScissor(currentX, currentY, tooltipRect.getWidth(), tooltipRect.getHeight());
		}

		currentIndex++;
		return currentY;
	}

	@Inject(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = Shift.AFTER))
	private void fixLayering(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo info)
	{
		TooltipRenderContext context = Tooltips.getCurrentRenderContext();
		GuiGraphics self = (GuiGraphics)(Object)this;
		float zOffset = context.index() * 30.0f;

		if (!((ITooltipAccess)self).getIcebergTooltipStack().isEmpty())
		{
			zOffset += 90.0f;
		}

		self.pose().translate(0, 0, -zOffset);
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
		tooltipY = 0;
		startScrollIndex = 0;
		hasItemModel = false;
	}

	@Unique
	private static int arsNouveauOffsetX = 0;

	@Unique
	private static int arsNouveauOffsetY = 0;

	@Unique
	private static boolean arsNouveauComponent = false;

	@Redirect(method = "renderTooltipInternal",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;getWidth(Lnet/minecraft/client/gui/Font;)I"))
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
					// Make the school glyphs component wider than the title to ensure they fit.
					arsNouveauOffsetX = instance.getWidth(font) - 3;

					if (LegendaryTooltipsConfig.showModelForItem(((ITooltipAccess)(Object)(this)).getIcebergTooltipStack()))
					{
						arsNouveauOffsetY = -7;
					}
					else
					{
						arsNouveauOffsetY = 0;
					}

					return title.getWidth(font) + (instance.getWidth(font) - font.width(title.text));
				}
			}
		}

		return instance.getWidth(font);
	}

	@ModifyArgs(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;", ordinal = 1))
	private void arsNouveauCompatComponentCheck(Args args, Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner)
	{
		int i = args.get(0);
		arsNouveauComponent = i < components.size() && components.get(i).getClass().getName().contentEquals("com.hollingsworth.arsnouveau.client.gui.SchoolTooltip$SchoolTooltipRenderer");
	}

	@ModifyArgs(method = "renderTooltipInternal",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;renderImage(Lnet/minecraft/client/gui/Font;IILnet/minecraft/client/gui/GuiGraphics;)V"))
	private void arsNouveauCompatOffsetComponent(Args args)
	{
		if (arsNouveauComponent &&
			(LegendaryTooltipsConfig.getInstance().centeredTitle.get() ||
			 LegendaryTooltipsConfig.showModelForItem(((ITooltipAccess)(Object)(this)).getIcebergTooltipStack())))
		{
			Rect2i tooltipRect = Tooltips.getCurrentRect();
			int x = args.get(1);
			int y = args.get(2);

			args.set(1, x + tooltipRect.getWidth() - arsNouveauOffsetX);
			args.set(2, y + arsNouveauOffsetY);
		}
	}
}