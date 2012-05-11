/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.process;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.guiutils.widgets.CoordWidget;
import com.iontorrent.guiutils.widgets.Widget;
import com.iontorrent.rawdataaccess.pgmacquisition.DataAccessManager;
import com.iontorrent.rawdataaccess.pgmacquisition.RawType;
import com.iontorrent.rawdataaccess.wells.BitMask;
import com.iontorrent.scout.experimentviewer.exptree.ExpNamesNodeFilter;
import com.iontorrent.torrentscout.explorer.ExplorerContext;
import com.iontorrent.utils.ErrorHandler;
import com.iontorrent.utils.StringTools;
import com.iontorrent.utils.io.FileTools;
import com.iontorrent.vaadin.TSVaadin;
import com.iontorrent.vaadin.mask.MaskSelect;
import com.iontorrent.vaadin.utils.CoordSelect;
import com.iontorrent.vaadin.utils.ExportTool;
import com.iontorrent.vaadin.utils.InputDialog;
import com.iontorrent.vaadin.utils.OkDialog;
import com.iontorrent.vaadin.utils.OptionsDialog;
import com.iontorrent.vaadin.utils.WindowOpener;
import com.iontorrent.vaadin.utils.ZoomControl;
import com.iontorrent.wellalgorithms.NearestNeighbor;
import com.iontorrent.wellmodel.RasterData;
import com.iontorrent.wellmodel.WellCoordinate;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.graphics.canvas.Canvas;
import com.vaadin.graphics.canvas.shape.Cross;
import com.vaadin.graphics.canvas.shape.FrameWidget;
import com.vaadin.graphics.canvas.shape.Point;
import com.vaadin.graphics.canvas.shape.Polygon;
import com.vaadin.graphics.canvas.shape.UIElement;
import com.vaadin.terminal.DownloadStream;
import com.vaadin.terminal.FileResource;
import com.vaadin.terminal.StreamResource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Select;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

/**
 * 
 * @author Chantal Roth chantal.roth@lifetech.com
 */
public class ProcessWindowCanvas extends WindowOpener implements Property.ValueChangeListener {

	private TSVaadin app;
	CoordSelect coordsel;
	ExplorerContext maincont;
	ExperimentContext exp;
	int x;
	
	boolean issnap;
	int y;
	int flow;
	int frame;
	RawType type;
	TextField tflow;
	TextField tframe;
	Select typeselect;
	Canvas canvassub;
	Canvas canvascurve;
	WellCoordinate coord;
	SubregionImage subimage;
	CurveImage curveimage;
	BitMask showmask;
	HorizontalLayout hor;
	FrameWidget fwleft;
	FrameWidget fwright;
	FrameWidget fwstart;
	FrameWidget fwend;
	// MaskSelect bgmask;
	MaskSelect usemask;
	CoordWidget curwidget;
	StreamResource image;
	ZoomControl zoom;
	StreamResource exportimage;
	// here a bucket corresponds to the zoomlevel
	// 3 = 4 pix per well
	// 2= 8 pix per well
	// 1 = 16 pix per well
	// 4 = 2 pix per well

	int bucket = 3;
	int pixperwell = 4;
	boolean coordChanged;

	public ProcessWindowCanvas(TSVaadin app, Window main, String description, int x, int y) {
		super("Process (many wells)", main, description, x, y, 1350, 580);
		this.app = app;
		frame = 15;
		flow = 0;

	}

	public void setCoord(WellCoordinate coord) {
		this.coord = coord;
		canvassub = null;
	}

	
	@Override
	public void openButtonClick(Button.ClickEvent event) {
		if (app.getExperimentContext() == null) {
			mainwindow.showNotification("No Experiment Selected", "<br/>Please open an experiment first", Window.Notification.TYPE_WARNING_MESSAGE);
			return;
		}
		this.exp = app.getExperimentContext();
		if (app.getExperimentContext().getWellContext() == null) {
			mainwindow.showNotification("No Location Selected", "<br/>Please pick an area", Window.Notification.TYPE_WARNING_MESSAGE);
			return;
		}
		exp = app.getExperimentContext();
		maincont = app.getExplorerContext();

		if (coord != null && exp.getWellContext().getCoordinate() != null) {
			// p("reopen: Coord before:" + coord);
			// p("exp.getWellContext().getCoordinate():" +
			// exp.getWellContext().getCoordinate());
			if (!coord.equals(exp.getWellContext().getCoordinate())) {
				p("reopen: Coord different:" + coord + " vs " + exp.getWellContext().getCoordinate() + "- clearing data ");
				coordChanged = true;
				maincont.clearData();
				this.canvassub = null;
			}
		}
		coord = exp.getWellContext().getCoordinate();
		if (coord == null) {
			// p("Coord is null, creating a new coord");
			coord = new WellCoordinate(100, 100);
			app.setWellCoordinate(coord);
		}

		super.openButtonClick(event);
	}

	@SuppressWarnings("serial")
	@Override
	public void windowOpened(final Window mywindow) {

		maincont.setPreferrednrwidgets(5);

		if (coord == null) {
			// p("Coord is null, creating a new coord");
			coord = new WellCoordinate(100, 100);
		}
		maincont.setRelativeCenterAreaCoord(coord);
		if (!coord.equals(maincont.getAbsCenterAreaCoord())) {
			err("NOT SAME Center COORDS:" + coord + "/" + maincont.getAbsCenterAreaCoord());

		}
		p("====================== windowOpened " + coord + "/" + maincont.getRelativeCenterAreaCoord() + " coordChanged=" + coordChanged + " ===============================");
		String prevmaskname = null;
		if (showmask != null) prevmaskname = showmask.getName();
		if (coordChanged) {
			coordChanged = false;
			showmask = null;
		}

		getSelectedType();
		if (type == null) type = RawType.ACQ;

		maincont.setFrame(frame);
		maincont.setFiletype(type);
		loadData();

		if (prevmaskname != null) {
			if (maincont.getMasks() != null) {
				for (BitMask m : maincont.getMasks()) {
					if (m.getName().equalsIgnoreCase(prevmaskname)) {
						showmask = m;
						break;
					}
				}
			}
		}
		x = coord.getX();
		y = coord.getY();

		final HorizontalLayout h = new HorizontalLayout();
		mywindow.addComponent(h);

		// bgmask = new MaskSelect("Show bg mask", maincont, MaskSelect.BG,
		// null, maincont.getBgMask());
		// bgmask.addGuiElements(h);
		usemask = new MaskSelect("process_overlay", " ", "Pick a mask to overlay in the subregion image", maincont, MaskSelect.NONE, new Property.ValueChangeListener() {

			@Override
			public void valueChange(ValueChangeEvent event) {
				// redraw
				// hor.removeComponent(canvassub);
				// canvassub = null;
				// addCanvasSub(hor);
				// hor.addComponent(canvascurve);

				showmask = usemask.getSelection();
				canvassub = null;
				// p("Showmask is now: " + showmask);
				reopen();

			}
		}, this.showmask);

		final Button snap = new Button("Snap to mask");
		snap.setDescription("Snap the cursors to the selected mask");
		snap.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				canvassub = null;
				issnap = snap.booleanValue();
				snapWidgets();
				updateCurves();
				reopen();
			}

		});
		h.addComponent(snap);
		usemask.addGuiElements(h);

		this.addTypeSelection(h);
		addCoordAndFlowSelection(h);

		zoom = new ZoomControl(bucket, new Button.ClickListener() {
			@Override
			public void buttonClick(com.vaadin.ui.Button.ClickEvent event) {

				String question = "<html>This will reload the masks and data. <br>Do you still want to change the zoom level?";
				OkDialog okdialog = new OkDialog(mainwindow, "Reload masks and data", question, new OkDialog.Recipient() {
					@Override
					public void gotInput(String name) {
						if (!name.equalsIgnoreCase("OK")) return;
						bucket = zoom.getBucket();

						if (bucket == 2) pixperwell = 8;
						else if (bucket == 1) pixperwell = 16;
						else if (bucket == 4) pixperwell = 2;
						else
							pixperwell = 4;

						int raster_size = 400 / pixperwell;// default 100. At
															// least 25, at most
															// 200
						maincont.setRasterSize(raster_size);
						// need to reload data unfortunatley... but we could of
						// course compute it
						maincont.clearData();
						canvassub = null;
						p("Got pixperwell=" + pixperwell + ", rastersize=" + maincont.getRasterSize());
						reopen();
						app.reopenMaskedit(false);
						app.reopenFit();

					}
				});

			}
		});
		zoom.setMax(4);
		zoom.addGuiElements(h);

		Button help = new Button();
		help.setDescription("Click me to get information on this window");
		help.setIcon(new ThemeResource("img/help-hint.png"));
		h.addComponent(help);
		help.addListener(new Button.ClickListener() {
			public void buttonClick(Button.ClickEvent event) {
				 app.showHelpMessage("Help", getHelpMessage());
			}
		});

		final Button export = new Button();
		export.setIcon(new ThemeResource("img/export.png"));
		export.setDescription("Export data for multiple flows or this image");
		export.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				ExportTool export = new ExportTool(app, mainwindow, showmask, h);
				export.doExportAction();
			}

		});
		h.addComponent(export);
		//h.addComponent(new Label(app.getExperimentContext().getRawDir()));

		hor = new HorizontalLayout();
		mywindow.addComponent(hor);
		addCanvasSub(hor);

		addCanvasCurve(hor);

		// app.showMessage(this,
		// "Drag the cursors to select a different wells");

	}

	public void close() {
		super.close();
		
	}

	
	public ArrayList<Integer> parseFlows(String sflows) {
		ArrayList<Integer> flows = new ArrayList<Integer>();
		if (sflows == null) {
			return flows;
		}
		flows = StringTools.parseInts(sflows);
		p("parsed flows: " + flows);
		return flows;
	}

	public String getHelpMessage() {
		String msg = "<ul>";
		msg += "<li><b>Pick the masks for NN subtraction in the Mask Editor Component</b></li>";
		msg += "<li>Drag the green bars: <br>" + "This will set the range on the functsions (such as integral) in the Fit component to compute the histogram</li>";
		msg += "<li>Drag the red bars: <br>" + "This will set values for certain functions (such max-end height) in the Fit component to compute the histogram</li>";
		msg += "<li>drag the cursors around in the left view with the <b>left</b> mouse button </li>";

		msg += "<li>overlay masks by selecting them in the drop down box</li>";
		msg += "<li>zoom in or out of the image (<b>this will reload the data and the masks!</b>)</li>";
		msg += "<li>select a different file type or flow (<b>this will reload the data and the masks!</b>)</li>";
		msg += "<li>Snap the cursors to a selected mask</li>";
		msg += "<li>Enter new x/y coordinates for the <b>center</b> (<b>this will reload the data and the masks!</b>)</li>";
		msg += "</ul>";
		return msg;
	}

	public WellCoordinate getCoord() {
		return coord;
	}

	public void loadData() {
		exp = app.getExperimentContext();
		maincont = app.getExplorerContext();
		if (maincont == null) return;
		DataAccessManager manager = DataAccessManager.getManager(app.getExperimentContext().getWellContext());
		WellCoordinate relcenter = maincont.getRelativeCenterAreaCoord();

		// p("Reading data at RELATIVE coords "+rel);
		exp.makeRelative(relcenter);
		p("Reading data at RELATIVE coords " + relcenter);
		maincont.setRelativeCenterAreaCoord(relcenter);
		RasterData data = maincont.getData();
		if (data == null || !data.getRelMiddleCoord().equals(relcenter)) {
			p("need to load data at " + relcenter);
			app.showMessage(this, "(Re)Loading data for flow " + exp.getFlow() + ", " + maincont.getFiletype() + " at " + maincont.getAbsCenterAreaCoord());
			if (data == null) p("  ... because data is null");
			else
				p(" .... because old reldataareacord changed: maincont.getrelativecoord=" + relcenter + " vs data.getRelMiddleCoord" + data.getRelMiddleCoord());

			try {
				data = manager.getRasterDataForArea(maincont.getData(), maincont.getRasterSize(), relcenter, exp.getFlow(), maincont.getFiletype(), null, 0, -1);
				maincont.setData(data);
				// compute nn
				if (maincont.getMasks() == null || maincont.getMasks().size() < 1 || !maincont.getMasks().get(0).getRelCoord().equals(data.getRelStartcoord())) {
					maincont.createMasks();
				}
				data = computeNN();
				maincont.setData(data);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				p("Got error when reading data: " + ErrorHandler.getString(e));
			}
		} else {
			//p("Not loading data: " + data);
		}
		coord = exp.getWellContext().getCoordinate();
		manager.clear();
	}

	public RasterData computeNN() {
		p("computeNN. Maincont is: " + maincont);
		if (maincont.getData() == null) {
			app.showLongMessage("NN Calculation", "I see no data yet - did you already pick a region?<br>(Even if you see something somewhere, if you didn't actually select a region, it might just show some sample data)");
			return null;
		}
		RasterData nndata = null;
		try {

			int span = Math.max(8, maincont.getSpan());
			p("got span: " + span);
			NearestNeighbor nn = new NearestNeighbor(span, maincont.getMedianFunction());
			BitMask ignore = maincont.getIgnoreMask();
			BitMask take = maincont.getBgMask();

			if (take != null && take == ignore) {
				app.showLongMessage("NN Calculation", "You selected the same mask for ignore and bg :-).<br>You should select another mask for the bg (or you get a null result. I will just return the old data.");
				return maincont.getData();
			}

			// p("Masked neighbor subtraction: ignore mask " + ignore +
			// " and empty mask " + take);
			nndata = nn.computeBetter(maincont.getData(), ignore, take, null, span);

		} catch (Exception e) {
			app.showError(this, "Error with nn: " + ErrorHandler.getString(e));
			return null;
		}
		if (nndata == null) {
			app.showLongMessage("NN Calculation", "I was not able to do the masked neighbor subtraction - I got no error but also no result :-) ");
		}
		return nndata;
	}

	private void snapWidgets() {

		ArrayList<Widget> widgets = maincont.getWidgets();
		if (widgets == null) {
			p("snapWidgets: got no widgets to snap to!");
			return;
		}

		if (showmask == null) {
			p("Got no mask to snap to!");
			return;
		}
		app.showMessage("Snap", "Snapping widgets (except main widget) to mask " + showmask.getName());
		// p("Snapping widgets (except main widget) to mask " + showmask);
		for (Widget w : widgets) {
			if (!w.isMainWidget()) {
				maincont.snapWidgetToMask((CoordWidget) w, showmask);
			}
		}

	}

	private void addCanvasCurve(AbstractOrderedLayout v) {
		// p("Creating curve image. Got Widgets: " + maincont.getWidgets());

		curveimage = new CurveImage(maincont);

		curveimage.getImage();
		String key = exp.getFileKey() + "_c_" + flow + "_" + type + "_" + coord.getX() + "_" + coord.getY() + "_";
		// key must also contain widget coords - just use some simple hash
		int sum = 0;
		if (maincont.getWidgets() != null) {
			for (Widget w : maincont.getWidgets()) {
				sum += w.getX() + w.getY();
			}
		}
		key += sum + "_" + (int) (Math.random() * 1000);
		StreamResource imageresource = new StreamResource((StreamResource.StreamSource) curveimage, key + ".png", app);
		imageresource.setCacheTime(0);
		exportimage = imageresource;
		String relative = imageresource.getApplication().getRelativeLocation(imageresource);
		String appurl = imageresource.getApplication().getURL().toString();
		String url = relative;
		url = appurl + url.replace("app://", "");
		// p("URl from stream resource curve: " + url);

		canvascurve = new Canvas();
		canvascurve.setWidth("800px");
		canvascurve.setHeight("500px");
		canvascurve.setBackgroundColor("black");
		// canvascurve.setImmediate(true);

		int leftframe = maincont.getCropleft();
		int rightframe = maincont.getCropright();
		int greenframe = maincont.getStartframe();
		p("Got startframe: " + greenframe);
		int endframe = maincont.getEndframe();

		int y0 = 400;
		int h = 350;
		fwleft = new FrameWidget(curveimage.getXFromFrame(leftframe), y0, 6, h, Color.red, 1);
		fwleft.setId("Left");
		fwleft.setDescription("Left");
		canvascurve.drawUIElement(fwleft);

		fwright = new FrameWidget(curveimage.getXFromFrame(rightframe), y0, 6, h, Color.red, 1);
		fwright.setId("right");
		fwright.setDescription("right");
		canvascurve.drawUIElement(fwright);

		fwstart = new FrameWidget(curveimage.getXFromFrame(greenframe), y0, 6, h, Color.green, 1);
		fwstart.setId("start");
		fwstart.setDescription("start");
		canvascurve.drawUIElement(fwstart);

		fwend = new FrameWidget(curveimage.getXFromFrame(endframe), y0, 6, h, Color.green, 1);
		fwend.setId("end");
		fwend.setDescription("end");
		canvascurve.drawUIElement(fwend);

		String bg = url;
		canvascurve.setBackgroundImage(bg);
		v.addComponent(canvascurve);

		canvascurve.addListener(new Canvas.CanvasMouseUpListener() {

			@Override
			public void mouseUp(Point p, UIElement child) {
				int x = (int) p.getX();

				if (child != null && child instanceof Polygon) {
					// p("curvecanvas: Got mouse UP: " + p + ", child=" +
					// child);
					int frame = curveimage.getFrameForX(x);
					p("Frame: " + frame);
					String id = child.getId();
					if (fwleft.getId().equalsIgnoreCase(id)) {
						p("left widget moved: " + frame);
						maincont.setCropleft(frame);
					} else if (fwright.getId().equalsIgnoreCase(id)) {
						p("fwright widget moved: " + frame);
						maincont.setCropright(frame);
					} else if (fwstart.getId().equalsIgnoreCase(id)) {
						p("fwstart widget moved: " + frame);
						maincont.setStartframe(frame);
					} else if (fwend.getId().equalsIgnoreCase(id)) {
						p("fwemd widget moved: " + frame);
						maincont.setEndframe(frame);
					}
				}
				// poly.moveTo(p);
			}
		});
	}

	private int rand(int max) {
		return (int) (Math.random() * max);
	}

	public void clear() {
		canvassub = null;
	}

	private void addCanvasSub(AbstractOrderedLayout v) {
		if (canvassub != null) {
			p("NOT CREATING CANVASSUB");
			v.addComponent(canvassub);
			return;
		}
		// showmask = usemask.getSelection();
		if (!coord.equals(maincont.getAbsCenterAreaCoord())) {
			err("NOT SAME CENTER COORDS:" + coord + "/" + maincont.getRelativeCenterAreaCoord());

		}
		p("Creating SUBREGION image at rel " + coord + "/" + maincont.getAbsCenterAreaCoord() + " with mask: " + showmask);
		String name = "none";
		if (showmask != null) name = showmask.getName();

		subimage = new SubregionImage(maincont, showmask, pixperwell);

		if (subimage == null) return;
		
		String key = exp.getFileKey() + "_raster" + flow + "_" + frame + "_" + type + "_" + coord.getX() + "_" + coord.getY() + "_" + pixperwell + "_" + name;
		StreamResource imageresource = new StreamResource((StreamResource.StreamSource) subimage, key + Math.random() * 1000 + ".png", app);
		imageresource.setCacheTime(10000);
		String relative = imageresource.getApplication().getRelativeLocation(imageresource);
		String appurl = imageresource.getApplication().getURL().toString();
		String url = relative;
		url = appurl + url.replace("app://", "");
		// p("URl from stream resource sub: " + url);
		// app.getMainWindow().open(imageresource, "_blank");

		canvassub = new Canvas();
		// canvassub.setImmediate(true);
		canvassub.setBackgroundColor("black");
		canvassub.setWidth("450px");
		canvassub.setHeight("500px");

		ArrayList<Widget> widgets = maincont.getWidgets();
		if (widgets.size() < 5) {
			// p("=== Adding 5 widgets === NOTE: also update fit Window!");
			WellCoordinate abscenter = maincont.getAbsCenterAreaCoord();
			for (int i = 0; i < 5; i++) {
				WellCoordinate absc = null;
				// USE ABSOLUTE COORDINATES
				if (i == 0) absc = new WellCoordinate(coord.getCol() + exp.getColOffset(), coord.getRow() + exp.getRowOffset());
				else
					absc = new WellCoordinate(rand(50)-25 + abscenter.getCol(), rand(50)-25 + abscenter.getRow());
				if (issnap && this.showmask != null) {
					// move coords to widget
					absc = snapWidgetToMask(absc, showmask);
					exp.makeAbsolute(absc);
				}
				java.awt.Point imagep = subimage.getPointFromWell(absc);
				if (imagep != null) {
					Color col = new Color(rand(255), rand(255), rand(255));
					CoordWidget w = new CoordWidget(col, (int) imagep.getX(), (int) imagep.getY(), i);
					// w.setId("cross"+(i+1));
					w.setAbsoluteCoords(absc);
					widgets.add(w);
				}
			}

		} else
			p("NOT adding more new widgets, already got them, but checking coords");

		if (subimage.getSubregionView() == null) return;
		for (int i = 0; i < widgets.size(); i++) {
			// p("=== Creating Cross for widget " + i);
			CoordWidget w = (CoordWidget) widgets.get(i);
			
			w.setName("Widget " + i);
			int arm = 5;
			int width = 3;
			int dx = 2;
			int dy = 7;
			if (w.isMainWidget()) {
				arm = 7;
				width = 5;
				dy = 8;
			}
			if (issnap && this.showmask != null) {
				// move coords to widget
				// p("Got to snap widgets to mask");
				WellCoordinate absc = w.getAbsoluteCoord();
				absc = snapWidgetToMask(absc, showmask);
				exp.makeAbsolute(absc);
				w.setAbsoluteCoords(absc);
				// p("Coord is now: " + w.getAbsoluteCoord());
			}
			java.awt.Point imagep = subimage.getSubregionView().getMiddleImageCoord(w.getAbsoluteCoord(), true);
			if (imagep != null) {
				w.setX((int) imagep.getX());
				w.setY((int) imagep.getY());
				// p("Coord " + w.getAbsoluteCoord() + "->" + imagep.x + "/" +
				// imagep.y);
			}
			// } else
			// p("Could not compute x/y, got no point from subimage");
			Cross cross = new Cross(w.getX() - dx, w.getY() - dy, width, arm, w.getColor(), (i + 1));
			cross.setId("Widget " + i);
			// cross.setDescription("Widget " + i);
			canvassub.drawUIElement(cross);
		}

		// p("Reopening FIT ");
		app.reopenFit();
		String bg = url;
		canvassub.setBackgroundImage(bg);
		v.addComponent(canvassub);

		canvassub.addListener(new Canvas.CanvasMouseUpListener() {

			@Override
			public void mouseUp(Point p, UIElement child) {
				int newx = (int) p.getX();
				int newy = (int) p.getY();

				if (child != null && child instanceof Polygon) {
					x = newx;
					y = newy;
					// p("canvassub: Got mouse UP: " + p + ", child=" + child);
					final WellCoordinate coord = subimage.getWellCoordinate(x, y);
					exp.makeAbsolute(coord);
					for (Widget w : maincont.getWidgets()) {
						final CoordWidget cw = (CoordWidget) w;
						if (cw.getName().equalsIgnoreCase(child.getId())) {
							
							if (w.isMainWidget()) {								
								String question = "<html>This will reload the masks and data. <br>Do you still want to change the main coordinate?";
								OkDialog okdialog = new OkDialog(mainwindow, "Reload masks and data", question, new OkDialog.Recipient() {
									@Override
									public void gotInput(String name) {
										if (!name.equalsIgnoreCase("OK")) return;
										cw.setAbsoluteCoords(coord);
										cw.setX(x);
										cw.setY(y);
										// p("Found widget id " + child.getId() +
										// ". New position of widget " + w + ": " + x + "/"
										// + y);
										coordsel.setX(coord.getX());
										coordsel.setY(coord.getY());
										updateCurves();
										app.setWellCoordinate(coord);
										app.reopenRaw();

									}
								});
								
							}
							else {
								cw.setAbsoluteCoords(coord);
								cw.setX(x);
								cw.setY(y);
								// p("Found widget id " + child.getId() +
								// ". New position of widget " + w + ": " + x + "/"
								// + y);
								coordsel.setX(coord.getX());
								coordsel.setY(coord.getY());
								updateCurves();
							}
							

						}
					}

				}
				// poly.moveTo(p);
			}

		});
	}

	private void updateCurves() {
		hor.removeComponent(canvascurve);
		addCanvasCurve(hor);
		app.reopenFit();
	}

	private WellCoordinate snapWidgetToMask(WellCoordinate c, BitMask mask) {
		RasterData data = maincont.getData();
		int offx = exp.getColOffset();
		int offy = exp.getRowOffset();
		int x = c.getX() - offx - data.getRelStartcoord().getCol();
		int y = c.getY() - offy - data.getRelStartcoord().getRow();
		int size = data.getRaster_size();
		for (int j = 0; j < size; j++) {
			for (int i = 0; i < size; i++) {
				x++;
				if (x >= size) {
					x = 0;
					y++;
					if (y >= size) {
						y = 0;
					}
				}
				if (mask.get(x, y)) {
					c = new WellCoordinate(x + offx + data.getRelStartcoord().getCol(), y + offy + data.getRelStartcoord().getRow());
					p("Moving widget to " + c);
					return c;
				}
			}
			y++;
			if (y >= size) {
				y = 0;
			}
		}
		return c;
	}

	public void addCoordAndFlowSelection(HorizontalLayout h) {
		tflow = new TextField();
		tflow.setWidth("60px");
		// tflow.setHeight("25px");
		tflow.setImmediate(true);

		String s = "" + flow;
		tflow.setValue(s);
		tflow.setDescription("Enter the zero based flow number");
		h.addComponent(new Label("Flow:"));
		h.addComponent(tflow);

		tflow.addListener(new Property.ValueChangeListener() {

			public void valueChange(ValueChangeEvent event) {
				// addChart(chartTab);

				flow = parseFlow();
				maincont.setFlow(flow);
				maincont.clearData();
				canvassub = null;
				coordChanged = true;
				p("Clearing explorercontext.data");
				p("Got flows " + flow);
				reopen();
			}
		});
		tframe = new TextField();
		tframe.setWidth("60px");
		// tflow.setHeight("25px");
		tframe.setImmediate(true);

		s = "" + frame;
		tframe.setValue(s);
		h.addComponent(new Label("Frame:"));
		h.addComponent(tframe);

		tframe.addListener(new Property.ValueChangeListener() {

			public void valueChange(ValueChangeEvent event) {
				// addChart(chartTab);
				parseFrame();
				canvassub = null;
				p("Got frame " + frame);
				reopen();
			}
		});

		coordsel = new CoordSelect(coord.getCol() + exp.getColOffset(), coord.getRow() + exp.getRowOffset(), new Button.ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				p("========= REFRESH CLICKED =========");
				WellCoordinate newcoord = coordsel.getCoord();
				if (newcoord != null && !coord.equals(newcoord)) {
					coord = newcoord;
					exp.makeRelative(coord);
					maincont.setRelativeCenterAreaCoord(coord);
					app.setWellCoordinate(coord);
					app.reopenRaw();
					coordChanged = true;
				} else
					p("No coord change, but will reload data anyway");
				canvassub = null;
				maincont.clearData();
				p("Maincont data should be NULL: " + maincont.getData());
				maincont = app.getExplorerContext();
				p("Maincont data should be NULL: " + maincont.getData());
				reopen();

			}
		});
		coordsel.addGuiElements(h);
	}

	public void addTypeSelection(HorizontalLayout h) throws UnsupportedOperationException {
		typeselect = new Select();

		for (RawType t : RawType.values()) {
			typeselect.addItem(t);
			typeselect.setItemCaption(t, t.getDescription());
		}
		if (type == null) {
			type = RawType.ACQ;
		}
		typeselect.select(type);
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
			p("RawType changed: Clearing explorercontext.data");
			maincont.clearData();

			canvassub = null;
			coordChanged = true;
			reopen();
		}
	}

	private RawType getSelectedType() {
		if (typeselect == null || typeselect.getValue() == null) {
			type = RawType.ACQ;
		} else {
			type = (RawType) typeselect.getValue();
		}
		// maincont.setFiletype(type);
		// maincont.setData(null);
		return type;
	}

	public int parseFlow() {
		if (tflow == null) {
			return flow;
		}
		String s = "" + tflow.getValue();
		if (s != null) {
			flow = Integer.parseInt(s);
			p("parsed flow: " + flow);
		}

		return flow;
	}

	public int parseFrame() {
		if (tframe == null) {
			return frame;
		}
		String s = "" + tframe.getValue();
		if (s != null) {
			frame = Integer.parseInt(s);
			p("parsed frame: " + frame);
		}
		return frame;
	}

	private static void err(String msg, Exception ex) {
		Logger.getLogger(ProcessWindowCanvas.class.getName()).log(Level.SEVERE, msg, ex);
	}

	private static void err(String msg) {
		Logger.getLogger(ProcessWindowCanvas.class.getName()).log(Level.SEVERE, msg);
	}

	private static void warn(String msg) {
		Logger.getLogger(ProcessWindowCanvas.class.getName()).log(Level.WARNING, msg);
	}

	private static void p(String msg) {
		//system.out.println("ProcessWindowCanvas: " + msg);
		Logger.getLogger(ProcessWindowCanvas.class.getName()).log(Level.INFO, msg);
	}
}
