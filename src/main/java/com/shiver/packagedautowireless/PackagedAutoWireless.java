package com.shiver.packagedautowireless;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.shiver.packagedautowireless.block.BlockWirelessPackagingProvider;
import com.shiver.packagedautowireless.event.CommonEventHandler;
import org.jetbrains.annotations.NotNull;

@Mod(
		modid = PackagedAutoWireless.MOD_ID,
		name = PackagedAutoWireless.NAME,
		version = PackagedAutoWireless.VERSION,
		dependencies = PackagedAutoWireless.DEPENDENCIES
)
public class PackagedAutoWireless {

	public static final String MOD_ID = "packagedautowireless";
	public static final String NAME = "PackagedAuto Wireless";
	public static final String VERSION = Tags.VERSION;
	public static final String DEPENDENCIES =
			"required-after:packagedauto@[1.12.2-1.0.24,);" +
			"required-after:packagingprovider;";

	public static final CreativeTabs CREATIVE_TAB = new CreativeTabs(MOD_ID) {
		@SideOnly(Side.CLIENT)
		@Override
		public @NotNull ItemStack createIcon() {
			return new ItemStack(BlockWirelessPackagingProvider.INSTANCE);
		}
	};

	@SidedProxy(
			clientSide = "com.shiver.packagedautowireless.client.event.ClientEventHandler",
			serverSide = "com.shiver.packagedautowireless.event.CommonEventHandler",
			modId = MOD_ID)
	public static CommonEventHandler eventHandler;

	@EventHandler
	public void onPreInit(FMLPreInitializationEvent event) {
		eventHandler.onPreInit(event);
	}
}
