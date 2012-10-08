package com.iontorrent.vaadin.utils;

import com.iontorrent.rawdataaccess.pgmacquisition.RawType;
import com.iontorrent.torrentscout.explorer.ExplorerContext;
import com.vaadin.data.Property;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Select;

public class TypeSelection implements Property.ValueChangeListener {

	Select typeselect;
	RawType type;
	ExplorerContext maincont;
	Property.ValueChangeListener listener;
	
	public TypeSelection(ExplorerContext maincont, RawType type, Property.ValueChangeListener listener) {
		this.maincont = maincont;
		this.type = type;
		this.listener = listener;
	}	
	
	public void addTypeSelection(HorizontalLayout h) {
		typeselect = new Select();

		for (RawType t : RawType.values()) {
			typeselect.addItem(t);
			typeselect.setItemCaption(t, t.getDescription());
		}
		if (type == null) {
			type = RawType.ACQ;
		}
		typeselect.select(type);
		typeselect.setWidth("70px");
		h.addComponent(new Label("File type:"));
		h.addComponent(typeselect);

		typeselect.setImmediate(true);
		typeselect.addListener(this);
	}

	public void valueChange(Property.ValueChangeEvent event) {
		// The event.getProperty() returns the Item ID (IID)
		// of the currently selected item in the component.
		Property id = event.getProperty();
		if (id.getValue() instanceof RawType) {
			type = (RawType) id.getValue();
			maincont.clearData();
			maincont.setFiletype(type);

			if (listener != null) listener.valueChange(event);
			
		}
	}

}
