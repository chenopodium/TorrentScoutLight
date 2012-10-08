package com.iontorrent.vaadin.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.torrentscout.explorer.ExplorerContext;
import com.iontorrent.vaadin.TSVaadin;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Select;

public class AreaSelect {

	ExplorerContext maincont;
	TSVaadin app;
	ExperimentContext exp;
	Select.ValueChangeListener listener;
	int maxsize;
	
	public AreaSelect(TSVaadin app, ExplorerContext maincont, Select.ValueChangeListener listener, int maxsize) {
		this.maincont = maincont;
		this.app = app;
		 exp = maincont.getExp();
		 this.listener = listener;
		 this.maxsize = maxsize;
	}
		
	public void addComponents(AbstractLayout hor){
		final Select rastersel = new Select();
		rastersel.setImmediate(true);
		rastersel.setDescription("Pick the size of the visible chip area");
		rastersel.addItem("25 x 25");
		rastersel.addItem("50 x 50");
		if (maxsize <=0) maxsize = Integer.MAX_VALUE;
		if (maxsize > 50) rastersel.addItem("100 x 100");
		if (maxsize > 100) rastersel.addItem("200 x 200");
		if (maxsize > 200) rastersel.addItem("400 x 400");
		if (maxsize > 400) rastersel.addItem("800 x 800");
		if (maxsize > 800) rastersel.addItem("1600 x 1600");
		if (maxsize > 800) rastersel.addItem("chip size");
		rastersel.setWidth("100px");
		int size = maincont.getRasterSize();
		
		
		if (size <= 0)
			size = Math.max(exp.getNrcols(), exp.getNrrows());
		p("Got raster size from maincont: " + size);
		
		
		if (size == 25 || size == 100 || size == 200 || size == 400 || size == 800 || size == 1600) {
			rastersel.select(size + " x " + size);
		} else
			rastersel.select("chip size");
		rastersel.addListener(new Select.ValueChangeListener() {

			@Override
			public void valueChange(ValueChangeEvent event) {
				String val = "" + rastersel.getValue();
				int size = Math.max(exp.getNrcols(), exp.getNrrows());
				if (val.startsWith("chip")) {
					// chip size
				} else {
					try {
						val = val.substring(0, 3).trim();
						size = Integer.parseInt(val);

					} catch (Exception e) {
						p("Could not parse: " + val);
					}
				}
				if (size != maincont.getRasterSize()) {
					maincont.setRasterSize(size);					
					app.reopenOtherMaskedit(false);
					app.reopenProcess(false);
					app.reopenFit();
					if (listener != null) listener.valueChange(event);
				}
			}

		});

		hor.addComponent(new Label(" Area:"));
		hor.addComponent(rastersel);
	}
	
	private static void p(String msg) {
		System.out.println("AreaSelect: " + msg);
		Logger.getLogger(AreaSelect.class.getName()).log(Level.INFO, msg);
	}
}
