package danelli.mario.dropbox.db.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import danelli.mario.dropbox.db.bean.UserBean;

public class UserDAO {

	private static final String SELECT_ALL_ENTRIES_WITH_CREDENTIALS = "SELECT * FROM users WHERE token_key IS NOT NULL AND token_secret IS NOT NULL ORDER BY id_users ASC";
	
	private static final String SELECT_FIRST_USER_WITHOUT_CREDENTIALS = "SELECT * FROM users WHERE token_key IS NULL AND token_secret IS NULL ORDER BY id_users ASC LIMIT 1";
	
	private static final String UPDATE_USER_CREDENTIALS = "UPDATE users SET token_key = ?, token_secret = ? WHERE id_users = ?";
	
	private static final String UPDATE_USER_CURSOR = "UPDATE users SET cursor_string = ? WHERE id_users = ?";

	private static final String GET_LAST_CURSOR = "SELECT cursor_string FROM users WHERE id_users = ?";

	private Connection conn = null;
	
	public UserDAO(Connection conn){
		this.conn = conn;
	}
	
	public ArrayList<UserBean> selectEntriesWithCredentials() throws SQLException{
		ArrayList<UserBean> toRet = new ArrayList<UserBean>();
		
		if (conn != null){
		    Statement stmt = conn.createStatement();
		 
		    ResultSet rs = stmt.executeQuery(SELECT_ALL_ENTRIES_WITH_CREDENTIALS);
		 
		    while (rs.next()){
		    	UserBean userBean = new UserBean(	rs.getInt("id_users"), 
		    										rs.getString("user_email"),
		    										rs.getString("token_key"),
		    										rs.getString("token_secret"),
		    										rs.getString("cursor_string"));
		    	toRet.add(userBean);
		    }
		
		}
		
		return toRet;
	}
	
	public UserBean selectFirstUserWithoutCredentials() throws SQLException{
		UserBean toRet = null;
		
		if (conn != null){
		    Statement stmt = conn.createStatement();
		 
		    ResultSet rs = stmt.executeQuery(SELECT_FIRST_USER_WITHOUT_CREDENTIALS);
		 
		    while (rs.next()){
		    	toRet = new UserBean(	rs.getInt("id_users"), 
		    							rs.getString("user_email"),
		    							rs.getString("token_key"),
		    							rs.getString("token_secret"),
		    							rs.getString("cursor_string"));
		    	return toRet;
		    }
		
		}
		
		return toRet;
	}
	
	public boolean setTokenKeyAndSecret(UserBean userBean) throws SQLException{
		
		if (conn != null){
			PreparedStatement prepStmt = conn.prepareStatement(UPDATE_USER_CREDENTIALS);

			prepStmt.setString(1, userBean.getKey());
			prepStmt.setString(2, userBean.getSecret());
			prepStmt.setInt(3, userBean.getId());
		 
			return prepStmt.executeUpdate() == 1 ? true : false;
		}
		
		return false;
	}
	
	public boolean updateCursor(UserBean userBean) throws SQLException{
		
		if (conn != null){
			PreparedStatement prepStmt = conn.prepareStatement(UPDATE_USER_CURSOR);

			prepStmt.setString(1, userBean.getCursor());
			prepStmt.setInt(2, userBean.getId());
		 
			return prepStmt.executeUpdate() == 1 ? true : false;
		}
		
		return false;
	}
	
	public String getLastCursor(int id_users) throws SQLException{
		
		if (conn != null){
			PreparedStatement prepStmt = conn.prepareStatement(GET_LAST_CURSOR);

			prepStmt.setInt(1, id_users);
		 
			ResultSet rs = prepStmt.executeQuery();
			
		    while (rs.next()){
		    	return rs.getString("cursor_string");
		    }
		}
		
		return null;
	}
	
}
