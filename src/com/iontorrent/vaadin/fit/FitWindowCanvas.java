/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.fit;

import java.awt.Color;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.guiutils.widgets.CoordWidget;
import com.iontorrent.guiutils.widgets.Widget;
import com.iontorrent.heatmaps.ScoreMaskGenerator;
import com.iontorrent.rawdataaccess.wells.BitMask;
import com.iontorrent.rawdataaccess.wells.ScoreMaskFlag;
import com.iontorrent.results.scores.ScoreMask;
import com.iontorrent.torrentscout.explorer.ExplorerContext;
import com.iontorrent.torrentscout.explorer.fit.AbstractHistoFunction;
import com.iontorrent.torrentscout.explorer.fit.FitFunctionsFactory;
import com.iontorrent.utils.stats.HistoStatistics;
import com.iontorrent.utils.stats.StatPoint;
import com.iontorrent.vaadin.TSVaadin;
import com.iontorrent.vaadin.mask.MaskSelect;
import com.iontorrent.vaadin.scoremask.ScoreMaskWindowCanvas;
import com.iontorrent.vaadin.utils.InputDialog;
import com.iontorrent.vaadin.utils.InputDialog.Recipient;
import com.iontorrent.vaadin.utils.WindowOpener;
import com.iontorrent.wellmodel.RasterData;
import com.iontorrent.wellmodel.WellCoordinate;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.graphics.canvas.Canvas;
import com.vaadin.graphics.canvas.shape.CutWidget;
import com.vaadin.graphics.canvas.shape.Point;
import com.vaadin.graphics.canvas.shape.Polygon;
import com.vaadin.graphics.canvas.shape.UIElement;
import com.vaadin.terminal.StreamResource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Select;
import com.vaadin.ui.Window;

/**
 * 
 * @author Chantal Roth chantal.roth@lifetech.com
 */
public class FitWindowCanvas extends WindowOpener {

	private TSVaadin app;
	ExplorerContext maincont;
	ExperimentContext exp;
	Select funselect;
	Select typeselect;
	Canvas canvashist;
	MaskSelect histomask;
	HorizontalLayout hor;
	CutWidget fwleft;
	CutWidget fwright;
	int cutleft;
	int cutright;
	double minx;
	double maxx;
	CheckBox add;
	boolean zoom;
	AbstractHistoFunction curfunction;
	HistImage himage;
	StatPoint datapoints;
	double[][] histodata;
	boolean addToHist;
	  StreamResource imageresource;

	public FitWindowCanvas(TSVaadin app, Window main, String description, int x, int y) {
		super("Fit (create masks)", main, description, x, y, 800, 500);
		this.app = app;
		cutleft = 50;
		cutright = 400;

	}

	@Override
	public void openButtonClick(Button.ClickEvent event) {
		if (app.getExperimentContext() == null) {
			appwindow.showNotification("No Experiment Selected", "<br/>Please open an experiment first", Window.Notification.TYPE_WARNING_MESSAGE);
			return;
		}
		exp = app.getExperimentContext();
		maincont = app.getExplorerContext();
		RasterData data = maincont.getData();

		if (data == null) {
			appwindow.showNotification("No Data Loaded", "<br/>Opening the Process component first", Window.Notification.TYPE_WARNING_MESSAGE);
			app.reopenProcess(true);
			// return;
		}
		super.openButtonClick(event);
	}

	@Override
	public void windowOpened(final Window mywindow) {
		p("====================== windowOpened ===============================");
		maincont.setPreferrednrwidgets(5);

		HorizontalLayout h = new HorizontalLayout();
		mywindow.addComponent(h);

		funselect = new Select(null);
		ArrayList<AbstractHistoFunction> functions = FitFunctionsFactory.getFunctions(maincont, true);
		for (AbstractHistoFunction f : functions) {
			funselect.addItem(f);
		}
		if (curfunction == null) curfunction = functions.get(0);
		funselect.select(curfunction);
		funselect.setDescription(curfunction.getDescription());
		h.addComponent(funselect);

		add = new CheckBox("Add");
		h.addComponent(add);
		add.setDescription("The next histogram will the added to the currently displayed histogram - that lets you compare data sets");
		add.addListener(new CheckBox.ValueChangeListener() {

			@Override
			public void valueChange(ValueChangeEvent event) {
				addToHist = (Boolean)add.getValue();
				
			}
			
		});
		Button bcomp = new Button("Compute");
		h.addComponent(bcomp);
		bcomp.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				reopen();
			}
		});

		Button bmask = new Button("Create Mask");
		h.addComponent(bmask);
		bmask.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				createMask();
			}
		});
		Button bzoom = new Button("Zoom");
		h.addComponent(bzoom);
		bzoom.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				zoom = true;
				reopen();
			}
		});
		
		histomask = new MaskSelect("histo", "Mask for histogram","Only wells of this mask will be used to compute the histogram",  maincont, MaskSelect.HISTO, new Property.ValueChangeListener() {

			@Override
			public void valueChange(ValueChangeEvent event) {
				if (histomask.getSelection() == null) {
					maincont.setHistoMask(null);
				}
				reopen();

			}
		}, maincont.getHistoMask());
		histomask.addGuiElements(h);
		
		
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
			export.setDescription("Open image in another browser window so that you can save it to file");	
			export.addListener(new Button.ClickListener(){
				@Override
				public void buttonClick(com.vaadin.ui.Button.ClickEvent event) {
					app.getMainWindow().open(imageresource, "_blank");				
				}			
			});
			h.addComponent(export);

		compute();

		funselect.addListener(new Property.ValueChangeListener() {

			@Override
			public void valueChange(ValueChangeEvent event) {
				Property id = event.getProperty();
				if (id.getValue() instanceof AbstractHistoFunction) {
					AbstractHistoFunction b = (AbstractHistoFunction) id.getValue();
					curfunction = b;
					
					reopen();
				}

			}
		});

		hor = new HorizontalLayout();
		mywindow.addComponent(hor);
		addCanvasHist(hor);

		// app.showMessage(this,
		// "Drag the cursors to select a different wells");

	}
	 public String getHelpMessage() {
	        String msg = "<ul>";
	        msg += "<li>Pick a function to classify wells.<br>"
	                + "Some cases (integral), it will simply compute the value based on the data as you would expect from the name of the function<br>"
	                + "Where functions are used such as peak or parameterized adjustment, it will usually compute the <b>root of the sum of error squared</b><br>"
	                + "where error means simply the difference between value from the function and value from the data</li>";
	        msg += "<li>You can then move the scissors around to define the area you want to use to create a mask from</li>";
	        msg += "<li>To create a mask with these wells, click on the mask icon. It will then use all wells that are:<br>"
	                + "- in the selected mask (from the drop down)<br>"
	                + "- and are in the selected range (scissors)</li>";
//	        msg += "<li>You can change the min and max x coordinate in the view eith the blue and red line icons</li>";
//	        msg += "<li>by clicking the add button, it will add the next histogram to the existing histogram,<br>"
//	                + "this helps to see differences between functions or between masks</li>";
	        msg += "<li>You can also export the data to file with the save icon</li>";
	      
	        msg += "</ul>";
	        return msg;
	 }
	private void createMask() {
		int bleft = himage.getBin(cutleft);
		int bright = himage.getBin(cutright);
		HistoStatistics stats = himage.getHisto();
		minx = stats.getX(bleft);
		maxx = stats.getX(bright);
		if (minx > maxx) {
			double tmp = minx;
			minx = maxx;
			maxx = tmp;
		}
		p("left: " + minx + ", right: " + maxx);

		// ask what KIND of mask, a bit mask, or a score mask - or just add both?
		
		
		InputDialog input = new InputDialog(mywindow, "Name of new mask: ", new Recipient() {

			@Override
			public void gotInput(String name) {
				if (name == null || name.length() < 1) {
					return;
				}
				p("Created mask with name: " + name);
				BitMask mask = curfunction.createMask(maincont.getHistoMask(), minx, maxx);
				// also add score mask
				ScoreMask scoremask = app.getScoreMask();
				ScoreMaskGenerator gen = new ScoreMaskGenerator(scoremask, exp);
				ScoreMaskFlag flag = ScoreMaskFlag.CUSTOM1;
				flag.setFilename(null);
				flag.setName(name +"(with values)");
				flag.setDescription("Result from fit "+curfunction.getName()+", with values");
				double[][] res = curfunction.createValueMask(maincont.getHistoMask(), minx, maxx);
				try {
					gen.createCustomMask(flag, res);
				} catch (IOException e) {
					err("Could not create custom mask for "+flag);
				}
				mask.setName(name);
				app.logModule(FitWindowCanvas.this.getName(), "create mask "+name);
				DecimalFormat f = new DecimalFormat("#.###");
				String msg = "<html>Created TWO masks <b>" + mask.getName() + "</b> with " + mask.computePercentage() + "% wells<br>using interval " + f.format(minx) + " - " + f.format(maxx) + 
						"<br>and a mask with VALUES to be used in the XYZ chart<br> ";
				if (maincont.getHistoMask() != null) {
					msg += " using wells <b>only from mask " + maincont.getHistoMask() + "</b>";
				} else {
					msg += "using <b>all wells</b>";
				}
				msg += "</html>";
				app.showLongMessage("Mask "+mask.getName()+" created", msg);
				
				addMask(mask);
			}
		});

	}
	private void addMask(BitMask mask) {
		maincont.addMask(mask);
		p("Adding mask "+mask.getName()+" to maincont");
//		p("masks are now: ");
//		for (BitMask m: maincont.getMasks()) {
//			p(m.getName());
//		}
		p("need to reopen process and mask edit and automate");
		app.reopenProcess(false);
		app.reopenMaskedit(false);
		app.reopenAutomate();
	}

	private void compute() {
		BitMask histo = histomask.getSelectedMask();
		//p("Got histo mask: "+histo.getName()+"/"+maincont.getHistoMask());
		maincont.setHistoMask(histo);
		p("Computing " + curfunction);
		curfunction.setMinx(-10000);
		curfunction.setMaxx(10000);
		
		curfunction.execute();
		
		datapoints = curfunction.getDataPoints();
		histodata = curfunction.getResult();
	}

	private void addCanvasHist(AbstractOrderedLayout v) {

		p("Creating canvashist image. zoom="+zoom+", add="+addToHist);
		if (zoom && himage != null) {
			himage.zoom();
		}
		else if (addToHist && himage != null) {
			himage.addToHistoPanel(datapoints);
		}
		else himage = new HistImage(maincont, datapoints, histodata,cutleft, cutright);
		
		imageresource = new StreamResource((StreamResource.StreamSource) himage, exp.getFileKey() + "_" + exp.getWellContext().getAbsoluteCoordinate().getX() + "_" + exp.getWellContext().getAbsoluteCoordinate().getY() + curfunction.getName() + (int) (Math.random() * 1000) + "_"+zoom+".png", app);
		imageresource.setCacheTime(10000);
		zoom = false;
		addToHist = false;
		add.setValue(addToHist);
		// app.getMainWindow().open(imageresource, "_blank");
		canvashist = new Canvas();
		// canvashist.setImmediate(true);
		canvashist.setBackgroundColor("black");
		canvashist.setWidth("800px");
		canvashist.setHeight("400px");

		String bg =  app.getBgUrl(imageresource.getApplication().getRelativeLocation(imageresource));
		canvashist.setBackgroundImage(bg);
		v.addComponent(canvashist);

		addScissors();

		canvashist.addListener(new Canvas.CanvasMouseUpListener() {

			@Override
			public void mouseUp(Point p, UIElement child) {
				int x = (int) p.getX();
			//	p("canvashist: Got mouse UP: " + p + ", child=" + child);
				int bin = himage.getBin(x);
			//	p("bin: " + bin + ", some x value");

				if (child != null && child instanceof Polygon) {
					String id = child.getId();
					himage.getXForXval(bin);
					if (id.equals(fwleft.getId())) {
					//	p("left moved");
						cutleft = x;						
						//reopen();
					} else if (id.equals(fwright.getId())) {
					//	p("right moved");
						cutright = x;
						//reopen();
					}
				} else {
					WellCoordinate randcoord = himage.findCoordForBin(bin);
					exp.makeAbsolute(randcoord);
					
					boolean found = false;
					int nr = maincont.getWidgets().size();
					if (nr == 0) {
						err("GOT NO COORD WIDGETS");
					}
					p("Got random coordinate " + randcoord + " for bin " + bin+" "+nr+" widgets ");
					while (!found) {
						int rand = (int)(Math.random()*nr);
						Widget w = maincont.getWidgets().get(rand);
						if (!w.isMainWidget() || nr==1) {
							found = true;	
							p("setting coord of "+w+" to "+ randcoord);
							CoordWidget cw = (CoordWidget) w;
							cw.setAbsoluteCoords(randcoord);
							app.reopenProcess(false);
							reopen();
						}
					}
				}
			}
		});

	}

	private void addScissors() {
		int y0 = 370;
		int h = 100;
		if (cutleft <= 0) cutleft = 50;
		if (cutright <= 0) cutright = 300;
		cutright = Math.max(10, cutright);
		cutright = (int) Math.min(cutright, canvashist.getWidth());
		cutleft = Math.max(10, cutleft);
		fwleft = new CutWidget(cutleft, y0, 6, h, Color.LIGHT_GRAY, 1);
		fwleft.setId("Left");
		fwleft.setDescription("Left");
		
				
		canvashist.drawUIElement(fwleft);

		fwright = new CutWidget(cutright, y0, 6, h, Color.LIGHT_GRAY, 1);
		fwright.setId("Right");
		fwright.setDescription("Right");
		canvashist.drawUIElement(fwright);
	}

	private static void err(String msg, Exception ex) {
		Logger.getLogger(FitWindowCanvas.class.getName()).log(Level.SEVERE, msg, ex);
	}

	private static void err(String msg) {
		Logger.getLogger(FitWindowCanvas.class.getName()).log(Level.SEVERE, msg);
	}

	private static void warn(String msg) {
		Logger.getLogger(FitWindowCanvas.class.getName()).log(Level.WARNING, msg);
	}

	private static void p(String msg) {
		//system.out.println("FitWindowCanvas: " + msg);
		Logger.getLogger(FitWindowCanvas.class.getName()).log(Level.INFO, msg);
	}
}
