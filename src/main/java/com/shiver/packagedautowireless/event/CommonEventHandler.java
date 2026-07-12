package com.shiver.packagedautowireless.event;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import com.shiver.packagedautowireless.block.BlockWirelessPackagingProvider;
import com.shiver.packagedautowireless.item.ItemPackageConnector;
import com.shiver.packagedautowireless.tile.TileWirelessPackagingProvider;

public class CommonEventHandler {

	public void registerBlock(Block block) {
		ForgeRegistries.BLOCKS.register(block);
	}

	public void registerItem(Item item) {
		ForgeRegistries.ITEMS.register(item);
	}

	public void onPreInit(FMLPreInitializationEvent event) {
		registerBlocks();
		registerItems();
		registerTileEntities();
	}

	protected void registerBlocks() {
		registerBlock(BlockWirelessPackagingProvider.INSTANCE);
	}

	protected void registerItems() {
		registerItem(BlockWirelessPackagingProvider.ITEM_INSTANCE);
		registerItem(ItemPackageConnector.INSTANCE);
	}

	protected void registerTileEntities() {
		GameRegistry.registerTileEntity(TileWirelessPackagingProvider.class,
				new ResourceLocation("packagedautowireless:wireless_packaging_provider"));
	}
}
