package dev.nolij.zume.archaic;

import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

public enum ZumeKeyBind {
	
	ZOOM("key.zume.zoom", Keyboard.KEY_Z),
	ZOOM_IN("key.zume.zoom_in", Keyboard.KEY_EQUALS),
	ZOOM_OUT("key.zume.zoom_out", Keyboard.KEY_MINUS),
	
	;
	
	public final KeyBinding value;
	
	public boolean isPressed() {
		return value.getIsKeyPressed();
	}
	
	ZumeKeyBind(String translationKey, int code, String category) {
		this.value = new KeyBinding(translationKey, code, category);
	}
	
	ZumeKeyBind(String translationKey, int code) {
		this(translationKey, code, "category.zume");
	}
	
}