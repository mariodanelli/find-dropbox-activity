package danelli.mario.dropbox;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DeltaEntry;
import com.dropbox.client2.DropboxAPI.DeltaPage;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;
import com.dropbox.client2.session.WebAuthSession;

import danelli.mario.dropbox.db.bean.ActivityBean;
import danelli.mario.dropbox.db.bean.UserBean;
import danelli.mario.dropbox.db.dao.ActivityDAO;
import danelli.mario.dropbox.db.dao.UserDAO;
import danelli.mario.dropbox.util.Utilities;

public class ActivitiesThread implements Runnable {
	
	private DropboxAPI<WebAuthSession> mDBApi = null;	
	private DeltaPage<Entry> delta = null;
	private DropboxAPI.Account account = null;
	
	private UserBean userBean = null;
	
	private static final String CONFIGURATION_FILE = "./conf/configuration.properties";
	
	private static long CHECK_ACTIVITIES_THREAD_PERIOD = 60000;
	
	private static final String APP_KEY;
	private static final String APP_SECRET;
	
	private static final String ACCESS_TYPE;

	private static final boolean SAVE_DB;
	
	private static final String DBMS_USER;
	private static final String DBMS_PASSWORD;

	private static final String DBMS_HOST;
	private static final String DBMS_PORT;
	private static final String DBMS_DB_NAME;
		
	private static final String DBMS_DRIVER;
	
	static {
		Properties props = null;
		try {
			props = Utilities.configure(CONFIGURATION_FILE);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		CHECK_ACTIVITIES_THREAD_PERIOD = Long.parseLong(props.getProperty("CHECK_ACTIVITIES_THREAD_PERIOD").trim());
		
		APP_KEY = props.getProperty("APP_KEY").trim();
		APP_SECRET = props.getProperty("APP_SECRET").trim();

		ACCESS_TYPE = props.getProperty("ACCESS_TYPE").trim();
		
		SAVE_DB = Boolean.parseBoolean(props.getProperty("SAVE_DB").trim());

		DBMS_USER = props.getProperty("DBMS_USER").trim();
		DBMS_PASSWORD = props.getProperty("DBMS_PASSWORD").trim();

		DBMS_HOST = props.getProperty("DBMS_HOST").trim();
		DBMS_PORT = props.getProperty("DBMS_PORT").trim();
		DBMS_DB_NAME = props.getProperty("DBMS_DB_NAME").trim();
		
		DBMS_DRIVER = props.getProperty("DBMS_DRIVER").trim();
	}
	
	private ActivitiesThread(UserBean userBean) throws IOException, DropboxException, URISyntaxException, InterruptedException, ExecutionException, ClassNotFoundException, SQLException{
		
		this.userBean = userBean;
		
		AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
		AccessType accessType = AccessType.valueOf(ACCESS_TYPE.toUpperCase());
		
		WebAuthSession session = new WebAuthSession(appKeys, accessType);
		
		AccessTokenPair accessTokenPair = new AccessTokenPair(userBean.getKey(), userBean.getSecret());
			
		session.setAccessTokenPair(accessTokenPair);	
			
		mDBApi = new DropboxAPI<WebAuthSession>(session);
			
		account = mDBApi.accountInfo();
		
		Connection conn = Utilities.getConnection(DBMS_DRIVER, DBMS_HOST, DBMS_PORT, DBMS_DB_NAME, DBMS_USER, DBMS_PASSWORD);
		UserDAO userDAO = new UserDAO(conn);
		
		String cursorString = userDAO.getLastCursor(this.userBean.getId());
		if ("".equals(cursorString)){
			cursorString = null;
		}				
		conn.close();
		
        System.out.println("Account: " + account.displayName);
		delta = mDBApi.delta(cursorString);
		storeDeltaEntries(delta);
				
        Thread thread = new Thread(this, "Check DROPBOX activity for " + userBean.getEmail());
		thread.start();
	}
	
	public static void main(String[] args) {
		try {
			init();
		} catch (Exception ex){
			ex.printStackTrace();
		}
	}
	

	private static void init() throws IOException, DropboxException, URISyntaxException, InterruptedException, ExecutionException, ClassNotFoundException, SQLException{	
				
		Connection conn = Utilities.getConnection(DBMS_DRIVER, DBMS_HOST, DBMS_PORT, DBMS_DB_NAME, DBMS_USER, DBMS_PASSWORD);
		UserDAO userDAO = new UserDAO(conn);
		
		ArrayList<UserBean> alUsers = userDAO.selectEntriesWithCredentials();
		
		for(UserBean userBean : alUsers){
			new ActivitiesThread(userBean);
		}
        
		conn.close();        
	}

	@Override
	public void run() {
		
		while(true){

	        try{
				Thread.sleep(CHECK_ACTIVITIES_THREAD_PERIOD);
			} catch (InterruptedException ex){
				
			}
			
			try {
				delta = mDBApi.delta(delta.cursor);
			} catch (DropboxException e) {
				e.printStackTrace();
			}
			
			try {
				storeDeltaEntries(delta);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
		}
		
	}
	
	private void storeDeltaEntries(DeltaPage<Entry> delta) throws ClassNotFoundException, SQLException{
		if (SAVE_DB){
			System.out.println(userBean.getEmail() + " * " + new Timestamp(System.currentTimeMillis()) + " * Num. entries: '" + delta.entries.size() + "'");
			int i = 0;
			
			Connection connActivity = Utilities.getConnection(DBMS_DRIVER, DBMS_HOST, DBMS_PORT, DBMS_DB_NAME, DBMS_USER, DBMS_PASSWORD);
			ActivityDAO activityDAO = new ActivityDAO(connActivity);
			
			Connection connUser = Utilities.getConnection(DBMS_DRIVER, DBMS_HOST, DBMS_PORT, DBMS_DB_NAME, DBMS_USER, DBMS_PASSWORD);
			UserDAO userDAO = new UserDAO(connUser);
			
			this.userBean.setCursor(delta.cursor);
			userDAO.updateCursor(this.userBean);
			
			for(DeltaEntry<Entry> entry : delta.entries){
				System.out.print(userBean.getEmail() + " * " + ++i + " - LCPATH: '" + entry.lcPath + "'");
				if (entry.metadata != null)
					System.out.print(" - SIZE: '" + entry.metadata.size + "' - MODIFIED: '" + entry.metadata.modified + "' - FILENAME: '" + entry.metadata.fileName() + "' - REV.: '" + entry.metadata.rev + "' - TYPE: '" + (entry.metadata.isDir ? "Directory" : "File") + "'");
				else
					System.out.print(" - Deleted");
				System.out.println();
				if (entry.metadata != null)
					activityDAO.insertNewActivity(new ActivityBean(this.userBean.getId(), 0, entry.lcPath, entry.metadata.fileName(), entry.metadata.rev, entry.metadata.size, entry.metadata.modified, entry.metadata.isDir ? "D" : "F", false, delta.cursor));
				else 
					activityDAO.insertNewActivity(new ActivityBean(this.userBean.getId(), 0, entry.lcPath, null, null, null, null, null, true, delta.cursor));
			}
			if (connUser != null)
				connUser.close();
			if (connActivity != null)
				connActivity.close();
		}
	}
	
}
