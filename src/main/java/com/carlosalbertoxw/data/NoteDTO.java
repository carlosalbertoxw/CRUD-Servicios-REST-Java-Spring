/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.carlosalbertoxw.data;

import com.carlosalbertoxw.models.Note;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Carlos
 */
@Repository
@Qualifier("NoteDTO")
public class NoteDTO {

    @Autowired
    private DataAccess dataAccess;

    public NoteDTO() {
    }

    public String delete(Long id) {
        Connection connection = dataAccess.getConnection();
        PreparedStatement preparedStatement = null;
        try {
            connection.setAutoCommit(false);
            int i = 1;
            String mensaje = "", sql = "CALL deleteNote(?)";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setLong(i++, id);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                mensaje = rs.getString("Mensaje");
                connection.commit();
            } else {
                connection.rollback();
            }
            return mensaje;
        } catch (SQLException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            try {
                connection.rollback();
            } catch (SQLException ex) {
                Logger.getLogger(NoteDTO.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        } finally {
            dataAccess.closeConnection(connection, preparedStatement);
        }
    }

    public String update(Object object) {
        Connection connection = dataAccess.getConnection();
        PreparedStatement preparedStatement = null;
        try {
            Note note = (Note) object;
            connection.setAutoCommit(false);
            int i = 1;
            String mensaje = "", sql = "CALL updateNote(?,?,?)";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(i++, note.getTitle());
            preparedStatement.setString(i++, note.getText());
            preparedStatement.setLong(i++, note.getId());
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                mensaje = rs.getString("Mensaje");
                connection.commit();
            } else {
                connection.rollback();
            }
            return mensaje;
        } catch (SQLException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            try {
                connection.rollback();
            } catch (SQLException ex) {
                Logger.getLogger(NoteDTO.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        } finally {
            dataAccess.closeConnection(connection, preparedStatement);
        }
    }

    public String save(Object object) {
        Connection connection = dataAccess.getConnection();
        PreparedStatement preparedStatement = null;
        try {
            Note note = (Note) object;
            connection.setAutoCommit(false);
            int i = 1;
            String mensaje = "", sql = "CALL insertNote(?,?)";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(i++, note.getTitle());
            preparedStatement.setString(i++, note.getText());
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                mensaje = rs.getString("Mensaje");
                connection.commit();
            } else {
                connection.rollback();
            }
            return mensaje;
        } catch (SQLException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            try {
                connection.rollback();
            } catch (SQLException ex) {
                Logger.getLogger(NoteDTO.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        } finally {
            dataAccess.closeConnection(connection, preparedStatement);
        }
    }

    public Note get(Long id) {
        Connection connection = dataAccess.getConnection();
        PreparedStatement preparedStatement = null;
        try {
            int i = 1;
            String sql = "SELECT * FROM notes WHERE id=?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setLong(i++, id);
            ResultSet rs = preparedStatement.executeQuery();
            Note note = new Note();
            if (rs.next()) {
                note.setId(rs.getLong("id"));
                note.setTitle(rs.getString("title"));
                note.setText(rs.getString("text"));
            }
            return note;
        } catch (SQLException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            return null;
        } finally {
            dataAccess.closeConnection(connection, preparedStatement);
        }
    }

    public List<Note> list() {
        Connection connection = dataAccess.getConnection();
        PreparedStatement preparedStatement = null;
        try {
            List<Note> list = new ArrayList<>();
            String sql = "SELECT * FROM notes";
            preparedStatement = connection.prepareStatement(sql);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                Note item = new Note();
                item.setId(rs.getLong("id"));
                item.setTitle(rs.getString("title"));
                item.setText(rs.getString("text"));
                list.add(item);
            }
            return list;
        } catch (SQLException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            return null;
        } finally {
            dataAccess.closeConnection(connection, preparedStatement);
        }
    }
}
