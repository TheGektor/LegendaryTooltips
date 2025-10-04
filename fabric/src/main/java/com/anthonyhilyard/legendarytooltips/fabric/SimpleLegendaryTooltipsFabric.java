package com.anthonyhilyard.legendarytooltips.fabric;

import net.fabricmc.api.ClientModInitializer;

public class SimpleLegendaryTooltipsFabric implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        System.out.println("Legendary Tooltips Fabric client initialized for Minecraft 1.21.8");
    }
}