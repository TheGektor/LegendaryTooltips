package com.anthonyhilyard.legendarytooltips;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import org.apache.logging.log4j.LogManager;
import com.google.common.collect.Maps;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;

import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents.ColorExtResult;
import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents.GatherResult;
import com.anthonyhilyard.iceberg.services.Services;
import com.anthonyhilyard.iceberg.services.IKeyMappingRegistrar.KeyMappingContext;
import com.anthonyhilyard.iceberg.util.Tooltips;

import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig;
import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig.FrameDefinition;
import com.anthonyhilyard.legendarytooltips.tooltip.ItemModelComponent;
import com.anthonyhilyard.legendarytooltips.tooltip.TooltipDecor;
import com.anthonyhilyard.legendarytooltips.tooltip.TooltipScroll;


public class LegendaryTooltips
{
	public static final String MODID = "legendarytooltips";
	public static final Logger LOGGER = LogManager.getLogger(MODID);

	public static final int NUM_FRAMES = 16;

	private static Map<Integer, ItemStack> lastTooltipItems = Maps.newHashMap();

	public static final KeyMapping scrollTooltips = Services.getKeyMappingRegistrar().registerMapping(
		new KeyMapping("legendarytooltips.key.scrollTooltips", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_SHIFT, KeyMapping.CATEGORY_INVENTORY), KeyMappingContext.NO_CONFLICT);

	public static boolean scrollTooltipsKeyDown = false;

	public static void init()
	{
		LegendaryTooltipsConfig.register(LegendaryTooltipsConfig.class, MODID);
	}

	public static GatherResult onGatherComponentsEvent(ItemStack itemStack, int screenWidth, int screenHeight, List<Either<FormattedText, TooltipComponent>> tooltipElements, int maxWidth, int index)
	{
		// If compact tooltips are turned on, remove a few unneeded lines from the tooltip.
		if (LegendaryTooltipsConfig.getInstance().compactTooltips.get())
		{
			// Search for any translatable components with translation keys that start with "item.modifiers." for removal.
			for (int i = 0; i < tooltipElements.size(); i++)
			{
				if (tooltipElements.get(i).left().isPresent())
				{
					FormattedText text = tooltipElements.get(i).left().get();
					if (text instanceof MutableComponent component && component.getContents() instanceof TranslatableContents contents)
					{
						// If we find a translatable component with a translation key that starts with "item.modifiers.", remove it and the blank line before it.
						if (contents.getKey().startsWith("item.modifiers."))
						{
							tooltipElements.remove(i);

							if (tooltipElements.size() > i - 1 && i > 0 &&
								(tooltipElements.get(i - 1).right().isPresent() && tooltipElements.get(i - 1).right().get() == CommonComponents.EMPTY) ||
								(tooltipElements.get(i - 1).left().isPresent()  && tooltipElements.get(i - 1).left().get().getString().isEmpty()))
							{
								tooltipElements.remove(i - 1);
							}
						}
					}
				}
			}
		}


		// If this item should have an item model, add one now.
		if (LegendaryTooltipsConfig.showModelForItem(itemStack) && !tooltipElements.isEmpty() && tooltipElements.get(0).left().isPresent())
		{
			// Insert an item model component before the title, and an empty line after it.
			tooltipElements.add(0, Either.<FormattedText, TooltipComponent>right(new ItemModelComponent(itemStack)));
		}

		if (LegendaryTooltipsConfig.getMaxTooltipWidth() < maxWidth || maxWidth == -1)
		{
			maxWidth = LegendaryTooltipsConfig.getMaxTooltipWidth();
		}

		return new GatherResult(InteractionResult.PASS, maxWidth, tooltipElements);
	}

	public static void onRenderTick(DeltaTracker tracker)
	{
		if (LegendaryTooltipsConfig.getInstance() == null || !LegendaryTooltipsConfig.getInstance().isLoaded())
		{
			return;
		}

		float deltaTime = tracker.getRealtimeDeltaTicks() * 0.05f;
		TooltipDecor.updateTimer(deltaTime);
		ItemModelComponent.updateTimer(deltaTime);

		if (!Tooltips.anyTooltipsVisible())
		{
			TooltipDecor.resetTimer();
			TooltipScroll.resetAll();
			lastTooltipItems.clear();
		}
	}

	private static boolean areStacksEqual(ItemStack first, ItemStack second)
	{
		if (first == second)
		{
			return true;
		}

		// If they are different references, check the item type, quantity, and data components.
		if (ItemStack.hashItemAndComponents(first) + first.getCount() == ItemStack.hashItemAndComponents(second) + second.getCount())
		{
			return true;
		}
		
		return false;
	}

	public static ColorExtResult onTooltipColorEvent(ItemStack stack, GuiGraphics graphics, int x, int y, Font font, int backgroundStart, int backgroundEnd, int borderStart, int borderEnd, List<ClientTooltipComponent> components, boolean comparison, int index)
	{
		ColorExtResult result = new ColorExtResult(backgroundStart, backgroundEnd, borderStart, borderEnd);
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.level.registryAccess() == null)
		{
			return result;
		}

		if (!areStacksEqual(lastTooltipItems.computeIfAbsent(index, k -> ItemStack.EMPTY), stack))
		{
			TooltipDecor.resetTimer();
			TooltipScroll.reset(index);
			lastTooltipItems.put(index, stack);
		}

		FrameDefinition frameDefinition = LegendaryTooltipsConfig.getDefinitionColors(stack, borderStart, borderEnd, backgroundStart, backgroundEnd, minecraft.level.registryAccess());

		// Every tooltip will send a color event before a posttext event, so we can store the color here.
		TooltipDecor.setCurrentTooltipBorderStart(frameDefinition.startBorder().get());
		TooltipDecor.setCurrentTooltipBorderEnd(frameDefinition.endBorder().get());
		TooltipDecor.setCurrentTooltipBackgroundStart(frameDefinition.startBackground().get());
		TooltipDecor.setCurrentTooltipBackgroundEnd(frameDefinition.endBackground().get());

		return new ColorExtResult(frameDefinition.startBackground().get(), frameDefinition.endBackground().get(), frameDefinition.startBorder().get(), frameDefinition.endBorder().get());
	}

	public static void onPostTooltipEvent(ItemStack stack, GuiGraphics graphics, int x, int y, Font font, int width, int height, List<ClientTooltipComponent> components, boolean comparison, int index)
	{
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.level.registryAccess() == null)
		{
			return;
		}

		FrameDefinition frameDefinition = LegendaryTooltipsConfig.getInstance().getFrameDefinition(stack, minecraft.level.registryAccess());
		PoseStack poseStack = graphics.pose();

		// If tooltip shadows are enabled, draw one now.
		if (LegendaryTooltipsConfig.getInstance().tooltipShadow.get())
		{
			TooltipDecor.drawShadow(poseStack, x, y, width, height);
		}

		// If this item has a defined border, draw it.
		TooltipDecor.drawBorder(poseStack, x, y, width, height, stack, components, font, frameDefinition, comparison, index);
	}
}
