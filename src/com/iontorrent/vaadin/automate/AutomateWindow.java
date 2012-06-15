/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.automate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.iontorrent.acqview.MultiAcqPanel;
import org.iontorrent.acqview.MultiFlowPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;

import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.rawdataaccess.pgmacquisition.DataAccessManager;
import com.iontorrent.rawdataaccess.pgmacquisition.RawType;
import com.iontorrent.rawdataaccess.wells.BitMask;
import com.iontorrent.threads.Task;
import com.iontorrent.threads.TaskListener;
import com.iontorrent.torrentscout.explorer.ExplorerContext;
import com.iontorrent.utils.ErrorHandler;
import com.iontorrent.utils.ProgressListener;
import com.iontorrent.utils.StringTools;
import com.iontorrent.vaadin.TSVaadin;
import com.iontorrent.vaadin.utils.CoordSelect;
import com.iontorrent.vaadin.utils.ExportTool;
import com.iontorrent.vaadin.utils.FileDownloadResource;
import com.iontorrent.vaadin.utils.OkDialog;
import com.iontorrent.vaadin.utils.OptionsDialog;
import com.iontorrent.vaadin.utils.WindowOpener;
import com.iontorrent.wellalgorithms.NearestNeighbor;
import com.iontorrent.wellalgorithms.WellAlgorithm;
import com.iontorrent.wellmodel.RasterData;
import com.iontorrent.wellmodel.WellContext;
import com.iontorrent.wellmodel.WellCoordinate;
import com.iontorrent.wellmodel.WellFlowDataResult;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.JFreeChartWrapper;
import com.vaadin.ui.Label;
import com.vaadin.ui.NativeButton;
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
public class AutomateWindow extends WindowOpener implements Button.ClickListener, Property.ValueChangeListener, ProgressListener, TaskListener {

	private HorizontalLayout tophor;
	private TSVaadin app;
	// private Embedded chart;
	MultiAcqPanel cview;
	String baselist;
	RawType filetype;
	org.iontorrent.acqview.MultiFlowPanel cmulti;
	private WellContext context;
	private ExplorerContext maincont;
	private ExperimentContext exp;
	private CoordSelect coordsel;
	private WellCoordinate coord;
	private TextField tflow;
	// private int flow;
	ArrayList<Integer> flows;
	ProgressIndicator indicator;
	boolean dosubtract;
	VerticalLayout optionsTab ;
	RasterData maindata;
	DataAccessManager manager;
	private int subtractflow = -1;
	JFreeChart singlechart;
	JFreeChart flowchart;
	WellFlowDataResult subtract;
	WellFlowDataResult multiflow;
	VerticalLayout dataTab;
	private RawType type;
	private VerticalLayout chartTab;
	private VerticalLayout multichartTab;
	private Select typeselect;
	private TabSheet tabsheet;

	private Embedded embeededmultichart;
	private Embedded embeededflowchart;

	TextArea area;
	TextArea areamulti;

	private CheckBox boxsub;
	private TextField textsubtract;

	WellFlowDataResult[] results;
	int[] flownr;
	boolean[] taskdone;
	AutomateTask task;
	
	public AutomateWindow(TSVaadin app, Window main, String description, int x, int y) {
		super("Automate (mean signal)", main, description, x, y, 600, 500);
		this.app = app;
	}
	@Override
	public void openButtonClick(Button.ClickEvent event) {
		if (!super.checkExperimentOpened()) return;
		this.exp = app.getExperimentContext();
		if (app.getExperimentContext().getWellContext() == null) {
			appwindow.showNotification("No Location Selected", "<br/>Please pick an area", Window.Notification.TYPE_WARNING_MESSAGE);
			return;
		}

		if (exp.getWellContext().getCoordinate() == null) {
			coord = exp.getWellContext().getCoordinate();
			if (coord == null) {
				p("Coord is null, creating a new coord");
				coord = new WellCoordinate(100, 100);
				app.setWellCoordinate(coord);
			}
		}
		app.reopenMaskedit(true);
		exp = app.getExperimentContext();
		maincont = app.getExplorerContext();
		RasterData data = maincont.getData();

		if (data == null) {
			app.reopenProcess(true);
		}
		
		super.openButtonClick(event);
	}
	@Override
	public void windowOpened(Window mywindow) {
	
		
		context = app.getExperimentContext().getWellContext();
		coord = context.getCoordinate();
		if (coord == null) {
			coord = new WellCoordinate(510, 510);
			context.setCoordinate(coord);
		}

		tabsheet = new TabSheet();
		tabsheet.setImmediate(true);
		chartTab = new VerticalLayout();
		// chartTab.addComponent(new Label("Chart"));
		tabsheet.addTab(chartTab);
		tabsheet.getTab(chartTab).setCaption("Chart");

		multichartTab = new VerticalLayout();
		// chartTab.addComponent(new Label("Chart"));
		tabsheet.addTab(multichartTab);
		tabsheet.getTab(multichartTab).setCaption("Multi Chart");

		optionsTab = new VerticalLayout();
		tabsheet.addTab(optionsTab);
		tabsheet.getTab(optionsTab).setCaption("Options");

		dataTab = new VerticalLayout();
		tabsheet.addTab(dataTab);
		tabsheet.getTab(dataTab).setCaption("Data");

		VerticalLayout datamultiTab = new VerticalLayout();
		tabsheet.addTab(datamultiTab);
		tabsheet.getTab(datamultiTab).setCaption("Data Multi Flow");

		tophor = new HorizontalLayout();
		
		tophor.addComponent(new Button("Compute Mean", new Button.ClickListener() {
			public void buttonClick(ClickEvent event) {
				recomputeCharts();
			}
		}));

		
	        
		mywindow.addComponent(tophor);

		VerticalLayout vzoom = new VerticalLayout();
		NativeButton help = new NativeButton();
		help.setStyleName("nopadding");
		help.setDescription("Click me to get information on this window");
		help.setIcon(new ThemeResource("img/help-hint.png"));
		
		help.addListener(new Button.ClickListener() {
			public void buttonClick(Button.ClickEvent event) {
				app.showHelpMessage("Help", getHelpMessage());
			}
		});
		
		final NativeButton options = new NativeButton();
		options.setStyleName("nopadding");
		options.setIcon(new ThemeResource("img/configure-3.png"));
		options.setDescription("Options (such as file type or if a flow should be subtracted)");
		options.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				doOptionAction();
			}

		});

		final NativeButton export = new NativeButton();
		export.setStyleName("nopadding");
		export.setIcon(new ThemeResource("img/export.png"));
		export.setDescription("Open image in another browser window so that you can save it to file");
		export.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(com.vaadin.ui.Button.ClickEvent event) {
				doExportAction();
			}
		});

		HorizontalLayout hcan = new HorizontalLayout();
		vzoom.addComponent(export);
		vzoom.addComponent(options);
		vzoom.addComponent(help);			
		hcan.addComponent(vzoom);
		mywindow.addComponent(hcan);
		hcan.addComponent(tabsheet);
		addOptions(optionsTab);
		addChart(chartTab);
		addMultiChart(multichartTab);
		addData(dataTab);
		addDataMulti(datamultiTab);

		
	}
	public void doOptionAction() {
		tabsheet.setSelectedTab(this.optionsTab);
	}
	public void doExportAction() {
		String options[] =  { 
				"... save chart images",
				"... save data points", 				
				"... raw data, ionograms, alignments for signal mask wells"};
		
		OptionsDialog input = new OptionsDialog(mywindow,
				"What would you like to export?", "Export...",options,0,			
				new OptionsDialog.Recipient() {

					@Override
					public void gotInput(final int selection) {
						if (selection < 0)
							return;
						// / do the search
						if (selection == 0) {
							saveCharts();
						} else if (selection == 1) {
							tabsheet.setSelectedTab(dataTab);					
						} else {
							ExportTool export = new ExportTool(app, mywindow,
									maincont.getSignalMask(), tophor);
							export.doExportAction();

						}
					}

				});
	}
	public void saveCharts() {
		File f = null;
		if (singlechart == null || flowchart == null) return;
		String what = "automate";
		for (int i = 0; i < 2; i++) {
			try {
				f = File.createTempFile("export_"+what+"_" + exp.getId() + maincont.getAbsoluteCorner().getCol() + "_"+i
						+ "_"+ maincont.getAbsoluteCorner().getRow(), ".png");
			} catch (IOException e) {
	
			}
			if (f != null) {
				f.deleteOnExit();
				if (i == 0) this.exportPng(f, singlechart);
				else this.exportPng(f, flowchart);
				FileDownloadResource down = new FileDownloadResource(f, this.app);
				app.getMainWindow().open(down, "_blank", 600, 600, 1);
			}
		}
	
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
	public String getHelpMessage() {
		 String msg = "<ul>";
	        msg += "<li>it will automatically do the masked neighbor subtraction and compute a median signal for any number of flows</li>";
	        msg += "<li>put in the flow numbers or ranges in the text box</li>";
	        msg += "<li>pick a bg and ignore and signal mask in the Mask Edit window (and maybe check them!)</li>";	        
	        msg += "<li>if you like it can also subtract the result of any flow from the other results <br>(subtract flow in options tab)</li>";
	        msg += "<li>You can also export the data to excel with the Data tabs</li>";
	        msg += "<li>To change the file type (such as beadfind pre) go to he Options tab and pick the type there</li>";
	        msg += "</ul>";
	        return msg;
	}
	private void recomputeCharts() {
		getSelectedType();
		cview = new MultiAcqPanel(maincont.getFiletype());
		cview.setYaxis("Median count");

		cmulti = new MultiFlowPanel(maincont.getFiletype());
		cmulti.setYaxis("Median count");
		flows = this.parseFlow();

		if (maincont.getIgnoreMask() == null) {
			app.showMessage(this, "I see no ignore/pinned mask - you can select it in the Mask component\n.Make sure you have selected a region");

		}
		if (maincont.getIgnoreMask() == maincont.getSignalMask()) {
			app.showMessage(this, "The ignore mask = signal mask - please select the appropriate signal mask in the Mask component");
			return;
		}

		if (maincont.getIgnoreMask() == maincont.getBgMask()) {
			app.showMessage(this, "The ignore mask = bg mask, this won't work :-). You can select a bg mask in the Mask window");
			return;
		}
		if (maincont.getBgMask() == null) {
			app.showMessage(this, "You selected no BG mask - will use ALL (no pinned) wells - you can select a bg mask in the Mask windows");

		}
		if (maincont.getSignalMask() != null) {
			double perc = maincont.getSignalMask().computePercentage();
			if (perc < 1) {
				String question = "<html>The signal mask "+maincont.getSignalMask().getName()+" has only " + perc + "% wells, do you want to still use it?" + "<br><b>Did you already select a region?</b>" + "<br>You might want to use the MaskEditor (and <b>refresh</b> the masks possibly) to check them</html>";
				OkDialog okdialog = new OkDialog(mywindow, "Few wells", question, new OkDialog.Recipient() {
					@Override
					public void gotInput(String name) {
						if (!name.equalsIgnoreCase("OK")) return;
						continueRebuildingChart();
					}
				});
				return;
			} else if (perc < 10) {
				app.showMessage(this, "Small percentage of flagged wells " + perc + "%  for mask " + maincont.getSignalMask());
			}
		}

		if (maincont.getBgMask() != null && maincont.getBgMask().computePercentage() < 2) {
			String question = "The bg mask only has " + maincont.getBgMask().computePercentage() + "% wells, do you want to still use it?";
			OkDialog okdialog = new OkDialog(mywindow, "Few wells", question, new OkDialog.Recipient() {
				@Override
				public void gotInput(String name) {
					if (!name.equalsIgnoreCase("OK")) return;
					continueRebuildingChart();
				}
			});
			return;
		}
		
		continueRebuildingChart();
	}

	public void continueRebuildingChart() {
		results = new WellFlowDataResult[flows.size()];
		flownr = new int[flows.size()];
		taskdone = new boolean[flows.size()];
		int subtractflow = -1;
		subtract = null;
		this.multiflow = null;
		if (this.boxsub.booleanValue()) {
				subtractflow = this.getInt(textsubtract);			
		}

		if (subtractflow > -1) {
			subtract = automateFlow(subtractflow, null);
			updateChart(subtractflow, subtract);
		}

		
		for (int i = 0; i < flows.size(); i++) {
			int flow = flows.get(i);
			flownr[i] = flow;

		//	if (flow != subtractflow) {
				if (indicator != null) tophor.removeComponent(indicator);
				indicator = new ProgressIndicator(new Float(0.0));
				indicator.setHeight("40px");
				// indicator.setCaption("Creating whole Proton image");
				indicator.setDescription("I am computing the mean signal of flow "+flow);
				indicator.setPollingInterval(5000);
				tophor.addComponent(indicator);

				task = new AutomateTask(flow, this);
				task.execute();
		//	} else {
		//		taskdone[i] = true;
		//	}
		}
		app.logModule(getName(), "compute mean "+Arrays.toString(flownr));
	}

	private RasterData computeNN(RasterData data, ProgressListener prog) {

		p("=========== compute NN ==========");
		p("Data before nn: " + maincont.getData());
		int span = Math.max(4, this.maincont.getSpan());
		maincont.getExp().setFlow(maincont.getFlow());
		NearestNeighbor nn = new NearestNeighbor(maincont.getExp(), span, maincont.getMedianFunction());

		boolean useBg = false;
		// xxx FIX
		RasterData nndata = nn.computeBetter(data, maincont.getIgnoreMask(), maincont.getBgMask(), prog, span, useBg);
		p("Got nndata (frame 10) " + nndata.getValue(0, 0, 0, 10));
		return nndata;
	}

	public WellFlowDataResult automateFlow(int flow, ProgressListener list) {

		RasterData data = loadData(flow, list);
		if (flow == 0) {
			p("Loaded data for flow " + flow + ":(frame 10) " + data.getValue(0, 0, 0, 10));
		}
		maindata = data;
		WellFlowDataResult result = null;
		if (data != null) {
			RasterData nndata = null;

			nndata = computeNN(data, list);

			p("Got nndata for flow " + flow + " (frame 10): " + nndata.getValue(0, 0, 0, 10));
			// now compute average
			WellAlgorithm alg = new WellAlgorithm(maincont.getExp(), null, maincont.getSpan(), false, maincont.getMedianFunction());
			p("Computing median on nndata " + nndata);

			try {
				result = alg.computeMedian(nndata, flow, maincont.getFiletype(), maincont.getIgnoreMask(), maincont.getSignalMask());
				p("Got result for median: " + result + "," + Arrays.toString(result.getData()));
			} catch (Exception e) {
				// p("Got an error" + ErrorHandler.getString(e));
			}

		} else {

			String msg = "I got no data for flow " + flow + ", " + maincont.getFiletype() + " in <b>" + maincont.getExp().getRawDir() + " at " + maincont.getAbsCenterAreaCoord();
			app.showMessage(this, msg);
		}
		return result;
	}

	public RasterData loadData(int flow, ProgressListener list) {

		RasterData data = null;

		p(" =============================== load Data for flow " + flow + "===========");
		if (list != null) {
			list.setMessage("Loading flow " + flow + ", type: " + maincont.getFiletype() + ", center coord: " + maincont.getAbsCenterAreaCoord());
		}
		p("About to load data for " + maincont.getExp().getRawDir() + ", flow " + flow + ", type: " + maincont.getFiletype() + ", REL center coord: " + maincont.getRelativeCenterAreaCoord());
		manager = DataAccessManager.getManager(maincont.getExp().getWellContext());
		try {
			data = manager.getRasterDataForArea(data, maincont.getRasterSize(), maincont.getRelativeCenterAreaCoord(), flow, maincont.getFiletype(), null, 0, -1);
			if (data != null) {
				maindata = data;
				p("Got data for flow: " + flow);
			} else {
				p("Got NO data for flow " + flow);
			}
		} catch (Exception e) {
			p("Got an error when loading: " + ErrorHandler.getString(e));
		}
		return data;

	}

	public void addCoordAndFlowSelection(HorizontalLayout h) {
		tflow = new TextField();
		tflow.setWidth("60px");
		// tflow.setHeight("25px");
		tflow.setImmediate(true);
		if (flows == null) {
			flows = new ArrayList<Integer>();
			flows.add(0);
		}
		String s = "" + flows;
		tflow.setValue(s.substring(1, s.length() - 1));
		h.addComponent(new Label("Flow(s):"));
		h.addComponent(tflow);

		tflow.addListener(new Property.ValueChangeListener() {

			public void valueChange(ValueChangeEvent event) {
				// addChart(chartTab);
				parseFlow();
				p("Got flows " + flows);
				//reopen();
			}
		});

		coordsel = new CoordSelect(coord.getCol() + exp.getColOffset(), coord.getRow() + exp.getRowOffset(), this);
		coordsel.addGuiElements(h);
	}
	public void close() {
		super.close();
		if (task != null && !task.isCancelled()) {
			task.cancel(true);
			task = null;
		}		
	}
	@Override
	public void taskDone(Task t) {
		p("Task " + t + " is done");
		// if (t.isSuccess()) {
		AutomateTask task = (AutomateTask) t;
		WellFlowDataResult res = task.getResult();
		int flow = task.getFlow();
		// store result
		int index = -1;
		for (int i = 0; i < flownr.length; i++) {
			if (flownr[i] == flow) {
				index = i;
				break;
			}
		}
		taskdone[index] = true;
		if (res != null) {
			p("Got result for flow " + flow);
			if (subtract != null) {
				p("SUBTRACTING " + subtract);
				res = (WellFlowDataResult) res.deepClone();
				res.subtract(subtract);
			}

			results[index] = res;
			// p("Adding result " + res + " to chart");
			updateChart(flow, res);

		} else {
			p("Could not load flow  " + flow + ":" + manager.getErrorMsg());
			app.showMessage(this, "I was not able to get data for flow " + flow + ":" + manager.getErrorMsg());
			return;
		}
		boolean alldone = true;
		for (int i = 0; i < flows.size(); i++) {
			if (!taskdone[i]) {
				alldone = false;
			}
		}
		if (alldone) {
			p("ALL tasks are done, will now create multichart in correct flow order: " + flownr);
			// if (subtract != null) {
			// p("SUBTRACTING " + subtract);
			// res = (WellFlowDataResult) res.deepClone();
			// res.subtract(subtract);
			// }
			tophor.removeComponent(indicator);
			updateMultiChart();
			tabsheet.setSelectedTab(chartTab);
		} else {
			// p("Not all done yet: " + Arrays.toString(taskdone));
		}
	}

	public void addChart(AbstractLayout tab) {
		chartTab.removeAllComponents();
		HorizontalLayout h = new HorizontalLayout();
		tab.addComponent(h);
		addCoordAndFlowSelection(h);

		if (cview == null || results == null) {
			tab.addComponent(new Label("No data to show yet - compute the mean signal first"));
			return;
		}

		if (this.embeededmultichart != null) {
			tab.removeComponent(this.embeededmultichart);
		}
		Embedded chart = this.createEmbeededChart(cview);
		if (chart != null) {
			tab.addComponent(chart);
		} else {
			tab.addComponent(new Label("Could not load data for those flows"));
		}
		area.setValue(cview.toCSV());
	}

	public void addMultiChart(AbstractLayout tab) {
		multichartTab.removeAllComponents();
		HorizontalLayout h = new HorizontalLayout();
		tab.addComponent(h);
		if (cmulti == null || results == null) {
			tab.addComponent(new Label("No data to show yet - compute the mean signal first"));
			return;
		}

		if (this.embeededflowchart != null) {
			tab.removeComponent(this.embeededflowchart);
		}
		Embedded chart = this.createEmbeededFlowChart(cmulti);
		if (chart != null) {
			tab.addComponent(chart);
		} else {
			tab.addComponent(new Label("Could not load data for those flows"));
		}
		areamulti.setValue(cmulti.toCSV());
	}

	private void updateChart(int flow, WellFlowDataResult nndata) {
		// update(String region, ExperimentContext expContext,
		// WellFlowDataResult nndata, ArrayList<Integer> flows) {

		p("updatechart with nndata=" + Arrays.toString(nndata.getData()));
		String mask = "all wells";
		if (maincont.getSignalMask() != null) {
			mask = maincont.getSignalMask().getName() + ", " + maincont.getSignalMask().computePercentage() + "% wells";
		}
		String subtitle = mask + ", signal ";
		cview.update("Area " + maindata.getAbsStartCol() + "/" + maindata.getAbsStartRow() + "+" + (maindata.getRaster_size() + maincont.getExp().getRowOffset()), subtitle, maincont.getExp(), flows);
		cview.update(nndata, 0);
		// cview.addResult(nndata, flow);
		// p("cview should now be visible!!!");
		cview.repaint();
		addChart(this.chartTab);

	}

	private void updateMultiChart() {
		// create one huge dataset for all flows, show it
		baselist = "";
		filetype = maincont.getFiletype();
		long starttime = 0;
		ArrayList<WellFlowDataResult> chartres = new ArrayList<WellFlowDataResult>();
		for (int i = 0; i < flownr.length; i++) {
			WellFlowDataResult nndata = results[i];

			int flow = flownr[i];
			if (nndata != null) {
				p("starttime for flow " + flow + "=" + starttime);
				nndata.setStarttime(starttime);
				starttime += nndata.getLastTimeStamp();
				p("adding nndata: " + Arrays.toString(nndata.getData()));
				chartres.add(nndata);
			}

		}
		cmulti.setResults(chartres);
		cmulti.update("Multiflow " + (maindata.getAbsStartCol()) + "/" + (maindata.getAbsStartRow()) + "+" + maindata.getRaster_size(), maincont.getExp());
		cmulti.repaint();
		addMultiChart(this.multichartTab);
	}

	private class AutomateTask extends Task {

		boolean ok;
		int flow;
		WellFlowDataResult result;

		public AutomateTask(int flow, TaskListener tlistener) {
			super(tlistener);
			setProglistener(AutomateWindow.this);
			this.flow = flow;
		}

		public WellFlowDataResult getResult() {
			return result;
		}

		public int getFlow() {
			return flow;
		}

		@Override
		public Void doInBackground() {
			try {
				result = automateFlow(flow, this);
				ok = true;
			} catch (Exception e) {
				p("Error in automate task: " + ErrorHandler.getString(e));
				ok = false;
			}
			return null;
		}

		@Override
		public boolean isSuccess() {
			return ok;
		}
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

	private RawType getSelectedType() {
		if (typeselect == null || typeselect.getValue() == null) {
			type = RawType.ACQ;
		} else {
			type = (RawType) typeselect.getValue();
		}
		maincont.setFiletype(type);
		return type;
	}

	public void addData(AbstractLayout tab) {
		area = new TextArea();
		tab.addComponent(area);
		if (cview != null) area.setValue(cview.toCSV());
		area.setWidth("400px");
		area.setHeight("400px");
	}

	public void addDataMulti(AbstractLayout tab) {
		areamulti = new TextArea();
		tab.addComponent(areamulti);
		if (cmulti != null) areamulti.setValue(cmulti.toCSV());
		areamulti.setWidth("400px");
		areamulti.setHeight("400px");
	}

	public void valueChange(Property.ValueChangeEvent event) {
		// The event.getProperty() returns the Item ID (IID)
		// of the currently selected item in the component.
		Property id = event.getProperty();
		if (id.getValue() instanceof RawType) {
			type = (RawType) id.getValue();
			addChart(chartTab);
		}
	}

	

	private void addOptions(AbstractLayout tab) {
		HorizontalLayout h = new HorizontalLayout();
		tab.addComponent(h);
		addTypeSelection(h);
		
		
		 h = new HorizontalLayout();
		 tab.addComponent(h);
		boxsub = new CheckBox("Subtract results of flow");
		if (dosubtract) boxsub.setValue(this.dosubtract);
		h.addComponent(boxsub);		//
		this.textsubtract = new TextField();
		if (this.subtractflow>-1) textsubtract.setValue(subtractflow);		
		h.addComponent(textsubtract);
		
	}

	public void buttonClick(Button.ClickEvent event) {
		WellCoordinate newcoord = coordsel.getCoord();
		exp.makeRelative(newcoord);
		if (coord == null || !coord.equals(newcoord)) {
			
			coord = newcoord;
			app.setWellCoordinate(newcoord);
			
		}
		flows = parseFlow();
		recomputeCharts();
	}

	public int getInt(TextField t) {
		return getInt(t, 0);
	}

	public int getInt(TextField t, int defaultvalue) {
		if (t.getValue() == null) {
			return defaultvalue;
		}
		String v = "" + t.getValue();
		int i = defaultvalue;
		try {
			i = Integer.parseInt(v);
		} catch (Exception e) {}
		return i;
	}

	private static void err(String msg, Exception ex) {
		Logger.getLogger(AutomateWindow.class.getName()).log(Level.SEVERE, msg, ex);
	}

	private static void err(String msg) {
		Logger.getLogger(AutomateWindow.class.getName()).log(Level.SEVERE, msg);
	}

	private static void warn(String msg) {
		Logger.getLogger(AutomateWindow.class.getName()).log(Level.WARNING, msg);
	}

	private static void p(String msg) {
		//system.out.println("AutomateWindow: " + msg);
		Logger.getLogger(AutomateWindow.class.getName()).log(Level.INFO, msg);
	}

	public void setProgressValue(int p) {
		if (indicator != null) indicator.setValue(((double) p / 100.0d));
		// progress.setValue("Creating composite image: " + p + "%");
	}

	public void setMessage(String msg) {

	}

	@Override
	public void stop() {

	}

	private Embedded createEmbeededFlowChart(MultiFlowPanel pan) {
		p("create createEmbeededChart called");
		flowchart = pan.getChart();
		if (flowchart == null) {
			p("Could not create JFreeChart object");
			return null;
		}
		JFreeChartWrapper wrapper = new JFreeChartWrapper(flowchart);
		wrapper.setWidth("550px");
		wrapper.setHeight("320px");
		if (wrapper == null) {
			p("Could not create wrapper object");
		}
		return wrapper;

	}

	private Embedded createEmbeededChart(MultiAcqPanel pan) {
		p("create createEmbeededChart called");
		singlechart = pan.getChart();
		if (singlechart == null) {
			p("Could not create JFreeChart object");
			return null;
		}
		JFreeChartWrapper wrapper = new JFreeChartWrapper(singlechart);
		wrapper.setWidth("550px");
		wrapper.setHeight("320px");
		if (wrapper == null) {
			p("Could not create wrapper object");
		}
		return wrapper;

	}
}
