/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.bfmask;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.iontorrent.expmodel.CompositeExperiment;
import com.iontorrent.expmodel.DatBlock;
import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.guiutils.heatmap.GradientPanel;
import com.iontorrent.rawdataaccess.wells.BfMaskFlag;
import com.iontorrent.vaadin.TSVaadin;
import com.iontorrent.vaadin.utils.CoordSelect;
import com.iontorrent.vaadin.utils.GradientLegend;
import com.iontorrent.vaadin.utils.WindowOpener;
import com.iontorrent.vaadin.utils.ZoomControl;
import com.iontorrent.wellmodel.WellCoordinate;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.graphics.canvas.Canvas;
import com.vaadin.graphics.canvas.shape.Cross;
import com.vaadin.graphics.canvas.shape.Point;
import com.vaadin.graphics.canvas.shape.Polygon;
import com.vaadin.graphics.canvas.shape.Text;
import com.vaadin.graphics.canvas.shape.UIElement;
import com.vaadin.terminal.StreamResource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.Select;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * 
 * @author Chantal Roth chantal.roth@lifetech.com
 */
public class BfMaskWindowCanvas extends WindowOpener implements
		Button.ClickListener, Property.ValueChangeListener {

	private TSVaadin app;
	CoordSelect coordsel;
	ExperimentContext exp;
	ExperimentContext oldexp;
	int x;
	int y;
	Select sel;
	BfMaskFlag flag;
	Canvas canvas;
	StreamResource imageresource;
	int mousecount;
	ZoomControl zoom;
	int gradmin;
	int gradmax;
	int bucket;
	BfMaskImage bfmask;

	public BfMaskWindowCanvas(TSVaadin app, Window main, String description,
			int x, int y) {
		super("BF Heat Map", main, description, x, y, 800, 600);
		this.app = app;
		bucket = 8;
	}

	@Override
	public void openButtonClick(Button.ClickEvent event) {
		if (!super.checkExperimentOpened())
			return;
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
			if (exp.is318() || exp.getNrrows()>3000)
				bucket = 15;
			else if (exp.is316() || exp.getNrrows()>2000)
				bucket = 10;
			else if (exp.isThumbnails() || exp.getNrrows()<1000)
				bucket = 4;
		}
		oldexp = exp;
		WellCoordinate coord = exp.getWellContext().getCoordinate();
		if (coord == null) {
			p("Coord is null, creating a new coord");
			coord = new WellCoordinate(exp.getNrcols() / 2, exp.getNrrows() / 2);
			exp.getWellContext().setCoordinate(coord);
		} else
			p("already got coord: " + coord);
		p("Creating bfmask image for coord " + coord);
		x = coord.getX();
		y = coord.getY();

		HorizontalLayout h = new HorizontalLayout();
		mywindow.addComponent(h);

		if (bfmask == null)
			bfmask = new BfMaskImage(exp, flag, bucket);

		imageresource = new StreamResource(
				(StreamResource.StreamSource) bfmask, exp.getFileKey()
						+ flag.getName() + (Math.random() * 100) + "_bf.png",
				app);
		imageresource.setCacheTime(1000);
		int width = Math.max(bfmask.getImage().getHeight() + 100, 600);
		int height = Math.max(bfmask.getImage().getHeight() + 100, 200);
		mywindow.setHeight(height + "px");
		mywindow.setWidth(width + "px");
		// imageresource.

		// if (canvas == null) {
		canvas = new Canvas();
		canvas.setBackgroundColor("black");
		canvas.setHeight((bfmask.getImage().getHeight() + 200) + "px");
		java.awt.Point point = bfmask.getPointFromWell(coord);
//		Cross cross = new Cross((int) point.getX(), (int) point.getY(), 3, 5);
//		cross.setDescription("");
//		canvas.drawUIElement(cross);

		String bg = app.getBgUrl(imageresource.getApplication()
				.getRelativeLocation(imageresource));
		p("Using bg: " + bg);
		canvas.setBackgroundImage(bg);

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

		HorizontalLayout hcan = new HorizontalLayout();

		VerticalLayout vzoom = new VerticalLayout();

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

		hcan.addComponent(vzoom);
		final GradientPanel grad = bfmask.getGradient();
		grad.setInPercent(true);
		GradientLegend leg = new GradientLegend(this.bucket*bucket, grad,
				new GradientLegend.Recipient() {

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

		mywindow.addComponent(hcan);

		app.showTopMessage(this.getName(),
				"Drag the cursor and <b>double click</b> to select a different well/area");

		canvas.addListener(new Canvas.CanvasMouseUpListener() {

			@Override
			public void mouseUp(Point p, UIElement child) {
				mousecount++;
				if (mousecount > 3) {
					app.showMessage("Double click",
							"Double click on anywhere to select well");
					mousecount = 0;
				}
			}
		});

		canvas.addListener(new Canvas.CanvasMouseDownListener() {

			@Override
			public void mouseDown(Point p, int count, UIElement child) {
				int newx = (int) p.getX();
				int newy = (int) p.getY();
				if (count > 1) {
					p("Got double CLICK on canvas: " + p + ", child=" + child);

					x = newx;
					y = newy;
					Polygon po = (Polygon) child;
					// p("Location of child po: "+po.getCenter());

					WellCoordinate coord = bfmask.getWellCoordinate(x, y);
					// p("Got coord: " + coord + ", setting description");
					coordsel.setX(coord.getX() + exp.getColOffset());
					coordsel.setY(coord.getY() + exp.getRowOffset());
					// po.setDescription("changed description :"+coord);
					buttonClick(null);

				} else if (count == 1) {
					x = newx;
					y = newy;
					WellCoordinate coord = bfmask.getWellCoordinate(x, y);
					// p("Got coord: " + coord + ", setting description");
					coordsel.setX(coord.getX() + exp.getColOffset());
					coordsel.setY(coord.getY() + exp.getRowOffset());
					// po.setDescription("changed description :"+coord);
					buttonClick(null);

				}

			}
		});
	}

	public String getHelpMessage() {
		String msg = "<ul>";
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

	private static void err(String msg, Exception ex) {
		Logger.getLogger(BfMaskWindowCanvas.class.getName()).log(Level.SEVERE,
				msg, ex);
	}

	private static void err(String msg) {
		Logger.getLogger(BfMaskWindowCanvas.class.getName()).log(Level.SEVERE,
				msg);
	}

	private static void warn(String msg) {
		Logger.getLogger(BfMaskWindowCanvas.class.getName()).log(Level.WARNING,
				msg);
	}

	private static void p(String msg) {
		// system.out.println("BfMaskWindowCanvas: " + msg);
		Logger.getLogger(BfMaskWindowCanvas.class.getName()).log(Level.INFO,
				msg);
	}
}
