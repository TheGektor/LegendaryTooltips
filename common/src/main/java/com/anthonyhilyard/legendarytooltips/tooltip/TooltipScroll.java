package com.anthonyhilyard.legendarytooltips.tooltip;

import java.util.Map;

import com.anthonyhilyard.iceberg.util.Easing;
import com.anthonyhilyard.iceberg.util.Easing.EasingDirection;
import com.anthonyhilyard.iceberg.util.Easing.EasingType;
import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig;
import com.google.common.collect.Maps;

import net.minecraft.client.DeltaTracker;

public final class TooltipScroll
{
	// How many seconds it should take to scroll once.
	private static final float scrollDuration = 0.15f;

	// For bounce effect at end of scroll.
	private static final float overScroll = 3.0f;

	private static class ScrollData
	{
		public float scrollOffset = 0.0f;
		public float targetOffset = 0.0f;
		public float prevTargetOffset = 0.0f;

		public float scrollTimer = 0.0f;

		public float scrollTop = 0.0f;
		public float scrollBottom = 0.0f;
		public float contentHeight = 0.0f;
		public boolean tooltipVisible = false;

		public  float getScrollableHeight()
		{
			return Math.max(contentHeight - Math.max(scrollBottom - scrollTop, 0.0f), 0.0f);
		}
	}

	private static Map<Integer, ScrollData> dataMap = Maps.newHashMap();

	private TooltipScroll() {}

	public static void setScrollBounds(int index, float top, float bottom)
	{
		ScrollData data = dataMap.computeIfAbsent(index, k -> new ScrollData());
		data.scrollTop = top;
		data.scrollBottom = bottom;
	}

	public static void setContentHeight(int index, float height)
	{
		dataMap.computeIfAbsent(index, k -> new ScrollData()).contentHeight = height;
	}

	public static void setTooltipVisible(int index, boolean visible)
	{
		dataMap.computeIfAbsent(index, k -> new ScrollData()).tooltipVisible = visible;
	}

	public static boolean isTooltipVisible(int index) { return dataMap.computeIfAbsent(index, k -> new ScrollData()).tooltipVisible; }
	public static float getScrollTop(int index) { return dataMap.computeIfAbsent(index, k -> new ScrollData()).scrollTop; }
	public static float getScrollBottom(int index) { return dataMap.computeIfAbsent(index, k -> new ScrollData()).scrollBottom; }

	public static float currentScroll(int index) { return dataMap.computeIfAbsent(index, k -> new ScrollData()).scrollOffset; }

	public static void reset(int index) { dataMap.put(index, new ScrollData()); }

	public static void resetAll()
	{
		dataMap.clear();
	}

	public static void scroll(float amount)
	{
		for (ScrollData data : dataMap.values())
		{
			data.prevTargetOffset = data.scrollOffset;
			data.targetOffset = data.targetOffset + amount * LegendaryTooltipsConfig.getInstance().scrollSpeed.get().floatValue();
			data.targetOffset = Math.clamp(data.targetOffset, -overScroll, data.getScrollableHeight() + overScroll);
			data.prevTargetOffset = Math.clamp(data.prevTargetOffset, 0.0f, data.contentHeight);
			data.scrollTimer = 0.0f;
		}
	}

	public static void onRenderTick(DeltaTracker tracker)
	{
		for (ScrollData data : dataMap.values())
		{
			if (data.targetOffset != data.scrollOffset)
			{
				data.scrollTimer += tracker.getRealtimeDeltaTicks() * 0.05f;
				if (data.scrollTimer < scrollDuration)
				{
					float alpha = data.scrollTimer / scrollDuration;

					data.scrollOffset = Easing.Ease(data.prevTargetOffset, data.targetOffset, alpha, EasingType.Quad, EasingDirection.Out);
				}
				else
				{
					data.scrollOffset = data.targetOffset;
					data.scrollTimer = 0.0f;
					data.prevTargetOffset = data.targetOffset;
					data.targetOffset = Math.clamp(data.targetOffset, 0.0f, data.getScrollableHeight());
				}
			}
		}
	}
}
