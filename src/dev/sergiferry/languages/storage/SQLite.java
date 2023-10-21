package dev.sergiferry.languages.storage;

import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Creado por SergiFerry el 25/09/2023
 */
public class SQLite extends Database{

    public SQLite(String database) {
        super(database, Type.SQLITE);
    }

    @Override
    public void connect() throws SQLException, ClassNotFoundException {
        if(connection != null && !connection.isClosed()) connection.close();
        Class.forName("org.sqlite.JDBC");
        this.connection = DriverManager.getConnection("jdbc:sqlite:plugins/LanguagesAPI/" + this.database + ".db");
    }
}
