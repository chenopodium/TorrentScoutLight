package com.iontorrent.vaadin.mask;

import java.util.ArrayList;

import com.iontorrent.guiutils.widgets.Widget;
import com.iontorrent.rawdataaccess.pgmacquisition.RawType;
import com.iontorrent.rawdataaccess.wells.BitMask;
import com.iontorrent.torrentscout.explorer.ContextChangedListener;
import com.iontorrent.torrentscout.explorer.ExplorerContext;
import com.iontorrent.wellmodel.RasterData;
import com.iontorrent.wellmodel.WellCoordinate;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.ui.AbstractComponentContainer;
import com.vaadin.ui.Label;
import com.vaadin.ui.Select;

public class MaskSelect {

	public static int PIN = 0;
	public static int BG = 1;
	public static int USE = 2;
	public static int HISTO = 3;
	public static int NONE = -1;

	Select sel;
	ExplorerContext exp;
	ArrayList<BitMask> masks;
	int type;
	Property.ValueChangeListener listener;
	BitMask curmask;
	private String title;
	Property.ValueChangeListener thislistener;
	String desc;
	String key;
	AbstractComponentContainer h;
	private BitMask selectedMask;

	public MaskSelect(String key, String title, String desc, ExplorerContext exp, int type, Property.ValueChangeListener listener, BitMask curmask) {
		this.exp = exp;
		this.key = key;
		masks = exp.getMasks();
		this.type = type;
		this.title = title;
		this.listener = listener;
		this.desc = desc;
		this.curmask = curmask;
		exp.addListener(new ContextChangedListener() {

			@Override
			public void flowChanged(ArrayList<Integer> flows) {
				// TODO Auto-generated method stub

			}

			@Override
			public void flowChanged(int flow) {
				// TODO Auto-generated method stub

			}

			@Override
			public void frameChanged(int frame) {
				// TODO Auto-generated method stub

			}

			@Override
			public void maskChanged(BitMask mask) {
				// rebuildCombo();

			}

			@Override
			public void maskSelected(BitMask mask) {
				// TODO Auto-generated method stub

			}

			@Override
			public void maskAdded(BitMask mask) {
				p("maskAdded called from explorercontext: " + mask);
				if (sel != null) {

					sel.addItem(mask);
				} else
					p("sel is null, not adding mask");

			}

			@Override
			public void maskRemoved(BitMask mask) {
				if (sel != null) sel.removeItem(mask);

			}

			@Override
			public void coordChanged(WellCoordinate coord) {
				// TODO Auto-generated method stub

			}

			@Override
			public void dataAreaCoordChanged(WellCoordinate coord) {
				p("dataAreaCoordChanged rebuilding combo");
				rebuildCombo();

			}

			@Override
			public void masksChanged() {
				rebuildCombo();

			}

			@Override
			public void widgetChanged(Widget w) {
				// TODO Auto-generated method stub

			}

			@Override
			public void fileTypeChanged(RawType t) {
				// TODO Auto-generated method stub

			}

			@Override
			public void dataChanged(RasterData data, int startrow, int startcol, int startframe, int endrow, int endcol, int endframe) {
				// TODO Auto-generated method stub

			}
		});
	}

	private BitMask getMainContMask() {
		BitMask m = null;
		if (type == PIN) m = exp.getIgnoreMask();
		else if (type == BG) m = exp.getBgMask();
		else if (type == USE) m = exp.getSignalMask();
		else if (type == HISTO) m = exp.getHistoMask();
		return m;
	}

	private void setMainContMask(BitMask m) {
		// if (m == null) return;
		if (type == PIN) exp.setIgnoreMask(m);
		else if (type == BG) exp.setBgMask(m);
		else if (type == USE) exp.setSignalMask(m);
		else if (type == HISTO) exp.setHistoMask(m);
	}

	public BitMask getSelectedMask() {
		return selectedMask;
	}

	public BitMask getSelection() {
		return selectedMask;
	}

	private void p(String msg) {
		////system.out.println("Maskselect: " + key + ":" + msg);
		//Logger.getLogger(MaskSelect.class.getName()).log(Level.INFO, msg);
	}

	private void rebuildCombo() {
		boolean isnew = false;
		if (sel == null) {
			p("new maskselect");
			sel = new Select();
			sel.setNullSelectionAllowed(true);
			sel.setDescription(desc);
			sel.setWidth("90px");
			isnew = true;
		} else {
			p("rebuilding maskselect. not new. removing all listeners");
			sel.removeListener(thislistener);
			sel.removeAllItems();
		}

		for (BitMask m : masks) {
			// p("Adding mask: "+m.getName());
			sel.addItem(m);
		}
		sel.setImmediate(true);
		if (curmask == null) curmask = getMainContMask();
		if (curmask != null) {
			p("selecting mask: " + curmask);
			sel.select(curmask);
			sel.setValue(curmask);
			this.selectedMask = curmask;

			// sel.setc
		}

		if (thislistener == null) {
			thislistener = new Property.ValueChangeListener() {
				@Override
				public void valueChange(ValueChangeEvent event) {
					Property id = event.getProperty();
					if (id.getValue() instanceof BitMask) {

						BitMask m = (BitMask) id.getValue();
						selectedMask = m;
						setMainContMask(m);
						p("Mask got selected: " + m);
						
						if (listener != null) listener.valueChange(event);

					}

				}
			};
		}
		sel.addListener(thislistener);

	}

	public void addGuiElements(AbstractComponentContainer h) {
		// if (title != null) sel = new Select(title);
		// else sel = new Select();
		this.h = h;
		rebuildCombo();

		if (title != null) {
			Label lbl = new Label(title);
			lbl.setHeight(sel.getHeight() + "px");
			h.addComponent(lbl);
		}
		h.addComponent(sel);

	}

	public void addItem(String string) {
		sel.addItem(string);
		sel.setValue(string);
	}

	public void setEnabled(boolean b) {
		sel.setEnabled(b);

	}

	public void selectMask(BitMask showmask) {
		sel.removeListener(listener);
		sel.select(showmask);
		sel.addListener(listener);

	}
}
