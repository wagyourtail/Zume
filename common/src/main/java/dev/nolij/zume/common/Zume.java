package dev.nolij.zume.common;

import dev.nolij.zume.common.config.ZumeConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Zume {
	
	//region Constants
	private static final ClassLoader CLASS_LOADER = Zume.class.getClassLoader();
	
	public static final String MOD_ID = "zume";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
	public static final String CONFIG_FILE_NAME = MOD_ID + ".json5";
	public static final ZumeVariant ZUME_VARIANT;
	
	//region Variant Detection
	static {
		var connectorPresent = false;
		try {
			Class.forName("dev.su5ed.sinytra.connector.service.ConnectorLoaderService");
			connectorPresent = true;
		} catch (ClassNotFoundException ignored) {}
		
		if (!connectorPresent &&
			CLASS_LOADER.getResource("net/fabricmc/fabric/api/client/keybinding/v1/KeyBindingHelper.class") != null)
			ZUME_VARIANT = ZumeVariant.MODERN;
		else if (CLASS_LOADER.getResource("net/legacyfabric/fabric/api/client/keybinding/v1/KeyBindingHelper.class") != null)
			ZUME_VARIANT = ZumeVariant.LEGACY;
		else if (CLASS_LOADER.getResource("net/modificationstation/stationapi/api/client/event/option/KeyBindingRegisterEvent.class") != null)
			ZUME_VARIANT = ZumeVariant.PRIMITIVE;
		else if (CLASS_LOADER.getResource("cpw/mods/fml/client/registry/ClientRegistry.class") != null)
			ZUME_VARIANT = ZumeVariant.ARCHAIC_FORGE;
		else if (CLASS_LOADER.getResource("net/minecraftforge/oredict/OreDictionary.class") != null)
			ZUME_VARIANT = ZumeVariant.VINTAGE_FORGE;
		else if (CLASS_LOADER.getResource("net/neoforged/neoforge/common/NeoForge.class") != null)
			ZUME_VARIANT = ZumeVariant.NEOFORGE;
		else {
			String forgeVersion = null;
			
			try {
				final Class<?> forgeVersionClass = Class.forName("net.minecraftforge.versions.forge.ForgeVersion");
				final Method getVersionMethod = forgeVersionClass.getMethod("getVersion");
				forgeVersion = (String) getVersionMethod.invoke(null);
			} catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {}
			
			if (forgeVersion != null) {
				final int major = Integer.parseInt(forgeVersion.substring(0, forgeVersion.indexOf('.')));
				if (major > 40)
					ZUME_VARIANT = ZumeVariant.LEXFORGE;
				else if (major > 36)
					ZUME_VARIANT = ZumeVariant.LEXFORGE18;
				else
					ZUME_VARIANT = ZumeVariant.LEXFORGE16;
			} else {
				ZUME_VARIANT = null;
			}
		}
	}
	//endregion
	
	public static final HostPlatform HOST_PLATFORM;
	
	//region Platform Detection
	static {
		final String OS_NAME = System.getProperty("os.name").toLowerCase();
		
		if (OS_NAME.contains("linux"))
			HOST_PLATFORM = HostPlatform.LINUX;
		else if (OS_NAME.contains("win"))
			HOST_PLATFORM = HostPlatform.WINDOWS;
		else if (OS_NAME.contains("mac"))
			HOST_PLATFORM = HostPlatform.MAC_OS;
		else
			HOST_PLATFORM = HostPlatform.UNKNOWN;
	}
	//endregion
	//endregion
	
	//region Helper Methods
	private static int sign(final int input) {
		return input >> (Integer.SIZE - 1) | 1;
	}
	
	private static double clamp(final double value) {
		return Math.max(Math.min(value, 1D), 0D);
	}
	//endregion
	
	//region Public Members
	public static IZumeImplementation implementation;
	public static ZumeConfig config;
	public static File configFile;
	public static boolean disabled = false;
	//endregion
	
	//region Private Members
	private static double inverseSmoothness = 1D;
	private static double fromZoom = -1D;
	private static double zoom = 1D;
	private static long tweenStart = 0L;
	private static long tweenEnd = 0L;
	private static int scrollDelta = 0;
	private static boolean toggle = false;
	private static boolean wasHeld = false;
	private static boolean wasZooming = false;
	private static long prevRenderTimestamp;
	//endregion
	
	//region Initialization Methods
	/**
	 * Trigger variant detection if class isn't already loaded (it shouldn't be the first time this is called).
	 */
	static void calculateZumeVariant() {}
	
	/**
	 * Invoke this in the initializer of your Zume implementation. 
	 * 
	 * @param implementation The {@linkplain IZumeImplementation} Zume should use. 
	 * @param configFile The {@linkplain File} Zume should use for storing the config.
	 */
	public static void init(final IZumeImplementation implementation, final File configFile) {
		if (Zume.implementation != null)
			throw new AssertionError("Zume already initialized!");
		
		Zume.implementation = implementation;
		Zume.configFile = configFile;
		
		ZumeConfig.init(configFile, config -> {
			Zume.config = config;
			inverseSmoothness = 1D / Zume.config.zoomSmoothnessMs;
			toggle = false;
		});
		
		disabled = config.disable;
	}
	//endregion
	
	//region Zoom Mutation Methods
	private static double getZoom() {
		if (tweenEnd != 0L && config.zoomSmoothnessMs != 0) {
			final long timestamp = System.currentTimeMillis();
			
			if (tweenEnd >= timestamp) {
				final long delta = timestamp - tweenStart;
				final double progress = 1 - delta * inverseSmoothness;
				
				var easedProgress = progress;
				for (var i = 1; i < config.easingExponent; i++)
					easedProgress *= progress;
				easedProgress = 1 - easedProgress;
				
				return fromZoom + ((zoom - fromZoom) * easedProgress);
			}
		}
		
		return zoom;
	}
	
	private static void setZoom(final double targetZoom) {
		if (config.zoomSmoothnessMs == 0) {
			setZoomNoTween(targetZoom);
			return;
		}
		
		final double currentZoom = getZoom();
		tweenStart = System.currentTimeMillis();
		tweenEnd = tweenStart + config.zoomSmoothnessMs;
		fromZoom = currentZoom;
		zoom = clamp(targetZoom);
	}
	
	private static void setZoomNoTween(final double targetZoom) {
		tweenStart = 0L;
		tweenEnd = 0L;
		fromZoom = -1D;
		zoom = clamp(targetZoom);
	}
	//endregion
	
	//region API Methods
	/**
	 * Attempts to open the {@linkplain Zume#configFile Zume config file} in the system's text editor.
	 */
	public static void openConfigFile() {
		try {
			try {
				Desktop.getDesktop().open(configFile);
			} catch (HeadlessException ignored) {
				final String CONFIG_PATH = configFile.getCanonicalPath();
				if (HOST_PLATFORM == HostPlatform.UNKNOWN) {
					Zume.LOGGER.error("Error opening config file: Unsupported Platform!");
					return;
				}
				
				final ProcessBuilder builder = new ProcessBuilder().inheritIO();
				
				switch (HOST_PLATFORM) {
					case LINUX -> builder.command("xdg-open", CONFIG_PATH);
					case WINDOWS -> builder.command("rundll32", "url.dll,FileProtocolHandler", CONFIG_PATH);
					case MAC_OS -> builder.command("open", "-t", CONFIG_PATH);
				}
				
				builder.start();
			}
		} catch (IOException e) {
			Zume.LOGGER.error("Error opening config file: ", e);
		}
	}
	
	/**
	 * ONLY INVOKE THIS METHOD WHEN {@linkplain Zume#isFOVModified()} RETURNS `true`. This check was explicitly excluded 
	 * for efficiency and for mixin compatibility. The {@linkplain IZumeImplementation} is responsible for this check.
	 * 
	 * @param original The unmodified FOV value
	 * {@return The new FOV transformed by Zume}
	 */
	public static double transformFOV(final double original) {
		var zoom = getZoom();
		
		if (config.useQuadratic) {
			zoom *= zoom;
		}
		
		return config.minFOV + ((original - config.minFOV) * zoom);
	}
	
	/**
	 * The return value of this method can be safely used without checking whether Zume is active.
	 * 
	 * @param original The unmodified cinematic camera state
	 * {@return The new cinematic camera state, transformed by Zume}
	 */
	public static boolean transformCinematicCamera(final boolean original) {
		if (Zume.config.enableCinematicZoom && isEnabled())
			return true;
		
		return original;
	}
	
	/**
	 * The return value of this method can be safely used without checking whether Zume is active.
	 *
	 * @param original The unmodified mouse sensitivity
	 * {@return The new mouse sensitivity, transformed by Zume}
	 */
	public static double transformMouseSensitivity(final double original) {
		if (!isEnabled())
			return original;
		
		final double zoom = getZoom();
		var result = original;
		
		result *= config.mouseSensitivityFloor + (zoom * (1 - config.mouseSensitivityFloor));
		
		return result;
	}
	
	public static boolean shouldCancelScroll() {
		return Zume.config.enableZoomScrolling && isEnabled();
	}
	
	/**
	 * The return value of this method can be safely used without checking whether Zume is active.
	 *
	 * @param scrollDelta The scroll delta (magnitude will be ignored, only the sign is used)
	 * {@return `true` if the invoker should prevent further handling of this scroll event}
	 */
	public static boolean interceptScroll(final int scrollDelta) {
        if (!shouldCancelScroll() || scrollDelta == 0)
            return false;
		
        Zume.scrollDelta += sign(scrollDelta);
        return true;
    }
	
	/**
	 * Returns `true` if Zume is active.
	 */
	public static boolean isEnabled() {
		if (disabled || implementation == null)
			return false;
		
		if (config.toggleMode)
			return toggle;
		
		return implementation.isZoomPressed();
	}
	
	/**
	 * Returns `true` if there is an active Zoom animation. Important distinction between this method and
	 * {@linkplain Zume#isEnabled()}: This method will return `true` when
	 * {@linkplain Zume#isEnabled()} returns `false` if the zoom out animation is still playing. Use this method to 
	 * determine whether the user's FOV and camera values should be hooked. For other scenarios, use
	 * {@linkplain Zume#isEnabled()}.
	 */
	public static boolean isFOVModified() {
		if (disabled)
			return false;
		
		return isEnabled() || (zoom == 1D && tweenEnd != 0L && System.currentTimeMillis() < tweenEnd);
	}
	
	/**
	 * This should be invoked once at the beginning of every frame. It will handle Keybindings and Scrolling if the
	 * {@linkplain IZumeImplementation} is implemented properly, and if {@linkplain Zume#scrollDelta} is maintained 
	 * via use of {@linkplain Zume#interceptScroll(int)}.
	 */
	public static void render() {
		if (disabled)
			return;
		
		final long timestamp = System.currentTimeMillis();
		final boolean held = implementation.isZoomPressed();
		final boolean zooming = isEnabled();
		
		if (config.toggleMode && held && !wasHeld)
			toggle = !toggle;
		
		if (zooming) {
			if (!wasZooming) {
				implementation.onZoomActivate();
				setZoom(config.defaultZoom);
			}
			
			final long timeDelta = timestamp - prevRenderTimestamp;
			
			if (config.enableZoomScrolling && scrollDelta != 0) {
				setZoom(zoom - scrollDelta * config.zoomSpeed * 4E-3D);
			} else if (implementation.isZoomInPressed() ^ implementation.isZoomOutPressed()) {
				final double interpolatedIncrement = config.zoomSpeed * 1E-4D * timeDelta;
				
				if (implementation.isZoomInPressed())
					setZoom(zoom - interpolatedIncrement);
				else if (implementation.isZoomOutPressed())
					setZoom(zoom + interpolatedIncrement);
			}
		} else if (wasZooming) {
			setZoom(1D);
		}
		
		scrollDelta = 0;
		prevRenderTimestamp = timestamp;
		wasHeld = held;
		wasZooming = zooming;
	}
	//endregion
	
}
