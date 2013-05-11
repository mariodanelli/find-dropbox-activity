package danelli.mario.dropbox;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import javax.swing.JOptionPane;

import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.RequestTokenPair;
import com.dropbox.client2.session.Session.AccessType;
import com.dropbox.client2.session.WebAuthSession;
import com.dropbox.client2.session.WebAuthSession.WebAuthInfo;

import danelli.mario.dropbox.db.bean.UserBean;
import danelli.mario.dropbox.db.dao.ActivityDAO;
import danelli.mario.dropbox.db.dao.UserDAO;
import danelli.mario.dropbox.util.Utilities;

public class RetrieveCredentials {
	
	private static final String CONFIGURATION_FILE = "./conf/configuration.properties";
	
	private static final String APP_KEY;
	private static final String APP_SECRET;
	
	private static final String ACCESS_TYPE;

	private static final String DBMS_USER;
	private static final String DBMS_PASSWORD;

	private static final String DBMS_HOST;
	private static final String DBMS_PORT;
	private static final String DBMS_DB_NAME;
	
	private static final String DBMS_DRIVER;
	
	private static final int GET_TOKEN_SLEEP_MILLS;
	private static final int GET_TOKEN_NUM_MAX_ATTEMPTS;
	
	static {
		Properties props = null;
		try {
			props = Utilities.configure(CONFIGURATION_FILE);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		APP_KEY = props.getProperty("APP_KEY").trim();
		APP_SECRET = props.getProperty("APP_SECRET").trim();

		ACCESS_TYPE = props.getProperty("ACCESS_TYPE").trim();
		
		DBMS_USER = props.getProperty("DBMS_USER").trim();
		DBMS_PASSWORD = props.getProperty("DBMS_PASSWORD").trim();

		DBMS_HOST = props.getProperty("DBMS_HOST").trim();
		DBMS_PORT = props.getProperty("DBMS_PORT").trim();
		DBMS_DB_NAME = props.getProperty("DBMS_DB_NAME").trim();
		
		DBMS_DRIVER = props.getProperty("DBMS_DRIVER").trim();

		GET_TOKEN_SLEEP_MILLS = Integer.parseInt(props.getProperty("GET_TOKEN_SLEEP_MILLS").trim());
		GET_TOKEN_NUM_MAX_ATTEMPTS = Integer.parseInt(props.getProperty("GET_TOKEN_NUM_MAX_ATTEMPTS").trim());
	}
	
	private RetrieveCredentials() throws IOException, DropboxException, URISyntaxException, InterruptedException, ExecutionException, ClassNotFoundException, SQLException{
		
		Connection conn = Utilities.getConnection(DBMS_DRIVER, DBMS_HOST, DBMS_PORT, DBMS_DB_NAME, DBMS_USER, DBMS_PASSWORD);
		
		UserDAO userDAO = new UserDAO(conn);
		
		UserBean userBean = userDAO.selectFirstUserWithoutCredentials();
		
		conn.close();
		
		if (userBean != null){
		
			AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
			AccessType accessType = AccessType.valueOf(ACCESS_TYPE);
			
			WebAuthSession session = new WebAuthSession(appKeys, accessType);
			
			WebAuthInfo authInfo = session.getAuthInfo();
	
	        RequestTokenPair pair = authInfo.requestTokenPair;
	        String url = authInfo.url;
			
	        Desktop.getDesktop().browse(new URL(url).toURI());
	        JOptionPane.showMessageDialog(null, "Enter DROPBOX credentials for user " + userBean.getEmail() + " and allow the application.");
	             
	        boolean found = false;
	        for(int i = 0; ( (!found) && (i < GET_TOKEN_NUM_MAX_ATTEMPTS) ); i++){
		        try {
		        	session.retrieveWebAccessToken(pair);
		        	found = true;
		        } catch (DropboxException ex){
		        	
		        } finally {
		        	Thread.sleep(GET_TOKEN_SLEEP_MILLS);
		        }
		   	}
	
	        AccessTokenPair tokens = session.getAccessTokenPair();
	        
	        System.out.println("Retrieved token for user '" + userBean.getEmail() + "' with key '" + tokens.key + "' and secret '" + tokens.secret + "'");
	        
	        userBean.setKey(tokens.key);
	        userBean.setSecret(tokens.secret);
	        
	        
	        conn = Utilities.getConnection(DBMS_DRIVER, DBMS_HOST, DBMS_PORT, DBMS_DB_NAME, DBMS_USER, DBMS_PASSWORD);
			
	        conn.setAutoCommit(false);
	        userDAO = new UserDAO(conn);
			
	        try{
				boolean ret = userDAO.setTokenKeyAndSecret(userBean);

				if (ret){
					ActivityDAO activityDAO = new ActivityDAO(conn);

					ret = activityDAO.insertEmptyActivity(userBean.getId());

					if (ret){
						conn.commit();
						System.out.println("Updated user '" + userBean.getEmail() + "' with key '" + tokens.key + "' and secret '" + tokens.secret + "'");
				    } else {
						conn.rollback();
					}
				} else {
					conn.rollback();
				}
	        } catch (Exception ex){
	        	conn.rollback();
	        	ex.printStackTrace();
	        } finally {
				if (conn != null){
					conn.close();
				}
	        }
		} else {
			JOptionPane.showMessageDialog(null, "None user has to allow the application.");
	    }
		System.exit(0);
	}
	
	
	public static void main(String[] args) {
		try {
			//RetrieveCredentials retrieveCredentials = 
			new RetrieveCredentials();
		} catch (Exception ex){
			ex.printStackTrace();
		}
	}
}
