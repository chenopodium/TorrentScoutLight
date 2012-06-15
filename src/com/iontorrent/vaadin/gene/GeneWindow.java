/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.gene;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.iontorrent.seq.Read;
import org.iontorrent.seq.alignment.Alignment;
import org.iontorrent.seq.sam.SamUtils;

import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.sequenceloading.SequenceLoader;
import com.iontorrent.threads.Task;
import com.iontorrent.threads.TaskListener;
import com.iontorrent.utils.ErrorHandler;
import com.iontorrent.utils.ProgressListener;
import com.iontorrent.vaadin.TSVaadin;
import com.iontorrent.vaadin.utils.CoordSelect;
import com.iontorrent.vaadin.utils.DataUtils;
import com.iontorrent.vaadin.utils.WindowOpener;
import com.iontorrent.wellmodel.WellCoordinate;
import com.iontorrent.wellmodel.WellSelection;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.Select;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

/**
 * 
 * @author Chantal Roth chantal.roth@lifetech.com
 */
public class GeneWindow extends WindowOpener implements Button.ClickListener,
		TaskListener, ProgressListener {

	private TSVaadin app;
	Table table;
	HorizontalLayout hor;
	int curx;
	int cury;
	CoordSelect coordsel;
	ExperimentContext exp;
	ProgressIndicator indicator;
	TextField txt;
	long genomepos;
	DecimalFormat dec = new DecimalFormat("#.#");
	Select sel;
	ArrayList<Read> reads;
	String selectedref;
	WorkThread t;

	public GeneWindow(TSVaadin app, Window main, String description, int x,
			int y) {
		super("Find reads by genome position", main, description, x, y, 950,
				350);
		this.app = app;

		// super.openbutton.setEnabled(false);
	}

	@Override
	public void openButtonClick(Button.ClickEvent event) {
		this.exp = app.getExperimentContext();
		if (exp == null) {
			appwindow.showNotification("No Experiment Selected",
					"<br/>Please open an experiment first",
					Window.Notification.TYPE_WARNING_MESSAGE);
			return;
		}
		SequenceLoader loader = SequenceLoader.getSequenceLoader(this.exp);
		if (!loader.getBamFile().exists()) {
			app.showMessage(
					"No BAM",
					"Can't find a BAM file:"
							+ loader.getBamFile()
							+ "<br>Please check your experiment files and folders.<br>");
			return;
		}
		super.openButtonClick(event);
	}

	@Override
	public void windowOpened(Window mywindow) {
		p("Open gene window");

		hor = new HorizontalLayout();
		mywindow.addComponent(hor);

		sel = new Select();
		sel.setDescription("Chromosome/Reference");
		sel.setNullSelectionAllowed(false);
		SequenceLoader loader = SequenceLoader.getSequenceLoader(this.exp);

		SamUtils sam = loader.getSamUtils();
		boolean has = false;
		if (sam != null) {

			ArrayList<String> refs = sam.getReferenceNames();

			for (String ref : refs) {
				sel.addItem(ref);
				if (!has) {
					sel.select(ref);
					sel.setValue(ref);
				}
			}
		}

		sel.setImmediate(true);

		txt = new TextField();
		txt.setValue("1234");
		txt.setWidth("70px");
		txt.setDescription("Enter a genome location (a number)");
		txt.setImmediate(true);
		hor.addComponent(sel);
		hor.addComponent(txt);

		Button find = new Button("Find Reads");
		find.setDescription("Find reads that map to the specified genome location");
		hor.addComponent(find);
		find.setImmediate(true);
		find.addListener(new ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				// TODO Auto-generated method stub
				startToFindGenes();
			}
		});

		Button help = new Button();
		help.setDescription("Click me to get information on this window");
		help.setIcon(new ThemeResource("img/help-hint.png"));
		hor.addComponent(help);
		help.addListener(new Button.ClickListener() {
			public void buttonClick(Button.ClickEvent event) {
				app.showHelpMessage("Help", getHelpMessage());
			}
		});

		final Button export = new Button();
		export.setIcon(new ThemeResource("img/export.png"));
		export.setDescription("Store table in .csv file");
		export.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				doExportAction();
			}

		});
		hor.addComponent(export);


	}

	private void addTable() {
		String w = "900px";
		String h = "300px";
		boolean sortasc = false;
		Object sortids = null;
		if (table != null) {
			mywindow.removeComponent(table);
			w = table.getWidth() + "px";
			h = table.getHeight() + "px";
			sortasc = table.isSortAscending();
			sortids = table.getSortContainerPropertyId();
		}

		table = new Table();

		table.setSortAscending(sortasc);
		exp = app.getExperimentContext();
		table.setWidth(w);
		table.setHeight(h);

		table.addContainerProperty("x", Integer.class, null);
		table.addContainerProperty("y", Integer.class, null);
		table.addContainerProperty("reverse", Boolean.class, null);
		table.addContainerProperty("read len", Integer.class, null);
		table.addContainerProperty("flow", Integer.class, null);
		table.addContainerProperty("pos in read", Integer.class, null);
		table.addContainerProperty("pos in al", Integer.class, null);
		table.addContainerProperty("base", String.class, null);
		table.addContainerProperty("alignment info", String.class, null);
		table.addContainerProperty("reference", String.class, null);

		table.addStyleName("welltable");

		if (reads != null && reads.size() > 0) {
			p("adding " + reads.size() + " rows to table");
			for (int row = 0; row < reads.size(); row++) {
				addRow(row, reads.get(row));
			}

		} else {
			app.showMessage("No reads", "I found no reads at " + selectedref
					+ "/" + genomepos);
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
				int x = ((Integer) obj.getItemProperty("x").getValue())
						.intValue();
				int y = ((Integer) obj.getItemProperty("y").getValue())
						.intValue();
				int flow = ((Integer) obj.getItemProperty("flow").getValue())
						.intValue();
				WellCoordinate coord = new WellCoordinate(x, y);
				exp.makeRelative(coord);
				exp.setFlow(flow);
				app.setWellCoordinate(coord, false);
				// app.reopenAlign();
			}
		});
		mywindow.addComponent(table);
	}

	private void doExportAction() {
		// copy data to excel
		if (table == null || table.getItemIds() == null) {
			app.showMessage("No data", "Found no data in table to export");
			return;
		}
		DataUtils.export(table, mywindow);

	}

	public String getHelpMessage() {
		String msg = "<ul>";
		msg += "<li>Pick a reference and enter a genome position</li>";
		msg += "<li>The find button locates reads at that position</li>";
		msg += "<li>Sort the data by clicking on the headers</li>";
		msg += "<li>Click on a row to view data <br>(open a viewer component such as the ionogram or alignment view)</li>";
		msg += "<li>Export the data to excel in the data tab</li>";

		msg += "</ul>";
		return msg;
	}

	private void startToFindGenes() {
		// indicator.setCaption("Creating whole Proton image");
		String v = "" + txt.getValue();
		genomepos = -1;
		try {
			genomepos = Long.parseLong(v);
		} catch (Exception e) {
		}
		if (genomepos < 0) {
			app.showError(this, "Could not determing position based on " + v);
			return;
		}
		selectedref = (String) sel.getValue();

		if (indicator != null) {
			hor.removeComponent(indicator);
		}
		indicator = new ProgressIndicator(new Float(0.0));
		indicator.setHeight("40px");

		indicator.setDescription("Finding reads at genome position "
				+ genomepos + "...");
		indicator.setPollingInterval(5000);
		hor.addComponent(indicator);
		app.showMessage("Finding reads", "Finding reads at position "
				+ genomepos);
		app.logModule(getName(), "find " + genomepos);
		t = new WorkThread(this);
		t.execute();
	}

	@Override
	public void taskDone(Task task) {
		if (indicator != null) {
			hor.removeComponent(indicator);
		}
		addTable();

	}

	public void close() {
		super.close();
		if (t != null && !t.isCancelled()) {
			t.cancel(true);
			t = null;
		}
	}

	// Another thread to do some work
	class WorkThread extends Task {
		boolean has;

		public WorkThread(TaskListener list) {
			super(list);
		}

		@Override
		public boolean isSuccess() {
			// TODO Auto-generated method stub
			return has;
		}

		@Override
		protected Void doInBackground() {
			try {

				app.showMessage("Searching...", "Finding reads at position "
						+ genomepos);
				// result
				SequenceLoader loader = SequenceLoader.getSequenceLoader(exp);
				indicator.setValue(new Float(0.05));
				if (!loader.hasGenomeToReadIndex()) {
					app.showLongMessage("Searching...",
							"Creating genome to read index");
					loader.createGenomeToReadIndex();
				}
				indicator.setValue(new Float(0.5));
				ArrayList<WellCoordinate> coords = loader
						.findWellCoords(genomepos);
				p("FindCoords at " + genomepos + ":" + coords);
				String error = loader.getMsg();
				if (error != null) {
					app.showMessage(GeneWindow.this, error);
				}
				indicator.setValue(new Float(0.7));

				WellSelection sel = new WellSelection(coords);
				sel.setTitle("Reads at position " + genomepos);
				app.showMessage("Got result",
						"Loading info on " + coords.size() + " found wells...");
				sel.loadDataForWells(exp.getWellContext().getMask());
				app.setWellSelection(sel);
				indicator.setValue(new Float(0.8));
				p("Created well selection " + sel + " with " + coords.size()
						+ "  coords");
				p("Loading reads for coords");
				reads = loader.getReadForCoords(coords);
				indicator.setValue(new Float(0.9));
				String ref = selectedref;
				if (ref != null && ref.length() > 0) {
					p("Filtering for ref name " + ref);
					ArrayList<Read> filtered = new ArrayList<Read>();
					for (Read r : reads) {
						String name = r.getReferenceName();
						if (name == null || name.equalsIgnoreCase(ref)) {
							filtered.add(r);
						}
					}
					reads = filtered;
				} else
					p("Not filtering for ref name " + ref);

				error = loader.getMsg();
				if (error != null) {
					app.showMessage(GeneWindow.this, error);
				}

				indicator.setValue(new Float(1.0));

			} catch (Exception e) {
				err("Got an error when computing the heat map: "
						+ ErrorHandler.getString(e));
			}
			return null;

		}

	}

	public void setProgressValue(int p) {
		if (indicator != null)
			indicator.setValue(((double) p / 100.0d));
		// progress.setValue("Creating composite image: " + p + "%");
	}

	private int addRow(int row, Read r) {
		int x = r.getCol();
		int y = r.getRow();
		int basepos = r.getPosInRead(genomepos);
		int alpos = r.getAlign().getPosInAl(basepos);
		int flow = r.findFlow(basepos);
		String base = "" + r.getBaseChar(basepos);
		Alignment al = r.getAlign();
		String info = "ident=" + dec.format(al.getIdentityPerc())
				+ "%, indels=" + al.getGaps() + ", mismatches="
				+ al.getMismatches();

		Object[] rowdata = new Object[8 + 2];
		rowdata[0] = new Integer(x + exp.getColOffset());
		rowdata[1] = new Integer(y + exp.getRowOffset());
		rowdata[2] = new Boolean(r.isReverse());
		rowdata[3] = new Integer(r.getLength());

		rowdata[4] = new Integer(flow);
		rowdata[5] = new Integer(basepos);
		rowdata[6] = new Integer(alpos);
		rowdata[7] = base;
		rowdata[8] = info;
		rowdata[9] = r.getReferenceName();

		table.addItem(rowdata, new Integer(row));

		row++;
		return row;
	}

	public void buttonClick(Button.ClickEvent event) {
		int x = coordsel.getX();
		int y = coordsel.getY();
		WellCoordinate coord = new WellCoordinate(x, y);
		exp.makeRelative(coord);
		app.setWellCoordinate(coord);
	}

	private static void err(String msg, Exception ex) {
		Logger.getLogger(GeneWindow.class.getName()).log(Level.SEVERE,
				msg + ErrorHandler.getString(ex));
	}

	private static void err(String msg) {
		Logger.getLogger(GeneWindow.class.getName()).log(Level.SEVERE, msg);
	}

	private static void warn(String msg) {
		Logger.getLogger(GeneWindow.class.getName()).log(Level.WARNING, msg);
	}

	private static void p(String msg) {
		// system.out.println("GeneWindow: " + msg);
		Logger.getLogger(GeneWindow.class.getName()).log(Level.INFO, msg);
	}

	@Override
	public void setMessage(String msg) {
		indicator.setDescription(msg);
	}

	@Override
	public void stop() {

	}
}
