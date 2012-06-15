/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.bgmodel;

import java.awt.Point;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.iontorrent.background.EmptyTrace;
import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.guiutils.heatmap.GradientPanel;
import com.iontorrent.maskview.BfMaskDensityPanel;
import com.iontorrent.maskview.EmptyTraceDensityPanel;
import com.iontorrent.rawdataaccess.wells.BfMaskFlag;
import com.iontorrent.wellmodel.WellCoordinate;
import com.vaadin.terminal.StreamResource;

/**
 *
 * @author Chantal Roth 
 * chantal.roth@lifetech.com
 */
public class ETMaskImage implements StreamResource.StreamSource {

    ByteArrayOutputStream imagebuffer = null;
    ExperimentContext exp;
    int reloads = 0;
    EmptyTraceDensityPanel pan;
    RenderedImage image;
    public ETMaskImage(ExperimentContext exp, BfMaskFlag flag, int bucket, EmptyTrace et, ArrayList<Integer> flows) {
        this.exp = exp;
        if (exp != null) {
            pan = new EmptyTraceDensityPanel(exp);
            pan.setFlows(flows);
            pan.setEmptyTrace(et);
            p("Created ET density panel with flows "+flows);
            pan.setContext(exp.getWellContext(),flag, bucket);
 
        }
    }
    public Point getPointFromWell(WellCoordinate coord) {
    	return pan.getImagePointFromWell(coord);
    }
    public WellCoordinate getWellCoordinate(int x, int y) {
        return pan.getWellCoordinate(x, y);
    }
    /* We need to implement this method that returns
     * the resource as a stream. */

    public RenderedImage getImage() {
    	if (image == null) {
    		 /* Create an image and draw something on it. */
            image = pan.myCreateImage(500, 500);
       
    	}
    	return image;
    }
    public void repaint() {
    	image = null;
    
    	pan.redrawImage();
    	getImage();
    }
    public InputStream getStream() {
      //  p("GetStream called");
        if (exp == null) {
            err("Got no experiment context");
            return null;
        }
       // p("Got exp context: " + exp);
        getImage();
       
        reloads++;
        try {
            /* Write the image to a buffer. */
            imagebuffer = new ByteArrayOutputStream();

            ImageIO.write(image, "png", imagebuffer);
            /* Return a stream from the buffer. */
            imagebuffer.flush();
            ByteArrayInputStream res= new ByteArrayInputStream(
                    imagebuffer.toByteArray());
            imagebuffer.close();
            return res;
        } catch (IOException e) {
            err("Could not create bfmask image", e);
            return null;
        }
    }

     
    private static void err(String msg, Exception ex) {
        //system.out.println("BfMaskImage: " + msg);
        Logger.getLogger(ETMaskImage.class.getName()).log(Level.SEVERE, msg, ex);
    }

    private static void err(String msg) {
        //system.out.println("BfMaskImage: " + msg);
        Logger.getLogger(ETMaskImage.class.getName()).log(Level.SEVERE, msg);
    }

    private static void warn(String msg) {
        //system.out.println("BfMaskImage: " + msg);
        Logger.getLogger(ETMaskImage.class.getName()).log(Level.WARNING, msg);
    }

    private static void p(String msg) {
        //system.out.println("BfMaskImage: " + msg);
        Logger.getLogger(ETMaskImage.class.getName()).log(Level.INFO, msg);
    }
	public GradientPanel getGradient() {
		return pan.getGradient();
	}
	public void setMin(int gradmin) {
		pan.setMin(gradmin);
		
	}
	public void setMax(int gradmin) {
		pan.setMax(gradmin);
		
	}
}
