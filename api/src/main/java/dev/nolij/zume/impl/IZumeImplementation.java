package dev.nolij.zume.impl;

import java.util.function.Function;

public interface IZumeImplementation {
	
	boolean isZoomPressed();
	boolean isZoomInPressed();
	boolean isZoomOutPressed();
	
	CameraPerspective getCameraPerspective();
	
	void onZoomActivate();

	Function<Object, Object> constructConfigScreen();
	
}
