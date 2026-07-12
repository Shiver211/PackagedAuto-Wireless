package com.shiver.packagedautowireless.client.event;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import thelm.packagedauto.client.IModelRegister;
import com.shiver.packagedautowireless.event.CommonEventHandler;

public class ClientEventHandler extends CommonEventHandler {

	private static final List<IModelRegister> MODEL_REGISTER_LIST = new ArrayList<>();

	@Override
	public void registerBlock(Block block) {
		super.registerBlock(block);
		if(block instanceof IModelRegister) {
			MODEL_REGISTER_LIST.add((IModelRegister)block);
		}
	}

	@Override
	public void registerItem(Item item) {
		super.registerItem(item);
		if(item instanceof IModelRegister) {
			MODEL_REGISTER_LIST.add((IModelRegister)item);
		}
	}

	@Override
	public void onPreInit(FMLPreInitializationEvent event) {
		super.onPreInit(event);
		registerModels();
	}

	protected void registerModels() {
		for(IModelRegister model : MODEL_REGISTER_LIST) {
			model.registerModels();
		}
	}
}
