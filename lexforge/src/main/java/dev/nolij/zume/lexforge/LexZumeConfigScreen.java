package dev.nolij.zume.lexforge;

import dev.nolij.zume.api.platform.v1.ZumeAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

final class LexZumeConfigScreen {
	
	static void register() {
		ModLoadingContext.get().registerExtensionPoint(
			ConfigScreenHandler.ConfigScreenFactory.class,
			() -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new Screen(Component.nullToEmpty(null)) {
				@Override
				public void tick() {
					ZumeAPI.openConfigFile();
					Minecraft.getInstance().setScreen(parent);
				}
			}));
	}
	
}
