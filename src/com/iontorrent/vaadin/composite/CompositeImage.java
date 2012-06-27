/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.composite;

import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.iontorrent.expmodel.CompositeExperiment;
import com.iontorrent.expmodel.DatBlock;
import com.iontorrent.maskview.CompositeDensityPanel;
import com.iontorrent.rawdataaccess.pgmacquisition.RawType;
import com.iontorrent.rawdataaccess.wells.BfMaskFlag;
import com.iontorrent.utils.ProgressListener;
import com.iontorrent.wellmodel.BfHeatMap;
import com.iontorrent.wellmodel.WellCoordinate;
import com.vaadin.terminal.StreamResource;

/**
 *
 * @author Chantal Roth 
 * chantal.roth@lifetech.com
 */
public class CompositeImage implements StreamResource.StreamSource {

    ByteArrayOutputStream imagebuffer = null;
    CompositeExperiment comp;
    int reloads = 0;
    CompositeDensityPanel pan;
    BfMaskFlag flag;
    ProgressListener progress;
    RenderedImage image;
    int bucket;
    int flow;
    int frame;
    public CompositeImage(CompositeExperiment comp, BfMaskFlag flag, ProgressListener progress, int flow, int frame, int bucket) {
        this.comp = comp;
        this.flag = flag;
        this.flow = flow;
        this.frame = frame;
        this.bucket = bucket;
        this.progress = progress;
        if (comp != null) {
            pan = new CompositeDensityPanel(comp, null);           
            p("Created density panel for flag "+flag);
            pan.setFlag(flag);

        }
    }

    public WellCoordinate getWellCoordinate(int x, int y) {
        if (pan == null) return null;
        return pan.getWellCoordinate(x, y);
    }
    /* We need to implement this method that returns
     * the resource as a stream. */


	public RenderedImage getImage() {
		if (image != null) return image;
		//p("GetStream called, bucket is "+bucket);
        if (comp == null) {
            err("Got no CompositeExperiment context");
            return null;
        }
       // p("Got CompositeExperiment context: " + comp.getBlocks());

        BfHeatMap mask = BfHeatMap.getMask(comp.getRootContext());
        RawType type = RawType.ACQ;
       
     //   CompositeWellDensity gen = new CompositeWellDensity(comp, type, flow, frame, bucket);
        String msg = null;
        String file =mask.getImageFile("composite", flag, flow, type, frame);
              
        /* Create an image and draw something on it. */
      
        mask.readData(flag, file);

        pan.setScoreMask(mask, flag, bucket, type, 0, frame);

        image = pan.myCreateImage(1200, 1200);
        return image;
	}
	public void markBlocks(ArrayList<DatBlock> foundblocks) {
		pan.setFoundBlocks(foundblocks);		
		pan.redrawImage();
		image = null;
        
		
	}
    public InputStream getStream() {
        
    	image = getImage(); 
     //   p("Created image: " + image);
      //  reloads++;
        try {
            /* Write the image to a buffer. */
            imagebuffer = new ByteArrayOutputStream();

            ImageIO.write(image, "png", imagebuffer);
            /* Return a stream from the buffer. */
            return new ByteArrayInputStream(
                    imagebuffer.toByteArray());
        } catch (IOException e) {
            err("Could not create bfmask image", e);
            return null;
        }
    }

    private static void err(String msg, Exception ex) {
        //system.out.println("CompositeImage: " + msg);
        Logger.getLogger(CompositeImage.class.getName()).log(Level.SEVERE, msg, ex);
    }

    private static void err(String msg) {
        //system.out.println("CompositeImage: " + msg);
        Logger.getLogger(CompositeImage.class.getName()).log(Level.SEVERE, msg);
    }

    private static void warn(String msg) {
        //system.out.println("CompositeImage: " + msg);
        Logger.getLogger(CompositeImage.class.getName()).log(Level.WARNING, msg);
    }

    private static void p(String msg) {
        //system.out.println("CompositeImage: " + msg);
        Logger.getLogger(CompositeImage.class.getName()).log(Level.INFO, msg);
    }

	

}
