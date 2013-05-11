package danelli.mario.dropbox.db.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import danelli.mario.dropbox.db.bean.ActivityBean;

public class ActivityDAO {

	private static final String INSERT_NEW_ACTIVITY = "INSERT INTO activities (id_users, id_activities, lc_path, name, rev, doc_size, doc_modified, doc_type, fl_delete, cursor_string, timestamp) VALUES (?, (SELECT MAX(act.id_activities) + 1 FROM activities act WHERE act.id_users = ?), ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
	
	private static final String INSERT_EMPTY_ACTIVITY = "INSERT INTO activities (id_users, id_activities, timestamp) VALUES (?, 0, CURRENT_TIMESTAMP)";

	private Connection conn = null;
	
	public ActivityDAO(Connection conn){
		this.conn = conn;
	}
	
	public boolean insertNewActivity(ActivityBean activityBean) throws SQLException{
		
		if (conn != null){
			PreparedStatement prepStmt = conn.prepareStatement(INSERT_NEW_ACTIVITY);
		    
			prepStmt.setInt(1, activityBean.getIdUser());
			prepStmt.setInt(2, activityBean.getIdUser());
			prepStmt.setString(3, activityBean.getLcPath());
			prepStmt.setString(4, activityBean.getName());
			prepStmt.setString(5, activityBean.getRev());
			prepStmt.setString(6, activityBean.getSize());
			prepStmt.setString(7, activityBean.getModified());
			prepStmt.setString(8, activityBean.getType());
			prepStmt.setBoolean(9, activityBean.getDelete());
			prepStmt.setString(10, activityBean.getCursor());
		 
			return prepStmt.execute();
		
		}
		
		return false;
	}

	public boolean insertEmptyActivity(int id_users) throws SQLException{

		if (conn != null){
			PreparedStatement prepStmt = conn.prepareStatement(INSERT_EMPTY_ACTIVITY);

			prepStmt.setInt(1, id_users);

        	prepStmt.execute();	
			return true;
		}

		return false;
	}
	
}
