/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.composite;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.iontorrent.expmodel.CompositeExperiment;
import com.iontorrent.expmodel.DatBlock;
import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.rawdataaccess.pgmacquisition.RawType;
import com.iontorrent.rawdataaccess.wells.BfMaskFlag;
import com.iontorrent.threads.Task;
import com.iontorrent.threads.TaskListener;
import com.iontorrent.utils.ErrorHandler;
import com.iontorrent.utils.ProgressListener;
import com.iontorrent.vaadin.TSVaadin;
import com.iontorrent.vaadin.utils.WindowOpener;
import com.iontorrent.vaadin.utils.ZoomControl;
import com.iontorrent.wellmodel.BfHeatMap;
import com.iontorrent.wellmodel.CompositeWellDensity;
import com.iontorrent.wellmodel.WellCoordinate;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.event.MouseEvents.ClickEvent;
import com.vaadin.event.MouseEvents.ClickListener;
import com.vaadin.terminal.StreamResource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.terminal.UserError;
import com.vaadin.ui.Button;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.Select;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

/**
 * 
 * @author Chantal Roth chantal.roth@lifetech.com
 */
public class CompWindow extends WindowOpener implements ProgressListener, Property.ValueChangeListener, TaskListener {

	private TSVaadin app;
	CompositeExperiment comp;
	BfMaskFlag flag;
	int flow = 0;
	RawType type = RawType.ACQ;
	int frame;
	ProgressIndicator indicator;
	CompositeImage bfmask;
	BfHeatMap mask;
	Select sel;
	int bucket;
	ZoomControl zoom;
	StreamResource imageresource ;
	WorkThread t;
	public CompWindow(TSVaadin app, Window main, String description, int x, int y) {
		super("Pick a Block (Proton)", main, description, x, y, 800, 800);
		this.app = app;
		this.frame = 0;
		if (flag == null) flag = BfMaskFlag.RAW;
		bucket = 2;
	}

	@Override
	public void openButtonClick(Button.ClickEvent event) {
		if (app.getCompositeExperiment() == null) {
			mainwindow.showNotification("No Proton Experiment Selected", "<br/>Please open a proton experiment first" + "<br/>You can either browse the db or enter the paths manually", Window.Notification.TYPE_WARNING_MESSAGE);
			return;
		}
		super.openButtonClick(event);
	}

	public void selectBlock(DatBlock block) {
		if (block != null) {
			ExperimentContext exp = comp.getContext(block, false);
			// exp.getWellContext().setCoordinate(coord);
			// lblock.setValue("Seleced block: " + block.toShortString());
			app.setExperimentContext(exp);
			app.showExperiment();
		}
	}

	@Override
	public void windowOpened(Window mywindow) {
		p("Creating CompWindow ");
		comp = app.getCompositeExperiment();
		
		if (flag == null) flag = BfMaskFlag.RAW;
		
		// HorizontalLayout = new HorizontalLayout();

		HorizontalLayout h = new HorizontalLayout();
		mywindow.addComponent(h);

		sel = new Select();
		sel.addItem("Entire experiment");
		for (DatBlock b : comp.getBlocks()) {
			sel.addItem(b);
			sel.setItemCaption(b, b.toShortString());
		}
		h.addComponent(sel);
		
		sel.setImmediate(true);
		sel.addListener(this);
		
		Select flagsel = addFlagSelect(h);

		flagsel.setImmediate(true);
		flagsel.addListener(this);
		
	//	h.addComponent(new Label("Frame:"));
		final TextField txtFrame = new TextField();
		txtFrame.setValue(""+frame);
		txtFrame.setImmediate(true);
		//h.addComponent(txtFrame);
		txtFrame.addListener(new TextField.ValueChangeListener() {

			@Override
			public void valueChange(ValueChangeEvent event) {
				if (txtFrame.getValue() == null) return;
				frame = Integer.parseInt(""+txtFrame.getValue());
				reopen();
			}
			
		});
		
		mask = BfHeatMap.getMask(comp.getRootContext());
		
		boolean has = mask.hasImage("composite", flag, flow, type, frame);
		if (!has) {
			// start thread
			p("Need to compute image first, starting thread");
			indicator = new ProgressIndicator(new Float(0.0));
			indicator.setHeight("40px");
			//indicator.setCaption("Creating whole Proton image");
			indicator.setDescription("I am reading the beginning of all raw files of all Proton blocks and I am computing a composite heat map from that");
			indicator.setPollingInterval(5000);
			h.addComponent(indicator);
			t = new WorkThread(this);
			t.execute();

			app.showMessage(this, "I need to compute the heat map first<br>(this will take a few minutes)<br>But you can still pick a block!");
			addCompView(mywindow);

		} else {		
			addCompView(mywindow);
		}
		
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
			
		h.addComponent(new Label(" "+comp.getRootContext().getRawDir()));
	}
	public String getHelpMessage() {
		String msg = "<ul>";
        msg += "<li>Pick a bf flag in the drop down box to view a different heat map</li>";
        msg += "<li><b>Double click</b> to pick a block (to view results)</li>";       
       msg += "<li>zoom in or out of the image </li>";
       msg += "<li>export the image (opens a new windows, then right click on the image and click save as) </li>";
       msg += "<li>Pick a block in the drop down box </li>";
       msg += "<li>Note: if you change the frame in the raw view, it will <b>recompute the heat map</b> which can take <b>several minutes</b> </li>";
       msg += "</ul>";
       return msg;
	}
	private Select addFlagSelect(HorizontalLayout h) {
		final Select flagsel = new Select();
		for (BfMaskFlag f : BfMaskFlag.values()) {
			flagsel.addItem(f);
			flagsel.setItemCaption(f, f.getName());
		}
		flagsel.select(flag);		
		flagsel.addListener(new Select.ValueChangeListener() {

			@Override
			public void valueChange(ValueChangeEvent event) {
				flag = (BfMaskFlag) flagsel.getValue();
				reopen();
			}

		});

		h.addComponent(new Label(" Flag: "));
		h.addComponent(flagsel);
		return flagsel;
	}

	private void addCompView(Window mywindow) {
		String file = mask.getImageFile("composite", flag, flow, type, frame);
		app.showMessage(this,"Loading image "+file);
		mask.readData(flag, file, true);
		bfmask = new CompositeImage(comp, flag, null, flow, frame, bucket);
		
		File f = new File(file);
		imageresource = new StreamResource(bfmask, comp.getRootContext().getRawDir() + "_" +f.getName()+"_comp.png", app);
		imageresource.setCacheTime(10000);
		mywindow.setHeight(bfmask.getImage().getHeight()+110+"px");
		mywindow.setWidth(100+bfmask.getImage().getWidth()+"px");
		//
		final Embedded em = new Embedded(null, imageresource);
		// progress.setValue("Whole chip view completed");

		Panel pan = new Panel();
		pan.setSizeFull();
		pan.setScrollable(true);
		HorizontalLayout hpanel = new HorizontalLayout();
		hpanel.addComponent(em);
		pan.setContent(hpanel);
		mywindow.addComponent(pan);

		em.addListener(new ClickListener() {

			public void click(ClickEvent event) {
				int x = event.getRelativeX();
				int y = event.getRelativeY();
				// x = x-event.getComponent().getWindow().getPositionX();
				// y = y-event.getComponent().getWindow().getPositionY();
				p("image clicked at: " + x + "/" + y);

				WellCoordinate coord = bfmask.getWellCoordinate(x, y);
				p("Got coord: " + coord);
				DatBlock block = comp.findBlock(coord);
				if (block != null) {
					ExperimentContext exp = comp.getContext(block, false);
					WellCoordinate rel = new WellCoordinate(coord);
					exp.makeRelative(coord);
					exp.getWellContext().setCoordinate(rel);
					app.setExperimentContext(exp);
					app.setWellCoordinate(rel);
					sel.select(block);
					// lblock.setValue("Selected block: " +
					// block.toShortString());
					// app.reopenRaw();

				} else {
					p("Got no block for " + coord);
					em.setComponentError(new UserError("Found no valid block for " + coord));
				}
				// mainwindow.showNotification("Select from the drop down",
				// "<br/>Selecting coordinate via image does not work properly yet, please use the drop down",
				// Window.Notification.TYPE_HUMANIZED_MESSAGE);

			}
		});
	}

	private boolean createImageFileFromScoreFlag(ProgressListener progress) {
		p("Creating image file for flag " + flag);
		BfHeatMap mask = BfHeatMap.getMask(comp.getRootContext());

		CompositeWellDensity gen = new CompositeWellDensity(comp, type, flow, frame, bucket);
		String msg = null;
		String file = mask.getImageFile("composite", flag, flow, type, frame);
		p("About to compute image "+file);
		try {			
			msg = gen.createCompositeImages(progress, file, flag);
			p("Image "+ file+ " computed");
			
		} catch (Throwable e) {
			err("Could not compute image: "+ErrorHandler.getString(e));
			msg = e.getMessage();
			app.showMessage(this, msg);
			return false;
		}
		if (msg != null && msg.length() > 0) {
			app.showMessage(this, msg);
			return false;
		}

		return true;
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
				boolean ok = createImageFileFromScoreFlag(CompWindow.this);
				indicator.setValue(new Float(1.0));
				
				has = mask.hasImage("composite", flag, flow, type, frame);
			}
			catch (Exception e) {
				err("Got an error when computing the heat map: "+ErrorHandler.getString(e));
			}
			return null;

		}

	}

	public void valueChange(Property.ValueChangeEvent event) {
		// The event.getProperty() returns the Item ID (IID)
		// of the currently selected item in the component.
		Property id = event.getProperty();
		if (id.getValue() instanceof DatBlock) {
			DatBlock b = (DatBlock) id.getValue();
			selectBlock(b);
		} else {
			String val = "" + id.getValue();
			 if (val.toLowerCase().equalsIgnoreCase("thumbnails")) {
//				 ExperimentContext exp = comp.getThumbnailsContext();
//				 // exp.getWellContext().setCoordinate(coord);
//				 app.setExperimentContext(exp);
//				// lblock.setValue("Seleced block: " + val);
//				 app.showExperiment();
			 }
			 else  { // root context
				 ExperimentContext exp = comp.getRootContext();
				 // exp.getWellContext().setCoordinate(coord);
				 app.setExperimentContext(exp);
				 //lblock.setValue("Seleced block: " + val);
				 app.showExperiment();
			 }
		}
	}

	private static void err(String msg, Exception ex) {
		Logger.getLogger(CompWindow.class.getName()).log(Level.SEVERE, msg, ex);
	}

	private static void err(String msg) {
		Logger.getLogger(CompWindow.class.getName()).log(Level.SEVERE, msg);
	}

	private static void warn(String msg) {
		Logger.getLogger(CompWindow.class.getName()).log(Level.WARNING, msg);
	}

	private static void p(String msg) {
		//system.out.println("CompWindow: " + msg);
		Logger.getLogger(CompWindow.class.getName()).log(Level.INFO, msg);
	}

	public void setProgressValue(int p) {
		if (indicator != null) indicator.setValue(((double) p / 100.0d));
		// progress.setValue("Creating composite image: " + p + "%");
	}

	public void setMessage(String msg) {
		indicator.setDescription(msg);
	}

	public void stop() {}

	public void close() {
		super.close();
		if (t != null && !t.isCancelled()) {
			t.cancel(true);
			t = null;
		}		
	}
	@Override
	public void taskDone(Task task) {
		boolean has = task.isSuccess();
		if (!has) {
			String file = mask.getImageFile("composite", flag, flow, type, frame);
			
			//mask.readData(flag, file, true);
			app.showError(CompWindow.this, "Something went wrong when computing the heat map. <br>I still don't seem to have the file:<br>" + file);
		} else
			reopen();
	}
}
