/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.carlosalbertoxw.utilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Carlos
 */
public class JSON {

    private static String aJson(boolean status, Object object) {
        try {
            Map map = new HashMap();
            map.put("status", status);
            map.put("result", object);

            ObjectMapper mapper = new ObjectMapper();

            return mapper.writeValueAsString(map);
        } catch (Exception e) {
            Logger.getLogger(JSON.class.getName()).log(Level.SEVERE, null, e);
            return null;
        }
    }

    public static String errorToJson(String message) {
        return aJson(false, message);
    }

    public static String successToJson(Object obj) {
        return aJson(true, obj);
    }
}
