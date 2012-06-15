/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.raw;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;

import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.rawdataaccess.pgmacquisition.RawType;
import com.iontorrent.utils.StringTools;
import com.iontorrent.vaadin.TSVaadin;
import com.iontorrent.vaadin.utils.CoordSelect;
import com.iontorrent.vaadin.utils.ExportTool;
import com.iontorrent.vaadin.utils.FileDownloadResource;
import com.iontorrent.vaadin.utils.OptionsDialog;
import com.iontorrent.vaadin.utils.WindowOpener;
import com.iontorrent.wellmodel.WellContext;
import com.iontorrent.wellmodel.WellCoordinate;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ConversionException;
import com.vaadin.data.Property.ReadOnlyException;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.Select;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;

/**
 *
 * @author Chantal Roth 
 * chantal.roth@lifetech.com
 */
public class RawWindow extends WindowOpener implements Button.ClickListener, Property.ValueChangeListener {

    private TSVaadin app;
    private Embedded chart;
    VerticalLayout optionsTab;
    VerticalLayout dataTab ;
    HorizontalLayout tophor;
    private WellContext context;
    private ExperimentContext exp;
    private CoordSelect coordsel;
    private WellCoordinate coord;
    private TextField tflow;
    // private int flow;
    ArrayList<Integer> flows;
    private int subtract = -1;
    private RawType type;
    private VerticalLayout chartTab;
    private Select typeselect;
    private TabSheet tabsheet;
    private RawChart raw;
    
    boolean israw;
    boolean isbg;
    private CheckBox boxraw ;
    private CheckBox boxbg ;
    
    private TextField textsubtract;
    private  TextField tspan;

    public RawWindow(TSVaadin app, Window main, String description, int x, int y) {
        super("Raw Signal (one well)", main, description, x, y, 470, 450);
        this.app = app;
        isbg = true;
    }

    @Override
    public void windowOpened(Window mywindow) {
       
        exp = app.getExperimentContext();
        context = app.getExperimentContext().getWellContext();
        coord = context.getCoordinate();
        if (coord == null) {
            coord = new WellCoordinate(510, 510);
            context.setCoordinate(coord);
        }
       
        tabsheet = new TabSheet();
       
        chartTab = new VerticalLayout();
        //  chartTab.addComponent(new Label("Chart"));
        tabsheet.addTab(chartTab);
        tabsheet.getTab(chartTab).setCaption("Chart");
        optionsTab = new VerticalLayout();
        tabsheet.addTab(optionsTab);
        tabsheet.getTab(optionsTab).setCaption("Options");

        dataTab = new VerticalLayout();
        tabsheet.addTab(dataTab);
        tabsheet.getTab(dataTab).setCaption("Data");

        VerticalLayout transTab = new VerticalLayout();
        tabsheet.addTab(transTab);
        tabsheet.getTab(transTab).setCaption("Transformations");

        VerticalLayout debugTab = new VerticalLayout();
        tabsheet.addTab(debugTab);
        tabsheet.getTab(debugTab).setCaption("Debug");
        
        
        
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

		 tophor = new HorizontalLayout();
		HorizontalLayout hcan = new HorizontalLayout();
		vzoom.addComponent(export);
		vzoom.addComponent(options);
		vzoom.addComponent(help);			
		hcan.addComponent(vzoom);
		mywindow.addComponent(tophor);    
		mywindow.addComponent(hcan);        
        hcan.addComponent(tabsheet);
        
        addOptions(optionsTab);
        addChart(chartTab);
        addData(dataTab);
        addTrans(transTab);
        addDebugTab(debugTab);
    }
	public void doExportAction() {
		String options[] =  { 
				"... save chart image",
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
									null, tophor);
							export.doExportAction();

						}
					}

				});
	}
	public void saveCharts() {
		File f = null;
		if (raw == null || raw.getFreeChart() == null) return;
		String what = "automate";
		//for (int i = 0; i < 1; i++) {
			try {
				f = File.createTempFile("export_"+what+"_" + exp.getId() + exp.getWellContext().getAbsoluteCoordinate().getCol() 
						+ "_"+ exp.getWellContext().getAbsoluteCoordinate().getRow(), ".png");
			} catch (IOException e) {
	
			}
			if (f != null) {
				f.deleteOnExit();
			//f (i == 0) this.exportPng(f, singlechart);
				exportPng(f, raw.getFreeChart());
				FileDownloadResource down = new FileDownloadResource(f, this.app);
				app.getMainWindow().open(down, "_blank", 600, 600, 1);
			}
		//}
	
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
    public void doOptionAction() {
		tabsheet.setSelectedTab(this.optionsTab);
	}
    public void addTrans(AbstractLayout tab) {
        TransOptions t = new TransOptions();
        t.createGui(tab);
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
                //addChart(chartTab);
                parseFlow();
                p("Got flows " + flows);
                reopen();
            }
        });

        coordsel = new CoordSelect(coord.getCol() + exp.getColOffset(), coord.getRow() + exp.getRowOffset(), this);
        coordsel.addGuiElements(h);
    }
    public String getHelpMessage() {
        String msg = "<ul>";
        msg += "<li>You can enter another coordinate to view</li>";
        msg += "<li>Change the file type in the options tab</li>";
        msg += "<li>Export the data to excel in the data tab</li>";
        msg += "<li>View the raw or nn subtracted data in the options tab</li>";
        msg += "</ul>";
        return msg;
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

    public void addDebugTab(AbstractLayout tab) {

        TextArea area = new TextArea();
        tab.addComponent(area);
        area.setValue(raw.getMsgs());
        area.setWidth("400px");
        area.setHeight("400px");
    }

    public void addCurveSelectionOptions(AbstractLayout tab) throws ReadOnlyException, ConversionException {
        HorizontalLayout h = new HorizontalLayout();
        //
        Label lab= new Label("<b>Curve selection</b>", Label.CONTENT_XHTML);
       
       // lab.setStyleName("bold");
        tab.addComponent(lab);
        tab.addComponent(h);
        boxbg = new CheckBox("Bg subtract");
        h.addComponent(boxbg);
        boxbg.setValue(isbg);
        boxbg.setImmediate(true);
        boxbg.addListener(new ValueChangeListener() {

            public void valueChange(ValueChangeEvent event) {
                // Copy the value to the other checkbox.
                p("showbg clicked, recreating chart");
                addChart(chartTab);
            }
        });
        boxraw = new CheckBox("Show raw");
        h.addComponent(boxraw);
        boxraw.setValue(israw);
        boxraw.setImmediate(true);
        boxraw.addListener(new ValueChangeListener() {

            public void valueChange(ValueChangeEvent event) {
                // Copy the value to the other checkbox.
                p("showraw clicked, recreating chart");
                addChart(chartTab);
            }
        });
       lab = new Label("<b>Data operations</b>", Label.CONTENT_XHTML);  
       //lab.setStyleName("bold");
      
       tab.addComponent(new Label(" "));
       tab.addComponent(lab);
       HorizontalLayout v = new HorizontalLayout();
       tab.addComponent(v);
        
        Label sl = new Label("Subtract result of flow:");
        textsubtract = new TextField();
        textsubtract.setWidth("70px");
        if (subtract >-1) {
            textsubtract.setValue(subtract);
        }
        v.addComponent(sl);
        v.addComponent(textsubtract);
         textsubtract.addListener(new Property.ValueChangeListener() {

            public void valueChange(ValueChangeEvent event) {
                //addChart(chartTab);
                subtract = getInt(textsubtract, -1);
                reopen();
            }
        });
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
        return type;
    }

    public void addChart(AbstractLayout tab) {

        chartTab.removeAllComponents();
        HorizontalLayout h = new HorizontalLayout();
        tab.addComponent(h);
        addCoordAndFlowSelection(h);

        if (chart != null) {
            tab.removeComponent(chart);
        }
        israw = boxraw.booleanValue();
        isbg = boxbg.booleanValue();
        raw = new RawChart(getSelectedType(), app.getExperimentContext(), app.getExplorerContext(), parseFlow(), isbg, israw, getInt(textsubtract,-1));
        chart = raw.createChart();
        if (chart != null) {
            tab.addComponent(chart);
        } else {
           tab.addComponent(new Label("Could not load data for those flows"));
        }
    }

    public void addData(AbstractLayout tab) {
        TextArea area = new TextArea();
        tab.addComponent(area);
        area.setValue(raw.toCSV());
        area.setWidth("400px");
        area.setHeight("400px");

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

    @Override
    public void openButtonClick(Button.ClickEvent event) {
    	if (!super.checkExperimentOpened()) return;
        super.openButtonClick(event);
    }

    private void addOptions(AbstractLayout tab) {
        HorizontalLayout h = new HorizontalLayout();
        tab.addComponent(h);
        addTypeSelection(h);
        addCurveSelectionOptions(tab);
        
        h = new HorizontalLayout();
        Label sl = new Label("Span size for NN:");
        tspan = new TextField();
        tspan.setWidth("70px");
        tspan.setImmediate(true);
        tspan.setValue(app.getExplorerContext().getSpan());
        tspan.setDescription("Change span size for nn subtraction - hit enter after you change it");
        h.addComponent(sl);
        h.addComponent(tspan);
        tspan.addListener(new Property.ValueChangeListener() {

            public void valueChange(ValueChangeEvent event) {
                //addChart(chartTab);
                int span = Math.max(4, getInt(tspan, 8));
                app.getExplorerContext().setSpan(span);
                reopen();
            }
        });
        tab.addComponent(h);
    }

    public void buttonClick(Button.ClickEvent event) {
    	WellCoordinate coord = coordsel.getCoord();
        exp.makeRelative(coord);
        ArrayList<Integer> flows = parseFlow();
        addChart(chartTab);
        app.setWellCoordinate(coord);
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
        } catch (Exception e) {
        }
        return i;
    }

    private static void err(String msg, Exception ex) {
        Logger.getLogger(RawWindow.class.getName()).log(Level.SEVERE, msg, ex);
    }

    private static void err(String msg) {
        Logger.getLogger(RawWindow.class.getName()).log(Level.SEVERE, msg);
    }

    private static void warn(String msg) {
        Logger.getLogger(RawWindow.class.getName()).log(Level.WARNING, msg);
    }

    private static void p(String msg) {
        //system.out.println("RawWindow: " + msg);
        Logger.getLogger(RawWindow.class.getName()).log(Level.INFO, msg);
    }

}
