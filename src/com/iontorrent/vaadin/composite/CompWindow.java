/* * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.composite;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.iontorrent.expmodel.CompositeExperiment;
import com.iontorrent.expmodel.DatBlock;
import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.heatmaps.PrimerSMCalculator;
import com.iontorrent.heatmaps.ScoreMaskCalculatorIF;
import com.iontorrent.heatmaps.ScoreMaskGenerator;
import com.iontorrent.heatmaps.SpecificDeletionSMCalculator;
import com.iontorrent.rawdataaccess.pgmacquisition.RawType;
import com.iontorrent.rawdataaccess.wells.BfMaskFlag;
import com.iontorrent.rawdataaccess.wells.ScoreMaskFlag;
import com.iontorrent.results.scores.ScoreMask;
import com.iontorrent.threads.Task;
import com.iontorrent.threads.TaskListener;
import com.iontorrent.utils.ErrorHandler;
import com.iontorrent.utils.ProgressListener;
import com.iontorrent.vaadin.TSVaadin;
import com.iontorrent.vaadin.scoremask.AlignDialog;
import com.iontorrent.vaadin.scoremask.ScoreMaskWindowCanvas;

import com.iontorrent.vaadin.utils.FlowRangeDialog;
import com.iontorrent.vaadin.utils.InputDialog;
import com.iontorrent.vaadin.utils.OptionsDialog;
import com.iontorrent.vaadin.utils.WindowOpener;
import com.iontorrent.vaadin.utils.ZoomControl;
import com.iontorrent.wellmodel.BfHeatMap;
import com.iontorrent.wellmodel.CompositeWellDensity;
import com.iontorrent.wellmodel.WellCoordinate;
import com.iontorrent.wellmodel.WellSelection;
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
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.Panel;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.Select;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * 
 * @author Chantal Roth chantal.roth@lifetech.com
 */
public class CompWindow extends WindowOpener implements ProgressListener,
		Property.ValueChangeListener, TaskListener {

	private TSVaadin app;
	CompositeExperiment comp;
	BfMaskFlag flag;
	int flow = 0;
	RawType type = RawType.ACQ;
	int frame;
	ProgressIndicator indicator;
	CompositeImage bfmask;
	BfHeatMap mask;
	Embedded em;
	Select sel;
	int bucket;
	ZoomControl zoom;
	StreamResource imageresource;
	Button stop;
	WorkThread t;
	HorizontalLayout mainhor;
	// ComputeHeatMapTask task;
	// ScoreMaskFlag flags[];
	HorizontalLayout h;
	ComputeHeatMapTask customtask;
	long globaltotal;
	ArrayList<DatBlock> foundblocks;
	int nrblocks;
	boolean stopSearch;
	

	public CompWindow(TSVaadin app, Window main, String description, int x,
			int y) {
		super("Pick a Block (Proton)", main, description, x, y, 800, 800);
		this.app = app;
		this.frame = 0;
		
		bucket = 2;
	}

	@Override
	public void openButtonClick(Button.ClickEvent event) {
		if (app.getCompositeExperiment() == null) {
			appwindow
					.showNotification(
							"No Proton Experiment Selected",
							"<br/>Please open a proton experiment first"
									+ "<br/>You can either browse the db or enter the paths manually",
							Window.Notification.TYPE_WARNING_MESSAGE);
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

		mask = BfHeatMap.getMask(comp.getRootContext());
		if (flag == null) {
			flag = BfMaskFlag.LIVE;
			boolean has = mask.hasImage("composite", flag, flow, type, frame);
			if (!has) flag = BfMaskFlag.RAW;
			
		}
		
		// HorizontalLayout = new HorizontalLayout();

		h = new HorizontalLayout();
		mainhor = new HorizontalLayout();
		mywindow.addComponent(h);
		mywindow.addComponent(mainhor);

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

		// h.addComponent(new Label("Frame:"));
		final TextField txtFrame = new TextField();
		txtFrame.setValue("" + frame);
		txtFrame.setImmediate(true);
		// h.addComponent(txtFrame);
		txtFrame.addListener(new TextField.ValueChangeListener() {

			@Override
			public void valueChange(ValueChangeEvent event) {
				if (txtFrame.getValue() == null)
					return;
				frame = Integer.parseInt("" + txtFrame.getValue());
				reopen();
			}

		});

		Button filter = new Button("Search...");
		filter.setDescription("Search entire proton chip for alignment patterns");
		h.addComponent(filter);
		filter.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(com.vaadin.ui.Button.ClickEvent event) {
				startFilterAction();
			}
		});

		
		VerticalLayout ver = new VerticalLayout();
		mainhor.addComponent(ver);

		zoom = new ZoomControl(bucket, new Button.ClickListener() {
			@Override
			public void buttonClick(com.vaadin.ui.Button.ClickEvent event) {
				bucket = zoom.getBucket();
				reopen();
			}
		});

		zoom.addGuiElements(ver);

		final NativeButton help = new NativeButton();
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
		ver.addComponent(export);
		ver.addComponent(help);

		//h.addComponent(new Label(" " + comp.getRootContext().getRawDir()));

		boolean has = mask.hasImage("composite", flag, flow, type, frame);
		if (!has) {
			// start thread
			p("Need to compute image first, starting thread");
			indicator = new ProgressIndicator(new Float(0.0));
			indicator.setHeight("40px");
			// indicator.setCaption("Creating whole Proton image");
			indicator
					.setDescription("I am reading the beginning of all raw files of all Proton blocks and I am computing a composite heat map from that");
			indicator.setPollingInterval(5000);
			h.addComponent(filter);
			
			h.addComponent(indicator);
			t = new WorkThread(this);
			t.execute();

			app.showMessage(
					this,
					"I need to compute the heat map first<br>(this will take a few minutes)<br>But you can still pick a block!");
			addCompView(99999);

		} else {
			addCompView(10000);
		}

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

	private void startFilterAction() {
		if (customtask != null) {
			app.showMessage("Already searching", "Please wait until the current serach is done");
			return;
		}
		OptionsDialog input = new OptionsDialog(mywindow,
				"What would you like to find?", "Find reads with ...",
				"... an alignment pattern (such as TT_)",
				"... a certain subsequence (such as GATCGATCGA)",
				// "... a stretch of perfect matches (such as 100bp)",
				// "... a certain score (such as nr indels, % identity etc)",
				new OptionsDialog.Recipient() {

					@Override
					public void gotInput(final int selection) {
						// if (selection < 0 || selection > 3)
						// return;
						// if (selection == 0)
						// doFindAlignmentAction();
						// else if (selection == 1)
						// doFindAlignmentAction();
						// else if (selection == 2)
						// doFindAlignmentAction();
						// else if (selection == 3)
						doFindAlignmentAction();
					}

				});
	}

	private void doFindAlignmentAction() {

		p("Finding alignment");
		/**
		 * list.add(new SpecificDeletionSMCalculator()); list.add(new
		 * PrimerSMCalculator()); list.add(new PerfectReadCalculator());
		 */
		AlignDialog input = new AlignDialog(mywindow,
				"Find reads with a certain alignment pattern",
				"Alignment pattern", "TTTT___", "TTTTTTT",
				new AlignDialog.Recipient() {

					@Override
					public void gotInput(final String seq, final String ref) {
						if (seq.length() < 1 && ref.length() < 1)
							return;
						// we need ONE of them at least

						FlowRangeDialog input = new FlowRangeDialog(mywindow,
								"Flow Range", "Flow range:", 0, comp
										.getRootContext().getNrFlows(),
								new FlowRangeDialog.Recipient() {
									@Override
									public void gotInput(int start, int end) {
										if (end == comp.getRootContext()
												.getNrFlows())
											end = 0;

										app.logModule(
												CompWindow.this.getName(),
												"find al " + ref + "/" + seq);
										SpecificDeletionSMCalculator calc = new SpecificDeletionSMCalculator();
										calc.setEndflow((int) end);
										calc.setStartflow((int) start);
										calc.setPatref(ref);
										calc.setPatseq(seq);
										final ScoreMaskFlag customflag = ScoreMaskFlag.CUSTOM1;
										customflag.setFilename(null);
										calc.setFlag(customflag);
										ScoreMaskFlag sflag = customflag;
										sflag.setDescription("Alignment pattern  "
												+ calc.getPatseq()
												+ "/"
												+ calc.getPatref()
												+ ", flows "
												+ start + "-" + end);
										p("Got patrf=" + calc.getPatref()
												+ ", gotpatseq="
												+ calc.getPatseq());
										p("Got params: " + calc.toFullString());
										stopSearch = false;

										stop = new Button("Stop search");
										stop.addListener(new Button.ClickListener() {
											@Override
											public void buttonClick(com.vaadin.ui.Button.ClickEvent event) {
												stopSearch = true;
												h.removeComponent(stop);
												if (indicator != null) h.removeComponent(indicator);
											}
										});
										h.addComponent(stop);
										customtask = new ComputeHeatMapTask(
												calc, CompWindow.this, sflag);
										customtask.execute();

									}
								});

					}

				});
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

	private void addCompView(int key) {
		String file = mask.getImageFile("composite", flag, flow, type, frame);
	//	app.showMessage(this, "Loading image " + file);
		mask.readData(flag, file, true);
		bfmask = new CompositeImage(comp, flag, null, flow, frame, bucket);

		if (foundblocks != null) {
			// mark found blocks!
			bfmask.markBlocks(foundblocks);
		}
		File f = new File(file);
		imageresource = new StreamResource(bfmask, comp.getRootContext()
				.getRawDir() + "_" + f.getName() + "_comp_"+key+".png", app);
		imageresource.setCacheTime(300);
		mywindow.setHeight(bfmask.getImage().getHeight() + 160 + "px");
		mywindow.setWidth(150 + bfmask.getImage().getWidth() + "px");
		//mainhor.setHeight(bfmask.getImage().getHeight() + 110 + "px");
		//mainhor.setWidth(100 + bfmask.getImage().getWidth() + "px");

		//
		if (em != null) mainhor.removeComponent(em);
		em = new Embedded(null, imageresource);
		mainhor.addComponent(em);

		em.addListener(new ClickListener() {

			public void click(ClickEvent event) {
				int x = event.getRelativeX();
				int y = event.getRelativeY();
				// x = x-event.getComponent().getWindow().getPositionX();
				// y = y-event.getComponent().getWindow().getPositionY();
				p("image clicked at: " + x + "/" + y);
				
				WellCoordinate coord = bfmask.getWellCoordinate(x, y);
				p("Got coord: " + coord);
				if (coord == null) return;
				DatBlock block = comp.findBlock(coord);
				if (block != null) {
					sel.select(block);
					ExperimentContext exp = comp.getContext(block, false);
					WellCoordinate rel = new WellCoordinate(coord);
					exp.makeRelative(coord);
					boolean hassel = block.getSel() != null
							&& block.getSel().getNrWells() > 0;
					if (hassel) {
						p("Got selected wells");
						WellSelection wellsel = block.getSel();
						exp.getWellContext().setSelection(wellsel);						
						rel = wellsel.getAllWells().get(0);
						app.showMessage("Found wells", "Selecting "+wellsel.getNrWells()+" wells");
					}
					app.setExperimentContext(exp);
					exp.getWellContext().setCoordinate(rel);
					app.setWellCoordinate(rel, !hassel);
					
					if (hassel) {
						app.openTable();
						ScoreMaskWindowCanvas scorewindow = app.getScoreWindow();
						scorewindow.setFlag(ScoreMaskFlag.CUSTOM1);
						File file = getFileName(block, exp, false);
						app.showMessage("Search Result", "Attempting to load search result from "+file);
						scorewindow.open();
						scorewindow.loadResult(file);
					}
					// lblock.setValue("Selected block: " +
					// block.toShortString());
					// app.reopenRaw();

				} else {
					p("Got no block for " + coord);
					em.setComponentError(new UserError(
							"Found no valid block for " + coord));
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

		CompositeWellDensity gen = new CompositeWellDensity(comp, type, flow,
				frame, bucket);
		String msg = null;
		String file = mask.getImageFile("composite", flag, flow, type, frame);
		p("About to compute image " + file);
		try {
			msg = gen.createCompositeImages(progress, file, flag);
			p("Image " + file + " computed");

		} catch (Throwable e) {
			err("Could not compute image: " + ErrorHandler.getString(e));
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
			} catch (Exception e) {
				err("Got an error when computing the heat map: "
						+ ErrorHandler.getString(e));
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
				// ExperimentContext exp = comp.getThumbnailsContext();
				// // exp.getWellContext().setCoordinate(coord);
				// app.setExperimentContext(exp);
				// // lblock.setValue("Seleced block: " + val);
				// app.showExperiment();
			} else { // root context
				ExperimentContext exp = comp.getRootContext();
				// exp.getWellContext().setCoordinate(coord);
				app.setExperimentContext(exp);
				// lblock.setValue("Seleced block: " + val);
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
		// system.out.println("CompWindow: " + msg);
		Logger.getLogger(CompWindow.class.getName()).log(Level.INFO, msg);
	}

	public void setProgressValue(int p) {
		if (indicator != null)
			indicator.setValue(((double) p / 100.0d));
		// progress.setValue("Creating composite image: " + p + "%");
	}

	public void setMessage(String msg) {
		indicator.setDescription(msg);
	}

	public void stop() {
	}

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
		
		if (task instanceof WorkThread) {
			if (indicator != null) {
				h.removeComponent(indicator);
			}
			if (!has) {
				String file = mask.getImageFile("composite", flag, flow, type,
						frame);

				// mask.readData(flag, file, true);
				app.showError(
						CompWindow.this,
						"Something went wrong when computing the heat map. <br>I still don't seem to have the file:<br>"
								+ file);
			} else
				reopen();

		} else {
			customtask = (ComputeHeatMapTask) task;
			foundblocks = customtask.getFoundBlocks();
			
			int blocks = customtask.getFoundBlocks().size();
			globaltotal = customtask.getGlobalTotal();
			this.nrblocks = comp.getNrBlocks();
			p("Search done for block "+customtask.blocknr);			
			int block = customtask.blocknr;
			if (customtask.blocknr >= this.nrblocks || stopSearch) {
				if (indicator != null) {
					h.removeComponent(indicator);
				}
				if (stop != null) h.removeComponent(stop);
				app.showMessage("Proton search done",
						"Got " + globaltotal + " search results in " + blocks
								+ " blocks:<br>" + customtask.getFoundBlocks());
				customtask = null;
				addCompView(block);
			}
			else {
				
				p("Got " + customtask.total + " search results for block "+comp.getBlocks().get(block));
				// update 
				addCompView(block);
				customtask = new ComputeHeatMapTask(customtask);
				customtask.execute();
			}
			
		}
	}
	private class ComputeHeatMapTask extends Task {

		ScoreMaskCalculatorIF calc;
		ArrayList<DatBlock> foundblocks;
		ScoreMaskFlag sflag;
		long globaltotal;
		int blocknr;
		long total;
		
		public ComputeHeatMapTask(ComputeHeatMapTask task) {
			super(CompWindow.this);
			this.sflag = task.sflag;
			setProglistener((ProgressListener) CompWindow.this);
			this.calc = task.calc;
			this.globaltotal = task.globaltotal;
			this.blocknr = task.blocknr+1;
			this.foundblocks = task.foundblocks;
		}
		
		public ComputeHeatMapTask(ScoreMaskCalculatorIF calc,
				TaskListener tlistener, ScoreMaskFlag sflag) {
			super(CompWindow.this);
			this.sflag = sflag;
			indicator = new ProgressIndicator(new Float(0.0));
			indicator.setHeight("40px");
			// indicator.setCaption("Creating whole Proton image");
			indicator.setDescription("I searching the .BAM file ...");
			indicator.setPollingInterval(5000);
			h.addComponent(indicator);
			int nrblocks = comp.getNrBlocks();
			String time = nrblocks * 2  + " minutes";
			app.setTimeout(nrblocks * 10);
			app.showTopMessage(
					"Searching",
					"Searching the .BAM file of all blocks - can take over "
							+ time
							+ "<br>(I increased the session timeout accordingly)");
			setProglistener((ProgressListener) CompWindow.this);
			this.calc = calc;
			globaltotal = 0;
			foundblocks = new ArrayList<DatBlock>();
		}

		public long getGlobalTotal() {
			return globaltotal;
		}

		public ArrayList<DatBlock> getFoundBlocks() {
			return foundblocks;
		}

		@Override
		public Void doInBackground() {
			try {
				int nrblocks = comp.getNrBlocks();
				
				DatBlock block = comp.getBlocks().get(blocknr);
				
				ExperimentContext exp = comp.getContext(block, false);
				app.setExperimentContext(exp, true);
				ScoreMask smask = app.getScoreMask();
				ScoreMaskGenerator gen = new ScoreMaskGenerator(smask, exp);
				p("++++++ Searching the bam file " + exp.getBamFileName());
				File f = new File(smask.getFile(sflag));
				if (f.exists())
					f.delete();

				try {
					String msg = gen.processBamFileForCustomFlag(false,
							calc);
					if (msg != null && msg.length() > 0) {
						app.showMessage("Got Message: ", msg);
					}
				} catch (Exception e) {
					err(ErrorHandler.getString(e));
				}
				total = gen.getTotal();
				p("Total from generator: " + gen.getTotal());
				// total = mask.getTotal(customflag);

				p("+++++++++ Searching the bam file DONE: got " + total
						+ " results");
				globaltotal += total;
				if (total > 0) {
					foundblocks.add(block);
					p("added block " + block + " to search result with "
							+ total + " results");
					final double[][] data = smask.getData(sflag);
					if (total < 500) {
						// mark wells
						ArrayList<WellCoordinate> coords = smask
								.getAllCoordsWithData(sflag, 1000);
						WellSelection sel = new WellSelection(coords);
						block.setSel(sel);
						sel.setTitle("Search result");
					}
					else block.setNrMarkedWells((int) total);
					if (data != null) {						
						File file = getFileName(block, exp, true);
						sflag.setFilename(file.toString());
						gen.createImageFile(sflag, data);
						p("Stored data in file " + file);
					}
				}
				indicator.setValue(new Float(1.0 / nrblocks * blocknr));
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
	private File getFileName(DatBlock block, ExperimentContext exp, boolean write) {
		String name = "/searchresult_block_"
				+ block.getStart().getRow() + "_"
				+ block.getStart().getCol() + ".bmp";
		
		File file = new File(exp.getPluginDir()+name);
		if ((write && !file.canWrite())  || (!write && !file.exists())) {
			file = new File(exp.getCacheDir()+name);
		}
		else return file;
		if ((write && !file.canWrite())  || (!write && !file.exists())) {
			file = new File(exp.getResultsDirectory()+name);
		}
		else return file;
		if ((write && !file.canWrite())  || (!write && !file.exists())) {
			file = new File("/tmp"+name);
		}
		else return file;
		return file;
	}
}
