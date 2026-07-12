package com.shiver.packagedautowireless.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import thelm.packagedauto.block.BlockBase;
import thelm.packagedauto.tile.TileBase;
import thelm.packagingprovider.tile.TilePackagingProvider;
import com.shiver.packagedautowireless.PackagedAutoWireless;
import com.shiver.packagedautowireless.tile.TileWirelessPackagingProvider;

public class BlockWirelessPackagingProvider extends BlockBase {

	public static final BlockWirelessPackagingProvider INSTANCE = new BlockWirelessPackagingProvider();
	public static final Item ITEM_INSTANCE = new ItemBlock(INSTANCE).setRegistryName("packagedautowireless:wireless_packaging_provider");
	public static final ModelResourceLocation MODEL_LOCATION = new ModelResourceLocation("packagedautowireless:wireless_packaging_provider#normal");

	public BlockWirelessPackagingProvider() {
		super(Material.IRON);
		setHardness(10F);
		setResistance(25F);
		setSoundType(SoundType.METAL);
		setTranslationKey("packagedautowireless.wireless_packaging_provider");
		setRegistryName("packagedautowireless:wireless_packaging_provider");
		setCreativeTab(PackagedAutoWireless.CREATIVE_TAB);
	}

	@Override
	public TileBase createNewTileEntity(World worldIn, int meta) {
		return new TileWirelessPackagingProvider();
	}

	@Override
	public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
		TileEntity tileentity = worldIn.getTileEntity(pos);
		if(tileentity instanceof TilePackagingProvider) {
			TilePackagingProvider provider = (TilePackagingProvider)tileentity;
			if(provider.currentPattern != null) {
				for(ItemStack stack : provider.currentPattern.getInputs()) {
					if(!stack.isEmpty()) {
						InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), stack);
					}
				}
			}
			if(!provider.toSend.isEmpty()) {
				for(ItemStack stack : provider.toSend) {
					if(!stack.isEmpty()) {
						InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), stack);
					}
				}
			}
		}
		super.breakBlock(worldIn, pos, state);
	}

	@Override
	public void neighborChanged(@NotNull IBlockState state, World worldIn, @NotNull BlockPos pos, @NotNull Block blockIn, @NotNull BlockPos fromPos) {
		TileEntity tileentity = worldIn.getTileEntity(pos);
		if(tileentity instanceof TilePackagingProvider) {
			((TilePackagingProvider)tileentity).updatePowered();
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void registerModels() {
		ModelLoader.setCustomModelResourceLocation(ITEM_INSTANCE, 0, MODEL_LOCATION);
	}
}
