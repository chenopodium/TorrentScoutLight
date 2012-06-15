/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.table;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.heatmaps.ScoreMaskGenerator;
import com.iontorrent.rawdataaccess.wells.BfMaskDataPoint;
import com.iontorrent.rawdataaccess.wells.BfMaskFlag;
import com.iontorrent.rawdataaccess.wells.BitMask;
import com.iontorrent.rawdataaccess.wells.ScoreMaskFlag;
import com.iontorrent.results.scores.ScoreMask;
import com.iontorrent.threads.Task;
import com.iontorrent.threads.TaskListener;
import com.iontorrent.torrentscout.explorer.ExplorerContext;
import com.iontorrent.utils.ErrorHandler;
import com.iontorrent.utils.ProgressListener;
import com.iontorrent.vaadin.TSVaadin;
import com.iontorrent.vaadin.mask.MaskSelect;
import com.iontorrent.vaadin.utils.CoordSelect;
import com.iontorrent.vaadin.utils.DataUtils;
import com.iontorrent.vaadin.utils.WindowOpener;
import com.iontorrent.wellmodel.WellContext;
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
import com.vaadin.ui.Table;
import com.vaadin.ui.Window;

/**
 *
 * @author Chantal Roth 
 * chantal.roth@lifetech.com
 */
public class TableWindow extends WindowOpener implements Button.ClickListener, TaskListener, ProgressListener {

    private TSVaadin app;
    Table table;
    HorizontalLayout hor;
    int curx;
    int cury;
    CoordSelect coordsel;
    ExperimentContext exp;
    ProgressIndicator indicator;
    BitMask showmask;
    MaskSelect usemask;
    WorkThread t;
    private BfMaskFlag bfflags[] = {  BfMaskFlag.BEAD,
            BfMaskFlag.LIVE,
            BfMaskFlag.LIBRARY,
            BfMaskFlag.KEYPASS,
            BfMaskFlag.DUD};
    
    private ScoreMaskFlag scoreflags[] = {  ScoreMaskFlag.Q17LEN,ScoreMaskFlag.Q47LEN,
    		ScoreMaskFlag.INDEL,
    		ScoreMaskFlag.MATCH};
    
    
    public TableWindow(TSVaadin app, Window main, String description, int x, int y) {
        super("Well Table", main, description, x, y, 800, 550);
        this.app = app;
       
        //super.openbutton.setEnabled(false);
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
        if (exp.getWellContext().getMask() == null) {
            appwindow.showNotification("Found no bfmask.bin",
                    "<br/>Could not find " + app.getExperimentContext().getBfMaskFile(),
                    Window.Notification.TYPE_WARNING_MESSAGE);
            return;
        }
       
        super.openButtonClick(event);
    }

    private int rand() {
        return (int) (Math.random() * 100);
    }

    private double randd() {
        return (Math.random() * 100);
    }

    private boolean randb() {
        return Math.random() > 0.5;
    }

    @Override
    public void windowOpened(Window mywindow) {
        p("Creating table");
        String w = "700px";
        String h = "400px";
        boolean sortasc = false;
        Object sortids=null;
        if (table != null) {
            w = table.getWidth()+"px";
            h = table.getWidth()+"px";
            sortasc = table.isSortAscending();
            sortids=table.getSortContainerPropertyId();
        }
      
        table = new Table();
        
        table.setSortAscending(sortasc);
        exp = app.getExperimentContext();
        table.setWidth(w);

        table.addContainerProperty("x", Integer.class, null);
        table.addContainerProperty("y", Integer.class, null);
        //table.addContainerProperty("Bead", Boolean.class, null);
        
        for (BfMaskFlag flag: bfflags) {
        	table.addContainerProperty(flag.getName(), Boolean.class, null);
        }
        for (ScoreMaskFlag flag: scoreflags) {
        	table.addContainerProperty(flag.getName(), Integer.class, null);
        }
        table.addStyleName("welltable");
        WellContext cont = app.getExperimentContext().getWellContext();
        WellCoordinate coord = cont.getCoordinate();
        if (coord == null) {
            coord = new WellCoordinate(500, 500);
        }
        int mx = coord.getX();
        int my = coord.getY();
        
        WellSelection sel = cont.getSelection();
        // fix: if too many, still use data from well selection, just not all results!
        if (sel != null && sel.getAllWells() != null && sel.getAllWells().size()>0) {
        	// use the selection insead
        	int row = 0;
        	p("using well selection");
        	for (WellCoordinate well: sel.getAllWells()) {        		
        		row = addRow(cont, row,well);
        		if (row > 5000) break;
        	}
        }
        else {
        	 int span = 5;             
             
             int x0 = Math.max(0, mx - span);
             int y0 = Math.max(0, my - span);
             int row = 0;
	        for (int x = x0; x < x0 + 2 * span; x++) {
	            for (int y = y0; y < y0 + 2 * span; y++) {
	                row = addRow(cont, row, new WellCoordinate(x, y));
	            }
	        }
        }
        // Allow selecting items from the table.
        table.setSelectable(true);

// Send changes in selection immediately to server.
        table.setImmediate(true);

        if (sortids != null) {
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
                int x = ((Integer) obj.getItemProperty("x").getValue()).intValue();
                int y = ((Integer) obj.getItemProperty("y").getValue()).intValue();
                WellCoordinate coord = new WellCoordinate(x, y);
                exp.makeRelative(coord);
                app.setWellCoordinate(coord, false);
            }
        });

        // h.addComponent(current);
        coordsel = new CoordSelect(mx + exp.getColOffset(), my + exp.getRowOffset(), this);
        hor = new HorizontalLayout();
        mywindow.addComponent(hor);
        coordsel.addGuiElements(hor);
        
        if (app.getExplorerContext() != null && app.getExplorerContext().getMasks() != null) {
        	ExplorerContext maincont = app.getExplorerContext();
        	usemask = new MaskSelect("table_mask", " ", "Pick a mask to view", maincont, MaskSelect.NONE, new Property.ValueChangeListener() {

			@Override
			public void valueChange(ValueChangeEvent event) {
				
				showmask = usemask.getSelection();
				if (showmask == null) return;
				ArrayList<WellCoordinate> coords = showmask.getAllCoordsWithData(5000);
				if (coords != null && coords.size()>0) {
					WellSelection sel = new WellSelection(coords);
					sel.setTitle("Wells of mask "+showmask.getName());
					exp.getWellContext().setSelection(sel);
					app.setWellSelection(sel);					
				}

			}
		}, this.showmask);
        	usemask.addGuiElements(hor);
        }
        Button btnscores = new Button("Load Scores");
        btnscores.setDescription("Computing the scores could take a few minutes");
        hor.addComponent(btnscores);
        btnscores.setImmediate(true);
        btnscores.addListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				// TODO Auto-generated method stub
				 startToLoadScores();
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
			export.setDescription("Open image in another browser window so that you can save it to file");
			export.addListener(new Button.ClickListener() {
				@Override
				public void buttonClick(ClickEvent event) {
					doExportAction();
				}

			});
			hor.addComponent(export);
        
		String msg = "No selection";
		
		if (sel != null) {
			if (sel.getTitle() != null) msg = sel.getTitle();
			else msg = "Wells in "+sel.toString();
		}
		hor.addComponent(new Label(msg));
        mywindow.addComponent(table);

    }
    private void doExportAction() {
    	// copy data to excel
    	if (table == null || table.getItemIds() == null) {
    		app.showMessage("No data", "Found no data in table to export");
    		return;
    	}
    	app.logModule(getName(), "export table");
    	DataUtils.export(table, mywindow);
    	    	
    }
    public String getHelpMessage() {
        String msg = "<ul>";
        msg += "<li>Sort the data by clicking on the headers</li>";
        msg += "<li>Click on a row to view data <br>(open a viewer component such as the ionogram or alignment view)</li>";
        msg += "<li>Export the data to excel in the data tab</li>";
        if (this.exp != null) {
        	msg += "<li>Current result name:"+exp.getResultsName()+"</li>";
        	msg += "<li>Current results dir:"+exp.getResultsDirectory()+"</li>";
        	msg += "<li>Current raw dir:"+exp.getRawDir()+"</li>";
        	msg += "<li>Current chip type:"+exp.getChipType()+"</li>";
        	
        }
        msg += "</ul>";
        return msg;
 }
    private void startToLoadScores() {
    	p("Need to compute image first, starting thread");
		indicator = new ProgressIndicator(new Float(0.0));
		indicator.setHeight("40px");
		//indicator.setCaption("Creating whole Proton image");
		indicator.setDescription("I am loading scores from the BAM and .sff files...");
		indicator.setPollingInterval(5000);
		hor.addComponent(indicator);
		app.showMessage("Loading scores", "Loading additional scores...");
		t = new WorkThread(this);
		t.execute();
    }
	@Override
	public void taskDone(Task task) {
		if (indicator != null) {
			hor.removeComponent(indicator);
		}
		reopen();
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
 				ScoreMask mask = ScoreMask.getMask(exp, exp.getWellContext());
				 boolean done = true;
				 if (!mask.hasAllBamImages()) {
					 app.showMessage("Bam File", "I will have to process the bam file<br>This might take a few minutes");
					 done = false;
				 }
				 if (!mask.hasAllSffImages()) {
					 done = false;
					 app.showMessage("Sff File", "I will have to process the sff file<br>This might take a few minutes");
				 }
				 if (!done) {
					 ScoreMaskGenerator gen = new ScoreMaskGenerator(mask, exp);				
					 gen.generateAllMissingHeatMaps();
				 }
				 app.showMessage("Loading...", "Reading all heat maps...");
				
		         mask.readAllData();
		         // in thread!
		        
 				indicator.setValue(new Float(1.0));
 				 				
 			}
 			catch (Exception e) {
 				err("Got an error when computing the heat map: "+ErrorHandler.getString(e));
 			}
 			return null;

 		}

 	}
    public void setProgressValue(int p) {
		if (indicator != null) indicator.setValue(((double) p / 100.0d));
		// progress.setValue("Creating composite image: " + p + "%");
	}

	private int addRow(WellContext cont, int row,WellCoordinate well) {
		int x = well.getCol();
		int y = well.getY();
		
		cont.loadMaskData(well);
		
		BfMaskDataPoint bf = cont.getMask().getDataPointAt(x, y);		
				
		if (bf == null) {
		    //err("Got no bf for " + x + "/" + y);
		} else {		  
			Object[] rowdata = new Object[bfflags.length+scoreflags.length+2];
			rowdata[0] = new Integer(x + exp.getColOffset());
			rowdata[1] = new Integer(y + exp.getRowOffset());
			for (int i = 0; i < bfflags.length; i++) {
				rowdata[i+2] = bf.hasFlag(bfflags[i]);
			}
			int offset = 2 + bfflags.length;
			for (int i = 0; i < scoreflags.length; i++) {
				ScoreMaskFlag flag = scoreflags[i];
				rowdata[i+offset] = (int)(well.getScoredata(flag)/flag.multiplier());
			}
		    table.addItem(rowdata, new Integer(row));		   
		}
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
        Logger.getLogger(TableWindow.class.getName()).log(Level.SEVERE, msg+ErrorHandler.getString(ex));
    }

    private static void err(String msg) {
        Logger.getLogger(TableWindow.class.getName()).log(Level.SEVERE, msg);
    }

    private static void warn(String msg) {
        Logger.getLogger(TableWindow.class.getName()).log(Level.WARNING, msg);
    }

    private static void p(String msg) {
        //system.out.println("TableWindow: " + msg);
        Logger.getLogger(TableWindow.class.getName()).log(Level.INFO, msg);
    }

	@Override
	public void setMessage(String msg) {
		indicator.setDescription(msg);		
	}

	@Override
	public void stop() {
		
		
	}
}
