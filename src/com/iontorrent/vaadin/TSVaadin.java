/*
 * TSVaadin.java
 *
 * Created on 20. Oktober 2011, 08:51
 */
package com.iontorrent.vaadin;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import com.github.wolfie.sessionguard.SessionGuard;
import com.iontorrent.expmodel.CompositeExperiment;
import com.iontorrent.expmodel.DatBlock;
import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.results.scores.ScoreMask;
import com.iontorrent.torrentscout.explorer.ExplorerContext;
import com.iontorrent.utils.SystemTool;
import com.iontorrent.utils.io.FileTools;
import com.iontorrent.vaadin.align.AlignWindow;
import com.iontorrent.vaadin.automate.AutomateWindow;
import com.iontorrent.vaadin.bfmask.BfMaskWindowCanvas;
import com.iontorrent.vaadin.composite.CompWindow;
import com.iontorrent.vaadin.db.DbWindow;
import com.iontorrent.vaadin.fit.FitWindowCanvas;
import com.iontorrent.vaadin.gene.GeneWindow;
import com.iontorrent.vaadin.iono.IonogramWindow;
import com.iontorrent.vaadin.mask.MaskEditWindowCanvas;
import com.iontorrent.vaadin.process.ProcessWindowCanvas;
import com.iontorrent.vaadin.raw.RawWindow;
import com.iontorrent.vaadin.scoremask.ScoreMaskWindowCanvas;
import com.iontorrent.vaadin.table.TableWindow;
import com.iontorrent.vaadin.utils.PerformanceMonitor;
import com.iontorrent.vaadin.utils.WindowOpener;
import com.iontorrent.vaadin.wholechip.WholeChipWindowCanvas;
import com.iontorrent.wellmodel.WellCoordinate;
import com.iontorrent.wellmodel.WellSelection;
import com.vaadin.Application;
import com.vaadin.terminal.ParameterHandler;
import com.vaadin.terminal.gwt.server.HttpServletRequestListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.Notification;
import com.vaadin.terminal.gwt.server.WebApplicationContext;

/**
 * 
 * @author Chantal Roth
 * @version
 */
public class TSVaadin extends Application implements
		HttpServletRequestListener, ParameterHandler {

	public static String VERSION = "2.2.5";

	static {
		Logger.global.setLevel(Level.WARNING);
	}
	
	
	private static final int WARNING_PERIOD_MINS = 2;
	
	private ExperimentContext exp;
	private ExplorerContext maincont;
	private ScoreMask scoremask;
	private CompositeExperiment comp;
	// private TabSheet tabsheet;
	private ExperimentWindow expWindow;
	private WindowOpener dbWindow;
	private WindowOpener ionoWindow;
	private RawWindow rawWindow;
	private WindowOpener tableWindow;
	private WindowOpener automateWindow;
	private WindowOpener processWindow;
	private WindowOpener fitWindow;
	private WindowOpener maskWindow;
	private WindowOpener scoreWindow;
	private WindowOpener geneWindow;
	private WindowOpener chipWindow;
	private WindowOpener helpWindow;
	private WindowOpener alignWindow;
	private WindowOpener compWindow;
	private WindowOpener maskeditWindow;
	private WindowOpener testWindow;
	// private MovieWindow movieWindow;
	private Window main;
	private String server;
	private HashMap<String, String> parameters;
	private String url;

	private String remote;
	private ArrayList<WindowOpener> windows;
	private PerformanceMonitor perf;
	private File logfolder;
	private VerticalLayout mainLayout;
	 
	@Override
	public void init() {
		    
		System.setProperty("java.util.prefs.PreferencesFactory",
				"com.iontorrent.vaadin.utils.DisabledPreferencesFactory");
		setTheme("torrentscout");

		main = new Window("Torrent Scout Light");
		main.addParameterHandler(this);
		main.setStyleName("black");
	//	main.getContent().setHeight("100%");
		
		setMainWindow(main);
		
		// main.setSizeFull();

		logfolder = getLogFolder();
		p("logfolder: " + logfolder);

		createWindows();
		addMenu();

		// showMessage("Open an Experiment",
		// "<br/>Browse the database or pick folders yourself");
		if (perf == null)
			perf = new PerformanceMonitor();
		logStats("tsl_perfstats.csv", perf.getCsvInfo(), perf.getCsvHeader());
		int mb = (int) perf.getFreeMb();
		if (mb < 500) {
			this.showWarning(
					"Low memory",
					"Less than "
							+ mb
							+ " MB memory available on server."
							+ "<br>You might want to try later, otherwise you might get an out of memory error at some point");
		}

	}

//	public void setKeepAlive(boolean alive) {	
//		sessionGuard.setKeepalive(alive);
//	}
//	public boolean isKeepAlive() {
//		return sessionGuard.isKeptAlive();
//	}
	public void setTimeout(int minutes) {
		((WebApplicationContext) getContext()).getHttpSession().setMaxInactiveInterval(minutes*60);
	}
	public int getTimeout() {
		int sessionTimeout = ((WebApplicationContext) getContext())
		        .getHttpSession().getMaxInactiveInterval() / 60;
		return sessionTimeout;
	}
	private void addMenu() {
		MenuBar menu = new MenuBar();
		mainLayout = new VerticalLayout();
		HorizontalLayout layout = new HorizontalLayout();
		layout.addComponent(menu);
		Label vl = new Label(" v" + VERSION);

		vl.setStyleName("smallgray");
		layout.addComponent(vl);
		mainLayout.addComponent(layout);
		main.addComponent(mainLayout);
		//mainLayout.setExpandRatio(mainLayout, 0);

		
		MenuBar.Command mycommand = new MenuBar.Command() {
			private static final long serialVersionUID = 1L;

			@Override
			public void menuSelected(MenuItem selectedItem) {
				showMessage(selectedItem.getText() + " got selected",
						"Menu Selection");

			}
		};
		MenuBar.MenuItem it = menu.addItem("Open Experiment ", null);
		it.setDescription("Select an experiment either by browsing the database, or by picking the data folders yourself");
		add(it, dbWindow);
		add(it, this.expWindow);

		it = menu.addItem("|", null);

		it = menu.addItem("Pick Region", null, null);
		it.setDescription("Pick a well or a region with one of the viewers below");
		add(it, this.compWindow);
		add(it, this.maskWindow);
		add(it, this.chipWindow);
		add(it, this.tableWindow);

		it = menu.addItem("|", null);

		it = menu.addItem("Find Reads", null, null);
		it.setDescription("Find perfect reads, search alignments or sequence patterns");
		add(it, this.scoreWindow);
		add(it, this.geneWindow);

		it = menu.addItem("|", null);

		it = menu.addItem("View Results", null, null);
		it.setDescription("View information about one well, such as the ionogram, the raw signal time series, the alignment and other data from the analysis pipeline (if available)");
		add(it, this.ionoWindow);
		add(it, this.alignWindow);
		add(it, this.rawWindow);
		add(it, "Export data (ionograms, alignments, raw data)",
				this.maskeditWindow);

		it = menu.addItem("|", null);

		it = menu.addItem("Raw Data", null, null);
		it.setDescription("View/Export raw data, find the incorporation signals, compute background subtraction and create masks yourself");

		add(it, this.rawWindow);
		add(it, this.processWindow);
		add(it, this.fitWindow);
		add(it, this.maskeditWindow);
		add(it, this.automateWindow);

		// it = menu.addItem("|",null);

		// it = menu.addItem("Test", null, null);
		// add(it, this.testWindow);

		it = menu.addItem("|", null);
		it = menu.addItem("Help", new Command() {
			@Override
			public void menuSelected(MenuItem selectedItem) {
				helpWindow.open();
			}

		});

		it = menu.addItem("|", null);
		it = menu.addItem("Exit", new Command() {
			@Override
			public void menuSelected(MenuItem selectedItem) {
				logModule("TSL", "exit");
				close();
			}

		});

	}

	public void close() {
		p("---- Close called");
		// for debugging
		Thread threads[] = new Thread[50];
		Thread.enumerate(threads);
		for (Thread t : threads) {
			if (t != null && t.isAlive()) {
				if (!t.getName().startsWith("http")
						&& !t.getName().startsWith("ajp")
						&& !t.getName().startsWith("main")
						&& !t.getName().startsWith("Cont")) {
					p("Got thread: " + t.toString() + ", name: " + t.getName());
					if (t.getName().startsWith("Swing")
							|| t.getName().startsWith("AWT")) {
						p("     interrupting....");
						try {
							t.interrupt();
						} catch (Throwable e) {
							// p("Got:"+e.getMessage());
						}
					}
				}
			}
		}

		super.close();
	}

	private void add(MenuItem it, String title, WindowOpener win) {
		// it.addSeparator();
		MenuItem child = it.addItem(title, null, win);
		addDesc(child, win);
	}

	private void add(MenuItem it, WindowOpener win) {
		// it.addSeparator();
		MenuItem child = it.addItem(win.getName(), null, win);
		addDesc(child, win);
	}

	private void addDesc(MenuItem it, WindowOpener win) {
		it.setDescription("<h2>" + win.getName() + "</h2>"
				+ win.getDescription());
	}

	private void createWindows() {
		int y = 50;
		int x = 1;

		windows = new ArrayList<WindowOpener>();
		dbWindow = new DbWindow(this, main,
				"Browse the experiment db to select a run", x, y);
		expWindow = new ExperimentWindow(this, main,
				"Pick folders with raw and results data", x, y);
		maskWindow = new BfMaskWindowCanvas(
				this,
				main,
				"View heat maps of well properties including live, bead, dud, ambigiuous etc",
				x, y);
		chipWindow = new WholeChipWindowCanvas(this, main,
				"View raw data of any flow and frame of the entire chip", x, y);
		scoreWindow = new ScoreMaskWindowCanvas(
				this,
				main,
				"Find perfect reads, sequence substrings or alignment patterns - and create masks from the results",
				x, y);
		tableWindow = new TableWindow(
				this,
				main,
				"View properties of selected wells (such as live, bead, dud) in a table",
				x + 600, y);
		geneWindow = new GeneWindow(
				this,
				main,
				"Find reads (and the flow nr the genome position maps to) based on genome location",
				x, y);
		ionoWindow = new IonogramWindow(
				this,
				main,
				"Display the ionogram of a selected well (from the .sff and 1.wells file)",
				x, y + 400);
		rawWindow = new RawWindow(
				this,
				main,
				"View the raw (or bg subtracted) signal for one well for <b>one or more flows</b>",
				x + 600, y);
		alignWindow = new AlignWindow(
				this,
				main,
				"View the sequence alignment of a selected well (from the .bam file)",
				x, y + 300);
		compWindow = new CompWindow(this, main,
				"Pick a block of a proton experiment", 100, 100);
		processWindow = new ProcessWindowCanvas(
				this,
				main,
				"View/Export timeseries of many wells in a region, find signals ",
				x, y + 300);
		fitWindow = new FitWindowCanvas(this, main,
				"Create masks with histograms based on the timeseries data", x,
				y);
		automateWindow = new AutomateWindow(this, main,
				"Compute mean signal for an entire area of a selected mask",
				x + 500, y);
		maskeditWindow = new MaskEditWindowCanvas(this, main,
				"View and edit masks, and export data to file based on a mask",
				x, y);

		helpWindow = new HelpWindow(this, main,
				"Help on all the components. Current version " + VERSION,
				x + 600, y);

		windows.add(dbWindow);
		windows.add(expWindow);
		windows.add(maskWindow);
		windows.add(chipWindow);
		windows.add(scoreWindow);
		windows.add(geneWindow);
		windows.add(tableWindow);
		windows.add(ionoWindow);
		windows.add(rawWindow);
		windows.add(alignWindow);
		windows.add(compWindow);
		windows.add(processWindow);
		windows.add(fitWindow);

		windows.add(automateWindow);
		windows.add(maskeditWindow);

		// main.addComponent(layout);
	}

	public ArrayList<WindowOpener> getMyWindows() {
		return windows;
	}

	public void showMessage(String title, String msg) {
		p(msg);
		main.showNotification(title, "<br>" + msg,
				Window.Notification.TYPE_HUMANIZED_MESSAGE);
	}

	public void showBottomMessage(String title, String msg) {
		p(msg);
		Notification not = new Notification(title, "<br>" + msg,
				Window.Notification.TYPE_HUMANIZED_MESSAGE);
		not.setPosition(Window.Notification.POSITION_CENTERED_BOTTOM);
		main.showNotification(not);
	}

	public void showLongMessage(String title, String msg) {
		p(msg);
		Notification not = new Notification(title, "<br>" + msg,
				Window.Notification.TYPE_HUMANIZED_MESSAGE);
		not.setDelayMsec(5000);
		// not.DELAY_FOREVER;
		main.showNotification(not);
	}

	public void showHelpMessage(String title, String msg) {
		msg += getHelpInfo();
		p(msg);
		Notification not = new Notification(title, "<br>" + msg,
				Window.Notification.TYPE_HUMANIZED_MESSAGE);
		not.setDelayMsec(10000);
		// not.DELAY_FOREVER;
		main.showNotification(not);
	}

	public String getHelpInfo() {
		String msg = "";
		if (this.exp != null) {
			msg += "<li>Current Experiment</li>";
			msg += "<ul>";
			msg += "<li>result name:" + exp.getResultsName() + "</li>";
			msg += "<li>results dir:" + exp.getResultsDirectory() + "</li>";
			msg += "<li>raw dir:" + exp.getRawDir() + "</li>";
			msg += "<li>chip type:" + exp.getChipType() + "</li>";
			msg += "<li>flow order:" + exp.getFlowOrder() + "</li>";
			msg += "<li>Library key:" + exp.getLibraryKey() + "</li>";
			msg += "<li>flows:" + exp.getNrFlows() + "</li>";
			msg += "<li>rows:" + exp.getNrrows() + "</li>";
			msg += "<li>cols:" + exp.getNrcols() + "</li>";
			msg += "</ul>";
		}
		if (this.maincont != null) {
			msg += "<li>Region Info</li>";
			msg += "<ul>";
			msg += "<li>Center coord:" + maincont.getAbsCenterAreaCoord()
					+ "</li>";
			msg += "<li>Raster size:" + maincont.getRasterSize() + "</li>";
			msg += "<li>Flow: " + maincont.getFlow() + "</li>";
			msg += "<li>File type:" + maincont.getFiletype() + "</li>";
			msg += "</ul>";
		}
		msg += "<li>Performance/App Info</li>";
		msg += "<ul>";
		msg += "<li>Application version " + VERSION + "</li>";
		
		msg += "<li>Session timeout " + getTimeout() + " minutes</li>";
		//msg += "<li>Session keepAlive " + sessionGuard.isKeptAlive() + "</li>";
	    
		msg += "<li>Current free memory in MB for app: "
				+ (int) perf.getFreeMb() + "</li>";
		msg += "<li>Total memory in MB availble to app: "
				+ (int) perf.getTotalMb() + "</li>";
		msg += "<li>Nr processors on server: " + (int) perf.nrProcessors()
				+ "</li>";
		int cpu = (int) perf.getCpuUsage();
		String s = "?";
		if (cpu > 0)
			s = "" + cpu * 100 + "%";
		msg += "<li>CPU usage: " + s + "</li>";
		msg += "</ul>";
		msg += "Please send an email to chantal.roth@lifetech.com if you have any questions, suggestions or problems<br>";

		return msg;
	}

	public void showTopMessage(String title, String msg) {
		showTopMessage(title, msg, false);
	}

	public void showTopMessage(String title, String msg, boolean islong) {
		p(msg);
		Notification n = new Notification(title, "<br>" + msg,
				Window.Notification.TYPE_HUMANIZED_MESSAGE);
		n.setPosition(Window.Notification.POSITION_TOP_LEFT);

		main.showNotification(n);
	}

	public void showTopRightMessage(String title, String msg) {
		p(msg);
		Notification n = new Notification(title, "<br>" + msg,
				Window.Notification.TYPE_HUMANIZED_MESSAGE);
		n.setPosition(Window.Notification.POSITION_TOP_RIGHT);
		main.showNotification(n);
	}

	public void showError(String title, String msg) {
		p(msg);
		main.showNotification(title, "<br>" + msg,
				Window.Notification.TYPE_ERROR_MESSAGE);
	}

	public void showError(WindowOpener win, String msg) {
		p(msg);
		main.showNotification(win.getName(), "<br>" + msg,
				Window.Notification.TYPE_ERROR_MESSAGE);
	}

	public void showWarning(String title, String msg) {
		p(msg);
		main.showNotification(title, "<br>" + msg,
				Window.Notification.TYPE_WARNING_MESSAGE);
	}

	public void showMessage(WindowOpener win, String msg) {
		p(msg);
		main.showNotification(win.getName(), "<br>" + msg,
				Window.Notification.TYPE_HUMANIZED_MESSAGE);
	}

	public HashMap<String, String> getParameters() {
		return parameters;
	}

	public String getParameters(String key) {
		if (parameters == null) {
			return null;
		} else {
			return parameters.get(key);
		}
	}

	public void handleParameters(Map par) {
		Iterator it = par.keySet().iterator();
		if (parameters != null) {
			return;
		}

		parameters = new HashMap<String, String>();
		// check if we have raw_dir and results_dir
		boolean hasraw = false;
		boolean hasres = false;
		for (; it.hasNext();) {
			String key = "" + it.next();
			String value = "" + ((String[]) par.get(key))[0];
			// p("Got value " + value + " for key " + key);
			parameters.put(key.toLowerCase(), value);

			if (key != null) {
				if (!key.equalsIgnoreCase("restartApplication"))
					p("Parameter " + key + "=" + value);
				if (key.equalsIgnoreCase("raw_dir"))
					hasraw = true;
				else if (key.equalsIgnoreCase("res_dir"))
					hasres = true;
			}
		}
		if (hasraw && hasres) {
			this.expWindow.open();
			expWindow.handleOk();
		} else
			main.showNotification("Select an analysis in the db",
					"<br/>Browse the database and select an analysis to view",
					Window.Notification.TYPE_HUMANIZED_MESSAGE);
		// dbWindow.open();

	}

	public CompositeExperiment getCompositeExperiment() {
		return comp;
	}

	public void setCompositeExperiment(CompositeExperiment comp) {
		this.comp = comp;

		compWindow.open();
		// exp = comp.getThumbnailsContext();
		// setWellCoordinate(new WellCoordinate(500, 500));
	}

	public ExplorerContext getExplorerContext() {
		return maincont;
	}

	public ExperimentContext getExperimentContext() {
		return exp;
	}

	public void setExperimentContext(ExperimentContext exp) {
		this.exp = exp;
		scoremask = null;
		if (exp != null) {
			int raster = 100;
			if (maincont != null) {
				raster = maincont.getRasterSize();
			}
			exp.setServerUrl(this.getServerName());
			maincont = new ExplorerContext(exp);
			maincont.setRasterSize(raster);
			maincont.setSpan(8);
		}
		if (exp == null) {
			expWindow.setDescription("Got no experiment context");
		} else {
			expWindow.setDescription("Got experiment with results folder<br>"
					+ exp.getResultsDirectory());
		}
	}

	public void showExperiment() {
		if (exp == null) {
			return;
		}
		exp.getWellContext();
		// set the default coord to about middle
		int mx = exp.getNrcols() / 2;
		int my = exp.getNrrows() / 2;
		if (mx == 0) {
			p("Got no rows/cols yet.. using 200");
			mx = 200;
			my = 200;
		}
		WellCoordinate mid = new WellCoordinate(mx, my);
		p("middle:" + mid);
		exp.getWellContext().setCoordinate(mid);
		WellSelection sel = new WellSelection(mx - 10, my - 10, mx + 10,
				my + 10);
		exp.getWellContext().setSelection(sel);
		if (exp.hasWells()) {
			if (tableWindow.isOpen())
				tableWindow.open();
			else
				tableWindow.close();
		}
		maskWindow.clear();
		this.scoreWindow.clear();
		this.processWindow.clear();
		// tabsheet.setSelectedTab(viewTab);
		if (!exp.doesExplogHaveBlocks()) {
			if (exp.hasBfMask()) {
				maskWindow.open();
			} else if (exp.hasDat()) {
				this.chipWindow.open();
			} else {
				this.showMessage("Missing files",
						"I see no bfmask.bin and no .dat files");
			}
			this.reopenProcess(false);
			this.reopenFit();
		}
		// if (exp.hasSff()) ionoWindow.open();
		// if (exp.hasBam()) alignWindow.open();
		// if(exp.getWellContext() != null &&
		// exp.getWellContext().getCoordinate()!=null) rawWindow.open();
	}

	public String getServer() {
		return server;
	}

	public void showExpContext(ExperimentContext context) {
		this.exp = context;
		logModule("Experiment", "open " + context.getResultsName());
		expWindow.setContext(context);
		expWindow.open();
	}

	public void reopenRaw() {
		if (!rawWindow.isOpen()) {
			// rawWindow.open();
		} else {
			rawWindow.reopen();
		}
	}

	public ProcessWindowCanvas getProcessWindow() {
		return (ProcessWindowCanvas) processWindow;
	}

	public void reopenProcess(boolean openwhenclosed) {
		if (!processWindow.isOpen()) {
			if (openwhenclosed)
				processWindow.open();
		} else {
			processWindow.reopen();
		}
		reopenMaskedit(false);
	}

	public void reopenFit() {
		if (!fitWindow.isOpen()) {
			// rawWindow.open();
		} else {
			fitWindow.reopen();
		}
	}

	public void reopenMaskedit(boolean open) {
		if (!maskeditWindow.isOpen()) {
			if (open)
				maskeditWindow.open();
		} else {
			maskeditWindow.reopen();
		}
	}

	public void setWellCoordinate(WellCoordinate coord) {
		setWellCoordinate(coord, true);
	}

	public void setWellCoordinate(WellCoordinate coord,
			boolean changeWellSelection) {
		if (exp == null) {
			return;
		}
		exp.makeRelative(coord);

		this.showMessage("Well selected", "Selecting well "
				+ (coord.getCol() + exp.getColOffset()) + "/"
				+ (coord.getRow() + exp.getRowOffset()));
		if (exp.doesExplogHaveBlocks() && this.comp != null) {
			DatBlock b = comp.findBlock(coord);
			if (b != null) {
				this.showMessage("Found block", "Selecting block " + b
						+ " for " + coord);
				exp = comp.getContext(b, false);
				this.setExperimentContext(exp);
				this.showExperiment();
			} else
				this.showMessage("No block", "Found no suitable block for "
						+ coord);
		}
		int mx = coord.getX();
		int my = coord.getY();
		getExperimentContext().getWellContext().setCoordinate(coord);
		if (changeWellSelection) {
			p("Setting maincont CENTER coord to " + coord
					+ " and recreating masks");
			maincont.setRelativeCenterAreaCoord(coord);
			this.maincont.createMasks();
			WellSelection sel = new WellSelection(mx - 10, my - 10, mx + 10,
					my + 10);
			exp.getWellContext().setSelection(sel);
		}

		if (ionoWindow.isOpen())
			ionoWindow.reopen();
		if (rawWindow.isOpen())
			rawWindow.reopen();
		if (changeWellSelection && tableWindow.isOpen())
			tableWindow.reopen();
		if (alignWindow.isOpen())
			alignWindow.reopen();
		if (processWindow.isOpen())
			processWindow.reopen();
		if (this.maskeditWindow.isOpen())
			this.maskeditWindow.reopen();
		// movieWindow.reopen();
	}

	public void setWellSelection(WellSelection sel) {
		if (exp == null) {
			return;
		}
		if (sel == null || sel.getAllWells() == null) {
			return;
		}
		// this.showMessage("Wells selected", "Selecting " + sel.toString());

		tableWindow.open();

	}

	public String getDbURL() {
		if (url != null && url.length() > 5) {
			if (!url.endsWith(":5432/iondb"))
				url = url + ":5432/iondb";
			return url;
		}
		if (url == null || url.length() < 5)
			url = getValue("db");
		if (url == null) {
			url = "localhost:5432/iondb";
		} else {
			url = url + ":5432/iondb";
		}
		if (server != null) {
			if (server.startsWith("10.25.3")
					&& !server.startsWith("10.25.3.240")) {
				// 240 is blackbird.ite
				url = "ioneast.ite:5432/iondb";
				p("Got ioneast db url");
			} else if (server.startsWith("10.33.106")) {
				url = "10.33.106.11:5432/iondb";
				p("Got carlsbad db url");
			} else if (server.startsWith("10.45.3")
					&& !server.startsWith("10.45.3.167")) {
				// 167 is blackbird.itw
				url = "10.45.3.90:5432/iondb";
				p("Got ion west db url");
			} else if (server.startsWith("172.18.241.195")) {
				url = "aruba.apg.per.na.ab.applera.net:5432/iondb";
				p("Got aruba db url");
			}
		}

		p("Got DB URL:" + url);
		return url;
	}

	public String getServerName() {
		String db = getDbURL();
		if (db.startsWith("localhost") || db.startsWith("127.0.0.1"))
			db = server;
		// 10.33.106.11:5432/iondb
		int col = db.lastIndexOf(":");
		if (col > 0)
			db = db.substring(0, col);
		if (!db.startsWith("http"))
			db = "http://" + db;
		return db;

	}

	public String getValue(String key) {
		if (key == null || parameters == null) {
			return null;
		}
		key = key.toLowerCase().trim();
		return parameters.get(key);
	}

	public void onRequestStart(HttpServletRequest request,
			HttpServletResponse response) {
		server = request.getLocalAddr();
		// p("Server address: " +server+"/"+request.getServerName());
		// p("Remote Adress: " + request.getRemoteAddr());
		remote = request.getRemoteAddr();
	}

	public void onRequestEnd(HttpServletRequest request,
			HttpServletResponse response) {
		// p(" End of request]");
	}

	public void logModule(String modulename, String function) {
		logStats("tsl_modulestats.csv", modulename + ", " + function,
				"module, function");
	}

	/** appens the current time, date, memory and cpu usage to a log file */
	private void logStats(String filename, String loginfo, String header) {
		if (logfolder == null) {
			p("Got no log folder");
			return;
		}

		java.util.Date d = new java.util.Date(System.currentTimeMillis());
		if (perf == null)
			perf = new PerformanceMonitor();

		Calendar cal = Calendar.getInstance();
		cal.setTime(d);

		String ds = cal.get(Calendar.YEAR) + "-"
				+ (cal.get(Calendar.MONTH) + 1) + "-"
				+ cal.get(Calendar.DAY_OF_MONTH);
		String info = ds + ", " + loginfo + ", " + remote + "\n";
		
		String name = logfolder.getAbsolutePath() ;
		name = FileTools.addSlashOrBackslash(name);
		
		File f = new File(name + filename);
		startLogStatsFile(f, header);
		FileTools.writeStringToFile(f, info, true);
		// p("Wrote to file "+f.getAbsolutePath()+":"+info);

	}

	private File getLogFolder() {

		File f = null;
		
		String base = SystemTool.getInfo("CATALINA_HOME");
		p("Catalina home: "+base);
		if (base != null && base.length()>1) {
			f = new File(base+"/logs");
			if (f.exists() && canWrite1(f))
				return f;		
		}
		f = new File("/var/log/tomcat6/");
		if (f.exists() && canWrite1(f))
			return f;
		f = new File("/var/log/tomcat7/");
		if (f.exists() && canWrite1(f))
			return f;
		f = new File("/results/log/");
		if (f.exists() && canWrite1(f))
			return f;
		f = new File("/tmp/");
		if (f.exists() && canWrite1(f))
			return f;

		f = new File("./");
		if (f.exists() && canWrite1(f))
			return f;
		
		f = new File("~/");
		if (f.exists() && canWrite1(f))
			return f;
		
		f = new File("/var/log/tomcat6/");
		return f;
	}

	private boolean canWrite2(File dir) {
		String path = dir.getAbsolutePath();
		path = FileTools.addSlashOrBackslash(path)+"*";
		try {
			AccessController.checkPermission(new FilePermission(path,
					"read,write"));
			return true;
		} catch (SecurityException e) {
			p("Cannot write to "+path);
			return false;
		}
	}

	private boolean canWrite1(File dir) {
		File sample = new File(dir.getAbsolutePath()+"/tmp.txt");
		try {
			sample.createNewFile();
			sample.delete();
			return true;
		} catch (Exception e) {
			p("Cannot create and delete "+sample+":"+e.getMessage());
		}
		return false;
	}

	private void startLogStatsFile(File f, String header) {
		if (f.exists())
			return;
		try {
			f.createNewFile();
		} catch (IOException e) {
		}
		String h = "date (year-month-day), " + header + ", address\n";
		FileTools.writeStringToFile(f, h, true);

	}

	private static void p(String msg) {

		System.out.println("TSVaadin: " + msg);
		Logger.getLogger(TSVaadin.class.getName()).log(Level.INFO, msg);
	}

	public void setDbURL(String string) {
		this.url = string;

	}

	public void openTable() {
		if (!this.tableWindow.isOpen()) {
			tableWindow.open();
		}
	}

	public void openAlign() {
		if (!this.alignWindow.isOpen()) {
			alignWindow.open();
		}
	}

	public void reopenTable() {
		if (!this.tableWindow.isOpen()) {
			// tableWindow.open();
		} else
			tableWindow.reopen();
	}

	public void reopenAlign() {
		if (!this.alignWindow.isOpen()) {
			alignWindow.open();

		} else {
			alignWindow.reopen();
		}
	}

	public void reopenAutomate() {
		if (!this.automateWindow.isOpen()) {
			// automateWindow.open();
		} else {
			automateWindow.reopen();
		}
	}

	public ScoreMask getScoreMask() {
		if (scoremask == null) {
			if (exp != null)
				scoremask = new ScoreMask(exp, exp.getWellContext());
		}
		return scoremask;
	}
}
