/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.iono;

import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.ionogram.IonogramPanel;
import com.iontorrent.rawdataaccess.wells.WellData;
import com.iontorrent.sequenceloading.SequenceLoader;
import com.iontorrent.sff.SffRead;
import com.iontorrent.wellmodel.WellContext;
import com.iontorrent.wellmodel.WellCoordinate;
import com.vaadin.terminal.StreamResource;

/**
 *
 * @author Chantal Roth 
 * chantal.roth@lifetech.com
 */
public class IonogramImage implements StreamResource.StreamSource {

    ByteArrayOutputStream imagebuffer = null;
    ExperimentContext exp;
    int reloads = 0;
    IonogramPanel pan;
    boolean norm;
    boolean raw;

    public IonogramImage(ExperimentContext exp) {
        this.exp = exp;
        if (exp != null) {
            pan = new IonogramPanel(exp);
            p("Created IonogramImage panel");
            p("wellsfile: " + exp.getWellContext().getWellsfile());
            //pan.setWellContext(exp.getWellContext(), false, true);
        }
        
    }
    /* We need to implement this method that returns
     * the resource as a stream. */

    public InputStream getStream() {
        p("GetStream called");
        if (exp == null) {
            err("Got no experiment context");
            return null;
        }
        p("Got exp context: " + exp);
       WellContext context = exp.getWellContext();
               
        if (context.getCoordinate()== null) context.setCoordinate(new WellCoordinate(510, 510));
        /* Create an image and draw something on it. */
        
        WellCoordinate coord = context.getCoordinate();
         
         norm = true;
         if (norm) {
            String msg = null;
            // GuiUtils.showNonModelMsg(msg, false, 60);
            //    ProgressHandle progress = ProgressHandleFactory.createHandle(msg);
            //   progress.start();
            SequenceLoader loader = SequenceLoader.getSequenceLoader(this.exp, false, false);
            loader.setInteractive(false);
            SffRead read = loader.getSffRead(coord.getCol(), coord.getRow(), null);
            WellData welldata = context.getWellData(coord);
            if (read == null) {
                msg = loader.getMsg();
                if (msg != null) {
                    p("Got no sff read");
                }
                norm = false;
                raw = true;
            } else {   
                raw = false;               
                if (welldata != null) welldata.setNormalizedValues(read.getFlowgram());                
            }
        }
        pan.setWellContext(exp.getWellContext(), raw,norm);
        p("nr flows from exp: "+exp.getWellContext().getNrFlwos());
        int flows = exp.getWellContext().getNrFlwos();
        if (flows < 1) flows = 1000;
        
        pan.setSize(flows*20,200);
        RenderedImage image = pan.myCreateImage(flows*20,200);
        p("Created image");
        reloads++;
        try {
            /* Write the image to a buffer. */
            imagebuffer = new ByteArrayOutputStream();

            ImageIO.write(image, "png", imagebuffer);
            /* Return a stream from the buffer. */
            return new ByteArrayInputStream(
                    imagebuffer.toByteArray());
        } catch (IOException e) {
            err("Could not create ionogram image", e);
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
