/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.wholechip;

import java.awt.Point;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.iontorrent.chipview.ChipDensityPanel;
import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.rawdataaccess.pgmacquisition.RawType;
import com.iontorrent.rawdataaccess.wells.BfMaskFlag;
import com.iontorrent.wellmodel.BfHeatMap;
import com.iontorrent.wellmodel.WellCoordinate;
import com.vaadin.terminal.StreamResource;

/**
 *
 * @author Chantal Roth 
 * chantal.roth@lifetech.com
 */
public class WholeChipImage implements StreamResource.StreamSource {

    ByteArrayOutputStream imagebuffer = null;
    ExperimentContext exp;
    int reloads = 0;
    ChipDensityPanel pan;
    RenderedImage image;
    RawType type;
    int flow;
    int frame;
    public WholeChipImage(ExperimentContext exp, RawType type, int flow, int frame, int bucket) {
        this.exp = exp;
        this.frame = frame;
        this.flow = flow;
        if (exp != null) {
        	BfHeatMap mask =  BfHeatMap.getMask(exp);
        	String file = mask.getImageFile("chip", BfMaskFlag.RAW, flow, type, frame);
        	p("reading data from file "+file);
        	mask.readData(BfMaskFlag.RAW, mask.getImageFile("chip",BfMaskFlag.RAW, flow, type, frame));
            pan = new ChipDensityPanel(exp);
            
                        
            pan.setContext(exp.getWellContext(), bucket);
            
            pan.setScoreMask(mask, bucket, flow, type,  frame);
            pan.repaint();
            p("Created chip density panel. Got image: "+getImage());
            
 
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
            image = pan.myCreateImage(600, 600);  
            if (image == null) {
            	p("Image is null of "+pan.getClass().getName());
            }
    	}
    	return image;
    }
    public InputStream getStream() {
        p("GetStream called");
        if (exp == null) {
            err("Got no experiment context");
            return null;
        }
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
            err("Could not create score image", e);
            return null;
        }
    }

     
    private static void err(String msg, Exception ex) {
        //system.out.println("WholeChipImage: " + msg);
        Logger.getLogger(WholeChipImage.class.getName()).log(Level.SEVERE, msg, ex);
    }

    private static void err(String msg) {
        //system.out.println("WholeChipImage: " + msg);
        Logger.getLogger(WholeChipImage.class.getName()).log(Level.SEVERE, msg);
    }

    private static void warn(String msg) {
        //system.out.println("WholeChipImage: " + msg);
        Logger.getLogger(WholeChipImage.class.getName()).log(Level.WARNING, msg);
    }

    private static void p(String msg) {
        //system.out.println("WholeChipImage: " + msg);
        Logger.getLogger(WholeChipImage.class.getName()).log(Level.INFO, msg);
    }
}
