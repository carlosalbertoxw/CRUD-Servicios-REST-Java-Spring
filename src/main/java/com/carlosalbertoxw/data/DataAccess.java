/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.carlosalbertoxw.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.Component;

/**
 *
 * @author Carlos
 */
@Component
public class DataAccess {

    protected Connection getConnection() {
        Connection coneccion = null;
        try {
            //InitialContext ctx = new InitialContext();
            //DataSource ds = (DataSource) ctx.lookup("jdbc/agricolazf");
            //coneccion = ds.getConnection();
            Class.forName("com.mysql.jdbc.Driver");
            coneccion = DriverManager.getConnection("jdbc:mysql://localhost:3306/notes", "root", "qwerty");
        } catch (ClassNotFoundException | SQLException e) {
            Logger.getLogger(DataAccess.class.getName()).log(Level.SEVERE, null, e);
        }
        return coneccion;
    }

    protected void closeConnection(Connection coneccion, PreparedStatement preparedStatement) {
        try {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (coneccion != null) {
                coneccion.close();
            }
        } catch (SQLException e) {
            Logger.getLogger(DataAccess.class.getName()).log(Level.SEVERE, null, e);
        }
    }
}
