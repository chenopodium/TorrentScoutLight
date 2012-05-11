/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.utils;

import java.io.File;
import java.util.Comparator;

/**
 *
 * @author Chantal Roth 
 * chantal.roth@lifetech.com
 */
public class FileDateComparator implements Comparator<File> {

   // Comparator interface requires defining compare method.
    public int compare(File filea, File fileb) {
       
        return (int)(fileb.lastModified()- filea.lastModified());
        
    }
}
