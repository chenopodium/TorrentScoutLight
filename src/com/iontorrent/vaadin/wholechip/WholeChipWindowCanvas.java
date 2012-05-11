/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.wholechip;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.rawdataaccess.pgmacquisition.RawType;
import com.iontorrent.rawdataaccess.wells.BfMaskFlag;
import com.iontorrent.utils.ErrorHandler;
import com.iontorrent.vaadin.TSVaadin;
import com.iontorrent.vaadin.utils.CoordSelect;
import com.iontorrent.vaadin.utils.WindowOpener;
import com.iontorrent.vaadin.utils.ZoomControl;
import com.iontorrent.wellmodel.BfHeatMap;
import com.iontorrent.wellmodel.ChipWellDensity;
import com.iontorrent.wellmodel.WellCoordinate;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.graphics.canvas.Canvas;
import com.vaadin.graphics.canvas.shape.Cross;
import com.vaadin.graphics.canvas.shape.Point;
import com.vaadin.graphics.canvas.shape.Polygon;
import com.vaadin.graphics.canvas.shape.UIElement;
import com.vaadin.terminal.StreamResource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Select;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

/**
 * 
 * @author Chantal Roth chantal.roth@lifetech.com
 */
public class WholeChipWindowCanvas extends WindowOpener implements Button.ClickListener,  Property.ValueChangeListener{

	private TSVaadin app;
	CoordSelect coordsel;
	ExperimentContext exp;
	int x;
	int y;
	int flow;
	int frame;
	RawType type= RawType.ACQ;;
	TextField tflow;
	TextField tframe;
	Select typeselect;
	Canvas canvas;
	WellCoordinate coord;
	WholeChipImage bfmask;
	  StreamResource imageresource;
	int bucket;
	ZoomControl zoom;
	
	public WholeChipWindowCanvas(TSVaadin app, Window main, String description, int x, int y) {
		super("Raw Heat Map", main, description, x, y, 1000, 800);
		this.app = app;
		frame = 0;
		flow = 0;
		bucket = 4;

	}

	@Override
	public void openButtonClick(Button.ClickEvent event) {
		if (app.getExperimentContext() == null) {
			mainwindow.showNotification("No Experiment Selected", "<br/>Please open an experiment first", Window.Notification.TYPE_WARNING_MESSAGE);
			return;
		}
		exp = app.getExperimentContext();
		// check if there are actually raw files...
		if (!exp.hasDat(type, flow)) {
			String file = type.getRawFileName(flow);
			mainwindow.showNotification("No .dat file", "<br/>I don't see the dat file "+file+"in "+exp.getRawDir(), Window.Notification.TYPE_WARNING_MESSAGE);
		//	return;
		} 
		
		super.openButtonClick(event);
	}

	@Override
	public void windowOpened(final Window mywindow) {
		//p("Creating chip image");
		exp = app.getExperimentContext();
		coord = exp.getWellContext().getCoordinate();
		if (coord == null) {
			p("Coord is null, creating a new coord" );
			coord = new WellCoordinate(100, 100);
		}
		x = coord.getX();
		y = coord.getY();
		
		HorizontalLayout h = new HorizontalLayout();
		mywindow.addComponent(h);
		this.addTypeSelection(h);
		addCoordAndFlowSelection(h);

		zoom = new ZoomControl(bucket, new Button.ClickListener(){
			@Override
			public void buttonClick(com.vaadin.ui.Button.ClickEvent event) {
				bucket = zoom.getBucket();
				reopen();			
			}			
		});
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
			export.setDescription("Open image in another browser window so that you can save it to file");	
			export.addListener(new Button.ClickListener(){
				@Override
				public void buttonClick(com.vaadin.ui.Button.ClickEvent event) {
					app.getMainWindow().open(imageresource, "_blank");				
				}			
			});
			h.addComponent(export);
			
		h.addComponent(new Label(app.getExperimentContext().getRawDir()));
		getSelectedType();
		if (type == null) type = RawType.ACQ;
		
		BfHeatMap mask = BfHeatMap.getMask(exp);
		 if (!mask.hasImage("chip", BfMaskFlag.RAW, flow, type, frame)) {
			 if (!computeImage(mask)) return;
		 }
		 
		
		bfmask = new WholeChipImage(exp, type, flow,frame, bucket);
		p("File "+mask.getImageFile("chip", BfMaskFlag.RAW, flow, type, frame)+" found, getting streamresource");
	
		mywindow.setHeight(100+bfmask.getImage().getHeight()+"px");
		mywindow.setWidth(100+bfmask.getImage().getWidth()+"px");
		imageresource = new StreamResource((StreamResource.StreamSource) bfmask, exp.getFileKey() + "_"+flow+"_"+frame+"_"+type+".png", app);
		imageresource.setCacheTime(10000);
		
		String relative = imageresource.getApplication().getRelativeLocation(imageresource);	
		String appurl = imageresource.getApplication().getURL().toString();
		String url = relative;
		url = appurl + url.replace("app://", "");
		p("URl from stream resource is: " + url);
	//	app.getMainWindow().open(imageresource, "_blank");
		addCanvas(mywindow, url);
		app.showMessage(this, "Drag the cursor to select a different well/area");
		
		canvas.addListener(new Canvas.CanvasMouseUpListener() {

			@Override
			public void mouseUp(Point p, UIElement child) {
				int newx = (int) p.getX();
				int newy = (int) p.getY();
				p("Got mouse UP on canvas: "+p+", child="+child);
				if (child != null && child instanceof Polygon) {
					x = newx;
					y = newy;
					Polygon po = (Polygon)child;
			//		p("Location of child po: "+po.getCenter());
					
					WellCoordinate coord = bfmask.getWellCoordinate(x, y);
				//	p("Got coord: " + coord + ", setting description");
					coordsel.setX(coord.getX() + exp.getColOffset());
					coordsel.setY(coord.getY() + exp.getRowOffset());
			//		po.setDescription("changed description :"+coord);
					buttonClick(null);
				}
			//	poly.moveTo(p);
			}
		});
	}
	public String getHelpMessage() {
		String msg = "<ul>";
        msg += "<li>drag the cursor and then <b>double click</b> to pick a well or region</li>";       
       msg += "<li>zoom in or out of the image </li>";
       msg += "<li>export the image (opens a new windows, then right click on the image and click save as) </li>";
       msg += "<li>enter a new x (column) or y (row) coordinat and hit refresh to also change the coordinate in other components </li>";
       msg += "</ul>";
       return msg;
	}
	private boolean computeImage(BfHeatMap mask) {
		app.showMessage("Whole Chip View", "I need to compute the heat map first");
		 ChipWellDensity gen = new ChipWellDensity(exp, flow, type, frame, bucket);
		 gen.setMask(mask);
		    String msg = null;
		    try {
		        p("Creating whole whip well density for flow " + flow + "  type " + type);
		        msg = gen.createHeatMapImages(null);
		        mask.updateInfo();
		        p("after generate whole chip image");
		    } catch (Exception e) {
		        msg = e.getMessage();
		        p("createImageFileFromScoreFlag: Got an error: " + ErrorHandler.getString(e));

		        return false;
		    }

		    if (msg != null && msg.length() > 0) {
		        app.showMessage("Something happened", msg);
		        return false;
		    }
		    if (!mask.hasImage("chip", BfMaskFlag.RAW, flow, type, frame)) {
				 app.showMessage("Whole Chip View", "I can't find the heat map");
				 return false;
			 }
		    return true;
	}

	private void addCanvas(final Window mywindow, String url) {
		canvas = new Canvas();
		canvas.setBackgroundColor("black");

		java.awt.Point point = bfmask.getPointFromWell(coord);
		Cross cross = new Cross((int)point.getX(), (int)point.getY(), 3, 5);
		canvas.drawUIElement(cross);

		String bg = url;
		canvas.setBackgroundImage(bg);
		mywindow.addComponent(canvas);
	}

	public void addCoordAndFlowSelection(HorizontalLayout h) {
        tflow = new TextField();
        tflow.setWidth("60px");
        // tflow.setHeight("25px");
        tflow.setImmediate(true);
        
        String s = "" + flow;
        tflow.setValue(s);
        h.addComponent(new Label("Flow:"));
        h.addComponent(tflow);

        tflow.addListener(new Property.ValueChangeListener() {

            public void valueChange(ValueChangeEvent event) {
                //addChart(chartTab);
                parseFlow();
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
                //addChart(chartTab);
                parseFrame();
                p("Got frame " + frame);
                reopen();
            }
        });

        coordsel = new CoordSelect(coord.getCol() + exp.getColOffset(), coord.getRow() + exp.getRowOffset(), this);
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
	            reopen();
	        }
	    }
	  private RawType getSelectedType() {
	        if (typeselect == null || typeselect.getValue() == null) {
	            type = RawType.ACQ;
	        } else {
	            type = (RawType) typeselect.getValue();
	        }
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
	public void buttonClick(Button.ClickEvent event) {
		 WellCoordinate coord = coordsel.getCoord();
	    exp.makeRelative(coord);
		app.setWellCoordinate(coord);
		app.reopenRaw();
	}

	private static void err(String msg, Exception ex) {
		Logger.getLogger(WholeChipWindowCanvas.class.getName()).log(Level.SEVERE, msg, ex);
	}

	private static void err(String msg) {
		Logger.getLogger(WholeChipWindowCanvas.class.getName()).log(Level.SEVERE, msg);
	}

	private static void warn(String msg) {
		Logger.getLogger(WholeChipWindowCanvas.class.getName()).log(Level.WARNING, msg);
	}

	private static void p(String msg) {
		//system.out.println("WholeChipWindowCanvas: " + msg);
		Logger.getLogger(WholeChipWindowCanvas.class.getName()).log(Level.INFO, msg);
	}
}
