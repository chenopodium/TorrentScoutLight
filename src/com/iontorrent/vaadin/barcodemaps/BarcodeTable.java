/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.barcodemaps;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.iontorrent.expmodel.DatasetsManager;
import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.expmodel.ReadGroup;
import com.iontorrent.guiutils.heatmap.GradientPanel;
import com.iontorrent.heatmaps.ScoreMaskGenerator;
import com.iontorrent.rawdataaccess.wells.BfMask;
import com.iontorrent.rawdataaccess.wells.BitMask;
import com.iontorrent.rawdataaccess.wells.ScoreMaskFlag;
import com.iontorrent.results.scores.ScoreMask;
import com.iontorrent.sequenceloading.SequenceLoader;
import com.iontorrent.stats.EnrichmentStats;
import com.iontorrent.stats.LoadingDensityStats;
import com.iontorrent.stats.PolyStats;
import com.iontorrent.stats.SimpleStats;
import com.iontorrent.stats.StatsComputer;
import com.iontorrent.torrentscout.explorer.ExplorerContext;
import com.iontorrent.vaadin.TSVaadin;
import com.iontorrent.vaadin.utils.CoordSelect;
import com.iontorrent.vaadin.utils.DataUtils;
import com.iontorrent.vaadin.utils.FileBrowserWindow;
import com.iontorrent.vaadin.utils.GradientLegend;
import com.iontorrent.vaadin.utils.InputDialog;
import com.iontorrent.vaadin.utils.OptionsDialog;
import com.iontorrent.vaadin.utils.WindowOpener;
import com.iontorrent.vaadin.utils.ZoomControl;
import com.iontorrent.wellmodel.WellCoordinate;
import com.iontorrent.wellmodel.WellSelection;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.graphics.canvas.Canvas;
import com.vaadin.graphics.canvas.shape.Point;
import com.vaadin.graphics.canvas.shape.UIElement;
import com.vaadin.terminal.StreamResource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.Select;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * 
 * @author Chantal Roth chantal.roth@lifetech.com
 */
public class BarcodeTable extends WindowOpener {

	private TSVaadin app;
	ExperimentContext exp;
	ExperimentContext oldexp;
	HorizontalLayout h;
	Table table;
	HorizontalLayout hcan;
	ReadGroup curgroup;
	DatasetsManager manager;

	public BarcodeTable(TSVaadin app, Window main, String description, int x, int y) {
		super("Barcode table", main, description, x, y, 980, 400);
		this.app = app;

	}

	@Override
	public void openButtonClick(Button.ClickEvent event) {
		exp = app.getExperimentContext();
		if (exp == null) {
			appwindow.showNotification("No Experiment Selected", "<br/>Please open an experiment first", Window.Notification.TYPE_WARNING_MESSAGE);
			return;
		}

		manager = exp.getDatasets();
		if (manager == null || manager.getReadGroups().size() < 2) {
			appwindow.showNotification("No Barcodes", "<br/>This experiment seems to have no barcodes " + exp.getBamFilePath(), Window.Notification.TYPE_WARNING_MESSAGE);
			return;
		}

		super.openButtonClick(event);
	}

	@Override
	public void windowOpened(final Window mywindow) {
		p("==== OPEN BARCODE WINDOWS ==== ");

		exp = app.getExperimentContext();
		StatsComputer comp = new StatsComputer(exp);

		comp.computeKeypassAndEnrichmehtForReadGroups();
		p("Computed keypass etc for read groups");
		manager = exp.getDatasets();

		if (curgroup == null) {
			curgroup = manager.getReadGroup(0);
		}

		h = new HorizontalLayout();

		final NativeButton export = new NativeButton();
		export.setStyleName("nopadding");
		export.setIcon(new ThemeResource("img/export.png"));
		export.setDescription("Save image");
		export.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(com.vaadin.ui.Button.ClickEvent event) {
				// copy data to excel
				if (table == null || table.getItemIds() == null) {
					app.showMessage("No data", "Found no data in table to export");
					return;
				}
				app.logModule(getName(), "export table");
				DataUtils.export(table, mywindow);

			}
		});

		NativeButton help = new NativeButton();
		help.setStyleName("nopadding");
		help.setDescription("Click me to get information on this window");
		help.setIcon(new ThemeResource("img/help-hint.png"));

		help.addListener(new Button.ClickListener() {
			public void buttonClick(Button.ClickEvent event) {
				app.showHelpMessage("Help", getHelpMessage());
			}
		});

		mywindow.addComponent(h);

		VerticalLayout vzoom = new VerticalLayout();

		vzoom.addComponent(export);
		vzoom.addComponent(help);
		hcan = new HorizontalLayout();

		hcan.addComponent(vzoom);

		mywindow.addComponent(hcan);

		addTable();

	}

	private void addTable() {
		String w = "900px";
		String h = "300px";
		boolean sortasc = false;
		Object sortids = null;
		if (table != null) {
			hcan.removeComponent(table);
			w = table.getWidth() + "px";
			h = table.getHeight() + "px";
			sortasc = table.isSortAscending();
			sortids = table.getSortContainerPropertyId();
		}

		table = new Table() {
			@Override
			protected String formatPropertyValue(Object rowId, Object colId, Property property) {
				Object v = property.getValue();
				if (v instanceof Double) {
					DecimalFormat df = new DecimalFormat("0.00");
					return df.format(v);
				} else if (v instanceof Integer) {
					DecimalFormat df = new DecimalFormat("0");
					return df.format(v);
				}
				return super.formatPropertyValue(rowId, colId, property);
			}
		};

		table.setSortAscending(sortasc);
		exp = app.getExperimentContext();
		table.setWidth(w);
		table.setHeight(h);

		table.addContainerProperty("index", Integer.class, null);
		table.addContainerProperty("Barcode", String.class, null);
		table.addContainerProperty("Sequence", String.class, null);
		table.addContainerProperty("Adapter", String.class, null);
		table.addContainerProperty("Read count", Integer.class, null);
		table.addContainerProperty("Rel.Loading %", Double.class, null);
		table.addContainerProperty("Nr Keypass", Integer.class, null);
		table.addContainerProperty("Enrichment %", Double.class, null);
		table.addContainerProperty("Polyclonal %", Double.class, null);
		table.addContainerProperty("Loading %", Double.class, null);

		table.addStyleName("welltable");

		ArrayList<ReadGroup> reads = manager.getReadGroups();
		if (reads != null && reads.size() > 0) {
			p("adding " + reads.size() + " rows to table");
			for (int row = 0; row < reads.size(); row++) {
				addRow(row, reads.get(row));
			}

		} else {
			app.showMessage("No reads", "I found no read groups");
		}
		// Allow selecting items from the table.
		table.setSelectable(true);

		// Send changes in selection immediately to server.
		table.setImmediate(true);

		if (sortids != null && reads != null && reads.size() > 0) {
			table.setSortContainerPropertyId(sortids);
			table.sort();
		}
		// Handle selection change.
		table.addListener(new Property.ValueChangeListener() {

			public void valueChange(ValueChangeEvent event) {
				Object id = table.getValue();
				if (id == null) {
					return;
				}
				Item obj = table.getItem(id);
				int index = ((Integer) obj.getItemProperty("index").getValue()).intValue();
				curgroup = manager.getReadGroup(index);
				app.getBarcodeCanvas().setCurGroup(curgroup);

			}
		});
		hcan.addComponent(table);
	}

	private int addRow(int row, ReadGroup r) {
		Object[] rowdata = new Object[10];
		rowdata[0] = new Integer(r.getIndex());
		rowdata[1] = r.getBarcodeName();
		rowdata[2] = r.getBarcodeSequence();
		rowdata[3] = r.getBarcodeAdapter();

		rowdata[4] = new Integer(r.getReadCount());
		rowdata[5] = new Double(r.getRelLoading());
		rowdata[6] = new Integer(r.getNrkeypass());
		rowdata[7] = new Double(r.getEnrichment());
		rowdata[8] = new Double(r.getPoly());
		rowdata[9] = new Double(r.getLoading());

		table.addItem(rowdata, new Integer(row));

		row++;
		return row;
	}

	public String getHelpMessage() {
		String msg = "<ul>";
		msg += "<li>Click on a row to pick a read group</li>";

		msg += "</ul>";
		return msg;
	}

	private static void err(String msg, Exception ex) {
		Logger.getLogger(BarcodeTable.class.getName()).log(Level.SEVERE, msg, ex);
	}

	private static void err(String msg) {
		Logger.getLogger(BarcodeTable.class.getName()).log(Level.SEVERE, msg);
	}

	private static void warn(String msg) {
		Logger.getLogger(BarcodeTable.class.getName()).log(Level.WARNING, msg);
	}

	private static void p(String msg) {
		System.out.println("BarcodeTable: " + msg);
		Logger.getLogger(BarcodeTable.class.getName()).log(Level.INFO, msg);
	}

}
