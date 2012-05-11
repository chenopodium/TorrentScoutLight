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
public class FileComparator implements Comparator<File> {

   // Comparator interface requires defining compare method.
    public int compare(File filea, File fileb) {
        //... Sort directories before files,
        //    otherwise alphabetical ignoring case.
        if (filea.isDirectory() && !fileb.isDirectory()) {
            return -1;

        } else if (!filea.isDirectory() && fileb.isDirectory()) {
            return 1;

        } else {
            return filea.getName().compareToIgnoreCase(fileb.getName());
        }
    }
}
