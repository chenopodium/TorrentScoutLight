/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.scoremask;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;


import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.guiutils.heatmap.GradientPanel;
import com.iontorrent.heatmaps.PerfectReadCalculator;
import com.iontorrent.heatmaps.PrimerSMCalculator;
import com.iontorrent.heatmaps.ScoreMaskCalculatorIF;
import com.iontorrent.heatmaps.ScoreMaskGenerator;
import com.iontorrent.heatmaps.SpecificDeletionSMCalculator;
import com.iontorrent.rawdataaccess.wells.BitMask;
import com.iontorrent.rawdataaccess.wells.ScoreMaskFlag;
import com.iontorrent.results.scores.ScoreMask;
import com.iontorrent.sequenceloading.SequenceLoader;
import com.iontorrent.threads.Task;
import com.iontorrent.threads.TaskListener;
import com.iontorrent.torrentscout.explorer.ExplorerContext;
import com.iontorrent.utils.ErrorHandler;
import com.iontorrent.utils.ProgressListener;
import com.iontorrent.utils.io.FileUtils;
import com.iontorrent.vaadin.TSVaadin;
import com.iontorrent.vaadin.utils.CoordSelect;
import com.iontorrent.vaadin.utils.FileBrowserWindow;
import com.iontorrent.vaadin.utils.FlowRangeDialog;
import com.iontorrent.vaadin.utils.GradientLegend;
import com.iontorrent.vaadin.utils.InputDialog;
import com.iontorrent.vaadin.utils.OptionsDialog;
import com.iontorrent.vaadin.utils.RangeDialog;
import com.iontorrent.vaadin.utils.WindowOpener;
import com.iontorrent.vaadin.utils.ZoomControl;
import com.iontorrent.wellmodel.WellCoordinate;
import com.iontorrent.wellmodel.WellSelection;
import com.vaadin.data.Property;
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
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.Select;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;

/**
 * 
 * @author Chantal Roth chantal.roth@lifetech.com
 */
public class ScoreMaskWindowCanvas extends WindowOpener implements Button.ClickListener, Property.ValueChangeListener, TaskListener, ProgressListener {

	private TSVaadin app;
	CoordSelect coordsel;
	ExperimentContext exp;
	ProgressIndicator indicator;
	int x;
	int y;
	ScoreMaskFlag flag;
	Canvas canvas;
	ScoreMaskImage bfmask;
	ExperimentContext oldexp;
	ZoomControl zoom;
	int bucket;
	HorizontalLayout h;
	   int gradmin;
		int gradmax;
	StreamResource imageresource;
	ScoreMask mask;
	BitMask bitmask;
	String maskname;
	long total;

	ScoreMaskFlag ALIGN_FLAG;
	ScoreMaskFlag SCORE_FLAG;
	ScoreMaskFlag PERFECT_FLAG;
	// ComputeHeatMapTask task;
	// ScoreMaskFlag flags[];
	ComputeHeatMapTask customtask;
	public ScoreMaskWindowCanvas(TSVaadin app, Window main, String description, int x, int y) {
		super("Find reads by scores and alignments", main, description, x, y, 800, 800);
		this.app = app;
		bucket = 5;
		ALIGN_FLAG = ScoreMaskFlag.CUSTOM1;
		SCORE_FLAG=  ScoreMaskFlag.CUSTOM2;
		PERFECT_FLAG=  ScoreMaskFlag.CUSTOM3;
		ALIGN_FLAG.setName("Alignment searches");
		ALIGN_FLAG.setDescription("The result of (previous) alignment or sequence searches");
		SCORE_FLAG.setName("Score filtering");
		SCORE_FLAG.setDescription("The result of (previous) score filtering");
		PERFECT_FLAG.setName("Perfect matches");
		PERFECT_FLAG.setDescription("The result of searches for stretches of perfect matches in reads");
		// flags = ScoreMaskFlag.values();
	}

	@Override
	public void openButtonClick(Button.ClickEvent event) {
		exp = app.getExperimentContext();
		if (exp == null) {
			mainwindow.showNotification("No Experiment Selected", "<br/>Please open an experiment first", Window.Notification.TYPE_WARNING_MESSAGE);
			return;
		}
		if (exp.getWellContext().getMask() == null) {
			mainwindow.showNotification("Found no bfmask.bin", "<br/>Could not find " + app.getExperimentContext().getResultsDirectory() + "bfmask.bin", Window.Notification.TYPE_WARNING_MESSAGE);
			return;
		}
		String bam = exp.getResultsDirectory() + exp.getBamFileName();
		if (bam == null || !FileUtils.exists(bam)) {
			mainwindow.showNotification("Found no .bam file " + bam + "<br/>Could not find bam file, please pick one", Window.Notification.TYPE_WARNING_MESSAGE);

		}
		String sff = exp.getResultsDirectory() + exp.getSffFileName();
		if (sff == null || !FileUtils.exists(sff)) {
			mainwindow.showNotification("Found no .sff file " + sff + "<br/>Could not find SFF file, please pick one", Window.Notification.TYPE_WARNING_MESSAGE);

		}
		super.openButtonClick(event);
	}

	@Override
	public void windowOpened(final Window mywindow) {
		p("Creating bfmask image");
		if (flag == null) flag = ScoreMaskFlag.INDEL;
		exp = app.getExperimentContext();
		if (oldexp == null || exp != oldexp) {
			if (exp.is318()) bucket = 12;
			else if (exp.is316()) bucket = 10;
		}
		oldexp = exp;
		WellCoordinate coord = exp.getWellContext().getCoordinate();
		if (coord == null) {
			p("Coord is null, creating a new coord");
			coord = new WellCoordinate(100, 100);
		}
		x = coord.getX();
		y = coord.getY();
		h = new HorizontalLayout();

		Button filter = new Button("Search...");
		h.addComponent(filter);
		filter.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(com.vaadin.ui.Button.ClickEvent event) {
				startFilterAction();
			}
		});

		Select sel = new Select();
		for (ScoreMaskFlag f : ScoreMaskFlag.values()) {
			sel.addItem(f);
			sel.setItemCaption(f, f.getName());
		}
		sel.select(flag);
		sel.setDescription(flag.getDescription());
		h.addComponent(new Label(" Flag: "));
		h.addComponent(sel);

		final Button open = new Button();
		open.setIcon(new ThemeResource("img/document-open-2.png"));
		open.setDescription("Load previous search result");
		open.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				openFile(true);
			}

		});
		h.addComponent(open);
		final Button export = new Button();
		export.setIcon(new ThemeResource("img/export.png"));
		export.setDescription("Open image in another browser window so that you can save it to file");
		export.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(com.vaadin.ui.Button.ClickEvent event) {
				OptionsDialog input = new OptionsDialog(mainwindow, "What would you like to export?", "Export...", 
						"... this image", 
						"... save the search result", 
						new OptionsDialog.Recipient() {

					@Override
					public void gotInput(final int selection) {
						if (selection < 0) return;
						// / do the search
						if (selection == 0) {
							app.getMainWindow().open(imageresource, "_blank");
							return;
						}
						saveFile(true);
						
					}

				});

				
			}
		});
		h.addComponent(export);
		
		Button bmask = new Button();
		bmask.setDescription("Create a mask that can be used in the Process/Automate component");
		bmask.setIcon(new ThemeResource("img/mask.png"));
		h.addComponent(bmask);
		bmask.addListener(new Button.ClickListener() {
			public void buttonClick(Button.ClickEvent event) {
				createMask();
			}
		});

		zoom = new ZoomControl(bucket, new Button.ClickListener() {
			@Override
			public void buttonClick(com.vaadin.ui.Button.ClickEvent event) {
				bucket = zoom.getBucket();
				bfmask= null;
				reopen();
			}
		});
		zoom.addGuiElements(h);

		coordsel = new CoordSelect(x + exp.getColOffset(), y + exp.getRowOffset(), this);
		coordsel.addGuiElements(h);

		Button help = new Button();
		help.setDescription("Click me to get information on this window");
		help.setIcon(new ThemeResource("img/help-hint.png"));
		h.addComponent(help);
		help.addListener(new Button.ClickListener() {
			public void buttonClick(Button.ClickEvent event) {
				 app.showHelpMessage("Help", getHelpMessage());
			}
		});

		

		// h.addComponent(new Label(exp.getResultsDirectory()));

		sel.setImmediate(true);
		sel.addListener(this);

		mywindow.addComponent(h);

		mask = app.getScoreMask();
		mask.readData(flag);

		if (!mask.hasImage(flag)) {
			p("Don't have image for flag " + flag + " yet: "+mask.getImageFile(flag));

			ScoreMaskGenerator gen = new ScoreMaskGenerator(mask, exp);
			String msg = gen.generateImageFiles(flag);
			if (msg != null && msg.length() > 0) {
				app.showMessage("Score Mask", msg);
				return;
			}
		}
		if (!mask.hasImage(flag)) {
			app.showMessage("Score Mask", "I don't have the heat map for  " + flag);
			return;
		}

		if (bfmask == null) bfmask = new ScoreMaskImage(exp, flag, bucket);
		this.setWidth(bfmask.getImage().getWidth() + 50 + "px");
		this.setHeight(bfmask.getImage().getHeight() + 50 + "px");
		p("Getting streamresource");
		imageresource = new StreamResource((StreamResource.StreamSource) bfmask, exp.getFileKey() + flag.getName() + "_score.png", app);
		imageresource.setCacheTime(60000);
		// imageresource.

		String relative = imageresource.getApplication().getRelativeLocation(imageresource);

		String appurl = imageresource.getApplication().getURL().toString();
		String url = relative;
		url = appurl + url.replace("app://", "");

		relative = relative.substring(12);

		// if (canvas == null) {
		canvas = new Canvas();
		canvas.setHeight(bfmask.getImage().getHeight()+"px");
		canvas.setBackgroundColor("black");

		java.awt.Point point = bfmask.getPointFromWell(coord);
		Cross cross = new Cross((int) point.getX(), (int) point.getY(), 3, 5);
		canvas.drawUIElement(cross);

		String bg = url;
		// p("Using bg: " + bg);
		canvas.setBackgroundImage(bg);
		canvas.setHeight((bfmask.getImage().getHeight()+200)+"px");
		HorizontalLayout hcan = new HorizontalLayout();
		
		final GradientPanel grad = bfmask.getGradient();
		GradientLegend leg = new GradientLegend(grad, new GradientLegend.Recipient() {
			
			@Override
			public void minOrMaxChanged() {
				p("Min or max changed");
				gradmin = (int) grad.getMin();
				gradmax = (int) grad.getMax();
				bfmask.setMin(gradmin);
				bfmask.setMax(gradmax);
				bfmask.repaint();
				//app.showMessage("Gradient", "This is not fully working yet :-)");
				reopen();
				
			}
		}, app, bfmask.getImage().getHeight(), (int)canvas.getHeight());
		leg.addGuiElements(hcan);
		hcan.addComponent(canvas);
		mywindow.addComponent(hcan);
		
		// app.showMessage(this,
		// "Drag the cursor to select a different well/area");

		canvas.addListener(new Canvas.CanvasMouseUpListener() {

			@Override
			public void mouseUp(Point p, UIElement child) {
				int newx = (int) p.getX();
				int newy = (int) p.getY();
				p("Got mouse UP on canvas: " + p + ", child=" + child);
				if (child != null && child instanceof Polygon) {
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
				}
				// poly.moveTo(p);
			}
		});

		if (flag.isCustom()) {
			total = mask.getTotal(flag);
			if (total < 5000) {
				selectAllWellsOfResult(5000, "All results of "+flag.getDescription());
			} else
				getLatestSelection("Subset of "+ flag.getDescription());
		}
		app.openTable();
	}
	public void clear() {
		bfmask = null;
	}
	public String getHelpMessage() {
		String msg = "<ul>";
		msg += "<li>drag the cursor and then <b>double click</b> to pick a well or region</li>";
		msg += "<li>click on Search to search for alignments, sequences or filter scores</li>";
		msg += "<li>zoom in or out of the image </li>";
		msg += "<li>Create a mask with a search result (that can be usedin Process/Automate) </li>";
		msg += "<li>export the image (opens a new windows, then right click on the image and click save as) </li>";
		msg += "<li>enter a new x (column) or y (row) coordinat and hit refresh to also change the coordinate in other components </li>";
		msg += "</ul>";
		return msg;
	}
	private void openFile(boolean ask) {
		File f = new File(exp.getPluginDir());
		if (flag.getFilename() != null) f = new File( flag.getFilename());
				
			FileBrowserWindow browser = new FileBrowserWindow("Pick a .bmp file", null,  new FileBrowserWindow.Recipient() {
				@Override
				public void fileSelected(File file) {
					p("Got file:"+file);
					if (file != null && file.isFile() && file.exists()) {
						flag.setFilename(file.toString());
						mask = app.getScoreMask();						
						mask.readData(flag);
						app.showMessage("Open", "Data loaded for "+flag.getName());
						bfmask= null;
						reopen();
					}
					else app.showMessage("Not Opened", "Could not load file "+file);
				}			
			public boolean allowInList(File f, boolean toSave) {
				if (f == null) return false;
				if (f.isDirectory()) {
					if (!f.canRead()) return false;
					String dir= f.getAbsolutePath().toString();					
				}				
				return true;
			}
			
		},mainwindow, f, ".bmp");
		browser.open();
		
	}
	private void saveFile(boolean ask) {
		mask = app.getScoreMask();
		
		final double[][] data = mask.getData(flag);
		if (data == null) {
			app.showMessage("Nothing to save", "See no data to save");
			return;
		}
		File f = new File(exp.getPluginDir());
		
		app.logModule(ScoreMaskWindowCanvas.this.getName(), "save mask");
		if (flag.getFilename() != null) f = new File(flag.getFilename() );
				
			FileBrowserWindow browser = new FileBrowserWindow("Pick a .bmp file", null, new FileBrowserWindow.Recipient() {
				@Override
				public void fileSelected(File file) {
					p("Got file:"+file);
					if (file != null && !file.isDirectory()) {
						flag.setFilename(file.toString());
						ScoreMaskGenerator gen = new ScoreMaskGenerator(mask, exp);						
						gen.createImageFile(flag, data);
						app.showMessage("Saved", "Data of "+flag.getName()+" saved to "+file);
					}
					else app.showMessage("Not saving", "Won't save to file "+file);
					
				}
				public boolean allowInList(File f, boolean toSave) {
					if (f == null) return false;
					if (f.isDirectory()) {
						if (!f.canRead()) return false;						
					}					
					return true;
				}
				
			}, FileBrowserWindow.SAVE, mainwindow, f, ".bmp");
			browser.open();
	}
	private void startFilterAction() {
		OptionsDialog input = new OptionsDialog(mainwindow, "What would you like to find?", "Find reads with ...", "... an alignment pattern (such as TT_)", "... a certain subsequence (such as GATCGATCGA)", "... a stretch of perfect matches (such as 100bp)", "... a certain score (such as nr indels, % identity etc)", new OptionsDialog.Recipient() {

			@Override
			public void gotInput(final int selection) {
				if (selection < 0 && selection > 3) return;
				if (selection == 0) doFindAlignmentAction();
				else if (selection == 1) doFindSequenceAction();
				else if (selection == 2) doFindPerfectReadAction();
				else if (selection == 3) doScoreFilterAction();
			}

		});
	}

	private void createMask() {
		final String defname = "custom";
		app.logModule(super.getName(), "createmask");
		InputDialog input = new InputDialog(mainwindow, "Name of this mask (can be used in Process/Automate)", new InputDialog.Recipient() {

			@Override
			public void gotInput(String name) {

				if (name == null || name.length() < 1) name = defname;
				maskname = name;
				mask =app.getScoreMask();
				
				if (flag == null || maskname == null) return;
				
				BitMask m = mask.createCustomMask(flag, maskname);
				ExplorerContext cont = app.getExplorerContext();
				p("exp  custom masks");
				for (BitMask mas: exp.getCustomMasks()) {
					p(mas.getName());
				}
				p("createMask: Adding custom mask to maincontext");
				cont.addCustomMasks();
				p("createMask: maincontext Masks are now: ");
				for (BitMask mas: cont.getMasks()) {
					p(mas.getName());
				}
				
			//	if (cont.getMasks()
				DecimalFormat f = new DecimalFormat("#.###");
				long tot = mask.getTotal(flag);
				p("Total nr flags:" + tot + ", mask total=" + m.getTotal() + ", %:" + f.format(m.computePercentage()));
				app.showMessage(ScoreMaskWindowCanvas.this, "Created mask " + maskname + " with " + m.getTotal() + "=" + f.format(m.computePercentage()) + "% wells<br>(use in Automate or Process)");
			}

		}, defname);

	}

	private void doFindAlignmentAction() {

		/**
		 * list.add(new SpecificDeletionSMCalculator()); list.add(new
		 * PrimerSMCalculator()); list.add(new PerfectReadCalculator());
		 */
		AlignDialog input = new AlignDialog(mainwindow, "Find reads with a certain alignment pattern", "Alignment pattern", "T_", "TT", new AlignDialog.Recipient() {

			@Override
			public void gotInput(final String seq, final String ref) {
				if (seq.length() < 1 && ref.length() < 1) return;
				// we need ONE of them at least

				FlowRangeDialog input = new FlowRangeDialog(mainwindow, "Flow Range", "Flow range:", 0, exp.getNrFlows(), new FlowRangeDialog.Recipient() {
					@Override
					public void gotInput(int start, int end) {
						if (end == exp.getNrFlows()) end = 0;

						if (end > 0) {
							if (!checkFlowOrder()) end =0;
						}
						app.logModule(ScoreMaskWindowCanvas.this.getName(), "find al "+ref+"/"+seq);
						SpecificDeletionSMCalculator calc = new SpecificDeletionSMCalculator();
						calc.setEndflow((int) end);
						calc.setStartflow((int) start);
						calc.setPatref(ref);
						calc.setPatseq(seq);
						final ScoreMaskFlag customflag = ALIGN_FLAG;
						customflag.setFilename(null);
						calc.setFlag(customflag);
						flag = customflag;
						flag.setDescription("Alignment pattern  "+ calc.getPatseq()+"/"+calc.getPatref()+", flows "+start+"-"+end);
						p("Got patrf=" + calc.getPatref() + ", gotpatseq=" + calc.getPatseq());
						p("Got params: " + calc.toFullString());
						customtask = new ComputeHeatMapTask(calc, ScoreMaskWindowCanvas.this);
						customtask.execute();

					}
				});

			}

		});
	}

	private void doFindSequenceAction() {

		/**
		 * list.add(new SpecificDeletionSMCalculator()); list.add(new
		 * PrimerSMCalculator()); list.add(new PerfectReadCalculator());
		 */
		InputDialog input = new InputDialog(mainwindow, "Find reads with a certain subsequence", new InputDialog.Recipient() {

			@Override
			public void gotInput(final String seq) {
				if (seq == null || seq.length() < 1) return;

				FlowRangeDialog input = new FlowRangeDialog(mainwindow, "Flow Range", "Flow range:", 0, exp.getNrFlows(), new FlowRangeDialog.Recipient() {
					@Override
					public void gotInput(int start, int end) {
						if (end == exp.getNrFlows()) end = 0;

						if (end > 0) {
							if (!checkFlowOrder()) end =0;
						}
						app.logModule(ScoreMaskWindowCanvas.this.getName(), "find "+ seq);
						PrimerSMCalculator calc = new PrimerSMCalculator();
						calc.setEndflow((int) end);
						calc.setStartflow((int) start);
						calc.setSeq(seq);
						final ScoreMaskFlag customflag = ALIGN_FLAG;
						calc.setFlag(customflag);
						customflag.setFilename(null);
						flag = customflag;
						flag.setDescription("Sequence substring  "+ seq+", flows "+start+"-"+end);
						p("Got params: " + calc.toFullString());
						ComputeHeatMapTask customtask = new ComputeHeatMapTask(calc, ScoreMaskWindowCanvas.this);
						customtask.execute();
						// app.showTopMessage("Searching","Finding reads with subsequence "+seq);
					}
				});

			}

		}, "GATCGATCGA");
	}

	private class ComputeHeatMapTask extends Task {

		ScoreMaskCalculatorIF calc;

		public ComputeHeatMapTask(ScoreMaskCalculatorIF calc, TaskListener tlistener) {
			super(ScoreMaskWindowCanvas.this);

			indicator = new ProgressIndicator(new Float(0.0));
			indicator.setHeight("40px");
			// indicator.setCaption("Creating whole Proton image");
			indicator.setDescription("I searching the .BAM file ...");
			indicator.setPollingInterval(5000);
			h.addComponent(indicator);
			String time = " a few ";
			if (exp.is318()) time = " about 10 ";
			else if (exp.is316()) time = " about 5 ";
			app.showTopMessage("Searching", "Searching the .BAM file - can take " + time + " minutes...");
			setProglistener((ProgressListener) ScoreMaskWindowCanvas.this);
			this.calc = calc;
		}

		@Override
		public Void doInBackground() {
			try {
				mask = app.getScoreMask();
				ScoreMaskGenerator gen = new ScoreMaskGenerator(mask, exp);
				p("++++++ Searching the bam file...");
				File f = new File(mask.getFile(flag));
				if (f.exists()) f.delete();

				
				try {
					String msg = gen.processBamFileForCustomFlag(false, calc);
					if (msg != null && msg.length() > 0) {
						app.showMessage("Got Message: ", msg);
					}
				} catch (Exception e) {
					err(ErrorHandler.getString(e));
				}
				total = gen.getTotal();
				p("Total from generator: " + gen.getTotal());
				// total = mask.getTotal(customflag);
				
				if (total <= 0) {
					app.showLongMessage("No results", "I found no reads");
				} else
					app.showTopMessage("Found " + total + " reads", "Found " + total + " results");
				p("+++++++++ Searching the bam file DONE: got " + total + " results");
				indicator.setValue(new Float(1.0));
				// if total is small, select all wells!

			} catch (Exception ex) {
				err(ErrorHandler.getString(ex));
			}
			return null;
		}

		

		public boolean isSuccess() {
			return true;
		}
	}
	private boolean checkFlowOrder() {
		if (exp.getFlowOrder() == null || exp.getFlowOrder().trim().length()<2) {
			err("Got no flow order");
			SequenceLoader loader = SequenceLoader.getSequenceLoader(exp);
			exp.setFlowOrder(loader.getFlowOrder());
			
		}
		if (exp.getFlowOrder() == null || exp.getFlowOrder().trim().length()<2) {
			app.showMessage("Got no flow order", "Unknown flow order - will search *all* flows");
			return false;
		}
//		if (exp.getLibraryKey() == null || exp.getLibraryKey().trim().length()<2) {
//			app.showMessage("Got no key", "Unknown library key  - will search *all* flows");
//			return false;
//		}
		else return true;
	}
	@Override
	public void taskDone(Task t) {
		p("Task " + t + " is done");
		if (indicator != null) {
			h.removeComponent(indicator);
		}
		p("Got flag:" + flag);
		if (total <= 5000) {
			selectAllWellsOfResult(5000, "All wells of "+ flag.getDescription());
		} else
			getLatestSelection("Subset of "+ flag.getDescription());

		// createMask();
		bfmask= null;
		reopen();

	}
	public void close() {
		super.close();
		if (customtask != null && !customtask.isCancelled()) {
			customtask.cancel(true);
			customtask = null;
		}		
	}
	private void doFindPerfectReadAction() {
		InputDialog input = new InputDialog(mainwindow, "Find reads with a perfect stretch", new InputDialog.Recipient() {

			@Override
			public void gotInput(String slen) {
				if (slen == null) return;
				int len = -1;
				try {
					len = Integer.parseInt(slen);
				} catch (Exception e) {}
				if (len <= 0) return;
				PerfectReadCalculator calc = new PerfectReadCalculator();
				calc.setLen(len);
				final ScoreMaskFlag customflag = PERFECT_FLAG;
				calc.setFlag(customflag);
				customflag.setFilename(null);
				flag = customflag;
				flag.setDescription("Perfect read stretches of size "+ slen);
				app.logModule(ScoreMaskWindowCanvas.this.getName(), "perfect read "+ slen);
				ComputeHeatMapTask customtask = new ComputeHeatMapTask(calc, ScoreMaskWindowCanvas.this);
				customtask.execute();
				// app.showTopMessage("Searching","Finding reads with perfect stretches of at least "+len+" bp");
			}

		}, "100");
		// app.showTopMessage("Filtering","Finding perfect reads");
	}

	private void doScoreFilterAction() {
		 mask =app.getScoreMask();
		mask.readData(flag);
		final ScoreMaskFlag customflag = SCORE_FLAG;
		double min = mask.getMin(flag);
		double max = mask.getMax(flag);

		RangeDialog input = new RangeDialog(mainwindow, "Enter a range of values for " + flag.getName(), "Enter values between " + min + " and " + max, min, max, new RangeDialog.Recipient() {

			@Override
			public void gotInput(double min, double max) {
				ScoreMaskGenerator gen = new ScoreMaskGenerator(mask, exp);
				app.showTopMessage("Filtering", "Searching "+flag.getName() +"   between " + min + " and " + max );
				customflag.setFilename(null);
				int count = gen.filterFlag(flag, customflag, min, max);

				app.logModule(ScoreMaskWindowCanvas.this.getName(), "filter "+ flag.getName()+" "+min+"-"+max);
				flag = customflag;
				
				String desc = "Found " + count + " reads with " + flag.getName() + " between " + min + " and " + max;

				flag.setDescription(desc);
				app.showTopMessage("Result", "Found "+count+" reads for "+flag.getName() +" between " + min + " and " + max );
				if (count <= 5000) {
					selectAllWellsOfResult(5000, "All wells for "+flag.getName() +" between " + min + " and " + max );

				} else
					getLatestSelection("Subset of "+ flag.getName() +" between " + min + " and " + max);

				// createMask();
				bfmask= null;
				reopen();

			}

		});
	}

	public void valueChange(Property.ValueChangeEvent event) {
		// The event.getProperty() returns the Item ID (IID)
		// of the currently selected item in the component.
		Property id = event.getProperty();
		if (id.getValue() instanceof ScoreMaskFlag) {
			ScoreMaskFlag b = (ScoreMaskFlag) id.getValue();
			this.flag = b;
			// pick wells o fthis flag
			if (flag.isCustom()) {
				total = mask.getTotal(flag);
				if (total < 5000) {
					selectAllWellsOfResult(5000, "All results of "+flag.getDescription());
				} else
					getLatestSelection("Subset of "+ flag.getDescription());
			}
			bfmask= null;
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
		mask.readAllData();

		ArrayList<WellCoordinate> wells = mask.getAllCoordsWithData(flag, 1000, x - d, y - d, x + d, y + d);
		WellSelection sel = new WellSelection(x - d, y - d, x + d, y + d, wells);
		sel.setTitle(title);
		exp.getWellContext().setSelection(sel);
		if (wells != null) {
			for (WellCoordinate well : wells) {

				well.setScoredata(mask.getDataPointsAt(well.getCol(), well.getRow()));
				// p("setting score data:"+Arrays.toString(well.getScoredata()));
			}
		}
		exp.makeRelative(coord);
		app.setWellSelection(sel);
		// Don't trigger a new well selection!
		app.setWellCoordinate(coord, false);
		app.reopenRaw();
	}

	private void selectAllWellsOfResult(int max, String title) {
		//app.showMessage("All wells", "Showing <b>all</b> wells of search result");
		ArrayList<WellCoordinate> wells = mask.getAllCoordsWithData(flag, max);
		if (wells == null) return;
		WellSelection sel = new WellSelection(wells);
		exp.getWellContext().setSelection(sel);
		sel.setTitle(title);
		if (wells != null) {
			for (WellCoordinate well : wells) {
				well.setScoredata(mask.getDataPointsAt(well.getCol(), well.getRow()));
				// p("setting score data:"+Arrays.toString(well.getScoredata()));
			}
		}
		app.setWellSelection(sel);
		// Don't trigger a new well selection!
	}

	private static void err(String msg, Exception ex) {
		Logger.getLogger(ScoreMaskWindowCanvas.class.getName()).log(Level.SEVERE, msg, ex);
	}

	private static void err(String msg) {
		Logger.getLogger(ScoreMaskWindowCanvas.class.getName()).log(Level.SEVERE, msg);
	}

	private static void warn(String msg) {
		Logger.getLogger(ScoreMaskWindowCanvas.class.getName()).log(Level.WARNING, msg);
	}

	private static void p(String msg) {
		//system.out.println("ScoreMaskWindowCanvas: " + msg);
		Logger.getLogger(ScoreMaskWindowCanvas.class.getName()).log(Level.INFO, msg);
	}

	@Override
	public void setMessage(String msg) {
		indicator.setDescription(msg);
	}

	@Override
	public void stop() {}

	public void setProgressValue(int p) {
		if (indicator != null) indicator.setValue(((double) p / 100.0d));
		// progress.setValue("Creating composite image: " + p + "%");
	}
}
