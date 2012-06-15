/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.bgmodel;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;

import com.iontorrent.background.Region;
import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.rawdataaccess.pgmacquisition.DataAccessManager;
import com.iontorrent.rawdataaccess.wells.BitMask;
import com.iontorrent.threads.Task;
import com.iontorrent.threads.TaskListener;
import com.iontorrent.torentscout.explorer.cube.DataCubeLoader;
import com.iontorrent.torentscout.explorer.cube.FlowData;
import com.iontorrent.torentscout.explorer.cube.FlowDataType;
import com.iontorrent.torentscout.explorer.cube.FlowFilter;
import com.iontorrent.torentscout.explorer.cube.FlowMaskFramePanel;
import com.iontorrent.torentscout.explorer.cube.FlowNucFramePanel;
import com.iontorrent.torentscout.explorer.cube.FlowPropertyData;
import com.iontorrent.torentscout.explorer.cube.FlowPropertyPanel;
import com.iontorrent.torentscout.explorer.cube.FlowTypes;
import com.iontorrent.torentscout.explorer.cube.TypeEmpty;
import com.iontorrent.torentscout.explorer.cube.TypeRaw;
import com.iontorrent.torentscout.explorer.cube.TypeStep;
import com.iontorrent.torrentscout.explorer.ExplorerContext;
import com.iontorrent.utils.ErrorHandler;
import com.iontorrent.utils.ProgressListener;
import com.iontorrent.utils.StringTools;
import com.iontorrent.utils.io.FileTools;
import com.iontorrent.vaadin.TSVaadin;
import com.iontorrent.vaadin.utils.ExportTool;
import com.iontorrent.vaadin.utils.FileDownloadResource;
import com.iontorrent.vaadin.utils.JFreeChartWrapper;
import com.iontorrent.vaadin.utils.OkDialog;
import com.iontorrent.vaadin.utils.OptionsDialog;
import com.iontorrent.vaadin.utils.WindowOpener;
import com.iontorrent.wellalgorithms.WellAlgorithm;
import com.iontorrent.wellmodel.RasterData;
import com.iontorrent.wellmodel.WellFlowDataResult;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.event.MouseEvents.ClickListener;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.Select;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * 
 * @author Chantal Roth chantal.roth@lifetech.com
 */
public class RegionPropertyChartWindow extends WindowOpener implements TaskListener,
		ProgressListener {

	private TSVaadin app;
	ExplorerContext maincont;
	ExperimentContext exp;
	TextArea text;
	String savefile;
	ProgressIndicator indicator;
	JFreeChart regionalcharts[];
	RegionalThread rthread;
	HorizontalLayout hor;
	AbstractLayout mainlayout;
	AbstractLayout chartlayout;
	Select typesel;
	FlowFilter filter ;
	VerticalLayout ver;
	int bucket;
	int MAX = 10000;
	Label titlelabel;
	TextField tdelay;
	TextField tbase;
	DataCubeLoader loader;
	Region region;
	ArrayList<Integer> delays;
	FlowDataType type;
	ArrayList<FlowPropertyData> regionalresults[];
	private boolean byBase;
	private boolean byDelay;
	
	private Layout curlayout = Layout.Tabs;
	
	
	private enum Layout {
		Horizontal, Vertical, Tabs;
	}
	public RegionPropertyChartWindow(TSVaadin app, Window main, String description,
			int x, int y) {
		super("Regional flow property chart", main, description, x, y, 750, 650);
		this.app = app;
		byBase = true;
		byDelay = false;

	}

	@Override
	public void openButtonClick(Button.ClickEvent event) {
		if (app.getExperimentContext() == null) {
			appwindow.showNotification("No Experiment Selected",
					"<br/>Please open an experiment first",
					Window.Notification.TYPE_WARNING_MESSAGE);
			return;
		}
		
		exp = app.getExperimentContext();
		maincont = app.getExplorerContext();
		maincont.setAbsCenterAreaCoord(exp.getWellContext()
				.getAbsoluteCoordinate());
		super.openButtonClick(event);
	}

	@Override
	public void windowOpened(final Window mywindow) {
		p("====================== windowOpened  ===============================");
		createGui();

	}

	private void saveChart(boolean regional) {
		File f = null;
		String what = "regional";
		if (!regional) what="per_well";
		if (type== null ) type = new TypeStep();
		if (regional) {
			for (int i = 0; i < regionalcharts.length; i++) {
				try {
					f = File.createTempFile("export_"+what+"_" + exp.getId() + "_"+type.getName()+"_"+i
							+ maincont.getAbsoluteCorner().getCol() + "_"
							+ maincont.getAbsoluteCorner().getRow(), ".png");
				} catch (IOException e) {
		
				}
				if (f != null) {
					f.deleteOnExit();
					this.exportPng(f, regionalcharts[i]);
					FileDownloadResource down = new FileDownloadResource(f, this.app);
					app.getMainWindow().open(down, "_blank", 600, 600, 1);
				}
			}
		}
		
	}

	private void saveFile(boolean ask, boolean regional) {
		File f = null;
		String what = "regional";
		if (!regional) what="per_well";
		try {
			f = File.createTempFile("export_"+what+"_" + exp.getId() + "_"
					+ maincont.getAbsoluteCorner().getCol() + "_"
					+ maincont.getAbsoluteCorner().getRow(), ".csv");
		} catch (IOException e) {

		}
		
		if (f != null) {
			p("Writing data to temp file :" + f.getAbsolutePath());
			f.deleteOnExit();
			StringBuffer s = new StringBuffer("");
			String nl = "\n";
			s = s.append("Data from " + exp.getResultsDirectory());
			s = s.append("Report link, " + exp.getServerUrl()
					+ exp.getReportLink() + nl);

			s = s.append("Data of area around center "
					+ maincont.getAbsoluteCorner() + ", size "
					+ maincont.getRasterSize() + nl);
			s = s.append("Type " + type.getName() + nl);
			s = s.append("Region: " + region.toString() + nl);

		
			FileTools.writeStringToFile(f, s.toString(), false);
			// export
			s = new StringBuffer();
			if (regional) {
				
				if (regionalresults != null) {
					for (int i = 0; i < regionalresults.length; i++) {
						ArrayList<FlowPropertyData> flowdatalist = regionalresults[i];
						
						for (FlowPropertyData fdat : flowdatalist) {
							String sdat = Arrays.toString(fdat.getData());
							sdat = sdat.substring(1, sdat.length()-2);
							s = s.append(fdat.getType().getName() + ", "+sdat	+ nl);
						}
					}
				}
			}
			
			FileTools.writeStringToFile(f, s.toString(), true);
			app.showMessage("Export done", "About to download result " + f
					+ " (" + f.length() / 1000000 + " MB)...");
			FileDownloadResource down = new FileDownloadResource(f, this.app);
			app.getMainWindow().open(down, "_blank", 600, 600, 1);
		}
	}

	private void createGui() {

		ver = new VerticalLayout();
		hor = new HorizontalLayout();
		mywindow.addComponent(ver);
		ver.addComponent(hor);

		typesel = new Select();
		typesel.setInvalidAllowed(false);
		typesel.setNullSelectionAllowed(false);
		typesel.setImmediate(true);
		typesel.setDescription("The type of regional data to view");
		for (FlowDataType type: FlowTypes.getFlowPropertyTypes() ) {
			typesel.addItem(type);
			typesel.setValue(type);
		}
		hor.addComponent(typesel);
	
		
		tdelay = new TextField();
		tdelay.setWidth("60px");
		tdelay.setDescription("You can limit the chart data by entering nuc delays if you like");
		// tflow.setHeight("25px");
		tdelay.setImmediate(true);
		if (delays == null) {
			delays = new ArrayList<Integer>();
		}
		
		hor.addComponent(new Label("Nuc Delays:"));
		hor.addComponent(tdelay);

		tdelay.addListener(new Property.ValueChangeListener() {

			public void valueChange(ValueChangeEvent event) {
				// addChart(chartTab);
				parseDelays();
				p("Got tdelay " + delays);
				
			}
		});
		
		tbase = new TextField();
		tbase.setWidth("40px");
		tbase.setDescription("You can limit the chart data by the bases(s), such as GA");
		// tflow.setHeight("25px");
		tbase.setImmediate(true);
		tbase.setValue("ACGT");
		hor.addComponent(new Label("Bases(s):"));
		hor.addComponent(tbase);

		
		Button compute = new Button("Compute");
		compute.setImmediate(true);
		hor.addComponent(compute);

		compute.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				startToCompute();
			}

		});

		NativeButton help = new NativeButton();
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
		export.setDescription("Save data to Excel format");
		export.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				doExportAction();
			}

		});
		final NativeButton options = new NativeButton();
		options.setStyleName("nopadding");
		options.setIcon(new ThemeResource("img/configure-3.png"));
		options.setDescription("Options (such as on BG subtraction)");
		options.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				doOptionAction();
			}

		});
		if (curlayout == Layout.Vertical) {
			mainlayout = new HorizontalLayout();
			chartlayout = new VerticalLayout();			
		}
		else if (curlayout == Layout.Horizontal){
			mainlayout = new  VerticalLayout();
			chartlayout = new HorizontalLayout();
			
		}
		else {
			mainlayout = new HorizontalLayout();
			chartlayout = new VerticalLayout();
			
		}
		
		VerticalLayout vsmall = new VerticalLayout();
		vsmall.addComponent(export);
		vsmall.addComponent(options);
		vsmall.addComponent(help);
		ver.addComponent(mainlayout);
		mainlayout.addComponent(vsmall);
		mainlayout.addComponent(chartlayout);
	
		if(this.regionalresults != null) {
			aftercomputeRegionalDone();
		}
		else this.startToCompute();
		
	}
	public ArrayList<Integer> parseDelays() {
		if (tdelay == null) {
			return delays;
		}
		String s = "" + tdelay.getValue();
		if (s != null) {
			delays = StringTools.parseInts(s);
			p("parsed delays: " + delays);
		}
		return delays;
	}

	public void experimentChanged() {
		// overwrite if something important needs to happen
		// for instance if cachces have to be cleared.
		regionalresults = null;
		
	}
	
	public void doOptionAction() {
		VerticalLayout h = new VerticalLayout();
		String w = "80px";
		
//		final OptionGroup group1 = new OptionGroup("Group results in chart...");
//		group1.addItem("1) by base (ACGT)");
//		group1.addItem("2) by nuc wait (nr flows between same base call)");
//		h.addComponent(group1);
//	
//		if (group1.getValue() != null){
//			String val = group1.getValue().toString();
//			if (val.startsWith("1")) byBase = true;
//			else byBase = false;
//			
//			if (val.startsWith("2")) byDelay = true;
//			else byDelay = false;
//			
//		}
		final OptionGroup group = new OptionGroup("Show GATC charts in which layout?");
		group.addItem("1) in separate tabs");
		group.addItem("2) in a vertical layout");
		group.addItem("3) in a horizontal layout");
		
		h.addComponent(group);
    
		///
		OkDialog okdialog = new OkDialog(mywindow, "Options", h, new OkDialog.Recipient() {
			@Override
			public void gotInput(String name) {
				if (name == null || !name.equalsIgnoreCase("OK")) return;
				if (group.getValue() != null){
					String val = group.getValue().toString();
					if (val.startsWith("1")) curlayout = Layout.Tabs;
					else if (val.startsWith("3")) curlayout = Layout.Horizontal;
					else curlayout = Layout.Vertical;
				}
//				if (group1.getValue() != null){
//					String val = group1.getValue().toString();
//					if (val.startsWith("1")) byBase = true;
//					else byBase = false;
//					
//					if (val.startsWith("2")) byDelay = true;
//					else byDelay = false;
//					
//				}
				reopen();
			}
		});
	}

	

	private void startToCompute() {
		if (typesel.getValue() == null)type = new TypeStep();
		else type = (FlowDataType) typesel.getValue();
		type = (FlowDataType) typesel.getValue();
		if (type == null)
			type = new TypeStep();
		if (region == null)
			region = new Region(maincont.getRelativeCenterAreaCoord());
		
		
		int nrcharts = 3;
		int nrtypes = 1;
		
		filter = new FlowFilter(exp, region);
		filter.andByBases(""+tbase.getValue());
		this.parseDelays();
		if (delays != null && delays.size() > 0)
			filter.andByDelays(delays);
		
		regionalresults = new ArrayList[nrtypes];
		regionalcharts = new JFreeChart[nrcharts*nrtypes];			
		
		indicator = new ProgressIndicator(new Float(0.0));
		indicator.setHeight("40px");

		indicator.setDescription("Loading cube data");
		indicator.setPollingInterval(5000);
		hor.addComponent(indicator);
		rthread = new RegionalThread(this);
		rthread.execute();

	}

	private void computeRegional() {
		loader = app.getCubeLoader();		
		
		// set raster size in maincont to smaller value so that it matches the regional thing
		
		setProgressValue(1);
		ArrayList<FlowPropertyData> res = loader.loadFlowPropertyData(type, this);
		regionalresults[0] = res;
		
		
		// get regional size, use as raster size
		int size = loader.getEmptyTrace().getRegionOffsetX();
		if (size != maincont.getRasterSize()) {
			p("Setting raster size to "+size);
			maincont.setRasterSize(size);
			maincont.clearData();
		}
		
		setProgressValue(100);
		p("Computation done");

	}
	
	private class RegionalThread extends Task {

		public RegionalThread(TaskListener list) {
			super(list);
		}

		@Override
		public boolean isSuccess() {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		protected Void doInBackground() {
			try {
				computeRegional();

			} catch (Exception e) {
				err("Got an error loading cube data "
						+ ErrorHandler.getString(e));
			}
			return null;

		}

	}

	
	@Override
	public void taskDone(Task task) {
		
		app.showMessage("Loading data done", "Loading cube data done");
		if (indicator != null) {
			hor.removeComponent(indicator);
		}
		if (task instanceof RegionalThread) {
			aftercomputeRegionalDone();
			rthread = null;
		}
		
		
	}

	public void aftercomputeRegionalDone() {

		titlelabel = new Label("Mean results for region "+region.toString());
		chartlayout.removeAllComponents();
		chartlayout.addComponent(titlelabel);
		TabSheet tabsheet = new TabSheet();
		tabsheet.setImmediate(true);
		if (curlayout == Layout.Tabs) chartlayout.addComponent(tabsheet);
		
		boolean ok = false;
		p("Computation done, adding charts");
		
		for (int resultid = 0; resultid < this.regionalresults.length; resultid++) {
			for (int charttype = 0; charttype < 3; charttype++) {
				AbstractComponent chartcomp = createRegionalChartComponent(type,charttype,  resultid);
				if (chartcomp != null) {
					ok = true;
					if (curlayout == Layout.Tabs) {
						VerticalLayout myTabRoot = new VerticalLayout();
						myTabRoot.addComponent(chartcomp);				 
						// Add the component to the tab sheet as a new tab.
						myTabRoot.setImmediate(true);
						tabsheet.addTab(myTabRoot);
						String what = " ";
						if (charttype ==0) what = " by base";
						else if (charttype ==2) what = " by nuc wait";
						if (charttype ==1) what = " by nuc wait and base";
						tabsheet.getTab(myTabRoot).setCaption(""+type.getName()+ what);
					}
					else chartlayout.addComponent(chartcomp);
				}
			}
		}
		if (!ok) {
			app.showLongMessage("No Charts","I was not able to compute the per region charts for some reason <br>(maybe some data files are missing?)");
		}
		
	}

	public AbstractComponent createRegionalChartComponent(FlowDataType type, int chartid, int resultid) {
		
		//int i = 0;
		p("Creating chart "+chartid+"/"+resultid);
		
		FlowPropertyPanel pan = new FlowPropertyPanel(maincont,
				this.regionalresults[resultid], filter );
		if (chartid == 0){
			pan.setByBase(true);
			pan.setByDelay(false);
		}
		else if (chartid ==2) {
			pan.setByBase(false);
			pan.setByDelay(true);
		}
		else {
			pan.setByBase(true);
			pan.setByDelay(true);
		}
		
		int nrres = this.regionalresults.length;
		int chartpos = nrres*resultid+chartid;
		regionalcharts[chartpos] = pan.createChart();
		if (regionalcharts[chartpos] == null) return null;
		
		final XYPlot plot = (XYPlot) regionalcharts[chartpos].getPlot();
		final JFreeChartWrapper wrapper = new JFreeChartWrapper(regionalcharts[chartpos],
				JFreeChartWrapper.RenderingMode.PNG);
		wrapper.setWidth("650px");
		wrapper.setHeight("450px");

		wrapper.addListener(new ClickListener() {

			@Override
			public void click(com.vaadin.event.MouseEvents.ClickEvent event) {
				Point p = new Point(event.getRelativeX(), event.getRelativeY());
				ChartRenderingInfo info = wrapper.getInfo(550, 450);
				PlotRenderingInfo plotInfo = info.getPlotInfo();

				double x = plot.getDomainAxis().java2DToValue(p.getX(),
						plotInfo.getDataArea(), plot.getDomainAxisEdge());

				double y = plot.getRangeAxis().java2DToValue(p.getY(),
						plotInfo.getDataArea(), plot.getRangeAxisEdge());
				String msg = "Got click at " + p;
				msg += "<br>x/y= " + x + "/" + y;

				app.showMessage("Click", msg);

			}

		});

		return wrapper;
	}
	
	
	private void exportPng(File f, JFreeChart chart) {
		// Draw png to bytestream
		try {
			ChartUtilities.saveChartAsPNG(f, chart, 600, 400, null);
		} catch (IOException e) {
			err("Could not save chart");
			app.showError("Save Chart", "Could not save chart to " + f);
		}
	}

	public void doExportAction() {
		String options[] =  {"... save regional data points", 
				"... save regional chart image",
				"... save per well data points", 
				"... save per well chart image",
				"... raw data, ionograms, alignments"};
		
		OptionsDialog input = new OptionsDialog(mywindow,
				"What would you like to export?", "Export...",options,0,			
				new OptionsDialog.Recipient() {

					@Override
					public void gotInput(final int selection) {
						if (selection < 0)
							return;
						// / do the search
						if (selection == 0) {
							saveFile(true, true);
						} else if (selection == 1) {
							saveChart(true);
						} else if (selection == 2) {
							saveFile(true, false);
						} else if (selection == 3) {
							saveChart(false);							
						} else {
							ExportTool export = new ExportTool(app, mywindow,
									null, hor);
							export.doExportAction();

						}
					}

				});
	}

	@Override
	public void setMessage(String msg) {
		indicator.setDescription(msg);
	}

	@Override
	public void stop() {
	}

	public void setProgressValue(int p) {
		if (indicator != null)
			indicator.setValue(((double) p / 100.0d));
		// progress.setValue("Creating composite image: " + p + "%");
	}

	public String getHelpMessage() {
		String msg = "<ul>";
		msg += "<li>Loads regional data (such as empty traces) </li>";
		msg += "<li>Filter by list of flows, and also filter by nuc waits (combined with flows)</li>";
		msg += "<li>Export image or chart data</li>";
		msg += "</ul>";
		return msg;
	}

	private static void err(String msg, Exception ex) {
		Logger.getLogger(RegionPropertyChartWindow.class.getName()).log(Level.SEVERE,
				msg, ex);
	}

	private static void err(String msg) {
		Logger.getLogger(RegionPropertyChartWindow.class.getName()).log(Level.SEVERE,
				msg);
	}

	private static void warn(String msg) {
		Logger.getLogger(RegionPropertyChartWindow.class.getName()).log(Level.WARNING,
				msg);
	}

	private static void p(String msg) {
		 System.out.println("RegionPropertyChartWindow: " + msg);
		Logger.getLogger(RegionPropertyChartWindow.class.getName())
				.log(Level.INFO, msg);
	}

}
