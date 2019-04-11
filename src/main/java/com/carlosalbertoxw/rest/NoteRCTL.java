/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.carlosalbertoxw.rest;

import com.carlosalbertoxw.interfaces.IController;
import com.carlosalbertoxw.interfaces.IDTO;
import com.carlosalbertoxw.models.Note;
import com.carlosalbertoxw.utilities.JSON;
import com.carlosalbertoxw.utilities.Mensaje;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class NoteRCTL implements IController {

    @Autowired
    @Qualifier("NoteDTO")
    private IDTO noteDTO;
    private String token;

    public NoteRCTL() {
        this.token = "qwerty";
    }

    @Override
    @RequestMapping(value = "/api/notes/{id}", method = RequestMethod.DELETE, produces = "application/json;charset=utf-8")
    public Map<String, Object> delete(@RequestHeader("Authentication") String authentication, @PathVariable("id") Long id) {
        try {
            if (authentication.equals(this.token)) {
                String mensaje = noteDTO.delete(id);
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

    @Override
    @RequestMapping(value = "/api/notes/{id}", method = RequestMethod.PUT, produces = "application/json;charset=utf-8")
    public Map<String, Object> update(@RequestHeader("Authentication") String authentication, @PathVariable("id") Long id, @RequestBody Object object) {
        try {
            if (authentication.equals(this.token)) {
                Note note = (Note) object;
                note.setId(id);
                String mensaje = noteDTO.update(note);
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

    @Override
    @RequestMapping(value = "/api/notes", method = RequestMethod.POST, produces = "application/json;charset=utf-8")
    public Map<String, Object> save(@RequestHeader("Authentication") String authentication, @RequestBody Map map) {
        try {
            if (authentication.equals(token)) {
                ObjectMapper mapper = new ObjectMapper();
                Note note = mapper.convertValue(map, Note.class);
                String mensaje = noteDTO.save(note);
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
    public Map<String, Object> get(@RequestHeader("Authentication") String authentication, @PathVariable("id") Long id) {
        try {
            if (authentication.equals(this.token)) {
                Note item = noteDTO.get(id);
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

    @Override
    @RequestMapping(value = "/api/notes", method = RequestMethod.GET, produces = "application/json;charset=utf-8")
    public Map<String, Object> list(@RequestHeader("Authentication") String authentication) {
        try {
            if (authentication.equals(this.token)) {
                List<Note> list = noteDTO.list();
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
