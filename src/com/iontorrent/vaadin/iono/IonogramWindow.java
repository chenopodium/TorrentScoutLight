/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.iono;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.ionogram.MultiFlowLoader;
import com.iontorrent.ionogram.MultiFlowPanel;
import com.iontorrent.rawdataaccess.wells.WellData;
import com.iontorrent.utils.ProgressListener;
import com.iontorrent.utils.StringTools;
import com.iontorrent.vaadin.TSVaadin;
import com.iontorrent.vaadin.utils.WindowOpener;
import com.iontorrent.wellmodel.WellCoordinate;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.terminal.StreamResource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * 
 * @author Chantal Roth chantal.roth@lifetech.com
 */
public class IonogramWindow extends WindowOpener implements ProgressListener {

	private TSVaadin app;
	MultiFlowLoader loader;
	MultiFlowPanel multipanel;
	ArrayList<Integer> flows;
	private TextField tflow;
	private TextField tsub;
	private int subtract;
	private ExperimentContext exp;
	HorizontalLayout multilayout;
	ProgressIndicator indicator;
	IonogramImage ionoimage;
	HorizontalLayout h;
	int x;
	int y;
	StreamResource imageresource;
	StreamResource imageresource1;

	public IonogramWindow(TSVaadin app, Window main, String description, int x, int y) {
		super("Ionogram", main, description, x, y, 1200, 420);
		this.app = app;
		subtract = -1;
		flows = new ArrayList<Integer>();
		for (int f = 0; f < 4; f++) {
			flows.add(f);
		}
	}

	@Override
	public void openButtonClick(Button.ClickEvent event) {
		if (app.getExperimentContext() == null) {
			appwindow.showNotification("No Experiment Selected", "<br/>Please open an experiment first", Window.Notification.TYPE_WARNING_MESSAGE);
			return;
		}
		File f = new File(app.getExperimentContext().getWellsFile());
		if (!f.exists()) {
			appwindow.showNotification("1.wells not found", "<br/>Could not find the file " + f, Window.Notification.TYPE_WARNING_MESSAGE);
			return;
		}
		exp = app.getExperimentContext();
		app.openTable();
		super.openButtonClick(event);
	}

	@Override
	public void windowOpened(Window mywindow) {
		p("Creating IonogramWindow ");
		h = new HorizontalLayout();
		// mywindow.addComponent(new
		// Label(app.getExperimentContext().getResultsDirectory()));
		ionoimage = new IonogramImage(app.getExperimentContext());

		// Create a resource that uses the stream source and give it a name.
		// The constructor will automatically register the resource in
		// the application.
		WellCoordinate coord = app.getExperimentContext().getWellContext().getCoordinate();
		if (coord == null) {
			coord = new WellCoordinate(510, 510);
			app.getExperimentContext().getWellContext().setCoordinate(coord);
		}
		x = coord.getX();
		y = coord.getY();
		p("Getting streamresource for " + x + "/" + y);

		imageresource1 = new StreamResource(ionoimage, app.getExperimentContext().getResultsName() + "_ionogram" + x + "_" + y + ".png", app);
		imageresource1.setCacheTime(1000);
		Embedded em = new Embedded(null, imageresource1);

		TabSheet tab = new TabSheet();

		HorizontalLayout v = new HorizontalLayout();
		
		mywindow.setScrollable(true);
		
		Panel p = new Panel(null, new HorizontalLayout());  
		p.setWidth(mywindow.getWidth()-50+"px");
		p.setHeight("230px");
        p.setScrollable(true);
        em.setSizeUndefined();
       
        p.addComponent(em);        
		v.addComponent(p);
		tab.addTab(v);
		tab.getTab(v).setCaption("Ionogram");
		
		loader = new MultiFlowLoader(app.getExperimentContext());
	
		multilayout = new HorizontalLayout();
		tab.addTab(multilayout);
		tab.getTab(multilayout).setCaption("MultiFlow and Ionogram");

		VerticalLayout infolayout = new VerticalLayout();
		tab.addTab(infolayout);
		tab.getTab(infolayout).setCaption("Information");
		addInfo(infolayout);
		
		addFlowSelection(h);
		addSubtractSelection(h);
		mywindow.addComponent(h);
		mywindow.addComponent(tab);

		parseFlow();
		startMultiFlowUpdate();

	}
	private void addInfo(VerticalLayout v) {
		 WellData data = ionoimage.getData();
		 if (data == null) {
			 v.addComponent(new Label("Got no well data info"));
		 }
		 else {
			 ArrayList<String> keys = data.getInfoKeys();
			 if (keys == null) {
				 v.addComponent(new Label("Got no key/value info for this well"));
			 }
			 else {
				 for (String key: keys) {
					 v.addComponent(new Label(key+"="+data.getInfo(key)));
				 }
			 }
		 }
	}
	public String getHelpMessage() {
		 String msg = "<ul>";
	        msg += "<li>view the ionogram</li>";
	        msg += "<li>view the raw, nn subtracted traces of flows in the second tab</li>";
	        msg += "<li>enter the flow numbers (or range, such as 1-4) to view the raw data</li>";
	        msg += "<li>export the image to file (right click on image that opens in new window)</li>";
	        msg += "</ul>";
	        return msg;
	}
	public void addSubtractSelection(HorizontalLayout h) {
		tsub = new TextField();
		tsub.setWidth("60px");
		// tflow.setHeight("25px");
		tsub.setImmediate(true);
		tsub.setValue("" + subtract);

		h.addComponent(new Label("Subtract flow: "));
		h.addComponent(tsub);

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
				app.getMainWindow().open(imageresource1, "_blank");
			}
			
		});
		h.addComponent(export);
		tsub.addListener(new Property.ValueChangeListener() {

			private static final long serialVersionUID = 1L;

			public void valueChange(ValueChangeEvent event) {
				// addChart(chartTab);
				parseSub();
				// reopen();
			}
		});
	}

	private void parseSub() {
		String s = "" + tsub.getValue();
		try {
			subtract = Integer.parseInt(s);
		} catch (Exception e) {
			subtract = -1;
		}
		this.updateMultiFlow(true);

	}

	public void addFlowSelection(HorizontalLayout h) {
		tflow = new TextField();
		tflow.setWidth("60px");
		// tflow.setHeight("25px");
		tflow.setImmediate(true);
		String s = "";
		if (flows == null) {
			s = "0-4";
			tflow.setValue(s);
			flows = StringTools.parseInts(s);
		} else {
			s = "" + flows;
			tflow.setValue(s.substring(1, s.length() - 1));
		}

		h.addComponent(new Label("Flow(s) in multi flow:"));
		h.addComponent(tflow);

		tflow.addListener(new Property.ValueChangeListener() {

			public void valueChange(ValueChangeEvent event) {
				// addChart(chartTab);
				parseFlow();
				startMultiFlowUpdate();
				p("Got flows " + flows);

				// reopen();
			}
		});
	}

	private void startMultiFlowUpdate() {
		if (indicator != null) h.removeComponent(indicator);
		indicator = new ProgressIndicator(new Float(0.0));
		indicator.setHeight("40px");
		// indicator.setCaption("Creating whole Proton image");
		indicator.setDescription("I am reading the raw traces of the specified flows ");
		indicator.setPollingInterval(5000);
		h.addComponent(indicator);
		p("startMultiFlowUpdate with flows="+flows);
		loader.compute(flows, (ProgressListener) this);
	}

	private void updateMultiFlow(boolean finished) {
		int max = 8;
		int min = 5000;
		for (int f: flows) {
			if (f > max) max = f;
			if (f < min) min = f;
		}
		min = Math.max(0, min-1);
		max = Math.max(max+2, min+8);
		multipanel = new MultiFlowPanel(exp, loader);
		multipanel.setMaxFlow(max);
		multipanel.setMinFlow(min);
		multipanel.setSubtract(subtract);
		//WellData welldata = this.exp.getWellContext().getWellData(this.c);
		//Ionogram iono = new Ionogram(welldata, this.exp.getWellContext());
		boolean raw = false;
		multipanel.setWellContext(exp.getWellContext(), raw, !raw);

		
		MultiFlowImage imagesource = new MultiFlowImage(app.getExperimentContext(), multipanel, min, max);
		imageresource = new StreamResource(imagesource, app.getExperimentContext().getFileKey()+ "_multi_"+min+"_"+max+"_" + x + "_" + y + "_" + (int) (Math.random() * 1000) + ".png", app);
		imageresource.setCacheTime(50000);
		Embedded em = new Embedded(null, imageresource);

		multilayout.removeAllComponents();
		
		
		Panel p = new Panel(null, new HorizontalLayout());  
		p.setWidth(mywindow.getWidth()-50+"px");
		p.setHeight("230px");
        p.setScrollable(true);
        em.setSizeUndefined();
       
        p.addComponent(em);        
        multilayout.addComponent(p);
		
		p("multilayout updated, maxflows="+max);

		if (finished) {
			String relative = imageresource.getApplication().getRelativeLocation(imageresource);
			String appurl = imageresource.getApplication().getURL().toString();
			String url = relative;
			url = appurl + url.replace("app://", "");
		//	p("URl from stream resource is: " + url);
		//	app.getMainWindow().open(imageresource, "_blank");
			h.removeComponent(indicator);
		}
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

	private static void err(String msg, Exception ex) {
		Logger.getLogger(IonogramWindow.class.getName()).log(Level.SEVERE, msg, ex);
	}

	private static void err(String msg) {
		Logger.getLogger(IonogramWindow.class.getName()).log(Level.SEVERE, msg);
	}

	private static void warn(String msg) {
		Logger.getLogger(IonogramWindow.class.getName()).log(Level.WARNING, msg);
	}

	private static void p(String msg) {
		//system.out.println("IonogramWindow: " + msg);
		Logger.getLogger(IonogramWindow.class.getName()).log(Level.INFO, msg);
	}

	@Override
	public void setMessage(String arg0) {

	}

	@Override
	public void setProgressValue(int prog) {
		p("Got progress value: " + prog + ", calling updateMultiFlow");
		if (indicator != null) indicator.setValue(((double) prog / 100.0d));
		updateMultiFlow(prog>99);

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}
}
