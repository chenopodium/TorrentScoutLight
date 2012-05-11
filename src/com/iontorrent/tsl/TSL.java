package com.iontorrent.tsl;

import com.vaadin.Application;
import com.vaadin.ui.Label;
import com.vaadin.ui.Window;

public class TSL extends Application {
	@Override
	public void init() {
		Window mainWindow = new Window("Torrent Scout Light");
		Label label = new Label("Hello Vaadin user");
		mainWindow.addComponent(label);
		setMainWindow(mainWindow);
	}

}
