/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.raw;

import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.iontorrent.acqview.AcqPanel;

import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.rawdataaccess.pgmacquisition.DataAccessManager;
import com.iontorrent.rawdataaccess.pgmacquisition.RawType;
import com.iontorrent.rawdataaccess.wells.BfMaskFlag;
import com.iontorrent.vaadin.iono.IonogramImage;
import com.iontorrent.wellalgorithms.NearestNeighbor;
import com.iontorrent.wellalgorithms.WellContextFilter;
import com.iontorrent.wellmodel.WellCoordinate;
import com.iontorrent.wellmodel.WellFlowData;
import com.iontorrent.wellmodel.WellFlowDataResult;
import com.vaadin.terminal.StreamResource;

/**
 *
 * @author Chantal Roth 
 * chantal.roth@lifetech.com
 */
public class RawImage implements StreamResource.StreamSource {

    ByteArrayOutputStream imagebuffer = null;
    ExperimentContext exp;
    int reloads = 0;
    int flow;
    AcqPanel pan;

    public RawImage(ExperimentContext exp, int flow) {
        this.exp = exp;
        this.flow=  flow;
        if (exp != null) {
            pan = new AcqPanel(RawType.ACQ);
            p("Created IonogramImage panel");
            p("wellsfile: " + exp.getWellContext().getWellsfile());
            //pan.setWellContext(exp.getWellContext(), false, true);

        }
    }
    /* We need to implement this method that returns
     * the resource as a stream. */

    public InputStream getStream() {
      //  p("GetStream called");
        if (exp == null) {
            err("Got no experiment context");
            return null;
        }
      //  p("Got exp context: " + exp);

        if (exp.getWellContext().getCoordinate() == null) {
            exp.getWellContext().setCoordinate(new WellCoordinate(510, 510));
        }
        WellCoordinate coord = exp.getWellContext().getCoordinate();
        /* Create an image and draw something on it. */
        //public String update(String region, ExperimentContext expContext, WellFlowData data, WellFlowDataResult nndata, WellContext context, int flow, int nrempty, boolean showRawSignal) {
        DataAccessManager manager = DataAccessManager.getManager(exp.getWellContext());
        BfMaskFlag[] haveflags = new BfMaskFlag[]{BfMaskFlag.EMPTY};
        RawType type = RawType.ACQ;     
        WellContextFilter filter = new WellContextFilter(exp.getWellContext(), haveflags, null, type, flow, coord);
        WellFlowData data = manager.getFlowData(filter, false);
        
         NearestNeighbor alg = new NearestNeighbor(filter, 5,false);
        ArrayList<WellFlowDataResult> res = alg.compute();
        WellFlowDataResult nn = res.get(0);
       
        int nrempty = alg.getNrEmpty();
        boolean showRaw = true;
        pan.update(exp.getWellContext().getAbsoluteCoordinate().toString(), exp, data, nn, exp.getWellContext(), flow, nrempty, showRaw);
        pan.setSize(400, 200);
        pan.repaint();
        // JFreeChartWrapper wrapper = new JFreeChartWrapper(chart);
        RenderedImage image = pan.myCreateImage(400, 200);
    //    p("Created image");
        reloads++;
        try {
            /* Write the image to a buffer. */
            imagebuffer = new ByteArrayOutputStream();

            ImageIO.write(image, "png", imagebuffer);
            /* Return a stream from the buffer. */
            return new ByteArrayInputStream(
                    imagebuffer.toByteArray());
        } catch (IOException e) {
            err("Could not create raw image", e);
            return null;
        }
    }

    private static void err(String msg, Exception ex) {
        //system.out.println("IonogramImage: " + msg);
        Logger.getLogger(IonogramImage.class.getName()).log(Level.SEVERE, msg, ex);
    }

    private static void err(String msg) {
        //system.out.println("IonogramImage: " + msg);
        Logger.getLogger(IonogramImage.class.getName()).log(Level.SEVERE, msg);
    }

    private static void warn(String msg) {
        //system.out.println("IonogramImage: " + msg);
        Logger.getLogger(IonogramImage.class.getName()).log(Level.WARNING, msg);
    }

    private static void p(String msg) {
        //system.out.println("IonogramImage: " + msg);
        Logger.getLogger(IonogramImage.class.getName()).log(Level.INFO, msg);
    }
}
