/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.raw;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.rawdataaccess.pgmacquisition.RawType;
import com.iontorrent.utils.StringTools;
import com.iontorrent.vaadin.TSVaadin;
import com.iontorrent.vaadin.utils.CoordSelect;
import com.iontorrent.vaadin.utils.WindowOpener;
import com.iontorrent.wellmodel.WellContext;
import com.iontorrent.wellmodel.WellCoordinate;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ConversionException;
import com.vaadin.data.Property.ReadOnlyException;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Select;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 *
 * @author Chantal Roth 
 * chantal.roth@lifetech.com
 */
public class RawWindow extends WindowOpener implements Button.ClickListener, Property.ValueChangeListener {

    private TSVaadin app;
    private Embedded chart;
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
        VerticalLayout optionsTab = new VerticalLayout();
        tabsheet.addTab(optionsTab);
        tabsheet.getTab(optionsTab).setCaption("Options");

        VerticalLayout dataTab = new VerticalLayout();
        tabsheet.addTab(dataTab);
        tabsheet.getTab(dataTab).setCaption("Data");

        VerticalLayout transTab = new VerticalLayout();
        tabsheet.addTab(transTab);
        tabsheet.getTab(transTab).setCaption("Transformations");

        VerticalLayout debugTab = new VerticalLayout();
        tabsheet.addTab(debugTab);
        tabsheet.getTab(debugTab).setCaption("Debug");
        
        mywindow.addComponent(tabsheet);
        addOptions(optionsTab);
        addChart(chartTab);
        addData(dataTab);
        addTrans(transTab);
        addDebugTab(debugTab);
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
       VerticalLayout v = new VerticalLayout();
       tab.addComponent(lab);
       tab.addComponent(v);
        
        Label sl = new Label("Subtract result of flow:");
        textsubtract = new TextField();
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
        raw = new RawChart(getSelectedType(), app.getExperimentContext(), parseFlow(), isbg, israw, getInt(textsubtract,-1));
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
        if (app.getExperimentContext() == null) {
            mainwindow.showNotification("No Experiment Selected",
                    "<br/>Please open an experiment first",
                    Window.Notification.TYPE_WARNING_MESSAGE);
            return;
        }
        super.openButtonClick(event);
    }

    private void addOptions(AbstractLayout tab) {
        HorizontalLayout h = new HorizontalLayout();
        tab.addComponent(h);
        addTypeSelection(h);
        addCurveSelectionOptions(tab);
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
