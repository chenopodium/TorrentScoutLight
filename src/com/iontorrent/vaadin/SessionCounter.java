/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin;

import java.util.HashMap;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class SessionCounter implements HttpSessionListener {

   private static Log log = LogFactory.getLog(SessionCounter.class);
    private static HashMap<String, Integer> map;

    public void sessionCreated(HttpSessionEvent se) {
        if (map == null) map = new HashMap<String, Integer>();
        String id = se.getSession().getId();
        Integer count = map.get(id);
        if (count == null) {
            count = new Integer(0);
            map.put(id, count);
        }
        p("sessionCreated. Got id:"+id+", count "+count);
        
    }

    public void sessionDestroyed(HttpSessionEvent se) {
        String id = se.getSession().getId();
//        Integer count = map.get(id);
//        if (count == null) {
//            count = new Integer(0);
//            map.put(id, count);
//        }
//        if (count > 0) {
//            count--;
//        }
//        if (count <1) 
        p("sessionDestroyed. Got id:"+id);
        if (map == null) map = new HashMap<String, Integer>();
        map.remove(id);
    }

    public static int getActiveSessions() {
        if (map == null) map = new HashMap<String, Integer>();
        int nr= map.keySet().size();
         p("getActiveSessions. Got:"+nr);
         return nr;
    }
    
    private static void p(String m) {
        //system.out.println("SessionCounter: "+m);
         log.info(m);
    }
}
