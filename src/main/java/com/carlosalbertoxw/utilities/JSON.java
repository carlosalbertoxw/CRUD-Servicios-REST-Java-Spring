/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.carlosalbertoxw.utilities;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Carlos
 */
public class JSON {

    private static Map<String, Object> aJson(Boolean status, Object object) {
        try {
            Map map = new HashMap();
            map.put("status", status);
            map.put("result", object);

            return map;
        } catch (Exception e) {
            Logger.getLogger(JSON.class.getName()).log(Level.SEVERE, null, e);
            return null;
        }
    }

    public static Map<String, Object> errorToJson(String message) {
        return aJson(false, message);
    }

    public static Map<String, Object> successToJson(Object obj) {
        return aJson(true, obj);
    }
}
