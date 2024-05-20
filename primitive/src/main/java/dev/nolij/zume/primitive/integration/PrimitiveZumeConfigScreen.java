package dev.nolij.zume.primitive.integration;

import dev.nolij.zume.api.platform.v1.ZumeAPI;
import dev.nolij.zume.mixin.primitive.MinecraftAccessor;
import net.minecraft.client.gui.screen.Screen;

public class PrimitiveZumeConfigScreen extends Screen {

	private final Screen parent;

	public PrimitiveZumeConfigScreen(Screen parent) {
		this.parent = parent;
	}
	
	@Override
	public void init() {
		ZumeAPI.openConfigFile();

		MinecraftAccessor.getInstance().setScreen(parent);
	}
	
	@SuppressWarnings("unused")
	public void render(int mouseX, int mouseY, float delta) {
		init();
	}
	
}
