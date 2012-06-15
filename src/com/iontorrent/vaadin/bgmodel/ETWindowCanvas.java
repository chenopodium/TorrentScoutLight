/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.bgmodel;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.iontorrent.seq.Read;

import com.iontorrent.background.EmptyTrace;
import com.iontorrent.expmodel.CompositeExperiment;
import com.iontorrent.expmodel.DatBlock;
import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.guiutils.heatmap.GradientPanel;
import com.iontorrent.rawdataaccess.wells.BfMaskFlag;
import com.iontorrent.sequenceloading.SequenceLoader;
import com.iontorrent.threads.Task;
import com.iontorrent.threads.TaskListener;
import com.iontorrent.utils.ErrorHandler;
import com.iontorrent.utils.ProgressListener;
import com.iontorrent.utils.StringTools;
import com.iontorrent.vaadin.TSVaadin;
import com.iontorrent.vaadin.utils.CoordSelect;
import com.iontorrent.vaadin.utils.GradientLegend;
import com.iontorrent.vaadin.utils.WindowOpener;
import com.iontorrent.vaadin.utils.ZoomControl;
import com.iontorrent.wellmodel.WellCoordinate;
import com.iontorrent.wellmodel.WellSelection;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.event.MouseEvents.ClickEvent;
import com.vaadin.event.MouseEvents.ClickListener;
import com.vaadin.graphics.canvas.Canvas;
import com.vaadin.graphics.canvas.shape.Cross;
import com.vaadin.graphics.canvas.shape.Point;
import com.vaadin.graphics.canvas.shape.Polygon;
import com.vaadin.graphics.canvas.shape.Text;
import com.vaadin.graphics.canvas.shape.UIElement;
import com.vaadin.terminal.StreamResource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.Select;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * 
 * @author Chantal Roth chantal.roth@lifetech.com
 */
public class ETWindowCanvas extends WindowOpener implements
		Button.ClickListener, Property.ValueChangeListener, TaskListener,
		ProgressListener {

	private TSVaadin app;
	CoordSelect coordsel;
	ExperimentContext exp;
	ExperimentContext oldexp;
	int x;
	int y;
	Select sel;
	BfMaskFlag flag;
	// Canvas canvas;
	Embedded canvas;
	StreamResource imageresource;
	int mousecount;
	ZoomControl zoom;
	int gradmin;
	int gradmax;
	int bucket;
	ETMaskImage bfmask;
	EmptyTrace emptyTrace;
	ProgressIndicator indicator;
	WorkThread t;
	HorizontalLayout h;
	ArrayList<Integer> flows;
	TextField tflow;
	HorizontalLayout hcan;
	WellCoordinate coord;
	GradientLegend leg;
	VerticalLayout vzoom;

	public ETWindowCanvas(TSVaadin app, Window main, String description, int x,
			int y) {
		super("Bf Heat Map", main, description, x, y, 800, 600);
		this.app = app;
		bucket = 5;
	}

	@Override
	public void openButtonClick(Button.ClickEvent event) {
		if (app.getExperimentContext() == null) {
			appwindow.showNotification("No Experiment Selected",
					"<br/>Please open an experiment first",
					Window.Notification.TYPE_WARNING_MESSAGE);
			return;
		}
		if (!app.getExperimentContext().hasBfMask()) {
			appwindow.showNotification("Found no bfmask.bin",
					"<br/>Could not find "
							+ app.getExperimentContext().getBfMaskFile(),
					Window.Notification.TYPE_WARNING_MESSAGE);
			return;
		}
		exp = app.getExperimentContext();
		if (exp.doesExplogHaveBlocks()) {
			appwindow.showNotification("Proton",
					"<br/>Pick a block first in this Proton experiment",
					Window.Notification.TYPE_HUMANIZED_MESSAGE);
			return;
		}

		super.openButtonClick(event);
	}

	@Override
	public void windowOpened(final Window mywindow) {

		if (flag == null)
			flag = BfMaskFlag.LIVE;
		exp = app.getExperimentContext();

		if (oldexp == null || exp != oldexp) {
			if (exp.is318())
				bucket = 15;
			else if (exp.is316())
				bucket = 10;
		}
		oldexp = exp;
		coord = exp.getWellContext().getCoordinate();
		if (coord == null) {
			p("Coord is null, creating a new coord");
			coord = new WellCoordinate(exp.getNrcols() / 2, exp.getNrrows() / 2);
			exp.getWellContext().setCoordinate(coord);
		} else
			p("already got coord: " + coord);
		p("Creating bfmask image for coord " + coord);
		x = coord.getX();
		y = coord.getY();

		h = new HorizontalLayout();
		mywindow.addComponent(h);

		sel = new Select();
		for (BfMaskFlag f : BfMaskFlag.values()) {
			sel.addItem(f);
			sel.setItemCaption(f, f.getName());
		}
		sel.select(flag);
		sel.addListener(new Select.ValueChangeListener() {

			@Override
			public void valueChange(ValueChangeEvent event) {
				flag = (BfMaskFlag) sel.getValue();
				bfmask = null;
				reopen();
			}

		});

		h.addComponent(new Label(" Flag: "));
		h.addComponent(sel);
		addFlowSelection(h);

		sel.setImmediate(true);
		sel.addListener(this);

		coordsel = new CoordSelect(x + exp.getColOffset(), y
				+ exp.getRowOffset(), this);
		coordsel.addGuiElements(h);

		NativeButton help = new NativeButton();
		help.setStyleName("nopadding");
		help.setDescription("Click me to get information on this window");
		help.setIcon(new ThemeResource("img/help-hint.png"));

		help.addListener(new Button.ClickListener() {
			public void buttonClick(Button.ClickEvent event) {
				app.showHelpMessage("Help", getHelpMessage());
			}
		});

		final NativeButton export = new NativeButton();
		export.setStyleName("nopadding");
		export.setIcon(new ThemeResource("img/export.png"));
		export.setDescription("Open image in another browser window so that you can save it to file");
		export.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(com.vaadin.ui.Button.ClickEvent event) {
				app.getMainWindow().open(imageresource, "_blank");
			}
		});

		vzoom = new VerticalLayout();

		zoom = new ZoomControl(bucket, new Button.ClickListener() {
			@Override
			public void buttonClick(com.vaadin.ui.Button.ClickEvent event) {
				bucket = zoom.getBucket();
				bfmask = null;
				reopen();
			}
		});
		vzoom.addComponent(export);
		zoom.addGuiElements(vzoom);
		vzoom.addComponent(help);

		hcan = new HorizontalLayout();
		hcan.addComponent(vzoom);

		mywindow.addComponent(hcan);

		checkForEmptyTrace();

		addBfCanvas();
		app.showTopMessage(this.getName(),
				"Drag the cursor and <b>double click</b> to select a different well/area");

	}

	private void addBfCanvas() {
		if (bfmask == null)
			bfmask = new ETMaskImage(exp, flag, bucket, emptyTrace, flows);

		String key = "";
		for (int flow : flows) {
			if (this.emptyTrace.hasLoaded(flow))
				key += "_" + flow;
		}
		String resname = exp.getFileKey() + flag.getName()
				+ (Math.random() * 100) + "_et" + key + ".png";
		p("Loading image resource: " + resname);
		imageresource = new StreamResource(
				(StreamResource.StreamSource) bfmask, resname, app);

		imageresource.setCacheTime(1000);
		int width = Math.max(bfmask.getImage().getHeight() + 100, 600);
		int height = Math.max(bfmask.getImage().getHeight() + 100, 200);
		mywindow.setHeight(height + "px");
		mywindow.setWidth(width + "px");
		// imageresource.

		if (canvas != null) {
			hcan.removeAllComponents();
			hcan.addComponent(vzoom);
		}

		canvas = new Embedded(null, imageresource);
		canvas.addListener(new ClickListener() {

			@Override
			public void click(ClickEvent event) {

				int newx = (int) event.getRelativeX();
				int newy = (int) event.getRelativeY();
				p("Got mouse CLICK on embedded: ");
				x = newx;
				y = newy;

				WellCoordinate coord = bfmask.getWellCoordinate(x, y);
				coordsel.setX(coord.getX() + exp.getColOffset());
				coordsel.setY(coord.getY() + exp.getRowOffset());
				buttonClick(null);

			}

		});
		p("Got embedded size: " + canvas.getWidth() + "/" + canvas.getHeight());
		p("Got image size: " + width + "/" + height);

		// canvas = new Canvas();
		// canvas.setBackgroundColor("black");
		// canvas.setHeight((bfmask.getImage().getHeight() + 200) + "px");
		// //java.awt.Point point = bfmask.getPointFromWell(coord);
		// String bg = app.getBgUrl(imageresource.getApplication()
		// .getRelativeLocation(imageresource));
		// p("Using bg: " + bg);
		// canvas.setBackgroundImage(bg);
		//
		// canvas.addListener(new Canvas.CanvasMouseUpListener() {
		//
		// @Override
		// public void mouseUp(Point p, UIElement child) {
		// mousecount++;
		// if (mousecount > 3) {
		// app.showMessage("Double click",
		// "Double click anywhere in chart to select well");
		// mousecount = 0;
		// }
		// }
		// });
		//
		// canvas.addListener(new Canvas.CanvasMouseDownListener() {
		//
		// @Override
		// public void mouseDown(Point p, int count, UIElement child) {
		// if (count > 0) {
		// int newx = (int) p.getX();
		// int newy = (int) p.getY();
		// p("Got mouse CLICK on canvas: " + p + ", child="
		// + child);
		// // if (child != null && child instanceof Polygon) {
		// x = newx;
		// y = newy;
		// // Polygon po = (Polygon) child;
		// // p("Location of child po: "+po.getCenter());
		//
		// WellCoordinate coord = bfmask.getWellCoordinate(x, y);
		// // p("Got coord: " + coord + ", setting description");
		// coordsel.setX(coord.getX() + exp.getColOffset());
		// coordsel.setY(coord.getY() + exp.getRowOffset());
		// // po.setDescription("changed description :"+coord);
		// buttonClick(null);
		// // }
		// }
		//
		// }
		// });

		final GradientPanel grad = bfmask.getGradient();
		leg = new GradientLegend(grad, new GradientLegend.Recipient() {

			@Override
			public void minOrMaxChanged() {
				p("Min or max changed");
				gradmin = (int) grad.getMin();
				gradmax = (int) grad.getMax();
				bfmask.setMin(gradmin);
				bfmask.setMax(gradmax);
				bfmask.repaint();
				// app.showMessage("Gradient",
				// "This is not fully working yet :-)");
				reopen();

			}
		}, app, bfmask.getImage().getHeight(), (int) canvas.getHeight());
		leg.addGuiElements(hcan);
		hcan.addComponent(canvas);
	}

	private void checkForEmptyTrace() {
		if (emptyTrace == null) {
			emptyTrace = app.getCubeLoader().getEmptyTrace();
		}
		this.parseFlow();
		if (flows == null) {
			flows = new ArrayList<Integer>();
			flows.add(0);

		}
		boolean gotall = true;
		boolean hasfiles = true;
		boolean hasflows = false;
		if (flows.size() > 0) {
			for (int flow : flows) {
				if (flow >= 0) {
					hasflows = true;
					if (emptyTrace.hasFile(flow)) {
						if (!emptyTrace.hasLoaded(flow)) {
							gotall = false;
							break;
						}
					} else
						hasfiles = false;
				}
			}
		}
		if (!hasfiles && hasflows) {
			// if (this.bucket < 4) {
			app.showLongMessage(
					"No Bkg Files",
					"I found no BkgModel files for this run and can't show the regional empty traces");
			// }
		}
		if (hasflows && !gotall) {

			app.showMessage("Loading..", "Loading empty traces file... ");
			t = new WorkThread(this, flows);
			indicator = new ProgressIndicator(new Float(0.0));
			indicator.setHeight("40px");

			indicator.setDescription("Loading empty traces");
			indicator.setPollingInterval(5000);
			h.addComponent(indicator);
			t.execute();

		} else
			afterGotEmptyTrace();
	}

	public void addFlowSelection(HorizontalLayout h) {
		tflow = new TextField();
		tflow.setWidth("60px");
		tflow.setDescription("Enter flows for which you would like to see the average empty trace per region. Note you have to zoom in all the way to see them!");
		// tflow.setHeight("25px");
		tflow.setImmediate(true);

		if (flows == null) {
			flows = new ArrayList<Integer>();
			flows.add(0);
		}
		String s = "" + flows;
		tflow.setValue(s.substring(1, s.length() - 1));
		h.addComponent(new Label("Flow(s):"));
		h.addComponent(tflow);

		tflow.addListener(new Property.ValueChangeListener() {

			public void valueChange(ValueChangeEvent event) {
				// addChart(chartTab);
				parseFlow();
				p("Got flows " + flows);
				reopen();
			}
		});

	}

	public void experimentChanged() {
		this.emptyTrace = null;
		this.bfmask = null;
	}

	public ArrayList<Integer> parseFlow() {
		if (tflow == null) {
			return flows;
		}
		String s = "" + tflow.getValue();
		if (s != null) {
			flows = StringTools.parseInts(s);
			p("parsed flows: " + flows);
		}
		return flows;
	}

	public String getHelpMessage() {
		String msg = "<ul>";
		msg += "<li>View the regional average empty traces</li>";
		msg += "<li>Pick the flow(s) for which you want to see the empty traces<br>"
				+ "(Ideally within the same 20 flows to avoid long loading times)</li>";
		msg += "<li>Pick a bf flag in the drop down box to view a different heat map</li>";
		msg += "<li>drag the cursor and then <b>double click</b> to pick a well or region</li>";
		msg += "<li>zoom in or out of the image </li>";
		msg += "<li>export the image (opens a new windows, then right click on the image and click save as) </li>";
		msg += "<li>enter a new x (column) or y (row) coordinat and hit refresh to also change the coordinate in other components </li>";
		msg += "</ul>";
		return msg;
	}

	public void valueChange(Property.ValueChangeEvent event) {
		// The event.getProperty() returns the Item ID (IID)
		// of the currently selected item in the component.
		Property id = event.getProperty();
		if (id.getValue() instanceof BfMaskFlag) {
			BfMaskFlag b = (BfMaskFlag) id.getValue();
			this.flag = b;
			bfmask = null;
			this.reopen();
		}
	}

	public void clear() {
		bfmask = null;
	}

	public void buttonClick(Button.ClickEvent event) {
		x = coordsel.getX();
		y = coordsel.getY();
		WellCoordinate coord = new WellCoordinate(x, y);
		exp.makeRelative(coord);
		app.setWellCoordinate(coord);

		// find only wells with that flag BfMask mask;

		if (exp.doesExplogHaveBlocks() && app.getCompositeExperiment() != null) {
			CompositeExperiment comp = app.getCompositeExperiment();
			DatBlock b = comp.findBlock(x, y);
			if (b != null) {
				app.showMessage("Block", "Loading the Proton block " + b);
				exp = comp.getContext(b, false);
				app.setExperimentContext(exp);
			} else {
				app.showMessage(
						"Block",
						"Could not find a block for "
								+ x
								+ "/"
								+ y
								+ "<br>You can also use the Proton View to pick a block");
			}
		}
		app.reopenRaw();
	}

	@Override
	public void taskDone(Task task) {
		if (indicator != null) {
			h.removeComponent(indicator);
		}
		afterGotEmptyTrace();

	}

	private void afterGotEmptyTrace() {
		this.bfmask = null;

		addBfCanvas();
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

		ArrayList<Integer> flows;

		public WorkThread(TaskListener list, ArrayList<Integer> flows) {
			super(list);
			this.flows = flows;
		}

		@Override
		public boolean isSuccess() {
			return true;
		}

		@Override
		protected Void doInBackground() {
			try {

				app.showMessage("Loading...",
						"Loading empty traces file for flows " + flows);
				// result

				for (int flow : flows) {
					emptyTrace.readFile(flow, this);
				}
				indicator.setValue(new Float(1.0));

			} catch (Exception e) {
				err("Got an error when loading an empty traces file: "
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

	private static void err(String msg, Exception ex) {
		Logger.getLogger(ETWindowCanvas.class.getName()).log(Level.SEVERE, msg,
				ex);
	}

	private static void err(String msg) {
		Logger.getLogger(ETWindowCanvas.class.getName()).log(Level.SEVERE, msg);
	}

	private static void warn(String msg) {
		Logger.getLogger(ETWindowCanvas.class.getName())
				.log(Level.WARNING, msg);
	}

	private static void p(String msg) {
		System.out.println("ETWindowCanvas: " + msg);
		Logger.getLogger(ETWindowCanvas.class.getName()).log(Level.INFO, msg);
	}

	@Override
	public void setMessage(String msg) {
		indicator.setDescription(msg);
	}

	@Override
	public void stop() {

	}
}
