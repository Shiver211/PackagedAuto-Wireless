package com.shiver.packagedautowireless.tile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.PowerUnits;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.energy.IEnergyGrid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import thelm.packagedauto.api.DirectionalGlobalPos;
import thelm.packagedauto.api.IPackageCraftingMachine;
import thelm.packagedauto.api.IRecipeInfo;
import thelm.packagedauto.api.ISettingsCloneable;
import thelm.packagedauto.api.MiscUtil;
import thelm.packagedauto.integration.appeng.recipe.PackageCraftingPatternHelper;
import thelm.packagedauto.integration.appeng.recipe.RecipeCraftingPatternHelper;
import thelm.packagedauto.tile.TilePackager;
import thelm.packagedauto.tile.TileUnpackager;
import thelm.packagingprovider.recipe.DirectCraftingPatternHelper;
import thelm.packagingprovider.tile.TilePackagingProvider;

/**
 * ME Wireless Packaging Provider — extends the stock ME Packaging Provider
 * and redirects package push targets to a remote linked list (1→N).
 */
public class TileWirelessPackagingProvider extends TilePackagingProvider {

	public final List<DirectionalGlobalPos> linkedTargets = new ArrayList<>();
	/** Remote inventory send target (pos + face). Preferred over {@link #sendDirection}. */
	public DirectionalGlobalPos sendTarget;

	@Override
	protected String getLocalizedName() {
		return I18n.translateToLocal("tile.packagedautowireless.wireless_packaging_provider.name");
	}

	@Override
	public String getConfigTypeName() {
		return "tile.packagedautowireless.wireless_packaging_provider.name";
	}

	public boolean addTarget(DirectionalGlobalPos target) {
		if(target == null) {
			return false;
		}
		if(world != null && world.provider.getDimension() != target.dimension()) {
			return false;
		}
		for(DirectionalGlobalPos existing : linkedTargets) {
			if(sameTarget(existing, target)) {
				return false;
			}
		}
		linkedTargets.add(target);
		markDirty();
		return true;
	}

	public int clearAllTargets() {
		int count = linkedTargets.size();
		linkedTargets.clear();
		markDirty();
		return count;
	}

	public static boolean sameTarget(DirectionalGlobalPos a, DirectionalGlobalPos b) {
		return a.dimension() == b.dimension()
				&& a.blockPos().equals(b.blockPos())
				&& a.direction() == b.direction();
	}

	@Override
	protected void sendUnpackaging() {
		if(toSend.isEmpty()) {
			return;
		}
		// Remote linked target takes priority over adjacent sendDirection
		if(sendTarget != null) {
			if(world.provider.getDimension() != sendTarget.dimension() || !world.isBlockLoaded(sendTarget.blockPos())) {
				return;
			}
			TileEntity tile = world.getTileEntity(sendTarget.blockPos());
			EnumFacing facing = sendTarget.direction();
			if(!validSendTarget(tile, facing)) {
				sendTarget = null;
				return;
			}
            IItemHandler itemHandler = null;
            if (tile != null) {
                itemHandler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
            }
            if(itemHandler == null) {
				sendTarget = null;
				return;
			}
			for(int i = 0; i < toSend.size(); ++i) {
				ItemStack stack = toSend.get(i);
				ItemStack stackRem = MiscUtil.insertItem(itemHandler, stack, sendOrdered, false);
				toSend.set(i, stackRem);
			}
			toSend.removeIf(ItemStack::isEmpty);
			markDirty();
			return;
		}
		// Adjacent / ME network fallback from parent
		super.sendUnpackaging();
	}

	@Override
	public boolean pushPattern(ICraftingPatternDetails patternDetails, InventoryCrafting table) {
		if(hostHelper.isActive() && !isBusy()) {
			IGrid grid = hostHelper.getNode().getGrid();
			IEnergyGrid energyGrid = grid.getCache(IEnergyGrid.class);
			double conversion = PowerUnits.RF.convertTo(PowerUnits.AE, 1);
			IRecipeInfo recipe = null;
			if(patternDetails instanceof DirectCraftingPatternHelper) {
				recipe = ((DirectCraftingPatternHelper)patternDetails).recipe;
			}
			else if(patternDetails instanceof RecipeCraftingPatternHelper) {
				recipe = ((RecipeCraftingPatternHelper)patternDetails).recipe;
			}
			else if(patternDetails instanceof PackageCraftingPatternHelper) {
				double request = TilePackager.energyReq*2*conversion;
				if(request - energyGrid.extractAEPower(request, Actionable.SIMULATE, PowerMultiplier.CONFIG) > 0.0001) {
					return false;
				}
				energyGrid.extractAEPower(request, Actionable.MODULATE, PowerMultiplier.CONFIG);
				currentPattern = ((PackageCraftingPatternHelper)patternDetails).pattern;
				return true;
			}
			if(recipe != null) {
				double request = (TilePackager.energyReq*2+TileUnpackager.energyUsage)*conversion;
				if(request - energyGrid.extractAEPower(request, Actionable.SIMULATE, PowerMultiplier.CONFIG) > 0.0001) {
					return false;
				}
				if(linkedTargets.isEmpty()) {
					return false;
				}
				List<DirectionalGlobalPos> targets = new ArrayList<>(linkedTargets);
				Collections.rotate(targets, roundRobinIndex % targets.size());
				if(recipe.getRecipeType().hasMachine()) {
					for(DirectionalGlobalPos target : targets) {
						if(world.provider.getDimension() != target.dimension() || !world.isBlockLoaded(target.blockPos())) {
							continue;
						}
						TileEntity tile = world.getTileEntity(target.blockPos());
						if(tile instanceof IPackageCraftingMachine) {
							IPackageCraftingMachine machine = (IPackageCraftingMachine)tile;
							if(!machine.isBusy() && machine.acceptPackage(recipe, Lists.transform(recipe.getInputs(), ItemStack::copy), target.direction(), blocking)) {
								// Match parent: simulate extract after accept (parent does SIMULATE here)
								energyGrid.extractAEPower(request, Actionable.SIMULATE, PowerMultiplier.CONFIG);
								roundRobinIndex = (roundRobinIndex+1) % linkedTargets.size();
								return true;
							}
						}
					}
					return false;
				}
				else {
					List<ItemStack> pendingSend = new ArrayList<>();
					recipe.getInputs().stream().map(ItemStack::copy).forEach(pendingSend::add);
					for(DirectionalGlobalPos target : targets) {
						if(world.provider.getDimension() != target.dimension() || !world.isBlockLoaded(target.blockPos())) {
							continue;
						}
						TileEntity tile = world.getTileEntity(target.blockPos());
						if(!validSendTarget(tile, target.direction())) {
							continue;
						}
                        IItemHandler itemHandler = null;
                        if (tile != null) {
                            itemHandler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, target.direction());
                        }
                        if(itemHandler == null) {
							continue;
						}
						if(blocking && !MiscUtil.isEmpty(itemHandler)) {
							continue;
						}
						boolean acceptsAll = true;
                        for (ItemStack stack : pendingSend) {
                            ItemStack stackRem = MiscUtil.insertItem(itemHandler, stack, false, true);
                            acceptsAll &= stackRem.getCount() < stack.getCount();
                        }
						if(acceptsAll) {
							energyGrid.extractAEPower(request, Actionable.MODULATE, PowerMultiplier.CONFIG);
							sendTarget = target;
							sendDirection = null;
							this.toSend.addAll(pendingSend);
							sendOrdered = recipe.getRecipeType().isOrdered();
							roundRobinIndex = (roundRobinIndex+1) % linkedTargets.size();
							sendUnpackaging();
							return true;
						}
					}
					return false;
				}
			}
		}
		return false;
	}

	@Override
	public ISettingsCloneable.Result loadConfig(NBTTagCompound nbt, EntityPlayer player) {
		ISettingsCloneable.Result result = super.loadConfig(nbt, player);
		if(nbt.hasKey("LinkedTargets")) {
			linkedTargets.clear();
			NBTTagList list = nbt.getTagList("LinkedTargets", 10);
			for(int i = 0; i < list.tagCount(); ++i) {
				DirectionalGlobalPos target = readTarget(list.getCompoundTagAt(i));
				if(target != null) {
					linkedTargets.add(target);
				}
			}
			markDirty();
		}
		return result;
	}

	@Override
	public ISettingsCloneable.Result saveConfig(NBTTagCompound nbt, EntityPlayer player) {
		ISettingsCloneable.Result result = super.saveConfig(nbt, player);
		nbt.setTag("LinkedTargets", writeTargets(linkedTargets));
		return result;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		linkedTargets.clear();
		if(nbt.hasKey("LinkedTargets")) {
			NBTTagList list = nbt.getTagList("LinkedTargets", 10);
			for(int i = 0; i < list.tagCount(); ++i) {
				DirectionalGlobalPos target = readTarget(list.getCompoundTagAt(i));
				if(target != null) {
					linkedTargets.add(target);
				}
			}
		}
		if(nbt.hasKey("SendTarget")) {
			sendTarget = readTarget(nbt.getCompoundTag("SendTarget"));
		}
		else {
			sendTarget = null;
		}
	}

	@Override
	public @NotNull NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setTag("LinkedTargets", writeTargets(linkedTargets));
		if(sendTarget != null) {
			nbt.setTag("SendTarget", writeTarget(sendTarget));
		}
		return nbt;
	}

	public static NBTTagCompound writeTarget(DirectionalGlobalPos pos) {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setInteger("Dimension", pos.dimension());
		tag.setIntArray("Position", new int[] {pos.x(), pos.y(), pos.z()});
		tag.setByte("Direction", (byte)pos.direction().getIndex());
		return tag;
	}

	public static NBTTagList writeTargets(List<DirectionalGlobalPos> targets) {
		NBTTagList list = new NBTTagList();
		for(DirectionalGlobalPos target : targets) {
			list.appendTag(writeTarget(target));
		}
		return list;
	}

	public static DirectionalGlobalPos readTarget(NBTTagCompound tag) {
		if(tag == null || !tag.hasKey("Position")) {
			return null;
		}
		int dimension = tag.getInteger("Dimension");
		int[] posArray = tag.getIntArray("Position");
		if(posArray.length < 3) {
			return null;
		}
		BlockPos blockPos = new BlockPos(posArray[0], posArray[1], posArray[2]);
		EnumFacing direction = EnumFacing.byIndex(tag.getByte("Direction"));
		return new DirectionalGlobalPos(dimension, blockPos, direction);
	}
}
