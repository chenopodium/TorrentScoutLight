package com.vaadin.graphics.canvas;

import com.vaadin.Application;
import com.vaadin.ui.Window;

public class VaadinCanvasApplication extends Application {
	@Override
	public void init() {
		Window mainWindow = new Window("VaadinCanvas Application");
		
		CanvasComposite composite = new CanvasComposite();
		composite.setSizeFull();
		mainWindow.addComponent(composite);
		mainWindow.getContent().setSizeFull();
		setMainWindow(mainWindow);
	}

}
