/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.carlosalbertoxw.rest;

import com.carlosalbertoxw.utilities.JSON;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Carlos
 */
@RestController
public class Inicio {

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = "application/json;charset=utf-8")
    private String inicio() {
        return JSON.successToJson("Inicio");
    }
}
