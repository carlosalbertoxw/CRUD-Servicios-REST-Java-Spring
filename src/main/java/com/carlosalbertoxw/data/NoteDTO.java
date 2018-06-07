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

/**
 *
 * @author Carlos
 */
public class NoteDTO {

    public String delete(Note item) {
        Connection connection = DataAccess.getConnection();
        PreparedStatement preparedStatement = null;
        try {
            int i = 1;
            String mensaje = "", sql = "CALL deleteNote(?)";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setLong(i++, item.getId());
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                mensaje = rs.getString("Mensaje");
            }
            return mensaje;
        } catch (SQLException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            return null;
        } finally {
            DataAccess.closeConnection(connection, preparedStatement);
        }
    }

    public String update(Note item) {
        Connection connection = DataAccess.getConnection();
        PreparedStatement preparedStatement = null;
        try {
            int i = 1;
            String mensaje = "", sql = "CALL updateNote(?,?,?)";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(i++, item.getTitle());
            preparedStatement.setString(i++, item.getText());
            preparedStatement.setLong(i++, item.getId());
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                mensaje = rs.getString("Mensaje");
            }
            return mensaje;
        } catch (SQLException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            return null;
        } finally {
            DataAccess.closeConnection(connection, preparedStatement);
        }
    }

    public String save(Note item) {
        Connection connection = DataAccess.getConnection();
        PreparedStatement preparedStatement = null;
        try {
            int i = 1;
            String mensaje = "", sql = "CALL insertNote(?,?)";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(i++, item.getTitle());
            preparedStatement.setString(i++, item.getText());
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                mensaje = rs.getString("Mensaje");
            }
            return mensaje;
        } catch (SQLException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            return null;
        } finally {
            DataAccess.closeConnection(connection, preparedStatement);
        }
    }

    public Note get(Note item) {
        Connection connection = DataAccess.getConnection();
        PreparedStatement preparedStatement = null;
        try {
            int i = 1;
            String sql = "SELECT * FROM notes WHERE id=?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setLong(i++, item.getId());
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                item.setId(rs.getLong("id"));
                item.setTitle(rs.getString("title"));
                item.setText(rs.getString("text"));
            }
            return item;
        } catch (SQLException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            return null;
        } finally {
            DataAccess.closeConnection(connection, preparedStatement);
        }
    }

    public List<Note> list() {
        Connection connection = DataAccess.getConnection();
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
            DataAccess.closeConnection(connection, preparedStatement);
        }
    }
}
