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
import com.iontorrent.stats.NrReadsStats;
import com.iontorrent.stats.PolyStats;
import com.iontorrent.stats.SimpleStats;
import com.iontorrent.stats.StatsComputer;
import com.iontorrent.torrentscout.explorer.ExplorerContext;
import com.iontorrent.vaadin.TSVaadin;
import com.iontorrent.vaadin.utils.CoordSelect;
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
public class BarcodeMaskWindowCanvas extends WindowOpener implements
		Button.ClickListener, Property.ValueChangeListener {

	private TSVaadin app;
	CoordSelect coordsel;
	ExperimentContext exp;
	int x;
	private static final int MAX_RES = 5000;
	int y;
	ScoreMaskFlag flag;
	Canvas canvas;
	BarcodeMaskImage bfmask;
	ExperimentContext oldexp;
	ZoomControl zoom;
	int bucket;
	HorizontalLayout h;
	int gradmin;
	boolean testing;
	int gradmax;
	StreamResource imageresource;
	ScoreMask mask;
	BitMask bitmask;
	String maskname;
	long total;

	Table table;
//	private boolean autoLoad;

	ScoreMaskFlag DENSITY_FLAG;
	ScoreMaskFlag ENRICH_FLAG;
	ScoreMaskFlag POLY_FLAG;
	ScoreMaskFlag READ_FLAG;

	SimpleStats typedens = new LoadingDensityStats();
	SimpleStats typeenrich = new EnrichmentStats();
	SimpleStats typepoly = new PolyStats();
	SimpleStats typeread = new NrReadsStats();
	StatsComputer comp;

	SimpleStats curtype;

	ReadGroup curgroup;
	DatasetsManager manager;

	public BarcodeMaskWindowCanvas(TSVaadin app, Window main,
			String description, int x, int y) {
		super("Barcode heat maps", main, description, x, y, 600, 600);
		this.app = app;
		bucket = 8;
		
	//	setAutoLoad(true);
		DENSITY_FLAG = ScoreMaskFlag.CUSTOM1;
		ENRICH_FLAG = ScoreMaskFlag.CUSTOM2;
		POLY_FLAG = ScoreMaskFlag.CUSTOM3;
		READ_FLAG = ScoreMaskFlag.CUSTOM4;
		DENSITY_FLAG.setName(typedens.getName());
		DENSITY_FLAG.setDescription(typedens.getDescription());
		ENRICH_FLAG.setName(typeenrich.getName());
		ENRICH_FLAG.setDescription(typeenrich.getDescription());
		POLY_FLAG.setName(typepoly.getName());
		POLY_FLAG.setDescription(typepoly.getDescription());
		READ_FLAG.setName(typeread.getName());
		READ_FLAG.setDescription(typeread.getDescription());
		// flags = ScoreMaskFlag.values();
	}

	private double[][] computeHeatMap(ReadGroup rg, SimpleStats type,
			ScoreMaskFlag flag) {
		
		BfMask barcodemask = exp.getBarcodeMask();
		p("Barcodemask: "+barcodemask.getNrCols()+"/"+barcodemask.getNrCols());
		BitMask mask = barcodemask.createMaskForCode(rg.getName(),rg.getIndex());
		int total = mask.getTotal();
		p("compute heat maps: "+barcodemask.getNrCols()+"/"+barcodemask.getNrRows());
		p("Total read count: "+total);
		p("bitmask: "+mask.getNrCols()+"/"+mask.getNrCols());
		rg.setBarcodemask(mask);
		if (rg.getReadCount() == 0) {
			rg.setReadCount(total);
		}
		double[][] data = comp.computeHeatMap(rg, type);
		p("Got data with size "+data.length+"/"+data[0].length);
		
		if (testing) {
			p("Adding random  test data");
			int cols = data.length;
			int rows = data[0].length;
			for (int i = 0; i < 10000; i++) {
				int x = (int)(Math.random()*cols);
				int y = (int)(Math.random()*rows);
				data[x][y] = Math.random()*(Math.sqrt(x*y)/10+100);					
			}
			
		}
		
		flag.setFilename(exp.getPluginDir() + type.getImageFileName(rg));

		return data;
	}

	@Override
	public void openButtonClick(Button.ClickEvent event) {
		exp = app.getExperimentContext();
		if (exp == null) {
			appwindow.showNotification("No Experiment Selected",
					"<br/>Please open an experiment first",
					Window.Notification.TYPE_WARNING_MESSAGE);
			return;
		}

		if (!exp.hasBam() && !exp.hasSff() && !exp.hasSeparator()) {
			// try to find other bam?
			appwindow.showNotification(
					"No BAM, separator or sff file not found",
					"<br/>Could not find the file " + exp.getBamFilePath(),
					Window.Notification.TYPE_WARNING_MESSAGE);
			return;
		}
		manager = exp.getDatasets();
		if (manager == null || manager.getReadGroups().size() < 2) {
			appwindow.showNotification(
					"No Barcodes",
					"<br/>This experiment seems to have no barcodes "
							+ exp.getBamFilePath(),
					Window.Notification.TYPE_WARNING_MESSAGE);
			return;
		}
		

		super.openButtonClick(event);
	}

	public void setFlag(ScoreMaskFlag flag) {
		this.flag = flag;
	//	this.setAutoLoad(true);
	}

	@Override
	public void windowOpened(final Window mywindow) {
		p("==== OPEN BARCODE WINDOWS ==== ");
		if (flag == null)
			flag = DENSITY_FLAG;

		if (curtype == null)
			curtype = this.typedens;
		exp = app.getExperimentContext();
		comp = new StatsComputer(exp);
		
		comp.computeKeypassAndEnrichmehtForReadGroups();
		if (oldexp == null || exp != oldexp) {
			if (exp.is318() || exp.getNrrows()>3000)
				bucket = 15;
			else if (exp.is316() || exp.getNrrows()>2000)
				bucket = 10;
			else if (exp.isThumbnails() || exp.getNrrows()<1000)
				bucket = 4;
		}
		
		oldexp = exp;

		manager = exp.getDatasets();

		if (curgroup == null) {
			curgroup = manager.getReadGroup(0);
		}
		WellCoordinate coord = exp.getWellContext().getCoordinate();
		if (coord == null) {
			p("Coord is null, creating a new coord");
			coord = new WellCoordinate(100, 100);
		}
		x = coord.getX();
		y = coord.getY();
		h = new HorizontalLayout();

		Select sel = new Select();
		ScoreMaskFlag[] flags = new ScoreMaskFlag[3];
		flags[0] = this.DENSITY_FLAG;
		flags[1] = this.ENRICH_FLAG;
		flags[2] = this.POLY_FLAG;
		//flags[3] = this.READ_FLAG;
		for (ScoreMaskFlag f : flags) {
			sel.addItem(f);
			String n = f.getName();
			sel.setItemCaption(f, n);
		}
		
		sel.select(flag);
		sel.setDescription(flag.getDescription());
		sel.addListener(this);
		h.addComponent(new Label(" Flag: "));
		h.addComponent(sel);

		Select rgsel = new Select();

		for (ReadGroup rg : manager.getReadGroups()) {
			rgsel.addItem(rg);
			String n = rg.getBarcodeName();
			if (n == null)
				n = "no barcode";
			rgsel.setItemCaption(rg, n);
		}
		rgsel.select(curgroup);
		rgsel.addListener(this);
		
		rgsel.setDescription(curgroup.getDescription());
		h.addComponent(new Label(" Barcode: "));
		h.addComponent(rgsel);

		final NativeButton export = new NativeButton();
		export.setStyleName("nopadding");
		export.setIcon(new ThemeResource("img/export.png"));
		export.setDescription("Save image");
		export.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(com.vaadin.ui.Button.ClickEvent event) {
				OptionsDialog input = new OptionsDialog(mywindow,
						"What would you like to export?", "Export...",
						"... this image", "... save the search result",
						new OptionsDialog.Recipient() {

							@Override
							public void gotInput(final int selection) {
								if (selection < 0)
									return;
								// / do the search
								if (selection == 0) {
									app.getMainWindow().open(imageresource,
											"_blank");
									return;
								}
								saveFile(true);

							}

						});

			}
		});
		
//		final NativeButton test = new NativeButton();
//		test.setStyleName("nopadding");
//		test.setIcon(new ThemeResource("img/bug.png"));
//		test.setDescription("Test");
//		test.addListener(new Button.ClickListener() {
//			@Override
//			public void buttonClick(com.vaadin.ui.Button.ClickEvent event) {
//				testing = !testing;
//				bfmask = null;
//				
//				reopen();
//			}
//		});
		
		// h.addComponent(export);

		Button bmask = new Button();
		bmask.setDescription("Create a mask that can be used in the Process/Automate component");
		bmask.setIcon(new ThemeResource("img/mask.png"));
		h.addComponent(bmask);
		bmask.addListener(new Button.ClickListener() {
			public void buttonClick(Button.ClickEvent event) {
				createMask();
			}
		});

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

		// h.addComponent(new Label(exp.getResultsDirectory()));

		sel.setImmediate(true);
		sel.addListener(this);

		mywindow.addComponent(h);

		

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
	//	vzoom.addComponent(test);
		HorizontalLayout hcan = new HorizontalLayout();

		hcan.addComponent(vzoom);
		
		computeMask( hcan);
		app.openBarcodeTable();

		mywindow.addComponent(hcan);

	}

	private void computeMask(HorizontalLayout hcan) {
		mask = app.getScoreMask();

		double[][] data = mask.getData(flag);
		if (data == null) {
			p("computing data for flag " + flag + " and rg " + curgroup);
			data = this.computeHeatMap(curgroup, curtype, flag);
		}
		else {
			p("==== Reusing data: "+data.length);
		}
		
		mask.setData(flag, data);
		p("mask setting data for flag "+flag.getName()+", scoremask size="+mask.getNrCols()+"/"+mask.getNrRows());
		flag.setMultiplier(1);
		
		if (bfmask == null)
			bfmask = new BarcodeMaskImage(mask, exp, flag, bucket);

		this.setWidth(bfmask.getImage().getWidth() + 50 + "px");
		this.setHeight(bfmask.getImage().getHeight() + 50 + "px");
		p("Getting streamresource for flag " + flag);
		imageresource = new StreamResource(
				(StreamResource.StreamSource) bfmask, exp.getFileKey()
						+ flag.getName()+"_"+curgroup.getIndex() + (int) (Math.random() * 100)
						+ "_barcode.png", app);
		imageresource.setCacheTime(60000);
		// imageresource.

		// if (canvas == null) {
		canvas = new Canvas();
		canvas.setHeight(bfmask.getImage().getHeight() + "px");
		canvas.setBackgroundColor("black");

		
		String bg = app.getBgUrl(imageresource.getApplication()
				.getRelativeLocation(imageresource));
		canvas.setBackgroundImage(bg);
		canvas.setHeight((bfmask.getImage().getHeight() + 100) + "px");
		final GradientPanel grad = bfmask.getGradient();
		grad.setInPercent(false);
		// get multiplier!
		int mult = flag.multiplier();

		int b = this.bucket * bucket;
		GradientLegend leg = new GradientLegend(mult * b, grad,
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
		canvas.addListener(new Canvas.CanvasMouseUpListener() {

			@Override
			public void mouseUp(Point p, UIElement child) {
				int newx = (int) p.getX();
				int newy = (int) p.getY();
				p("Got mouse UP on canvas: " + p + ", child=" + child);
				// if (child != null && child instanceof Polygon) {
				x = newx;
				y = newy;
				// Polygon po = (Polygon) child;
				// p("Location of child po: "+po.getCenter());

				WellCoordinate coord = bfmask.getWellCoordinate(x, y);
				// p("Got coord: " + coord + ", setting description");
				coordsel.setX(coord.getX() + exp.getColOffset());
				coordsel.setY(coord.getY() + exp.getRowOffset());
				// po.setDescription("changed description :"+coord);
				buttonClick(null);
				// }
				// poly.moveTo(p);
			}
		});

		if (flag.isCustom()) {
			total = mask.getTotal(flag);

			if (total < MAX_RES) {
				selectAllWellsOfResult(MAX_RES,
						total + " results of " + flag.getDescription());
			} else
				getLatestSelection(MAX_RES + " (sub)results of "
						+ flag.getDescription());
		}
	}
	
	public void clear() {
		bfmask = null;
	}

	public String getHelpMessage() {
		String msg = "<ul>";
		msg += "<li>drag the cursor and then <b>double click</b> to pick a well or region</li>";
		msg += "<li>zoom in or out of the image </li>";
		msg += "<li>Create a mask with a search result (that can be usedin Process/Automate) </li>";
		msg += "<li>export the image (opens a new windows, then right click on the image and click save as) </li>";
		msg += "<li>enter a new x (column) or y (row) coordinat and hit refresh to also change the coordinate in other components </li>";
		msg += "</ul>";
		return msg;
	}

	private void saveFile(boolean ask) {
		mask = app.getScoreMask();

		final double[][] data = mask.getData(flag);
		if (data == null) {
			app.showMessage("Nothing to save", "See no data to save");
			return;
		}
		File f = new File(exp.getPluginDir());

		app.logModule(BarcodeMaskWindowCanvas.this.getName(), "save mask");
		if (flag.getFilename() != null)
			f = new File(flag.getFilename());

		FileBrowserWindow browser = new FileBrowserWindow("Pick a .bmp file",
				null, new FileBrowserWindow.Recipient() {
					@Override
					public void fileSelected(File file) {
						p("Got file:" + file);
						if (file != null && !file.isDirectory()) {
							flag.setFilename(file.toString());
							ScoreMaskGenerator gen = new ScoreMaskGenerator(
									mask, exp);
							gen.createImageFile(flag, data);
							app.showMessage("Saved",
									"Data of " + flag.getName() + " saved to "
											+ file);
						} else
							app.showMessage("Not saving", "Won't save to file "
									+ file);

					}

					public boolean allowInList(File f, boolean toSave) {
						if (f == null)
							return false;
						if (f.isDirectory()) {
							if (!f.canRead())
								return false;
						}
						return true;
					}

				}, FileBrowserWindow.SAVE, mywindow, f, ".bmp");
		browser.open();
	}

	private void createMask() {
		final String defname = "custom";
		app.logModule(super.getName(), "createmask");
		InputDialog input = new InputDialog(mywindow,
				"Name of this mask (can be used in Process/Automate)",
				new InputDialog.Recipient() {

					@Override
					public void gotInput(String name) {

						if (name == null || name.length() < 1)
							name = defname;
						maskname = name;
						mask = app.getScoreMask();

						if (flag == null || maskname == null)
							return;

						BitMask m = mask.createCustomMask(flag, maskname);
						ExplorerContext cont = app.getExplorerContext();
						p("exp  custom masks");
						for (BitMask mas : exp.getCustomMasks()) {
							p(mas.getName());
						}
						p("createMask: Adding custom mask to maincontext");
						cont.addCustomMasks();
						p("createMask: maincontext Masks are now: ");
						for (BitMask mas : cont.getMasks()) {
							p(mas.getName());
						}

						// if (cont.getMasks()
						DecimalFormat f = new DecimalFormat("#.###");
						long tot = mask.getTotal(flag);
						p("Total nr flags:" + tot + ", mask total="
								+ m.getTotal() + ", %:"
								+ f.format(m.computePercentage()));
						app.showMessage(
								BarcodeMaskWindowCanvas.this,
								"Created mask "
										+ maskname
										+ " with "
										+ m.getTotal()
										+ "="
										+ f.format(m.computePercentage())
										+ "% wells<br>(use in Automate or Process)");
					}

				}, defname);

	}

	

	public void valueChange(Property.ValueChangeEvent event) {
		// The event.getProperty() returns the Item ID (IID)
		// of the currently selected item in the component.
		Property id = event.getProperty();
		p("valuechanged: "+id);
		mask.clear(flag);
		if (id.getValue() instanceof ScoreMaskFlag) {
			ScoreMaskFlag b = (ScoreMaskFlag) id.getValue();
			this.flag = b;
			if (flag == POLY_FLAG) curtype = this.typepoly;
			else if (flag == READ_FLAG) curtype = this.typeread;
			else if (flag == DENSITY_FLAG) curtype = this.typedens;
			else if (flag == ENRICH_FLAG) curtype = this.typeenrich;
				
			
			bfmask = null;
			this.reopen();
		} else if (id.getValue() instanceof ReadGroup) {
			curgroup = (ReadGroup) id.getValue();

	//		this.setAutoLoad(true);
			// pick wells o fthis flag
			bfmask = null;
			this.reopen();
		}

	}

	public void buttonClick(Button.ClickEvent event) {
		getLatestSelection("Selecting wells around specified area");
	}

	private void getLatestSelection(String title) {
		x = coordsel.getX();
		y = coordsel.getY();
		WellCoordinate coord = new WellCoordinate(x, y);
		// also only select certain wells!
		int d = 50;
		// app.showMessage("Selecing wells",
		// "Selecting wells with data for flag " + flag + " (at most 5000)");
		
		ArrayList<WellCoordinate> wells = mask.getAllCoordsWithData(flag, 1000,
				x - d, y - d, x + d, y + d);
		WellSelection wsel = new WellSelection(x - d, y - d, x + d, y + d, wells);
		wsel.setTitle(title);
		exp.getWellContext().setSelection(wsel);
		if (wells != null) {
			for (WellCoordinate well : wells) {

				well.setScoredata(mask.getDataPointsAt(well.getCol(),
						well.getRow()));
				// p("setting score data:"+Arrays.toString(well.getScoredata()));
			}
		}
		exp.makeRelative(coord);
		
		// Don't trigger a new well selection!
		app.setWellCoordinate(coord, false);
		
	}

	private void selectAllWellsOfResult(int max, String title) {
		// app.showMessage("All wells",
		// "Showing <b>all</b> wells of search result");
		ArrayList<WellCoordinate> wells = mask.getAllCoordsWithData(flag, max);
		if (wells == null)
			return;
		WellSelection sel = new WellSelection(wells);
		exp.getWellContext().setSelection(sel);
		sel.setTitle(title);
		if (wells != null) {
			for (WellCoordinate well : wells) {
				well.setScoredata(mask.getDataPointsAt(well.getCol(),
						well.getRow()));
				// p("setting score data:"+Arrays.toString(well.getScoredata()));
			}
		}
		app.setWellSelection(sel);
		// Don't trigger a new well selection!
	}

	private static void err(String msg, Exception ex) {
		Logger.getLogger(BarcodeMaskWindowCanvas.class.getName()).log(
				Level.SEVERE, msg, ex);
	}

	private static void err(String msg) {
		Logger.getLogger(BarcodeMaskWindowCanvas.class.getName()).log(
				Level.SEVERE, msg);
	}

	private static void warn(String msg) {
		Logger.getLogger(BarcodeMaskWindowCanvas.class.getName()).log(
				Level.WARNING, msg);
	}

	private static void p(String msg) {
		System.out.println("BarcodeMaskWindowCanvas: " + msg);
		Logger.getLogger(BarcodeMaskWindowCanvas.class.getName()).log(
				Level.INFO, msg);
	}

	public void setCurGroup(ReadGroup curgroup) {
		this.curgroup = curgroup;
		mask.clear(flag);
		reopen();
		
	}

}
