package com.anthonyhilyard.legendarytooltips.tooltip;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import com.anthonyhilyard.iceberg.events.client.RegisterTooltipComponentFactoryEvent;
import com.anthonyhilyard.iceberg.renderer.CustomItemRenderer;
import com.anthonyhilyard.iceberg.util.GuiHelper;
import com.anthonyhilyard.iceberg.util.Tooltips;
import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig;
import com.anthonyhilyard.prism.text.DynamicColor;
import com.anthonyhilyard.prism.util.ColorUtil;
import com.anthonyhilyard.prism.util.ConfigHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public class ItemModelComponent implements TooltipComponent, ClientTooltipComponent
{
	private static CustomItemRenderer customItemRenderer = null;

	private static float rotationTimer = 0.0f;

	private final ItemStack itemStack;

	public static void updateTimer(float partialTick)
	{
		double rotationInterval = LegendaryTooltipsConfig.getInstance().modelRotationSpeed.get();
		if (rotationInterval > 0)
		{
			rotationTimer += partialTick;
			if (rotationTimer > rotationInterval)
			{
				rotationTimer -= rotationInterval;
			}
		}
		else
		{
			rotationTimer = 0;
		}

	}

	public ItemModelComponent(ItemStack itemStack)
	{
		this.itemStack = itemStack;

		if (customItemRenderer == null)
		{
			customItemRenderer = CustomItemRenderer.getInstance();
		}
	}

	// |PAD|MODEL|PAD|TITLE|PAD|
	public static final int PADDING = 2;

	public static int getRenderHeight() { return 22; }
	public static int getRenderWidth() { return 22; }
	
	@Override
	public int getHeight() { return 4; }

	// Hacky hack to get rendering working properly...
	@Override
	public int getWidth(Font p_169952_) { return -(getRenderWidth() + PADDING * 3); }

	@Override
	public void renderImage(Font font, int x, int y, GuiGraphics graphics)
	{
		// We need to flush the GuiGraphics immediately since we are utilizing the modelViewStack for rendering the item model.
		graphics.flush();

		y--;
		x--;
		int z = 0;
		final int margin = 2;

		DynamicColor borderStartColor = DynamicColor.fromRgb(Tooltips.currentColors.borderColorStart());
		DynamicColor borderEndColor = DynamicColor.fromRgb(Tooltips.currentColors.borderColorEnd());
		DynamicColor backgroundStartColor = DynamicColor.fromRgb(Tooltips.currentColors.backgroundColorStart());
		DynamicColor backgroundEndColor = ConfigHelper.applyModifiers(List.of("v+35", "s+10"), DynamicColor.fromRgb(Tooltips.currentColors.backgroundColorEnd()));

		int borderColor = ColorUtil.combineARGB((int)(borderStartColor.alpha() * 0.35f), borderStartColor.red(), borderStartColor.green(), borderStartColor.blue());
		int backgroundStart = ColorUtil.combineARGB((int)(backgroundStartColor.alpha() * 0.15f), backgroundStartColor.red(), backgroundStartColor.green(), backgroundStartColor.blue());
		int backgroundEnd = ColorUtil.combineARGB((int)(backgroundEndColor.alpha() * 0.6f), backgroundEndColor.red(), backgroundEndColor.green(), backgroundEndColor.blue());

		PoseStack poseStack = graphics.pose();
		Matrix4f matrix = poseStack.last().pose();

		// Draw the background first.
		GuiHelper.drawGradientRect(matrix, z, x + margin + 1, y + margin + 1, x + getRenderWidth() - margin - 1, y + getRenderHeight() - margin - 1, backgroundStart, backgroundEnd);
		GuiHelper.drawGradientRect(matrix, z, x + margin + 1, y + margin + 1, x + getRenderWidth() - margin - 1, y + getRenderHeight() - margin - 1, backgroundEnd, backgroundStart);
		GuiHelper.drawGradientRectHorizontal(matrix, z, x + margin + 1, y + margin + 1, x + getRenderWidth() - margin - 1, y + getRenderHeight() - margin - 1, backgroundStart, backgroundEnd);
		GuiHelper.drawGradientRectHorizontal(matrix, z, x + margin + 1, y + margin + 1, x + getRenderWidth() - margin - 1, y + getRenderHeight() - margin - 1, backgroundEnd, backgroundStart);

		// Draw the border.
		GuiHelper.drawGradientRect(matrix, z, x + margin + 1, y + margin, x + getRenderWidth() - margin - 1, y + margin + 1, borderColor, borderColor);
		GuiHelper.drawGradientRect(matrix, z, x + margin + 1, y + getRenderHeight() - margin - 1, x + getRenderWidth() - margin - 1, y + getRenderHeight() - margin, borderColor, borderColor);
		GuiHelper.drawGradientRect(matrix, z, x + margin, y + margin + 1, x + margin + 1, y + getRenderHeight() - margin - 1, borderColor, borderColor);
		GuiHelper.drawGradientRect(matrix, z, x + getRenderWidth() - margin - 1, y + margin + 1, x + getRenderWidth() - margin, y + getRenderHeight() - margin - 1, borderColor, borderColor);

		borderColor = ColorUtil.combineARGB((int)(borderStartColor.alpha() * 0.15f),
				(int)((borderStartColor.red() + borderEndColor.red()) * 0.5f),
				(int)((borderStartColor.green() + borderEndColor.green()) * 0.5f),
				(int)((borderStartColor.blue() + borderEndColor.blue()) * 0.5f));
		GuiHelper.drawGradientRect(matrix, z, x + margin + 1,						y + margin + 1,						x + getRenderWidth() - margin - 1, y + margin + 2, borderColor, borderColor);
		GuiHelper.drawGradientRect(matrix, z, x + margin + 1,						y + getRenderHeight() - margin - 2, x + getRenderWidth() - margin - 1, y + getRenderHeight() - margin - 1, borderColor, borderColor);
		GuiHelper.drawGradientRect(matrix, z, x + margin + 1,						y + margin + 2,						x + margin + 2, y + getRenderHeight() - margin - 2, borderColor, borderColor);
		GuiHelper.drawGradientRect(matrix, z, x + getRenderWidth() - margin - 2,	y + margin + 2,						x + getRenderWidth() - margin - 1, y + getRenderHeight() - margin - 2, borderColor, borderColor);

		borderColor = ColorUtil.combineARGB((int)(borderStartColor.alpha() * 0.05f), borderEndColor.red(), borderEndColor.green(), borderEndColor.blue());

		GuiHelper.drawGradientRect(matrix, z, x + margin + 2,						y + margin + 2,						x + getRenderWidth() - margin - 2, y + getRenderHeight() - margin - 3, borderColor, borderColor);

		final Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
		modelViewStack.pushMatrix();
		modelViewStack.mul(matrix);
		modelViewStack.translate(x + margin - 1, y + margin - 1, -120.0f);
		modelViewStack.scale(1.25f, 1.25f, 1.0f);
		RenderSystem.applyModelViewMatrix();

		float rotationAngle = 0.0f;
		if (LegendaryTooltipsConfig.getInstance().modelRotationSpeed.get() > 0)
		{
			rotationAngle = Mth.lerp(rotationTimer / LegendaryTooltipsConfig.getInstance().modelRotationSpeed.get().floatValue(), 0, 360.0f);
		}

		customItemRenderer.renderDetailModelIntoGUI(itemStack, 0, 0, Axis.YP.rotationDegrees(rotationAngle), graphics);

		modelViewStack.popMatrix();
		RenderSystem.applyModelViewMatrix();
	}

	public static void registerFactory()
	{
		RegisterTooltipComponentFactoryEvent.EVENT.register(ItemModelComponent.class, data -> {
			if (data instanceof ItemModelComponent itemModelComponent)
			{
				return itemModelComponent;
			}
			return null;
		});
	}
}