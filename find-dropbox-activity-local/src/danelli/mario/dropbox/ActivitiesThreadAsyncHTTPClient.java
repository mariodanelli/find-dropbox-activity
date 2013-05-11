package danelli.mario.dropbox;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.FluentStringsMap;
import com.ning.http.client.Response;
import com.ning.http.client.oauth.ConsumerKey;
import com.ning.http.client.oauth.OAuthSignatureCalculator;
import com.ning.http.client.oauth.RequestToken;

import danelli.mario.dropbox.db.bean.ActivityBean;
import danelli.mario.dropbox.db.bean.UserBean;
import danelli.mario.dropbox.db.dao.ActivityDAO;
import danelli.mario.dropbox.db.dao.UserDAO;
import danelli.mario.dropbox.util.Utilities;

public class ActivitiesThreadAsyncHTTPClient implements Runnable {

	private int statusRunning = 0; //0 - ready to new call, 1 - waiting for results
	
	private String deltaCursor = null;
	
	private JSONObject accountJSONObj = null;
	
	private AsyncHttpClient client = null;
	private OAuthSignatureCalculator calc = null;
	
	private UserBean userBean = null;
	
	private static final String CONFIGURATION_FILE = "./conf/configuration.properties";
	
	private static long CHECK_ACTIVITIES_THREAD_PERIOD = 60000;
	
	private static final String APP_KEY;
	private static final String APP_SECRET;

	private static final boolean SAVE_DB;
	
	private static final String DBMS_USER;
	private static final String DBMS_PASSWORD;

	private static final String DBMS_HOST;
	private static final String DBMS_PORT;
	private static final String DBMS_DB_NAME;
		
	private static final String DBMS_DRIVER;
		
	private static final String DROPBOX_API_URL_ACCOUNT_INFO;
	private static final String DROPBOX_API_URL_DELTA;	
	
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
		
		SAVE_DB = Boolean.parseBoolean(props.getProperty("SAVE_DB").trim());

		DBMS_USER = props.getProperty("DBMS_USER").trim();
		DBMS_PASSWORD = props.getProperty("DBMS_PASSWORD").trim();

		DBMS_HOST = props.getProperty("DBMS_HOST").trim();
		DBMS_PORT = props.getProperty("DBMS_PORT").trim();
		DBMS_DB_NAME = props.getProperty("DBMS_DB_NAME").trim();
		
		DBMS_DRIVER = props.getProperty("DBMS_DRIVER").trim();
		
		DROPBOX_API_URL_ACCOUNT_INFO = props.getProperty("DROPBOX_API_URL_ACCOUNT_INFO").trim();
		DROPBOX_API_URL_DELTA = props.getProperty("DROPBOX_API_URL_DELTA").trim();
	}
	
	private ActivitiesThreadAsyncHTTPClient(UserBean userBean) throws IOException, URISyntaxException, InterruptedException, ExecutionException, ClassNotFoundException, SQLException{
		
		this.userBean = userBean;
		
		ConsumerKey consumer = new ConsumerKey(APP_KEY, APP_SECRET);
		RequestToken user = new RequestToken(userBean.getKey(), userBean.getSecret());
		calc = new OAuthSignatureCalculator(consumer, user);
		client = new AsyncHttpClient();
		Response response = client.preparePost(DROPBOX_API_URL_ACCOUNT_INFO).setSignatureCalculator(calc).execute().get();
		accountJSONObj = (JSONObject)JSONValue.parse(response.getResponseBody());
		
		Connection conn = Utilities.getConnection(DBMS_DRIVER, DBMS_HOST, DBMS_PORT, DBMS_DB_NAME, DBMS_USER, DBMS_PASSWORD);
		UserDAO userDAO = new UserDAO(conn);
		
		String cursorString = userDAO.getLastCursor(this.userBean.getId());
		if ("".equals(cursorString)){
			cursorString = null;
		}				
		conn.close();
		
        System.out.println("Account: " + accountJSONObj.get("display_name"));
		
        FluentStringsMap mapDeltaParams = new FluentStringsMap();
		mapDeltaParams.add("cursor", cursorString);
		
		++statusRunning;
		client.preparePost(DROPBOX_API_URL_DELTA).setSignatureCalculator(calc).setParameters(mapDeltaParams).execute(new AsyncCompletionHandler<Response>(){
        	@Override
            public Response onCompleted(Response response) throws Exception{
            	JSONObject deltaJSONObj = (JSONObject)JSONValue.parse(response.getResponseBody());
            	
            	deltaCursor = "" + deltaJSONObj.get("cursor");
				
            	storeDeltaEntries(deltaJSONObj);

        		statusRunning = 0;
            	return response;
            }
        	
        	@Override
             public void onThrowable(Throwable t){
        		statusRunning = 0;
        	 }
                 
        });
				
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
	

	private static void init() throws IOException, URISyntaxException, InterruptedException, ExecutionException, ClassNotFoundException, SQLException{	
				
		Connection conn = Utilities.getConnection(DBMS_DRIVER, DBMS_HOST, DBMS_PORT, DBMS_DB_NAME, DBMS_USER, DBMS_PASSWORD);
		UserDAO userDAO = new UserDAO(conn);
		
		ArrayList<UserBean> alUsers = userDAO.selectEntriesWithCredentials();
		
		for(UserBean userBean : alUsers){
			new ActivitiesThreadAsyncHTTPClient(userBean);
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
			
			if (statusRunning == 0){
				try {				
					FluentStringsMap mapDeltaParams = new FluentStringsMap();
					mapDeltaParams.add("cursor", this.deltaCursor);

	        		statusRunning = 1;
					client.preparePost(DROPBOX_API_URL_DELTA).setSignatureCalculator(calc).setParameters(mapDeltaParams).execute(new AsyncCompletionHandler<Response>(){
			        	@Override
			            public Response onCompleted(Response response) throws Exception{
			            	JSONObject deltaJSONObj = (JSONObject)JSONValue.parse(response.getResponseBody());
							
			            	deltaCursor = "" + deltaJSONObj.get("cursor");
							
			            	storeDeltaEntries(deltaJSONObj);

			        		statusRunning = 0;
			            	
							return response;
			            }

			        	@Override
			             public void onThrowable(Throwable t){
			        		statusRunning = 0;
			        	 }
			        });				
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}
		
	}
	
	private void storeDeltaEntries(JSONObject deltaJSONObj/*DeltaPage<Entry> delta*/) throws ClassNotFoundException, SQLException{
		if (SAVE_DB){
    		JSONArray entriesJSONArray = (JSONArray)deltaJSONObj.get("entries");
			System.out.println(userBean.getEmail() + " * " + new Timestamp(System.currentTimeMillis()) + " * Num. entries: '" + entriesJSONArray.size() + "'");
			
			Connection connActivity = Utilities.getConnection(DBMS_DRIVER, DBMS_HOST, DBMS_PORT, DBMS_DB_NAME, DBMS_USER, DBMS_PASSWORD);
			ActivityDAO activityDAO = new ActivityDAO(connActivity);
			
			Connection connUser = Utilities.getConnection(DBMS_DRIVER, DBMS_HOST, DBMS_PORT, DBMS_DB_NAME, DBMS_USER, DBMS_PASSWORD);
			UserDAO userDAO = new UserDAO(connUser);
	    	
			this.userBean.setCursor("" + deltaJSONObj.get("cursor"));
			userDAO.updateCursor(this.userBean);
			
			for(int i = 0; i < entriesJSONArray.size(); i++){
				JSONArray entryJSONArray = (JSONArray)JSONValue.parse("" + entriesJSONArray.get(i));
				JSONObject entyJSONObj = null;
				String fileName = null;
				if ( (entryJSONArray.size() > 1) && (entryJSONArray.get(1) != null) ) {
					entyJSONObj = (JSONObject)entryJSONArray.get(1);
					fileName = "" + entyJSONObj.get("path");
					fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
				}
				
				System.out.print(userBean.getEmail() + " * " + i + " - LCPATH: '" + entryJSONArray.get(0) + "'");
				if (entyJSONObj != null)
					System.out.print(" - SIZE: '" + entyJSONObj.get("size") + "' - MODIFIED: '" + entyJSONObj.get("modified") + "' - FILENAME: '" + fileName + "' - REV.: '" + entyJSONObj.get("rev") + "' - TYPE: '" + (Boolean.parseBoolean("" + entyJSONObj.get("is_dir")) ? "Directory" : "File") + "'");
				else {
					System.out.print(" - Deleted");
				}
				System.out.println();
				if (entyJSONObj != null)
					activityDAO.insertNewActivity(new ActivityBean(this.userBean.getId(), 0, "" + entryJSONArray.get(0), "" + fileName, "" + entyJSONObj.get("rev"), "" + entyJSONObj.get("size"), "" + entyJSONObj.get("modified"), (Boolean.parseBoolean("" + entyJSONObj.get("is_dir")) ? "D" : "F"), false, "" + deltaJSONObj.get("cursor")));
				else 
					activityDAO.insertNewActivity(new ActivityBean(this.userBean.getId(), 0, "" + entryJSONArray.get(0), null, null, null, null, null, true, "" + deltaJSONObj.get("cursor")));
			}
			if (connUser != null)
				connUser.close();
			if (connActivity != null)
				connActivity.close();
		}
	}
	
}
