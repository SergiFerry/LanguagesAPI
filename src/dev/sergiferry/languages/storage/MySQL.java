package dev.sergiferry.languages.storage;

import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Creado por SergiFerry el 25/09/2023
 */
public class MySQL extends Database{

    protected String host;
    protected int port;
    protected String user;
    protected String password;

    public MySQL(String host, int port, String database, String user, String password) {
        super(database, Type.MYSQL);
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
    }

    @Override
    public void connect() throws SQLException, ClassNotFoundException {
        if(connection != null && !connection.isClosed()) connection.close();
        Class.forName("com.mysql.jdbc.Driver");
        String url = "jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database;
        this.connection = DriverManager.getConnection(url, this.user, this.password);
    }
}
