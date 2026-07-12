package com.shiver.packagedautowireless.item;

import java.util.List;

import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import thelm.packagedauto.api.DirectionalGlobalPos;
import thelm.packagedauto.client.IModelRegister;
import com.shiver.packagedautowireless.PackagedAutoWireless;
import com.shiver.packagedautowireless.tile.TileWirelessPackagingProvider;

public class ItemPackageConnector extends Item implements IModelRegister {

	public static final ItemPackageConnector INSTANCE = new ItemPackageConnector();
	public static final ModelResourceLocation MODEL_LOCATION = new ModelResourceLocation("packagedautowireless:package_connector#inventory");
	public static final ModelResourceLocation MODEL_LOCATION_BOUND = new ModelResourceLocation("packagedautowireless:package_connector_bound#inventory");

	protected ItemPackageConnector() {
		setRegistryName("packagedautowireless:package_connector");
		setTranslationKey("packagedautowireless.package_connector");
		setCreativeTab(PackagedAutoWireless.CREATIVE_TAB);
		setMaxStackSize(64);
	}

	@Override
	public @NotNull EnumActionResult onItemUseFirst(@NotNull EntityPlayer player, World world, @NotNull BlockPos pos, @NotNull EnumFacing side, float hitX, float hitY, float hitZ, @NotNull EnumHand hand) {
		if(world.isRemote) {
			return EnumActionResult.SUCCESS;
		}

		ItemStack stack = player.getHeldItem(hand);
		TileEntity tile = world.getTileEntity(pos);

		if(tile instanceof TileWirelessPackagingProvider) {
			TileWirelessPackagingProvider provider = (TileWirelessPackagingProvider)tile;
			if(player.isSneaking()) {
				int cleared = provider.clearAllTargets();
				player.sendStatusMessage(new TextComponentTranslation(
						"item.packagedautowireless.package_connector.cleared", cleared), true);
				return EnumActionResult.SUCCESS;
			}

			DirectionalGlobalPos bound = getDirectionalGlobalPos(stack);
			if(bound == null) {
				// Unbound connector: list already linked targets
				sendLinkedTargets(player, provider);
				return EnumActionResult.SUCCESS;
			}
			if(bound.dimension() != world.provider.getDimension()) {
				player.sendStatusMessage(new TextComponentTranslation(
						"item.packagedautowireless.package_connector.wrong_dimension"), true);
				return EnumActionResult.FAIL;
			}
			if(provider.addTarget(bound)) {
				player.sendStatusMessage(new TextComponentTranslation(
						"item.packagedautowireless.package_connector.linked",
						provider.linkedTargets.size()), true);
				// Clear bind so the next package block can be selected
				setDirectionalGlobalPos(stack, null);
				return EnumActionResult.SUCCESS;
			}
			player.sendStatusMessage(new TextComponentTranslation(
					"item.packagedautowireless.package_connector.already_linked"), true);
			return EnumActionResult.FAIL;
		}

		// Bind package block position + clicked face
		if(!player.isSneaking()) {
            getDirectionalGlobalPos(stack);// Already bound — allow to rebind by overwriting
            int dim = world.provider.getDimension();
			DirectionalGlobalPos globalPos = new DirectionalGlobalPos(dim, pos, side);
			if(stack.getCount() > 1) {
				ItemStack stack1 = stack.splitStack(1);
				setDirectionalGlobalPos(stack1, globalPos);
				if(!player.inventory.addItemStackToInventory(stack1)) {
					EntityItem item = new EntityItem(world, player.posX, player.posY, player.posZ, stack1);
					item.setThrower(player.getName());
					world.spawnEntity(item);
				}
			}
			else {
				setDirectionalGlobalPos(stack, globalPos);
			}
			player.sendStatusMessage(new TextComponentTranslation(
					"item.packagedautowireless.package_connector.bound",
					pos.getX(), pos.getY(), pos.getZ(),
					I18n.translateToLocal("misc.packagedauto."+side.getName())), true);
			return EnumActionResult.SUCCESS;
		}

		return EnumActionResult.PASS;
	}

	@Override
	public @NotNull ActionResult<ItemStack> onItemRightClick(World worldIn, @NotNull EntityPlayer playerIn, @NotNull EnumHand handIn) {
		if(!worldIn.isRemote && playerIn.isSneaking() && isBound(playerIn.getHeldItem(handIn))) {
			ItemStack stack = playerIn.getHeldItem(handIn).copy();
			setDirectionalGlobalPos(stack, null);
			playerIn.sendStatusMessage(new TextComponentTranslation(
					"item.packagedautowireless.package_connector.unbind_item"), true);
			return new ActionResult<>(EnumActionResult.SUCCESS, stack);
		}
		return super.onItemRightClick(worldIn, playerIn, handIn);
	}

	@Override
	public void addInformation(@NotNull ItemStack stack, World worldIn, List<String> tooltip, @NotNull ITooltipFlag flagIn) {
		tooltip.add(I18n.translateToLocal("item.packagedautowireless.package_connector.tooltip.1"));
		tooltip.add(I18n.translateToLocal("item.packagedautowireless.package_connector.tooltip.2"));
		DirectionalGlobalPos pos = getDirectionalGlobalPos(stack);
		if(pos != null) {
			tooltip.add(I18n.translateToLocalFormatted("misc.packagedauto.dimension", pos.dimension()));
			String posString = "["+pos.x()+", "+pos.y()+", "+pos.z()+"]";
			tooltip.add(I18n.translateToLocalFormatted("misc.packagedauto.position", posString));
			String dirString = I18n.translateToLocal("misc.packagedauto."+pos.direction().getName());
			tooltip.add(I18n.translateToLocalFormatted("misc.packagedauto.direction", dirString));
		}
		super.addInformation(stack, worldIn, tooltip, flagIn);
	}

	public DirectionalGlobalPos getDirectionalGlobalPos(ItemStack stack) {
		if(isBound(stack)) {
			NBTTagCompound nbt = stack.getTagCompound();
            int dimension = 0;
            if (nbt != null) {
                dimension = nbt.getInteger("Dimension");
            }
            int[] posArray = null;
            if (nbt != null) {
                posArray = nbt.getIntArray("Position");
            }
            BlockPos blockPos = null;
            if (posArray != null) {
                blockPos = new BlockPos(posArray[0], posArray[1], posArray[2]);
            }
            EnumFacing direction = null;
            if (nbt != null) {
                direction = EnumFacing.byIndex(nbt.getByte("Direction"));
            }
            return new DirectionalGlobalPos(dimension, blockPos, direction);
		}
		return null;
	}

	public void setDirectionalGlobalPos(ItemStack stack, DirectionalGlobalPos pos) {
		if(pos != null) {
			if(!stack.hasTagCompound()) {
				stack.setTagCompound(new NBTTagCompound());
			}
			NBTTagCompound nbt = stack.getTagCompound();
            if (nbt != null) {
                nbt.setInteger("Dimension", pos.dimension());
            }
            if (nbt != null) {
                nbt.setIntArray("Position", new int[] {pos.x(), pos.y(), pos.z()});
            }
            if (nbt != null) {
                nbt.setByte("Direction", (byte)pos.direction().getIndex());
            }
        }
		else if(stack.hasTagCompound()) {
			NBTTagCompound nbt = stack.getTagCompound();
            if (nbt != null) {
                nbt.removeTag("Dimension");
            }
            if (nbt != null) {
                nbt.removeTag("Position");
            }
            if (nbt != null) {
                nbt.removeTag("Direction");
            }
            if (nbt != null && nbt.isEmpty()) {
                stack.setTagCompound(null);
            }
        }
	}

	public boolean isBound(ItemStack stack) {
		NBTTagCompound nbt = stack.getTagCompound();
		return nbt != null && nbt.hasKey("Dimension") && nbt.hasKey("Position") && nbt.hasKey("Direction");
	}

	protected void sendLinkedTargets(EntityPlayer player, TileWirelessPackagingProvider provider) {
		if(provider.linkedTargets.isEmpty()) {
			player.sendMessage(new TextComponentTranslation(
					"item.packagedautowireless.package_connector.list_empty"));
			return;
		}
		player.sendMessage(new TextComponentTranslation(
				"item.packagedautowireless.package_connector.list_header",
				provider.linkedTargets.size()));
		int index = 1;
		for(DirectionalGlobalPos target : provider.linkedTargets) {
			String dir = I18n.translateToLocal("misc.packagedauto."+target.direction().getName());
			player.sendMessage(new TextComponentTranslation(
					"item.packagedautowireless.package_connector.list_entry",
					index++,
					target.dimension(),
					target.x(), target.y(), target.z(),
					dir));
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void registerModels() {
		ModelLoader.setCustomMeshDefinition(this, stack->isBound(stack) ? MODEL_LOCATION_BOUND : MODEL_LOCATION);
		ModelBakery.registerItemVariants(this, MODEL_LOCATION, MODEL_LOCATION_BOUND);
	}
}
