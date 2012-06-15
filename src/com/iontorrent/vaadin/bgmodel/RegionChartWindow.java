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
import com.iontorrent.torentscout.explorer.cube.FlowTypes;
import com.iontorrent.torentscout.explorer.cube.TypeEmpty;
import com.iontorrent.torentscout.explorer.cube.TypeRaw;
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
import com.iontorrent.wellmodel.WellCoordinate;
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
public class RegionChartWindow extends WindowOpener implements TaskListener,
		ProgressListener {

	private TSVaadin app;
	ExplorerContext maincont;
	ExperimentContext exp;
	TextArea text;
	String savefile;
	ProgressIndicator indicator;
	JFreeChart regionalcharts[];
	JFreeChart maskcharts[];
	RegionalThread rthread;
	MaskThread mthread;
	HorizontalLayout hor;
	AbstractLayout mainlayout;
	AbstractLayout chartlayout;
	AbstractLayout maskslayout;
	Select typesel;
	
	VerticalLayout ver;
	int bucket;
	int MAX = 10000;
	Label titlelabel;

	TextField tflow;
	TextField tcurflow;
	TextField tdelay;

	ArrayList<Integer> flows;
	ArrayList<Integer> delays;
	DataCubeLoader loader;
	Region region;

	FlowDataType type;
	ArrayList<FlowData> regionalresults[];
	
	ArrayList<FlowData> perwellresults[];
	FlowFilter[] filters;
	
	private Layout curlayout = Layout.Tabs;
	
	private static final String BASES = "ACGT";

	private enum Layout {
		Horizontal, Vertical, Tabs;
	}
	public RegionChartWindow(TSVaadin app, Window main, String description,
			int x, int y) {
		super("Regional per frame charts", main, description, x, y, 1200, 900);
		this.app = app;
		

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
		if (regional) {
			for (int i = 0; i < 4; i++) {
				try {
					f = File.createTempFile("export_"+what+"_" + exp.getId() + "_"+filters[i].getBase()
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
		else {
			if (this.maskcharts != null) {
				ArrayList<BitMask> masks = maincont.getMasks();
				for (int i = 0; i < this.maskcharts.length; i++) {
					try {
						f = File.createTempFile("export_"+masks.get(i).getName()+"_" + exp.getId() + "_"+filters[i].getBase()
								+ maincont.getAbsoluteCorner().getCol() + "_"
								+ maincont.getAbsoluteCorner().getRow(), ".png");
					} catch (IOException e) {
			
					}
					if (f != null) {
						f.deleteOnExit();
						if (maskcharts[i] != null) {
							this.exportPng(f, maskcharts[i]);
							FileDownloadResource down = new FileDownloadResource(f, this.app);
							app.getMainWindow().open(down, "_blank", 600, 600, 1);
						}
					}
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
				s = s.append(nl+filters[0].toString() + nl);
				if (regionalresults != null) {
					for (int i = 0; i < regionalresults.length; i++) {
						
						s = s.append(nl+filters[0].getBase() + nl);
						ArrayList<FlowData> flowdatalist = regionalresults[i];
						
						for (FlowData fdat : flowdatalist) {
							String sdat = Arrays.toString(fdat.getData());
							sdat = sdat.substring(1, sdat.length()-2);
							s = s.append(fdat.getFlow() + ", "+sdat	+ nl);
						}
					}
				}
			}
			else {
				if (this.perwellresults != null) {
					ArrayList<BitMask> masks = maincont.getMasks();
					for (int i = 0; i < perwellresults.length; i++) {
						
						s = s.append(nl+masks.get(i).getName() + nl);
						ArrayList<FlowData> flowdatalist = perwellresults[i];
						if (flowdatalist != null) {
							for (FlowData fdat : flowdatalist) {
								String sdat = Arrays.toString(fdat.getData());
								sdat = sdat.substring(1, sdat.length()-2);
								s = s.append(""+ fdat.getCol()+", "+fdat.getRow()+ ", "+sdat	+ nl);
							}
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
		for (FlowDataType type: FlowTypes.getTypes() ) {
			typesel.addItem(type);
			typesel.setValue(type);
		}
		hor.addComponent(typesel);
		addFlowAndDelaySelection(hor);

		Button compute = new Button("Load");
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
			maskslayout = new VerticalLayout();
		}
		else if (curlayout == Layout.Horizontal){
			mainlayout = new  VerticalLayout();
			chartlayout = new HorizontalLayout();
			maskslayout = new VerticalLayout();
		}
		else {
			mainlayout = new HorizontalLayout();
			chartlayout = new VerticalLayout();
			maskslayout = new VerticalLayout();
		}
		
		VerticalLayout vsmall = new VerticalLayout();
		vsmall.addComponent(export);
		vsmall.addComponent(options);
		vsmall.addComponent(help);
		ver.addComponent(mainlayout);
		mainlayout.addComponent(vsmall);
		mainlayout.addComponent(chartlayout);
		mainlayout.addComponent(maskslayout);

		if(this.regionalresults != null) {
			aftercomputeRegionalDone();
		}
		else this.startToCompute();
		
		if (this.maskcharts != null && maskcharts.length>0 && maskcharts[0] != null) {
			this.afterComputeMaskCharts();
		}
	}
	public void experimentChanged() {
		// overwrite if something important needs to happen
		// for instance if cachces have to be cleared.
		regionalresults = null;
		maskcharts = null;
		this.perwellresults = null;
	}
	
	public void doOptionAction() {
		VerticalLayout h = new VerticalLayout();
		String w = "80px";
			
		final OptionGroup group = new OptionGroup("Show GATC charts in which layout?");
		group.addItem("1) in separate tabs");
		group.addItem("2) in a vertical layout");
		group.addItem("3) in a horizontal layout");
		
		///
		OkDialog okdialog = new OkDialog(mywindow, "Options", h, new OkDialog.Recipient() {
			@Override
			public void gotInput(String name) {
				if (!name.equalsIgnoreCase("OK")) return;
				if (group.getValue() != null){
					String val = group.getValue().toString();
					if (val.startsWith("1")) curlayout = Layout.Tabs;
					else if (val.startsWith("3")) curlayout = Layout.Horizontal;
					else curlayout = Layout.Vertical;
				}
				
				
				reopen();
			}
		});
	}

	public void addFlowAndDelaySelection(HorizontalLayout h) {
		tflow = new TextField();
		tflow.setWidth("60px");
		// tflow.setHeight("25px");
		tflow.setImmediate(true);
		tflow.setDescription("Limit the charts by entering flow ranges");
		if (flows == null) {
			tflow.setValue("0-19");
		}
		
		h.addComponent(new Label("Flow Range(s):"));
		h.addComponent(tflow);

		tflow.addListener(new Property.ValueChangeListener() {

			public void valueChange(ValueChangeEvent event) {
				// addChart(chartTab);
				flows = parseFlow(tflow);
				p("Got flows " + flows);
				
			}
		});

		tdelay = new TextField();
		tdelay.setWidth("60px");
		tdelay.setDescription("You can limit the chart data by entering nuc delays if you like");
		// tflow.setHeight("25px");
		tdelay.setImmediate(true);
		if (delays == null) {
			delays = new ArrayList<Integer>();
		}
		
		h.addComponent(new Label("Nuc Delays:"));
		h.addComponent(tdelay);

		tdelay.addListener(new Property.ValueChangeListener() {

			public void valueChange(ValueChangeEvent event) {
				// addChart(chartTab);
				parseDelays();
				p("Got tdelay " + delays);
				
			}
		});

		tcurflow = new TextField();
		tcurflow.setWidth("60px");
		// tflow.setHeight("25px");
		tcurflow.setImmediate(true);
		tcurflow.setDescription("Flow for per well view - hit enter to reload just the detail view");
		if (flows == null || flows.size()<1) {
			tcurflow.setValue("0");
		}
		else tflow.setValue(flows.get(0));
		
		h.addComponent(new Label("Flow for detail:"));
		h.addComponent(tcurflow);

		tcurflow.addListener(new Property.ValueChangeListener() {

			public void valueChange(ValueChangeEvent event) {
				// addChart(chartTab);
				startcomputePerMaskView();
				
			}

		});
		
	}
	

	public ArrayList<Integer> parseFlow(TextField tflow) {
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

	private void startToCompute() {
		if (typesel.getValue() == null)type = new TypeEmpty();
		else type = (FlowDataType) typesel.getValue();
		if (type == null)
			type = new TypeEmpty();
		
		region = maincont.getRegion();
		p("Got region "+region+" for center coord: "+maincont.getRelativeCenterAreaCoord());
		filters = new FlowFilter[4];
		regionalresults = new ArrayList[4];
		regionalcharts = new JFreeChart[4];
		
		chartlayout.removeAllComponents();
		maskslayout.removeAllComponents();
		
		flows = this.parseFlow(tflow);
		this.parseDelays();
		for (int i = 0; i < filters.length; i++) {
			FlowFilter filter = new FlowFilter(exp, region);
			filters[i] = filter;
			filter.andByBase(BASES.charAt(i));
			if (flows != null && flows.size() > 0)
				filter.andByFlows(flows);
			if (delays != null && delays.size() > 0)
				filter.andByDelays(delays);
		}
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
		
		for (int i = 0; i < filters.length; i++) {
			FlowFilter filter = filters[i];
			setProgressValue(i*25);
			p("Loading data "+i+" with filter:"+filter);			
			ArrayList<FlowData> res = loader.loadData(type, filter, null);
			regionalresults[i] = res;
			p("Done loading data "+i);
			
		}
		
		// get regional size, use as raster size
		int size = loader.getEmptyTrace().getRegionOffsetX();
		region = maincont.getRegion();
		p("Got region "+region);
//		if (size != maincont.getRasterSize()) {
//			p("Setting raster size to "+size);
//			maincont.setRasterSize(size);
//			maincont.setRelativeCornerAreaCoord(new WellCoordinate(region.getRegioncol(), region.getRegioncol()));
//			maincont.clearData();
//		}
		maincont.setRasterSize(size);
		maincont.setRelativeCornerAreaCoord(new WellCoordinate(region.getRegioncol(), region.getRegionrow()));
		maincont.clearData();
		
		setProgressValue(100);
		p("Computation done");

	}
	

	private void startcomputePerMaskView() {
		ArrayList<Integer> curflows = parseFlow(tcurflow);
		p("Got cur flows " + curflows);
		if (curflows != null && curflows.size()>0) {
			int flow = curflows.get(0);
			startToComputeMasks(flow);
		}
	}
	private void startToComputeMasks(int flow) {
		
		indicator = new ProgressIndicator(new Float(0.0));
		indicator.setHeight("40px");

		app.showMessage("Loading data", "Computing per well data for masks");
		indicator.setDescription("Loading per well data for masks");
		indicator.setPollingInterval(5000);
		hor.addComponent(indicator);
		mthread = new MaskThread(this, flow);
		mthread.execute();

	}
	private void computeMaskCharts(int curflow) {
		loader = app.getCubeLoader();
		ArrayList<BitMask> masks = maincont.getMasks();
		int nr = masks.size();
		TypeRaw type = new TypeRaw();
		perwellresults = new ArrayList[nr];
		maskcharts = new JFreeChart[nr];
		maincont.setFlow(curflow);
		maincont.clearData();
		for (int i = 0; i < nr; i++) {
			p("Computing for mask "+masks.get(i).getName());
			maincont.setSignalMask(masks.get(i));
			ArrayList<FlowData> res = loader.loadData(type, null, this);
			perwellresults[i] = res;			
			FlowMaskFramePanel pan = new FlowMaskFramePanel(maincont,res, masks.get(i));			
			maskcharts[i] = pan.createChart();
		}		
	}
	private void afterComputeMaskCharts() {
		
		ArrayList<BitMask> masks = maincont.getMasks();
		int nr = masks.size();
		int wells = 0;
		for (int i = 0; i <nr; i++) {
			if (this.perwellresults[i] != null) {
				wells += perwellresults[i].size();
			}
		}
		titlelabel.setDescription("(region: "+maincont.getRegion()+", dx/dy="+region.getRegionOffsetX()+"/"+region.getRegionOffsetY()+")");
		maskslayout.removeAllComponents();
		maskslayout.addComponent(titlelabel);
		TabSheet tabsheet = new TabSheet();
		tabsheet.setImmediate(true);
		if (curlayout == Layout.Tabs) maskslayout.addComponent(tabsheet);
		
		p("Computation per mask, adding charts: "+nr);
		boolean ok = false;
		for (int i = 0; i <nr; i++) {
			BitMask mask = masks.get(i);
			
			if (maskcharts != null && maskcharts[i] != null && masks.get(i) != null) {
				final XYPlot plot = (XYPlot) this.maskcharts[i].getPlot();
				ok = true;
				final JFreeChartWrapper wrapper = new JFreeChartWrapper(maskcharts[i],
						JFreeChartWrapper.RenderingMode.PNG);
				wrapper.setWidth("550px");
				wrapper.setImmediate(true);
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
				if (curlayout == Layout.Tabs) {
					VerticalLayout myTabRoot = new VerticalLayout();
					myTabRoot.addComponent(wrapper);	
					myTabRoot.setImmediate(true);
					// Add the component to the tab sheet as a new tab.
					tabsheet.addTab(myTabRoot);				 
					tabsheet.getTab(myTabRoot).setCaption(mask.getName());
				}
				else maskslayout.addComponent(wrapper);
			}
		}
		if (!ok) {
			app.showLongMessage("No Charts","I was not able to compute the per mask charts for some reason");
		}

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

	private class MaskThread extends Task {

		int flow;
		public MaskThread(TaskListener list, int flow) {
			super(list);
			this.flow = flow;
		}

		@Override
		public boolean isSuccess() {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		protected Void doInBackground() {
			try {
				computeMaskCharts(flow);

			} catch (Exception e) {
				err("Got an error loading cube data for masks and flow "
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
		else {
			this.afterComputeMaskCharts();
			mthread = null;
		}
		
	}

	public void aftercomputeRegionalDone() {

		titlelabel = new Label("Mean results for region with lower left corner "+maincont.getRegion().toString(exp));
		chartlayout.removeAllComponents();
		chartlayout.addComponent(titlelabel);
		TabSheet tabsheet = new TabSheet();
		tabsheet.setImmediate(true);
		if (curlayout == Layout.Tabs) chartlayout.addComponent(tabsheet);
		
		boolean ok = false;
		p("Computation done, adding charts");
		for (int i = 0; i < BASES.length(); i++) {
			
			AbstractComponent chartcomp = createRegionalChartComponent(i);
			if (chartcomp != null) {
				ok = true;
				if (curlayout == Layout.Tabs) {
					VerticalLayout myTabRoot = new VerticalLayout();
					myTabRoot.addComponent(chartcomp);				 
					// Add the component to the tab sheet as a new tab.
					myTabRoot.setImmediate(true);
					tabsheet.addTab(myTabRoot);				 
					tabsheet.getTab(myTabRoot).setCaption(""+BASES.charAt(i));
				}
				else chartlayout.addComponent(chartcomp);
			}
		}
		if (!ok) {
			app.showLongMessage("No Charts","I was not able to compute the per region charts for some reason <br>(maybe some data files are missing?)");
		}
		startcomputePerMaskView();
		
		// ver.addComponent(chart1);

	}

	public AbstractComponent createRegionalChartComponent(int i) {
		
		p("Creating chart "+i);
		FlowNucFramePanel pan = new FlowNucFramePanel(maincont,
				this.regionalresults[i]);

		
		regionalcharts[i] = pan.createChart();
		if (regionalcharts[i] == null) return null;
		
		final XYPlot plot = (XYPlot) regionalcharts[i].getPlot();

		final JFreeChartWrapper wrapper = new JFreeChartWrapper(regionalcharts[i],
				JFreeChartWrapper.RenderingMode.PNG);
		wrapper.setWidth("550px");
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
		Logger.getLogger(RegionChartWindow.class.getName()).log(Level.SEVERE,
				msg, ex);
	}

	private static void err(String msg) {
		Logger.getLogger(RegionChartWindow.class.getName()).log(Level.SEVERE,
				msg);
	}

	private static void warn(String msg) {
		Logger.getLogger(RegionChartWindow.class.getName()).log(Level.WARNING,
				msg);
	}

	private static void p(String msg) {
		 System.out.println("RegionChartWindow: " + msg);
		Logger.getLogger(RegionChartWindow.class.getName())
				.log(Level.INFO, msg);
	}

}
