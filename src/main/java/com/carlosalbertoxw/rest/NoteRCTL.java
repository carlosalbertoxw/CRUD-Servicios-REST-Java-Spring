/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.carlosalbertoxw.rest;

import com.carlosalbertoxw.data.NoteDTO;
import com.carlosalbertoxw.models.Note;
import com.carlosalbertoxw.utilities.JSON;
import com.carlosalbertoxw.utilities.Mensaje;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Carlos
 */
@RestController
public class NoteRCTL {

    private NoteDTO dto;
    private String token;

    public NoteRCTL() {
        this.dto = new NoteDTO();
        this.token = "qwerty";
    }

    @RequestMapping(value = "/api/notes/{id}", method = RequestMethod.DELETE, produces = "application/json;charset=utf-8")
    private String delete(@PathVariable("id") Long id, @RequestHeader("Authentication") String authentication) {
        try {
            if (authentication.equals(this.token)) {
                Note item = new Note();
                item.setId(id);
                String mensaje = dto.delete(item);
                if (mensaje.equals("OK")) {
                    return JSON.successToJson(Mensaje.SUCCESSFUL_DELETE);
                } else {
                    return JSON.errorToJson(Mensaje.ERROR_DELETE);
                }
            } else {
                return JSON.errorToJson(Mensaje.ERROR_SESSION_VALIDATION);
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            return JSON.errorToJson(Mensaje.ERROR_PROCESSING_DATA);
        }
    }

    @RequestMapping(value = "/api/notes/{id}", method = RequestMethod.PUT, produces = "application/json;charset=utf-8")
    private String update(@PathVariable("id") Long id, @RequestBody Note item, @RequestHeader("Authentication") String authentication) {
        try {
            if (authentication.equals(this.token)) {
                item.setId(id);
                String mensaje = dto.update(item);
                if (mensaje.equals("OK")) {
                    return JSON.successToJson(Mensaje.SUCCESSFUL_UPDATE);
                } else {
                    return JSON.errorToJson(Mensaje.ERROR_UPDATE);
                }
            } else {
                return JSON.errorToJson(Mensaje.ERROR_SESSION_VALIDATION);
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            return JSON.errorToJson(Mensaje.ERROR_PROCESSING_DATA);
        }
    }

    @RequestMapping(value = "/api/notes", method = RequestMethod.POST, produces = "application/json;charset=utf-8")
    private String save(
            @RequestBody Note item,
            @RequestHeader("Authentication") String authentication) {
        try {
            if (authentication.equals(token)) {
                String mensaje = dto.save(item);
                if (mensaje.equals("OK")) {
                    return JSON.successToJson(Mensaje.SUCCESSFUL_SAVE);
                } else {
                    return JSON.errorToJson(Mensaje.ERROR_SAVE);
                }
            } else {
                return JSON.errorToJson(Mensaje.ERROR_SESSION_VALIDATION);
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            return JSON.errorToJson(Mensaje.ERROR_PROCESSING_DATA);
        }
    }

    @RequestMapping(value = "/api/notes/{id}", method = RequestMethod.GET, produces = "application/json;charset=utf-8")
    private String get(@PathVariable("id") Long id, @RequestHeader("Authentication") String authentication) {
        try {
            if (authentication.equals(this.token)) {
                Note item = new Note();
                item.setId(id);
                item = dto.get(item);
                if (item != null) {
                    return JSON.successToJson(item);
                } else {
                    return JSON.errorToJson(Mensaje.ERROR_CONSULT);
                }
            } else {
                return JSON.errorToJson(Mensaje.ERROR_SESSION_VALIDATION);
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            return JSON.errorToJson(Mensaje.ERROR_PROCESSING_DATA);
        }
    }

    @RequestMapping(value = "/api/notes", method = RequestMethod.GET, produces = "application/json;charset=utf-8")
    private String list(@RequestHeader("Authentication") String authentication) {
        try {
            if (authentication.equals(this.token)) {
                List<Note> list = dto.list();
                if (list != null) {
                    return JSON.successToJson(list);
                } else {
                    return JSON.errorToJson(Mensaje.ERROR_CONSULT);
                }
            } else {
                return JSON.errorToJson(Mensaje.ERROR_SESSION_VALIDATION);
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            return JSON.errorToJson(Mensaje.ERROR_PROCESSING_DATA);
        }
    }
}
