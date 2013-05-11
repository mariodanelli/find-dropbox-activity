package danelli.mario.dropbox.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class Utilities {
	
	public static Properties configure(String propsFile) throws IOException{
		Properties props = new Properties();
	    FileInputStream fis = new FileInputStream(propsFile);
	    props.load(fis);    
	    fis.close();
	    return props;
	}
	
	public static Connection getConnection(String dbms_driver, String dbms_host, String dbms_port, String dbms_db, String dbms_user, String dbms_password) throws ClassNotFoundException, SQLException{
		synchronized(Utilities.class){
			Class.forName(dbms_driver);
			Connection conn = DriverManager.getConnection("jdbc:postgresql://" + dbms_host + ":" + dbms_port + "/" + dbms_db, dbms_user, dbms_password);
			
			return conn;
		}
	}
	
}
