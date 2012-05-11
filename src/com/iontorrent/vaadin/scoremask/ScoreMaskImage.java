/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.scoremask;

import java.awt.Point;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.guiutils.heatmap.GradientPanel;
import com.iontorrent.rawdataaccess.wells.ScoreMaskFlag;
import com.iontorrent.results.scores.ScoreMask;
import com.iontorrent.scoreview.ScoreDensityPanel;
import com.iontorrent.wellmodel.WellCoordinate;
import com.vaadin.terminal.StreamResource;

/**
 *
 * @author Chantal Roth 
 * chantal.roth@lifetech.com
 */
public class ScoreMaskImage implements StreamResource.StreamSource {

    ByteArrayOutputStream imagebuffer = null;
    ExperimentContext exp;    
    ScoreDensityPanel pan;
    RenderedImage image;
 
    public ScoreMaskImage(ExperimentContext exp, ScoreMaskFlag flag, int bucket) {
        this.exp = exp;
        if (exp != null) {
            pan = new ScoreDensityPanel(exp);
            p("Created score density panel");
            
            pan.setFlag(flag);
            pan.setExp(exp);            
           
            ScoreMask mask = ScoreMask.getMask(exp, exp.getWellContext());
            mask.readData(flag);
            if (!mask.hasImage(flag)){
            	p("Don't have image for flag "+flag+" yet");
            }
           
           pan.setScoreMask(mask, flag,  bucket);
 
        }
    }
    public void setMin(int gradmin) {
		pan.setMin(gradmin);
		
	}
	public void setMax(int gradmin) {
		pan.setMax(gradmin);
		
	}
    public GradientPanel getGradient() {
		return pan.getGradient();
	}
    public void repaint() {
    	image = null;
    	pan.redrawImage();
    	getImage();
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
    public Point getPointFromWell(WellCoordinate coord) {
    	return pan.getImagePointFromWell(coord);
    }
    public InputStream getStream() {
      //  p("GetStream called");
        if (exp == null) {
            err("Got no experiment context");
            return null;
        }
       // p("Got exp context: " + exp);
        getImage();
       
      
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
        //system.out.println("ScoreMaskImage: " + msg);
        Logger.getLogger(ScoreMaskImage.class.getName()).log(Level.SEVERE, msg, ex);
    }

    private static void err(String msg) {
        //system.out.println("ScoreMaskImage: " + msg);
        Logger.getLogger(ScoreMaskImage.class.getName()).log(Level.SEVERE, msg);
    }

    private static void warn(String msg) {
        //system.out.println("ScoreMaskImage: " + msg);
        Logger.getLogger(ScoreMaskImage.class.getName()).log(Level.WARNING, msg);
    }

    private static void p(String msg) {
        //system.out.println("ScoreMaskImage: " + msg);
        Logger.getLogger(ScoreMaskImage.class.getName()).log(Level.INFO, msg);
    }
}
