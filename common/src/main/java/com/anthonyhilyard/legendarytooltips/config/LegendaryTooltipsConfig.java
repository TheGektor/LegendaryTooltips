package com.anthonyhilyard.legendarytooltips.config;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import com.anthonyhilyard.iceberg.config.IcebergConfig;
import com.anthonyhilyard.iceberg.services.IIcebergConfigSpecBuilder;
import com.anthonyhilyard.iceberg.services.Services;
import com.anthonyhilyard.iceberg.util.Selectors;
import com.anthonyhilyard.iceberg.util.Selectors.SelectorDocumentation;
import com.anthonyhilyard.iceberg.util.StringRecomposer;
import com.anthonyhilyard.legendarytooltips.LegendaryTooltips;
import com.anthonyhilyard.legendarytooltips.tooltip.TooltipDecor;
import com.anthonyhilyard.prism.item.ItemColors;
import com.anthonyhilyard.prism.text.DynamicColor;
import com.anthonyhilyard.prism.util.ConfigHelper;
import com.anthonyhilyard.prism.util.IColor;
import com.anthonyhilyard.prism.util.ImageAnalysis;
import com.anthonyhilyard.prism.util.ConfigHelper.ColorFormatDocumentation;
import com.google.common.collect.Lists;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class LegendaryTooltipsConfig extends IcebergConfig<LegendaryTooltipsConfig>
{
	public static LegendaryTooltipsConfig getInstance() { return (LegendaryTooltipsConfig)configInstances.get(LegendaryTooltips.MODID); }

	public enum FrameSource
	{
		NONE,
		CONFIG,
		API,
		DATA
	}

	public enum ColorType
	{
		BORDER_START,
		BORDER_END,
		BG_START,
		BG_END
	}

	public static final int DEFAULT_FRAME_WIDTH = 64;
	public static final int DEFAULT_PART_SIZE = 8;
	public static final int DEFAULT_PART_OFFSET = -1;
	public static final int DEFAULT_CORNER_OFFSET = 2;

	public static final Map<ColorType, TextColor> defaultColors = Map.of(
		ColorType.BORDER_START, TextColor.fromRgb(0xFF996922),
		ColorType.BORDER_END, TextColor.fromRgb(0xFF5A3A1D),
		ColorType.BG_START, TextColor.fromRgb(0xF0160A00),
		ColorType.BG_END, TextColor.fromRgb(0xE8160A00)
	);

	public record FrameDefinition(ResourceLocation resource, int index, Supplier<Integer> startBorder, Supplier<Integer> endBorder, Supplier<Integer> startBackground, Supplier<Integer> endBackground, FrameSource source, int priority, int frameWidth, int partSize, int partOffset, int cornerOffset) {};
	public static final FrameDefinition STANDARD_BORDER = new FrameDefinition(null, -1, null, null, null, null, FrameSource.NONE, 0, DEFAULT_FRAME_WIDTH, DEFAULT_PART_SIZE, DEFAULT_PART_OFFSET, DEFAULT_CORNER_OFFSET);
	public static final FrameDefinition NO_BORDER = new FrameDefinition(null, -2, null, null, null, null, FrameSource.NONE, 0, 0, 0, 0, 0);

	private enum ModelRenderType
	{
		NONE,
		EQUIPMENT,
		ALL
	}

	private enum TooltipsScrollActive
	{
		ALWAYS,
		WITH_KEYBIND,
		NEVER
	}

	public final Supplier<Boolean> nameSeparator;
	public final Supplier<Boolean> showSeparatorForEmpty;
	public final Supplier<Boolean> bordersMatchRarity;
	public final Supplier<Boolean> tooltipShadow;
	public final Supplier<Boolean> shineEffect;
	public final Supplier<Boolean> centeredTitle;
	public final Supplier<Boolean> enforceMinimumWidth;
	public final Supplier<Boolean> compactTooltips;
	private final Supplier<ModelRenderType> renderItemModel;
	public final Supplier<Double> modelRotationSpeed;
	public final Supplier<Boolean> fixMC271840;
	private final Supplier<Integer> maxTooltipWidth;
	private final Supplier<Integer> maxTooltipHeight;
	private final Supplier<TooltipsScrollActive> enableTooltipScrolling;
	public final Supplier<Double> scrollSpeed;

	final TextColor[] startColors = new TextColor[LegendaryTooltips.NUM_FRAMES];
	final TextColor[] endColors = new TextColor[LegendaryTooltips.NUM_FRAMES];
	private final TextColor[] startBGColors = new TextColor[LegendaryTooltips.NUM_FRAMES];
	private final TextColor[] endBGColors = new TextColor[LegendaryTooltips.NUM_FRAMES];

	final Supplier<List<? extends Integer>> framePriorities;
	
	static final List<Supplier<List<? extends String>>> itemSelectors = new ArrayList<Supplier<List<? extends String>>>(LegendaryTooltips.NUM_FRAMES);
	private final Supplier<List<? extends String>> blacklist;

	private static final Map<FrameDefinition, Set<String>> customFrameDefinitions = new LinkedHashMap<>();

	private static final Map<ItemStack, FrameDefinition> frameDefinitionCache = new HashMap<>();

	private static final Map<FormattedText, FormattedText> formattedTitleCache = new HashMap<>();

	private static final List<Supplier<Supplier<?>>> colorSuppliers = new ArrayList<>();

	public LegendaryTooltipsConfig(IIcebergConfigSpecBuilder build)
	{
		build.comment(" Legendary Tooltips Configuration Instructions\n\n" +

					  " *** READ THIS FIRST ***\n\n" +

					  " By default, this mod does not apply special borders to most items.  It was designed to work well with mod packs where\n" +
					  " the available selection of items can vary widely, so it is up to the user or mod pack designer to customize as needed.\n" +
					  " There are many options available for setting up which custom borders (also called frames) apply to which items.  Follow these steps:\n" +
					  "   1. Decide which items you want to have custom borders, and which borders.  Note that each custom border has a number associated with it (starting at 0).\n" +
					  "   2. For each custom border you want to use, fill out the associated list in the \"definitions\" section.  This will be filled out with a list of \"selectors\",\n" +
					  "      each of which tell the mod what items have that border.  Please read the information above the definitions section for specifics.\n" +
					  "   3. Selectors for borders are checked in the order provided in the \"priorities\" section.  Once a match is found, that border is displayed.\n" +
					  "      For example, if border 0 had the selector \"%Diamond\" and border 1 had the selector \"diamond_sword\", they would both match for diamond swords.\n" +
					  "      In this case, whichever border number came first in the priority list would be the border that would get drawn in-game.\n" +
					  "   4. Optionally, border colors associated with custom borders can be set in the \"colors\" section.  The start color is the color at the top of the tooltip,\n" +
					  "      and the end color is the bottom, with a smooth transition between.  Please read the information above the color section for specifics.").push("client").push("visual_options");

		nameSeparator = build.comment(" Whether item names in tooltips should have a line under them separating them from the rest of the tooltip.").add("name_separator", true);
		showSeparatorForEmpty = build.comment(" If enabled, the name separator will be shown for all tooltips.  If disabled, it will only be shown for item tooltips.").add("show_separator_for_empty", true);
		bordersMatchRarity = build.comment(" If enabled, tooltip border colors will match item rarity colors (except for custom borders).").add("borders_match_rarity", true);
		tooltipShadow = build.comment(" If enabled, tooltips will display a drop shadow.").add("tooltip_shadow", true);
		shineEffect = build.comment(" If enabled, items showing a custom border will have a special shine effect when hovered over.").add("shine_effect", true);
		centeredTitle = build.comment(" If enabled, tooltip titles will be drawn centered.").add("centered_title", true);
		enforceMinimumWidth = build.comment(" If enabled, tooltips with custom borders will always be at least wide enough to display all border decorations.").add("enforce_minimum_width", false);
		compactTooltips = build.comment(" If enabled, some unnecessary text and spacing will be removed from equipment tooltips.").add("compact_tooltips", true);
		renderItemModel = build.comment(" Which items should have a 3D model rendered in the tooltip.  If set to \"equipment\", the model will only be rendered for armor, tools, and items with durability.").addEnum("render_item_model", ModelRenderType.EQUIPMENT);
		modelRotationSpeed = build.comment(" The speed at which 3D models in tooltips will rotate.  Lower values rotate faster, set to 0 to disable rotation.").addInRange("model_rotation_speed", 12.0, 0, 50.0);
		fixMC271840 = build.comment(" If enabled, fixes a vanilla bug where displayed tooltip damage values are incorrect for weapons with the Sharpness enchantment.").add("fix_mc271840", true);
		maxTooltipWidth = build.comment(" (EXPERIMENTAL) The maximum width of tooltips.  Set to 0 for no limit.").addInRange("max_tooltip_width", 0, 0, Integer.MAX_VALUE);
		maxTooltipHeight = build.comment(" (EXPERIMENTAL) The maximum height of tooltips.  Set to 0 for no limit.").addInRange("max_tooltip_height", 0, 0, Integer.MAX_VALUE);
		enableTooltipScrolling = build.comment(" (EXPERIMENTAL) If enabled, tooltips that are larger than the maximum height specified (or the screen if not specified) will be scrollable with the mouse wheel.").addEnum("enable_tooltip_scrolling", TooltipsScrollActive.NEVER);
		scrollSpeed = build.comment(" (EXPERIMENTAL) The speed at which tooltips will scroll when scrolling is enabled.").addInRange("scroll_speed", 10.0, 1.0, 50.0);

		build.pop().comment(String.format(" Custom borders are broken into %d \"levels\", with level 0 being intended for the \"best\" or \"rarest\" items. Only level 0 has a custom border built-in, but others can be added with resource packs.", LegendaryTooltips.NUM_FRAMES)).push("custom_borders");

		// Build the comment for manual borders.
		StringBuilder selectorsComment = new StringBuilder(" Entry types:\n");
		for (SelectorDocumentation doc : Selectors.selectorDocumentation())
		{
			selectorsComment.append("    ").append(doc.name()).append(" - ").append(doc.description());

			if (!doc.examples().isEmpty())
			{
				selectorsComment.append("  Examples: ");
				for (int i = 0; i < doc.examples().size(); i++)
				{
					if (i > 0)
					{
						selectorsComment.append(", ");
					}
					selectorsComment.append("\"").append(doc.examples().get(i)).append("\"");
				}
			}
			selectorsComment.append("\n");
		}

		// Remove the final newline.
		selectorsComment.setLength(selectorsComment.length() - 1);

		build.pop().comment(selectorsComment.toString());
		build.push("definitions");

		itemSelectors.clear();

		// Level 0 by default applies to epic and rare items.
		itemSelectors.add(build.addListAllowEmpty("level0_entries", Arrays.asList("!epic", "!rare"), e -> Selectors.validateSelector((String)e) ));

		// Other levels don't apply to anything by default.
		for (int i = 1; i < LegendaryTooltips.NUM_FRAMES; i++)
		{
			itemSelectors.add(build.addListAllowEmpty(String.format("level%d_entries", i), Lists.newArrayList(), e -> Selectors.validateSelector((String)e) ));
		}
		blacklist = build.comment(" Enter blacklist selectors here using the same format as above. Any items that match these selectors will NOT show a border.").addListAllowEmpty("blacklist", List.of(), e -> Selectors.validateSelector((String)e));

		build.pop().comment(" Set border priorities here.  This should be a list of numbers that correspond to border levels, with numbers coming first being higher priority.\n" +
							" Optionally, -1 can be inserted to indicate relative priority of data and api-defined borders.  If you don't know what that means, you don't need to worry about it.").push("priorities");
		framePriorities = build.addList("priorities", IntStream.rangeClosed(0, LegendaryTooltips.NUM_FRAMES - 1).boxed().collect(Collectors.toList()), e -> ((int)e >= -1 && (int)e < LegendaryTooltips.NUM_FRAMES));

		// Build the comment for manual borders.
		StringBuilder colorFormatsComment = new StringBuilder(" VALID COLOR FORMATS\n");
		for (ColorFormatDocumentation doc : ConfigHelper.colorFormatDocumentation())
		{
			colorFormatsComment.append("   ").append(doc.name()).append(" - ").append(doc.description().replace("\n", "\n         "));

			if (!doc.examples().isEmpty())
			{
				colorFormatsComment.append("\n     Examples: ");
				for (int i = 0; i < doc.examples().size(); i++)
				{
					if (i > 0)
					{
						colorFormatsComment.append(", ");
					}
					colorFormatsComment.append(doc.examples().get(i));
				}
			}
			colorFormatsComment.append("\n\n");
		}

		// Remove the final newline.
		colorFormatsComment.setLength(colorFormatsComment.length() - 2);

		build.pop().comment(" The colors used for each tooltip, in this order: top border color, bottom border color, top background color, bottom background color.\n" +
							" None of these colors are required, though any colors not specified will be replaced with the default tooltip colors.\n\n" +
							colorFormatsComment.toString()).push("colors");

		colorSuppliers.clear();
		for (int i = 0; i < LegendaryTooltips.NUM_FRAMES; i++)
		{
			// We need to define the configuration paths here.
			Supplier<?> colorsValue = build.addList(String.format("level%d_colors", i),
				i == 0 ?
				List.<Object>of(defaultColors.get(ColorType.BORDER_START).getValue(), defaultColors.get(ColorType.BORDER_END).getValue(), defaultColors.get(ColorType.BG_START).getValue(), defaultColors.get(ColorType.BG_END).getValue()) :
				List.<Object>of("auto", "auto", "auto", "auto"), v -> validateColor(v));
			
			// Store them as suppliers for resolution after the spec is finished being built.
			colorSuppliers.add(() -> colorsValue);
		}

		build.pop().pop();
	}

	public static int getMaxTooltipWidth()
	{
		int maxWidth = getInstance().maxTooltipWidth.get();

		if (maxWidth == 0)
		{
			Minecraft minecraft = Minecraft.getInstance();
			maxWidth = minecraft.getWindow().getGuiScaledWidth();
		}

		if (getInstance().enforceMinimumWidth.get() && maxWidth < 48)
		{
			maxWidth = 48;
		}

		return maxWidth;
	}

	public static int getMaxTooltipHeight()
	{
		int maxHeight = getInstance().maxTooltipHeight.get();

		if (maxHeight == 0)
		{
			Minecraft minecraft = Minecraft.getInstance();
			maxHeight = minecraft.getWindow().getGuiScaledHeight();
		}
		return maxHeight;
	}


	public static boolean shouldScrollTooltip()
	{
		switch (getInstance().enableTooltipScrolling.get())
		{
			case ALWAYS:
				return true;
			case WITH_KEYBIND:
				return LegendaryTooltips.scrollTooltipsKeyDown;
			case NEVER:
			default:
				return false;
		}
	}

	private static TagKey<Item> armorTag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "armors"));
	private static TagKey<Item> toolsTag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools"));


	public static boolean showModelForItem(ItemStack itemStack)
	{
		switch (getInstance().renderItemModel.get())
		{
			case NONE:
			default:
				return false;
			case ALL:
				return !itemStack.isEmpty();
			case EQUIPMENT:
				return !itemStack.isEmpty() && (itemStack.has(DataComponents.MAX_DAMAGE) || itemStack.is(armorTag) || itemStack.is(toolsTag));
		}
	}

	private static FormattedCharSequence getTitlePadding(Font font, int maxWidth)
	{
		String pad = " ";
		while (font.width(pad) < maxWidth)
		{
			pad += " ";
		}
		return FormattedCharSequence.forward(pad, Style.EMPTY);
	}

	private static Font getFontForTitle(FormattedText title)
	{
		Font result = null;
		Minecraft minecraft = Minecraft.getInstance();

		// TODO: Check if the title itself has a custom font set?

		// Check if the current screen specifies a font.
		if (minecraft.screen != null && minecraft.screen.font != null && minecraft.screen.font != minecraft.font)
		{
			result = minecraft.screen.font;
		}
		// Then check if minecraft has a font set.
		else
		{
			result = minecraft.font;
		}

		return result;
	}

	public static FormattedText getFormattedTitle(FormattedText title)
	{
		if (!formattedTitleCache.containsKey(title))
		{
			Font font = getFontForTitle(title);
			FormattedCharSequence paddedTitle = FormattedCharSequence.fromList(List.of(getTitlePadding(font, 24), Language.getInstance().getVisualOrder(title), getTitlePadding(font, 4)));
			List<FormattedText> recomposedTitle = StringRecomposer.recompose(List.of(ClientTooltipComponent.create(paddedTitle)));
			if (!recomposedTitle.isEmpty())
			{
				formattedTitleCache.put(title, recomposedTitle.get(0));
			}
		}

		return formattedTitleCache.get(title);
	}

	public static TextColor getColor(Object value)
	{
		return getColor(value, null, null, 0, null);
	}

	public static TextColor getColor(Object value, TextColor defaultColor, ResourceLocation borderImage, int index, ColorType colorType)
	{
		TextColor color = (TextColor)(Object)ConfigHelper.parseColor(value);
		if (color == null)
		{
			// If the specified color is automatic, try to get the right color for this border.
			if (value instanceof String string && string.contentEquals("auto") && borderImage != null)
			{
				Rect2i region = new Rect2i((index / 8) * 64, (index * 16) % 128, 64, 16);

				switch (colorType)
				{
					case BORDER_START:
						region.setHeight(8);
						break;
					case BORDER_END:
						region.setHeight(8);
						region.setY(region.getY() + 8);
						break;
					default:
				}

				color = ImageAnalysis.getDominantColor(borderImage, region);

				// Color can be null if the image was entirely transparent, white, or black for example.
				// Don't return the default color for automatic spec.
				if (color == null)
				{
					return defaultColor;
				}

				switch (colorType)
				{
					case BORDER_START:
						if (DynamicColor.fromColor((IColor)(Object)color).value() > 80)
						{
							color = ConfigHelper.applyModifiers(List.of("-v10", "+s10"), color);
							if (DynamicColor.fromColor((IColor)(Object)color).value() > 200)
							{
								color = ConfigHelper.applyModifiers(List.of("-v10"), color);
							}
						}
						if (DynamicColor.fromColor((IColor)(Object)color).saturation() > 40)
						{
							color = ConfigHelper.applyModifiers(List.of("+s10"), color);
						}
						break;
					case BORDER_END:
						if (DynamicColor.fromColor((IColor)(Object)color).value() > 80)
						{
							color = ConfigHelper.applyModifiers(List.of("-v30"), color);
							if (DynamicColor.fromColor((IColor)(Object)color).value() > 170 &&
								DynamicColor.fromColor((IColor)(Object)color).value() < 220)
							{
								color = ConfigHelper.applyModifiers(List.of("-v30"), color);
							}
						}
						if (DynamicColor.fromColor((IColor)(Object)color).saturation() > 40)
						{
							color = ConfigHelper.applyModifiers(List.of("+s50"), color);
						}
						break;
					case BG_START:
						color = ConfigHelper.applyModifiers(List.of("=v8", "+s50", "=a245"), color);
						break;
					case BG_END:
						color = ConfigHelper.applyModifiers(List.of("=v20", "+s75", "=a230"), color);
						break;
				}
				
				if (color != null)
				{
					return color;
				}
			}
			return defaultColor;
		}
		else
		{
			return color;
		}
	}

	private static boolean validateColor(Object value)
	{
		return (getColor(value) != null || (value instanceof String string && string.contentEquals("auto")));
	}

	private static void resolveColors()
	{
		for (int i = 0; i < LegendaryTooltips.NUM_FRAMES; i++)
		{
			Object colors = colorSuppliers.get(i).get().get();

			if (colors instanceof List<?> colorsList)
			{
				getInstance().startColors[i] =		getColor(colorsList.size() > 0 ? colorsList.get(0) : null, defaultColors.get(ColorType.BORDER_START), TooltipDecor.DEFAULT_BORDERS, i, ColorType.BORDER_START);
				getInstance().endColors[i] =		getColor(colorsList.size() > 1 ? colorsList.get(1) : null, defaultColors.get(ColorType.BORDER_END), TooltipDecor.DEFAULT_BORDERS, i, ColorType.BORDER_END);
				getInstance().startBGColors[i] =	getColor(colorsList.size() > 2 ? colorsList.get(2) : null, defaultColors.get(ColorType.BG_START), TooltipDecor.DEFAULT_BORDERS, i, ColorType.BG_START);
				getInstance().endBGColors[i] =		getColor(colorsList.size() > 3 ? colorsList.get(3) : null, defaultColors.get(ColorType.BG_END), TooltipDecor.DEFAULT_BORDERS, i, ColorType.BG_END);
			}
			else
			{
				getInstance().startColors[i] =		defaultColors.get(ColorType.BORDER_START);
				getInstance().endColors[i] =		defaultColors.get(ColorType.BORDER_END);
				getInstance().startBGColors[i] =	defaultColors.get(ColorType.BG_START);
				getInstance().endBGColors[i] =		defaultColors.get(ColorType.BG_END);
			}
		}
	}

	public static FrameDefinition getDefinitionColors(ItemStack item, int defaultStartBorder, int defaultEndBorder, int defaultStartBackground, int defaultEndBackground, HolderLookup.Provider provider)
	{
		FrameDefinition result = getInstance().getFrameDefinition(item, provider);

		if (result == NO_BORDER)
		{
			result = new FrameDefinition(result.resource(), result.index(), () -> defaultStartBorder, () -> defaultEndBorder, () -> defaultStartBackground, () -> defaultEndBackground, FrameSource.NONE, 0, result.frameWidth(), result.partSize(), result.partOffset(), result.cornerOffset());
		}
		else if (result == STANDARD_BORDER)
		{
			// If the "match rarity" option is turned on, calculate some good-looking colors.
			if (getInstance().bordersMatchRarity.get())
			{
				// First grab the item's name color.
				TextColor color = ItemColors.getColorForItem(item, TextColor.fromLegacyFormat(ChatFormatting.WHITE));
				DynamicColor rarityColor = DynamicColor.fromRgb(color.getValue());

				int hue = rarityColor.hue();
				boolean addHue = false;

				// These hue ranges are arbitrarily decided.  I just think they look the best.
				if (hue >= 62 && hue <= 240)
				{
					addHue = true;
				}

				// The start color will hue-shift by 0.6%, and the end will hue-shift the opposite direction by 4%.
				// This gives a very nice looking gradient, while still matching the name color quite well.
				int startHue = addHue ? hue - 4 : hue + 4;
				int endHue = addHue ? hue + 18 : hue - 18;
				int startBGHue = addHue ? hue - 3 : hue + 3;
				int endBGHue = addHue ? hue + 13 : hue - 13;

				// Ensure values stay between 0 and 360.
				startHue = (startHue + 360) % 360;
				endHue = (endHue + 360) % 360;
				startBGHue = (startBGHue + 360) % 360;
				endBGHue = (endBGHue + 360) % 360;

				DynamicColor startColor = DynamicColor.fromAHSV(0xFF, startHue, rarityColor.saturation(), rarityColor.value());
				DynamicColor endColor = DynamicColor.fromAHSV(0xFF, endHue, rarityColor.saturation(), (int)(rarityColor.value() * 0.95f));
				DynamicColor startBGColor = DynamicColor.fromAHSV(0xE4, startBGHue, (int)(rarityColor.saturation() * 0.9f), 14);
				DynamicColor endBGColor = DynamicColor.fromAHSV(0xFD, endBGHue, (int)(rarityColor.saturation() * 0.8f), 18);

				result = new FrameDefinition(result.resource(), result.index(), () -> startColor.getValue(), () -> endColor.getValue(), () -> startBGColor.getValue(), () -> endBGColor.getValue(), FrameSource.NONE, 0, result.frameWidth(), result.partSize(), result.partOffset(), result.cornerOffset());
			}
		}

		if (result.startBorder() == null)
		{
			result = new FrameDefinition(result.resource(), result.index(), () -> defaultStartBorder, result.endBorder(), result.startBackground(), result.endBackground(), FrameSource.NONE, 0, result.frameWidth(), result.partSize(), result.partOffset(), result.cornerOffset());
		}
		if (result.endBorder() == null)
		{
			result = new FrameDefinition(result.resource(), result.index(), result.startBorder(), () -> defaultEndBorder, result.startBackground(), result.endBackground(), FrameSource.NONE, 0, result.frameWidth(), result.partSize(), result.partOffset(), result.cornerOffset());
		}
		if (result.startBackground() == null)
		{
			result = new FrameDefinition(result.resource(), result.index(), result.startBorder(), result.endBorder(), () -> defaultStartBackground, result.endBackground(), FrameSource.NONE, 0, result.frameWidth(), result.partSize(), result.partOffset(), result.cornerOffset());
		}
		if (result.endBackground() == null)
		{
			result = new FrameDefinition(result.resource(), result.index(), result.startBorder(), result.endBorder(), result.startBackground(), () -> defaultEndBackground, FrameSource.NONE, 0, result.frameWidth(), result.partSize(), result.partOffset(), result.cornerOffset());
		}
		return result;
	}

	/**
	 * Adds a new custom frame definition.  If the same frame definition already exists,
	 * the provided selectors are added after the already-configured selectors.
	 */
	public void addFrameDefinition(ResourceLocation resource, int index, Supplier<Integer> startBorder, Supplier<Integer> endBorder, Supplier<Integer> background, int priority, List<String> selectors)
	{
		addFrameDefinition(resource, index, startBorder, endBorder, background, background, priority, selectors, DEFAULT_FRAME_WIDTH, DEFAULT_PART_SIZE, DEFAULT_PART_OFFSET, DEFAULT_CORNER_OFFSET);
	}

	public void addFrameDefinition(ResourceLocation resource, int index, Supplier<Integer> startBorder, Supplier<Integer> endBorder, Supplier<Integer> startBackground, Supplier<Integer> endBackground, int priority, List<String> selectors)
	{
		addFrameDefinition(resource, index, startBorder, endBorder, startBackground, endBackground, priority, selectors, DEFAULT_FRAME_WIDTH, DEFAULT_PART_SIZE, DEFAULT_PART_OFFSET, DEFAULT_CORNER_OFFSET);
	}

	public void addFrameDefinition(ResourceLocation resource, int index, Supplier<Integer> startBorder, Supplier<Integer> endBorder, Supplier<Integer> background, int priority, List<String> selectors, int frameWidth, int partSize, int partOffset, int cornerOffset)
	{
		addFrameDefinition(resource, index, startBorder, endBorder, background, background, priority, selectors, frameWidth, partSize, partOffset, cornerOffset);
	}

	public void addFrameDefinition(ResourceLocation resource, int index, Supplier<Integer> startBorder, Supplier<Integer> endBorder, Supplier<Integer> startBackground, Supplier<Integer> endBackground, int priority, List<String> selectors, int frameWidth, int partSize, int partOffset, int cornerOffset)
	{
		FrameDefinition definition = new FrameDefinition(resource, index, startBorder, endBorder, startBackground, endBackground, FrameSource.API, priority, frameWidth, partSize, partOffset, cornerOffset);
		addFrameDefinition(definition, selectors);
	}

	void addFrameDefinition(FrameDefinition definition, List<String> selectors)
	{
		if (definition.source != FrameSource.API && definition.source != FrameSource.DATA)
		{
			return;
		}

		Set<String> selectorSet = new LinkedHashSet<>();
		if (customFrameDefinitions.containsKey(definition))
		{
			selectorSet.addAll(customFrameDefinitions.get(definition));
		}
		selectorSet.addAll(selectors);
		customFrameDefinitions.put(definition, selectorSet);
	}

	void clearDataFrames()
	{
		customFrameDefinitions.entrySet().removeIf(entry -> entry.getKey().source == FrameSource.DATA);
	}

	public FrameDefinition getFrameDefinition(ItemStack item, HolderLookup.Provider provider)
	{
		if (frameDefinitionCache.containsKey(item))
		{
			return frameDefinitionCache.get(item);
		}

		if (item == null)
		{
			frameDefinitionCache.put(item, STANDARD_BORDER);
			return STANDARD_BORDER;
		}

		if (startColors[0] == null)
		{
			// Somehow colors haven't been resolved yet, so do it now.
			resolveColors();
		}

		// First check the blacklist.
		for (String entry : blacklist.get())
		{
			if (Selectors.itemMatches(item, entry, provider))
			{
				// Add to cache.
				frameDefinitionCache.put(item, NO_BORDER);
				return NO_BORDER;
			}
		}

		// Now check if this item has an external decoration.
		if (Services.getPlatformHelper().isModLoaded("relics"))
		{
			try
			{
				Minecraft minecraft = Minecraft.getInstance();
				Method hasTooltipDecorMethod = Class.forName("com.anthonyhilyard.legendarytooltips.compat.RelicsHandler").getDeclaredMethod("hasTooltipDecor", ItemStack.class, LocalPlayer.class);
				if ((boolean)hasTooltipDecorMethod.invoke(null, item, minecraft.player))
				{
					// Add to cache.
					frameDefinitionCache.put(item, NO_BORDER);
					return NO_BORDER;
				}
			}
			catch (Exception e)
			{
				LegendaryTooltips.LOGGER.debug(ExceptionUtils.getStackTrace(e));
			}
		}

		List<Integer> priorities = framePriorities.get().stream().map(i -> Integer.valueOf(i)).collect(Collectors.toCollection(ArrayList::new));
		if (!priorities.contains(-1))
		{
			priorities.add(0, -1);
		}

		// Check each level for matches for this item, from most specific to least.
		for (int i = 0; i < priorities.size(); i++)
		{
			int frameIndex = priorities.get(i);

			// Standard config-based frame.
			if (frameIndex != -1 && frameIndex < LegendaryTooltips.NUM_FRAMES)
			{
				TextColor startColor =		startColors[frameIndex] == null ? defaultColors.get(ColorType.BORDER_START) : startColors[frameIndex];
				TextColor endColor =		endColors[frameIndex] == null ? defaultColors.get(ColorType.BORDER_END) : endColors[frameIndex];
				TextColor startBGColor =	startBGColors[frameIndex] == null ? defaultColors.get(ColorType.BG_START) : startBGColors[frameIndex];
				TextColor endBGColor =		endBGColors[frameIndex] == null ? defaultColors.get(ColorType.BG_END) : endBGColors[frameIndex];

				for (String entry : itemSelectors.get(frameIndex).get())
				{
					if (Selectors.itemMatches(item, entry, provider))
					{
						// Add to cache.
						FrameDefinition frameDefinition = new FrameDefinition(TooltipDecor.DEFAULT_BORDERS, frameIndex, () -> startColor.getValue(), () -> endColor.getValue(), () -> startBGColor.getValue(), () -> endBGColor.getValue(), FrameSource.CONFIG, i, DEFAULT_FRAME_WIDTH, DEFAULT_PART_SIZE, DEFAULT_PART_OFFSET, DEFAULT_CORNER_OFFSET);
						frameDefinitionCache.put(item, frameDefinition);
						return frameDefinition;
					}
				}
			}
			// Either API or data-specified frames.
			else
			{
				// Sort the definitions by priority.
				List<FrameDefinition> sortedDefinitions = customFrameDefinitions.keySet().stream().sorted((a, b) -> Integer.compare(a.priority, b.priority)).toList();

				// Check API frames first, since they are probably less common and more likely to do something weird.
				for (FrameDefinition frameDefinition : sortedDefinitions)
				{
					for (String entry : customFrameDefinitions.get(frameDefinition))
					{
						if (Selectors.itemMatches(item, entry, provider))
						{
							// Add to cache.
							frameDefinitionCache.put(item, frameDefinition);
							return frameDefinition;
						}
					}
				}
			}
		}
		
		// Add to cache.
		frameDefinitionCache.put(item, STANDARD_BORDER);
		return STANDARD_BORDER;
	}

	public static void reset()
	{
		// Clear the frame level cache in case anything has changed.
		frameDefinitionCache.clear();

		// Clear the formatted title cache as well.
		formattedTitleCache.clear();

		// Also resolve the colors again.
		resolveColors();
	}

	@Override
	protected void onReload()
	{
		reset();
	}
}
