/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.raw;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.iontorrent.acqview.MultiAcqPanel;
import org.jfree.chart.JFreeChart;

import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.rawdataaccess.pgmacquisition.DataAccessManager;
import com.iontorrent.rawdataaccess.pgmacquisition.RawType;
import com.iontorrent.rawdataaccess.wells.BfMaskFlag;
import com.iontorrent.utils.ErrorHandler;
import com.iontorrent.wellalgorithms.NearestNeighbor;
import com.iontorrent.wellalgorithms.WellContextFilter;
import com.iontorrent.wellmodel.WellCoordinate;
import com.iontorrent.wellmodel.WellFlowData;
import com.iontorrent.wellmodel.WellFlowDataResult;
import com.iontorrent.wellmodel.WellFlowDataResult.ResultType;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.JFreeChartWrapper;

/**
 *
 * @author Chantal Roth 
 * chantal.roth@lifetech.com
 */
public class RawChart {

    private static int MAX = 100;
    ByteArrayOutputStream imagebuffer = null;
    ExperimentContext exp;
    int reloads = 0;
    ArrayList<Integer> flows;
    boolean showraw;
    MultiAcqPanel pan;
    boolean bg;
    private String error;
    RawType type;
    private LinkedList<String> msgs;
    private int subtract;

    public RawChart(RawType type, ExperimentContext exp, ArrayList<Integer> flows, boolean bg, boolean raw, int subtract) {
        this.exp = exp;
        this.flows = flows;
        this.type = type;
        this.bg = bg;
        this.subtract = subtract;
        msgs = new LinkedList<String>();
        this.showraw = raw;
        if (exp != null) {
            pan = new MultiAcqPanel(type);
            p("Created RawChart panel with flows: " + flows);
            p("bg is: " + bg + ", showraw is: " + raw);
            //     p("raw dir: " + exp.getRawDir());
            //pan.setWellContext(exp.getWellContext(), false, true);

        }
    }

    protected void addMsg(String msg) {
        msgs.add(msg);
        if (msgs.size() > MAX) {
            msgs.removeFirst();
        }
    }

    public String toCSV() {
        return pan.toCSV();
    }
    /* We need to implement this method that returns
     * the resource as a stream. */

    public Embedded createChart() {
        p("create Chart called");
        if (exp == null) {
            err("Got no experiment context");
            return null;
        }
        p("Got exp context raw dir: " + exp.getRawDir());

        if (exp.getWellContext().getCoordinate() == null) {
            exp.getWellContext().setCoordinate(new WellCoordinate(510, 510));
        }
        WellCoordinate coord = exp.getWellContext().getCoordinate();
        p("relative coord:"+coord);
        /* Create an image and draw something on it. */
        //public String update(String region, ExperimentContext expContext, WellFlowData data, WellFlowDataResult nndata, WellContext context, int flow, int nrempty, boolean showRawSignal) {
        DataAccessManager manager = DataAccessManager.getManager(exp.getWellContext());
        BfMaskFlag[] haveflags = new BfMaskFlag[]{BfMaskFlag.EMPTY};

        if (subtract>-1 && !flows.contains(subtract)) flows.add(subtract);
       
        pan.setSubtract(subtract);
        for (int flow : flows) {
            WellContextFilter filter = new WellContextFilter(exp.getWellContext(), haveflags, null, type, flow, coord);
            WellFlowData data = manager.getFlowData(filter, false);

            if (data == null) {
                err("Could not get data for flow " + flow + " and type " + type + ":" + manager.getErrorMsg());
                return null;
            }
            else p("Got data:"+data+":"+manager.getErrorMsg());
            WellFlowDataResult nn = null;

            //   p("BG subtract? " + bg);
            if (bg) {
                ResultType.NN_RAW_BG.setShow(true);
                NearestNeighbor alg = new NearestNeighbor(filter, 5, false);
                ArrayList<WellFlowDataResult> res = alg.compute();
                if (res != null && res.size() > 1) {
                    nn = res.get(1);
                    if (nn != null) {
                        //   p("Adding nn");
                        nn.setName("Raw - nn " + flow);
                        p("Adding Raw - NN");
                        pan.addResults(res, flow);
                    }
                }
            } else {
                p("Not showing BG");
                ResultType.NN_RAW_BG.setShow(false);
            }

            WellFlowDataResult raw = new WellFlowDataResult(coord.getCol(), coord.getRow(), flow, type, coord.getMaskdata());
            raw.setResultType(ResultType.RAW);
            //   p("Showraw is: "+showraw);
            ResultType.RAW.setShow(showraw);
            raw.setData(data.getData());
            raw.setTimestamps(data.getTimestamps());
           
            String title = exp.getWellContext().getAbsoluteCoordinate().toString();
            if (subtract>-1) {
                title = title +", subtracting flow "+subtract;
            }
            if (raw != null) {
                pan.update(title, exp, flows);
                if (showraw) {
                    pan.update(raw, 0);
                }
                pan.update(nn, 0);
            }
        }
        //public String update(String region, ExperimentContext expContext, WellFlowData data, WellFlowDataResult nndata, WellContext context, ArrayList<Integer> flows, int nrempty, boolean showRawSignal) {



        JFreeChart chart = pan.getChart();
        if (chart == null) {
            p("Could not create JFreeChart object");
            return null;
        }
        JFreeChartWrapper wrapper = new JFreeChartWrapper(chart);
        wrapper.setWidth("450px");
        wrapper.setHeight("320px");
        if (wrapper == null) {
            p("Could not create wrapper object");
        }
        return wrapper;
    }

    private void err(String msg, Exception ex) {
        //system.out.println("RawChart: " + msg);
        addMsg(msg + ErrorHandler.getString(ex));
        Logger.getLogger(RawChart.class.getName()).log(Level.SEVERE, msg + ErrorHandler.getString(ex), ex);
    }

    public String getError() {
        return error;
    }

    private void err(String msg) {
        // error = msg;
        //system.out.println("RawChart: " + msg);
        addMsg(msg);
        Logger.getLogger(RawChart.class.getName()).log(Level.SEVERE, msg);
    }

    private void warn(String msg) {
        //system.out.println("RawChart: " + msg);
        addMsg(msg);
        Logger.getLogger(RawChart.class.getName()).log(Level.WARNING, msg);
    }

    private void p(String msg) {
        //system.out.println("RawChart: " + msg);
        addMsg(msg);
        Logger.getLogger(RawChart.class.getName()).log(Level.INFO, msg);
    }

    public String getMsgs() {
        String s = "Raw Chart msgs:\n";
        for (String m : msgs) {
            s += m + "\n";
        }
        return s;
    }
}
