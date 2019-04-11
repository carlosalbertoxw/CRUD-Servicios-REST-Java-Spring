/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.carlosalbertoxw.interfaces;

import com.carlosalbertoxw.models.Note;
import java.util.List;

/**
 *
 * @author Carlos
 */
public interface IDTO {

    public List<Note> list();

    public String save(Object object);

    public Note get(Long id);

    public String update(Object object);

    public String delete(Long id);
}
