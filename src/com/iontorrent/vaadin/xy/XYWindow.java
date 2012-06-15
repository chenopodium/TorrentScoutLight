/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.xy;

import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.data.xy.AbstractXYZDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYZDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.vaadin.vaadinvisualizations.ScatterChart;
import org.vaadin.vaadinvisualizations.VisualizationComponent.SelectionListener;

import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.guiutils.heatmap.ColorModel;
import com.iontorrent.heatmaps.ScoreMaskGenerator;
import com.iontorrent.rawdataaccess.wells.BitMask;
import com.iontorrent.rawdataaccess.wells.ScoreMaskFlag;
import com.iontorrent.results.scores.ScoreMask;
import com.iontorrent.threads.Task;
import com.iontorrent.threads.TaskListener;
import com.iontorrent.torrentscout.explorer.ExplorerContext;
import com.iontorrent.utils.ErrorHandler;
import com.iontorrent.utils.ProgressListener;
import com.iontorrent.utils.io.FileTools;
import com.iontorrent.vaadin.TSVaadin;
import com.iontorrent.vaadin.mask.MaskSelect;
import com.iontorrent.vaadin.utils.ExportTool;
import com.iontorrent.vaadin.utils.FileDownloadResource;
import com.iontorrent.vaadin.utils.JFreeChartWrapper;
import com.iontorrent.vaadin.utils.OptionsDialog;
import com.iontorrent.vaadin.utils.WindowOpener;
import com.iontorrent.vaadin.utils.ZoomControl;
import com.iontorrent.wellmodel.WellCoordinate;
import com.iontorrent.wellmodel.WellSelection;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.event.MouseEvents.ClickListener;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;

import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.Select;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * 
 * @author Chantal Roth chantal.roth@lifetech.com
 */
public class XYWindow extends WindowOpener implements TaskListener,
		ProgressListener {

	private TSVaadin app;
	ExplorerContext maincont;
	ExperimentContext exp;
	MaskSelect maskselect;
	TextArea text;
	String savefile;
	XYCalculator calc;
	BitMask filtermask;
	ProgressIndicator indicator;
	ScoreMaskFlag flaga;
	ScoreMaskFlag flagb;
	JFreeChart freechart;
	BitMask intersection;
	WorkThread thread;
	ScoreMask scoremask;

	HorizontalLayout hor;
	VerticalLayout ver;
	int bucket;
	ZoomControl zoom;
	int MAX = 10000;
	Label titlelabel;
	Select sela;
	Select selb;

	AbstractComponent chart;
	AbstractComponent chart1;
	AbstractComponent chart2;

	private ArrayList<ScoreMaskFlag> ignoreflags;

	public XYWindow(TSVaadin app, Window main, String description, int x, int y) {
		super("XYZ Charts", main, description, x, y, 600, 600);
		this.app = app;
		bucket = 1;

	}

	@Override
	public void openButtonClick(Button.ClickEvent event) {
		if (app.getExperimentContext() == null) {
			appwindow.showNotification("No Experiment Selected",
					"<br/>Please open an experiment first",
					Window.Notification.TYPE_WARNING_MESSAGE);
			return;
		}
		app.reopenScore();
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

	private void saveChart() {
		File f = null;
		try {
			f = File.createTempFile(
					"export_" + flaga.getName() + "_" + flagb.getName() + "_"
							+ exp.getId() + "_"
							+ maincont.getAbsoluteCorner().getCol() + "_"
							+ maincont.getAbsoluteCorner().getRow(), ".png");
		} catch (IOException e) {

		}
		if (f != null) {
			f.deleteOnExit();
			this.exportPng(f, freechart);
			FileDownloadResource down = new FileDownloadResource(f, this.app);
			app.getMainWindow().open(down, "_blank", 600, 600, 1);
		}
	}

	private void saveFile(boolean ask) {
		File f = null;
		try {
			f = File.createTempFile(
					"export_" + flaga.getName() + "_" + flagb.getName() + "_"
							+ exp.getId() + "_"
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
			s = s.append("X axis: " + flaga.getName() + nl);
			s = s.append("Y axis: " + flagb.getName() + nl);
			s = s.append("Z axis: Number of other data points that fit into the same bucket (bucket size: "
					+ calc.xybuckets + ") " + nl);
			if (filtermask != null) {
				s = s.append("Only using data for mask " + filtermask.getName()
						+ ", with " + filtermask.getTotalSub(maincont.getRasterSize()) + "="
						+ filtermask.computePercentage() + "% wells" + nl + nl);
			} else {
				s = s.append("Using all wells" + nl + nl);
			}
			s = s.append(flaga.getName() + "," + flagb.getName()
					+ ", count, chip col, chip row\n");
			FileTools.writeStringToFile(f, s.toString(), false);
			// export
			s = new StringBuffer();
			for (XY point : calc.getValues()) {
				s = s.append(point.x + ", " + point.y + ", " + point.z + ", "
						+ point.c + ", " + point.r + "\n");
			}
			FileTools.writeStringToFile(f, s.toString(), true);
			app.showMessage("Export done", "About to download result " + f
					+ " (" + f.length() / 1000000 + " MB)...");
			FileDownloadResource down = new FileDownloadResource(f, this.app);
			app.getMainWindow().open(down, "_blank", 600, 600, 1);
		}
	}

	private void createGui() {

		scoremask = app.getScoreMask();

		ver = new VerticalLayout();
		hor = new HorizontalLayout();
		mywindow.addComponent(ver);
		ver.addComponent(hor);

		ArrayList<BitMask> masks = maincont.getMasks();

		if (filtermask == null)
			filtermask = find("live", false);

		sela = new Select();

		int count = 0;
		ScoreMaskFlag[] flags = ScoreMaskFlag.values();
		Arrays.sort(flags);
		for (ScoreMaskFlag f : flags) {
			if (checkFlag(f, sela)) {
				count++;
				if (count == 1 && flaga == null)
					sela.select(f);
			}
		}
		if (flaga != null)
			sela.select(flaga);

		sela.setDescription("Mask for X coordinate");
		hor.addComponent(new Label("X:"));
		hor.addComponent(sela);

		selb = new Select();
		count = 0;
		for (ScoreMaskFlag f : flags) {
			if (checkFlag(f, selb)) {
				selb.addItem(f);
				count++;
				if (count == 2 && flagb == null)
					selb.select(f);
				
			}
		}
		sela.addListener(new Select.ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				Object o = sela.getValue();
				if (o != null) {
					sela.setDescription(o.toString());
				}

			}
		});
		selb.addListener(new Select.ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) {
				Object o = selb.getValue();
				if (o != null) {
					selb.setDescription(o.toString());
				}

			}
		});
		if (flagb != null)
			selb.select(flagb);
		selb.setDescription("Mask for Y coordinate");
		sela.setWidth("70px");
		selb.setWidth("70px");

		hor.addComponent(new Label("Y:"));
		hor.addComponent(selb);

		maskselect = new MaskSelect("filter", null, "Use only wells with flag",
				maincont, -1, null, filtermask);

		hor.addComponent(new Label("for wells that are:"));
		maskselect.addGuiElements(hor);

		Button compute = new Button("Compute");
		compute.setImmediate(true);
		hor.addComponent(compute);

		compute.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				compute();
			}

		});

		final Button export = new Button();
		export.setIcon(new ThemeResource("img/export.png"));
		export.setDescription("Save data for this XYZ chart (to open with Excel)");
		export.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				doExportAction();
			}

		});
		hor.addComponent(export);
		// this.compute();
	}

	private boolean checkFlag(ScoreMaskFlag f, Select sel) {
		if (f.isHide())
			return false;
		if (ignoreflags != null && !ignoreflags.contains(f))
			return false;

		sel.addItem(f);
		String n = f.getName();
		if (!scoremask.hasImage(f)) n = n +" (no data yet)";
		
		sel.setItemCaption(f, n);

		return true;

	}

	private boolean checkHasMap(ScoreMaskFlag f) {
		if (scoremask.hasImage(f))
			return true;

		if (thread == null) {
			if (!f.isCustom()) {
				p("Flag " + f + ":  got no image, will start compute thread");
				thread = new WorkThread(this, f);
				if (indicator != null) {
					hor.removeComponent(indicator);
				}
				indicator = new ProgressIndicator(new Float(0.0));
				indicator.setHeight("40px");

				indicator.setDescription("Computing heat map for " + f);
				indicator.setPollingInterval(5000);
				hor.addComponent(indicator);
				thread.execute();
				app.showMessage("No heatmap" ,"Need to compute heat map first for "+f);
			}
			else app.showMessage("No heatmap" ,"There are no search results for the custom map "+f);
		}
		else {
			app.showMessage("No heatmap" ,"I am already in the process of computing the map for "+f);
		}

		return false;
	}

	private void compute() {
		flaga = (ScoreMaskFlag) sela.getValue();
		flagb = (ScoreMaskFlag) selb.getValue();
		if (flaga != null && flagb != null) {
			if (!checkHasMap(flaga)) return;
			if (!checkHasMap(flagb)) return;
			compute(flaga, flagb);
		}
	}

	private class WorkThread extends Task {
		ScoreMaskFlag flag;

		public WorkThread(TaskListener list, ScoreMaskFlag flag) {
			super(list);
			this.flag = flag;

		}

		@Override
		public boolean isSuccess() {
			// TODO Auto-generated method stub
			return scoremask.hasImage(flag);
		}

		@Override
		protected Void doInBackground() {
			try {
				if (!scoremask.hasImage(flag)) {
					p("Don't have image for flag " + flag + " yet: "
							+ scoremask.getImageFile(flag));

					ScoreMaskGenerator gen = new ScoreMaskGenerator(scoremask,
							exp);

					String msg = gen.generateImageFiles(flag);
					if (msg != null && msg.length() > 0) {
						app.showMessage("Score Mask", msg);

					}
				}
				if (!scoremask.hasImage(flag)) {
					app.showMessage("Score Mask",
							"After calculating heat map: I don't have the heat map for  "
									+ flag);
					// removing flag from list?
					if (ignoreflags == null)
						ignoreflags = new ArrayList<ScoreMaskFlag>();
					ignoreflags.add(flag);
				}

			} catch (Exception e) {
				err("Got an error when computing the heat map: "
						+ ErrorHandler.getString(e));
			}
			return null;

		}

	}

	@Override
	public void taskDone(Task task) {
		WorkThread t = (WorkThread) task;

		app.showMessage("Computation done", "Calculation of heat map " + t.flag
				+ " is done");
		thread = null;
		if (indicator != null) {
			hor.removeComponent(indicator);
		}
		if (t.isSuccess()) reopen();
	}

	private void compute(ScoreMaskFlag a, ScoreMaskFlag b) {
		if (a == null || b == null)
			return;

		scoremask.readData(flaga);
		scoremask.readData(flagb);
		p("Computing: " + a + "/" + b);
		app.logModule(getName(), "compute " + a + "/" + b);
		app.showMessage(this, "Computing 2D map of " + a + " and " + b);

		calc = new XYCalculator(app.getScoreMask(), a, b, 40);
		calc.compute();
		p("Computation result: " + calc);
		this.intersection = calc.getIntersection();
		p("Got intersection: " + intersection.getTotalSub(maincont.getRasterSize()));

		if (titlelabel != null)
			ver.removeComponent(titlelabel);
		if (chart != null)
			ver.removeComponent(chart);
		if (chart1 != null)
			ver.removeComponent(chart1);
		if (chart2 != null)
			ver.removeComponent(chart2);

		// chart = createChart(calc);
		chart2 = createChart2(calc);
		// chart1 = createChart1(calc);
		titlelabel = new Label("Chart around coordinate "
				+ app.getExplorerContext().getAbsoluteCorner() + "+"
				+ app.getExplorerContext().getRasterSize());
		ver.addComponent(titlelabel);

		ver.addComponent(chart2);
		// ver.addComponent(chart1);

	}

	private class XYCalculator {
		BitMask inter;

		ArrayList<XY> values;
		ArrayList[][] grid;
		double minx;
		double maxx;
		double miny;
		double maxy;
		double minz;
		double maxz;
		int total;
		int xybuckets;
		ScoreMaskFlag flaga;
		ScoreMaskFlag flagb;
		ScoreMask scoremask;

		public XYCalculator(ScoreMask scoremask, ScoreMaskFlag flaga,
				ScoreMaskFlag flagb, int buckets) {
			this.flaga = flaga;
			this.flagb = flagb;
			this.scoremask = scoremask;
			this.xybuckets = buckets;

		}

		public BitMask getIntersection() {
			return inter;
		}

		public String toString() {
			return "XY " + flaga + "/" + flagb + "  with " + total
					+ " points. x=[" + minx + "," + maxx + "], y=[" + miny
					+ "," + maxy + "]  z=[" + minz + "," + maxz
					+ "], xybuckets=" + xybuckets + ", dx=" + getDx() + ", dy="
					+ getDy();
		}

		public void compute() {
			values = new ArrayList<XY>();
			minx = Integer.MAX_VALUE;
			miny = Integer.MAX_VALUE;
			maxx = Integer.MIN_VALUE;
			maxy = Integer.MIN_VALUE;
			minz = 0;
			maxz = 1;
			inter = new BitMask(scoremask.getNrCols(), scoremask.getNrRows());
			inter.setName(flaga.getName() + " & " + flagb.getName());
			total = 0;

			double[][] a = scoremask.getData(flaga);
			double[][] b = scoremask.getData(flagb);

			// start in area around selected well coordinate!

			WellCoordinate corner = app.getExplorerContext()
					.getRelativeCorner();
			int startc = corner.getCol();
			int startr = corner.getRow();
			int size = app.getExplorerContext().getRasterSize();
			for (int i = 0; i < size; i++) {
				if (total > MAX)
					break;
				for (int j = 0; j < size; j++) {
					int c = (startc + i);
					int r = (startr + j);
					if (c > scoremask.getNrCols())
						c = c - scoremask.getNrCols();
					if (r > scoremask.getNrRows())
						r = r - scoremask.getNrRows();

					if (total > MAX)
						break;
					if (a[c][r] != 0 && b[c][r] != 0) {
						// both have a value
						// double x = flaga.getRealValue(a.getValue(c, r));
						// double y = flagb.getRealValue(b.getValue(c, r));

						double x = flaga.getRealValue((int) a[c][r]);
						double y = flagb.getRealValue((int) b[c][r]);
						inter.set(c, r, true);

						XY xy = new XY(x, y);
						xy.c = c + exp.getColOffset();
						xy.r = r + exp.getRowOffset();
						if (total % 1000 == 0)
							p("Got value " + xy + "  at " + c + "/" + r);
						values.add(xy);
						if (x > maxx)
							maxx = x;
						if (y > maxy)
							maxy = y;
						if (x < minx)
							minx = x;
						if (y < miny)
							miny = y;
						total++;
					}
				}
			}
			maxx = Math.max(minx, maxx);
			maxy = Math.max(miny, maxy);
			if (minx == maxx) {
				minx = 0;
				maxx = 0;
			}
			if (miny == maxy) {
				miny = 0;
				maxy = 0;
			}
			computeZ();
		}

		public double getDx() {
			return (maxx - minx) / (double) xybuckets;
		}

		public double getDy() {
			return (maxy - miny) / (double) xybuckets;
		}

		private void computeZ() {
			// for all xy, add z to all xy in that bucket
			double dx = getDx();
			double dy = getDy();
			// create a grid of bucket x bucket
			grid = new ArrayList[xybuckets + 1][xybuckets + 1];
			int count = 0;
			if (dx <= 0 || dy <= 0)
				return;

			for (XY point : values) {
				int bx = (int) ((point.x - minx) / dx);
				int by = (int) ((point.y - miny) / dy);
				// count ++;
				// if (count % 1000 == 0) {
				// p(point+"->"+bx+"/"+by);
				// }
				ArrayList<XY> points = grid[bx][by];
				if (points == null) {
					points = new ArrayList<XY>();
					grid[bx][by] = points;
				}
				points.add(point);
			}
			count = 0;
			for (int bx = 0; bx < xybuckets; bx++) {
				for (int by = 0; by < xybuckets; by++) {
					ArrayList<XY> points = grid[bx][by];
					if (points != null) {
						int nr = points.size();
						// count ++;
						// if (count % 1000 == 0) {
						// p(bx+"/"+by+", got "+nr);
						// }
						if (nr > maxz)
							maxz = nr;
						for (XY point : points) {
							point.addZ(nr);
						}
					}
				}
			}
		}

		public ArrayList<XY> getValues() {
			return values;
		}

		public double getMinX() {
			return minx;
		}

		public double getMinY() {
			return miny;
		}

		public double getMaxX() {
			return maxx;
		}

		public double getMaxY() {
			return maxy;
		}

		public double getMinZ() {
			return minz;
		}

		public double getMaxZ() {
			return maxz;
		}

		public ArrayList<XY> getValues(double x, double y) {
			double dx = getDx();
			double dy = getDy();
			if (dx <= 0 || dy <= 0)
				return null;
			int bx = (int) ((x - minx) / dx);
			int by = (int) ((y - miny) / dy);
			bx = Math.max(0, bx);
			by = Math.max(0, by);
			bx = Math.min(this.xybuckets, bx);
			by = Math.min(this.xybuckets, by);
			ArrayList<XY> res = grid[bx][by];
			return res;
		}

	}

	private class XY implements Comparator {
		double x;
		double y;
		double z;

		int c;
		int r;

		public XY(double x, double y) {
			this.x = x;
			this.y = y;
			z = 0;
		}

		public String toString() {
			return x + "/" + y;
		}

		public void addZ(double dz) {
			z += dz;
		}

		public double getZ() {
			return z;
		}

		public WellCoordinate getCoord() {
			return new WellCoordinate(c, r);
		}

		public boolean equals(Object o) {
			XY xy = (XY) o;
			return x == xy.x && y == xy.y;
		}

		public int hashCode() {
			return (int) (x * 10000 + (int) (y * 100));
		}

		@Override
		public int compare(Object o1, Object o2) {
			XY a = (XY) o1;
			XY b = (XY) o2;
			return b.hashCode() - a.hashCode();
		}
	}

	public AbstractComponent createChart1(XYCalculator calc) {

		// Add a scatterChart
		ScatterChart sc = new ScatterChart();
		sc.setOption("titleX", flaga.getName());
		sc.setOption("titleY", flagb.getName());

		String title = flaga + "/" + flagb;
		for (XY xy : calc.getValues()) {
			sc.add(new double[] { xy.x, xy.y });

		}

		sc.addListener(new SelectionListener() {

			@Override
			public void selectionChanged(List<String> selectedItems) {
				p("Got selection changed: " + selectedItems);
				app.showMessage("Chart clicked", "selectionChanged");

			}
		});
		sc.addXAxisLabel(flaga.getName());
		sc.setImmediate(true);
		sc.addPoint(title);
		// sc.addPoint("Height");
		sc.setWidth("550px");
		sc.setHeight("550px");
		sc.setSizeFull();
		return sc;
	}

	public AbstractComponent createChart2(final XYCalculator calc) {
		String title = flaga + "/" + flagb;
		final XYSeries series = new XYSeries(title);
		for (XY xy : calc.getValues()) {
			series.add(xy.x, xy.y);

		}
		final XYZDataset data = new SampleXYZDataset(calc.values);
		double minz = calc.getMinZ();
		double maxz = calc.getMaxZ();
		double minx = calc.getMinX();
		double maxx = calc.getMaxX();
		double miny = calc.getMinY();
		double maxy = calc.getMaxY();
		NumberAxis xAxis = new NumberAxis(flaga.getName());
		xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		// xAxis.setLowerMargin(0.0);
		// xAxis.setUpperMargin(0.0);
		xAxis.setRange(minx, maxx);
		xAxis.setAxisLinePaint(Color.white);
		xAxis.setTickMarkPaint(Color.white);
		xAxis.setLabelPaint(Color.white);
		xAxis.setTickLabelPaint(Color.white);
		if (maxx - minx < 1) {
			xAxis.setTickUnit(new NumberTickUnit(0.1));
		} else if (maxx - minx < 4) {
			xAxis.setTickUnit(new NumberTickUnit(0.5));
		}

		NumberAxis yAxis = new NumberAxis(flagb.getName());
		yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		// yAxis.setLowerMargin(0.0);
		// yAxis.setUpperMargin(0.0);
		yAxis.setRange(miny, maxy);
		if (maxy - miny < 1) {
			yAxis.setTickUnit(new NumberTickUnit(0.1));
		} else if (maxy - miny < 4) {
			yAxis.setTickUnit(new NumberTickUnit(0.5));
		}
		yAxis.setAxisLinePaint(Color.white);
		yAxis.setTickMarkPaint(Color.white);
		yAxis.setLabelPaint(Color.white);
		yAxis.setTickLabelPaint(Color.white);

		XYBlockRenderer renderer = new XYBlockRenderer();
		renderer.setBlockWidth(calc.getDx() * 0.9);
		renderer.setBlockHeight(calc.getDy() * 0.9);

		org.jfree.chart.renderer.LookupPaintScale paintScale = new LookupPaintScale(
				minz, maxz, Color.gray);
		Color[] gradientColors = new Color[] { Color.blue, Color.green,
				Color.yellow, Color.orange, Color.red };
		ColorModel colormodel = new ColorModel(gradientColors, minz, maxz);
		double delta = (maxz - minz) / 100;
		for (double d = minz; d <= maxz; d += delta) {
			paintScale.add(d, colormodel.getColor(d));
		}
		// code adding paints to paintScale
		renderer.setPaintScale(paintScale);

		final XYPlot plot = new XYPlot(data, xAxis, yAxis, renderer);
		// plot.setBackgroundPaint(Color.lightGray);
		plot.setBackgroundPaint(Color.black);
		// plot.setDomainGridlinesVisible(false);
		plot.setRangeGridlinePaint(Color.white);
		// plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
		// plot.setOutlinePaint(Color.blue);
		final JFreeChart chart = new JFreeChart(title, plot);
		chart.removeLegend();
		NumberAxis scaleAxis = new NumberAxis("Count");
		scaleAxis.setTickLabelPaint(Color.white);
		scaleAxis.setAxisLinePaint(Color.white);
		scaleAxis.setTickMarkPaint(Color.white);

		scaleAxis.setRange(minz, maxz);
		PaintScaleLegend legend = new PaintScaleLegend(paintScale, scaleAxis);
		legend.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
		legend.setAxisOffset(5.0);
		legend.setStripOutlinePaint(Color.white);

		legend.setMargin(new RectangleInsets(5, 5, 5, 5));
		// legend.setFrame(new BlockBorder(Color.red));
		legend.setPadding(new RectangleInsets(5, 5, 5, 5));
		legend.setStripWidth(10);

		legend.setPosition(RectangleEdge.RIGHT);
		legend.setBackgroundPaint(Color.black);

		chart.addSubtitle(legend);
		chart.setBorderPaint(Color.white);
		chart.setBackgroundPaint(Color.black);

		freechart = chart;

		final JFreeChartWrapper wrapper = new JFreeChartWrapper(chart,
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
				ArrayList<XY> res = calc.getValues(x, y);
				ArrayList<WellCoordinate> coords = new ArrayList<WellCoordinate>();
				if (res != null && res.size() > 0) {
					msg += "<br>Got " + res.size()
							+ " wells - showing first one in other views";
					for (XY point : res) {
						coords.add(point.getCoord());
					}
					WellSelection sel = new WellSelection(coords);
					sel.setTitle(flaga.getName() + "=" + x + "/"
							+ flagb.getName() + "=" + y);
					exp.getWellContext().setSelection(sel);
					if (coords != null) {
						for (WellCoordinate well : coords) {
							well.setScoredata(scoremask.getDataPointsAt(
									well.getCol(), well.getRow()));
							// p("setting score data:"+Arrays.toString(well.getScoredata()));
						}
					}
					app.setWellSelection(sel);
					// Don't trigger a new well selection!
					app.setWellCoordinate(coords.get(0), false);
				} else
					msg += "<br>Got no wells";
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

	
	public class SampleXYZDataset extends AbstractXYZDataset implements
			XYZDataset {

		ArrayList<XY> values;

		public SampleXYZDataset(ArrayList<XY> values) {
			this.values = values;
		}

		public int getSeriesCount() {
			return 1;
		}

		public Comparable getSeriesKey(int i) {
			return "XYZ";
		}

		public int getItemCount(int i) {
			return values.size();
		}

		public Number getX(int i, int j) {
			return new Double(values.get(j).x);
		}

		public Number getY(int i, int j) {
			return new Double(values.get(j).y);
		}

		public Number getZ(int i, int j) {
			return new Double(values.get(j).z);
		}

	}

	public void doExportAction() {
		OptionsDialog input = new OptionsDialog(
				mywindow,
				"What would you like to export?",
				"Export...",
				"... save data points",
				"... save chart image",
				"... raw data, ionograms, alignments (for intersecting data points)",
				new OptionsDialog.Recipient() {

					@Override
					public void gotInput(final int selection) {
						if (selection < 0)
							return;
						// / do the search
						if (selection == 0) {
							saveFile(true);
						} else if (selection == 1) {
							saveChart();
						} else {
							ExportTool export = new ExportTool(app, mywindow,
									intersection, hor);
							export.doExportAction();

						}
					}

				});
	}

	private BitMask find(String name, boolean strict) {
		return maincont.find(name, strict);
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
		msg += "<li>Computes XY charts based on 2 heat maps </li>";
		msg += "</ul>";
		return msg;
	}

	private static void err(String msg, Exception ex) {
		Logger.getLogger(XYWindow.class.getName()).log(Level.SEVERE, msg, ex);
	}

	private static void err(String msg) {
		Logger.getLogger(XYWindow.class.getName()).log(Level.SEVERE, msg);
	}

	private static void warn(String msg) {
		Logger.getLogger(XYWindow.class.getName()).log(Level.WARNING, msg);
	}

	private static void p(String msg) {
		// System.out.println("MaskWindowCanvas: " + msg);
		Logger.getLogger(XYWindow.class.getName()).log(Level.INFO, msg);
	}

}
