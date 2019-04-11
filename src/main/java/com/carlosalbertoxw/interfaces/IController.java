/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.carlosalbertoxw.interfaces;

import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 *
 * @author Carlos
 */
public interface IController {

    public Map<String, Object> list(@RequestHeader("Authentication") String authentication);

    public Map<String, Object> save(@RequestHeader("Authentication") String authentication, @RequestBody Map map);

    public Map<String, Object> get(@RequestHeader("Authentication") String authentication, @PathVariable("id") Long id);

    public Map<String, Object> update(@RequestHeader("Authentication") String authentication, @PathVariable("id") Long id, @RequestBody Object object);

    public Map<String, Object> delete(@RequestHeader("Authentication") String authentication, @PathVariable("id") Long id);
}
