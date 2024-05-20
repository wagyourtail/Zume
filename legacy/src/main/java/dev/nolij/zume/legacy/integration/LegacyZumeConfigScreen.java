package dev.nolij.zume.legacy.integration;

import dev.nolij.zume.api.platform.v1.ZumeAPI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public class LegacyZumeConfigScreen extends Screen {

	private final Screen parent;

	public LegacyZumeConfigScreen(Screen parent) {
		this.parent = parent;
	}
	
	@Override
	public void init() {
		ZumeAPI.openConfigFile();
		
		MinecraftClient.getInstance().setScreen(parent);
	}
	
	@SuppressWarnings("unused")
	public void render(int mouseX, int mouseY, float delta) {
		init();
	}
	
}
