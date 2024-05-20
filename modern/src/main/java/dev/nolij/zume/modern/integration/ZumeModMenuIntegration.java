package dev.nolij.zume.modern.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import dev.nolij.zume.impl.Zume;
import io.github.prospector.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Proxy;
import java.util.function.Function;

import static dev.nolij.zume.impl.ZumeConstants.MOD_ID;

public class ZumeModMenuIntegration implements ModMenuApi {
	
	@Override
	public String getModId() {
		return MOD_ID;
	}
	
	@Override
	public Function<Screen, ? extends Screen> getConfigScreenFactory() {
		return (Function) Zume.implementation.constructConfigScreen();
	}
	
	@Override
	public ConfigScreenFactory getModConfigScreenFactory() {
		return (ConfigScreenFactory) Proxy.newProxyInstance(
			ZumeModMenuIntegration.class.getClassLoader(),
			new Class[]{ConfigScreenFactory.class},
			(proxy, method, args) -> getConfigScreenFactory().apply((Screen) args[0])
		);
	}

}
