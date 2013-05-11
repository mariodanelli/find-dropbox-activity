package danelli.mario.dropbox.db.bean;

public class ActivityBean {

	private int idUser = 0;
	private int idActivity = 0;
	private String lcPath = null;
	private String name = null;
	private String rev = null;
	private String size = null;
	private String modified = null;
	private String type = null;
	private boolean delete = false;
	private String cursor = null;
	
	public ActivityBean(int idUser, int idActivity, String lcPath, String name, String rev, String size, String modified, String type, boolean delete, String cursor){
		this.idActivity = idActivity;
		this.idUser = idUser;
		this.lcPath = lcPath;
		this.name = name;
		this.rev = rev;
		this.size = size;
		this.modified = modified;
		this.type = type;
		this.delete = delete;
		this.cursor = cursor;
	}

	public int getIdUser() {
		return idUser;
	}

	public void setIdUser(int idUser) {
		this.idUser = idUser;
	}
	
	public int getIdActivity() {
		return idActivity;
	}
	public void setIdActivity(int idActivity) {
		this.idActivity = idActivity;
	}
	public String getLcPath() {
		return lcPath;
	}
	public void setLcPath(String lcPath) {
		this.lcPath = lcPath;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getRev() {
		return rev;
	}
	public void setRev(String rev) {
		this.rev = rev;
	}
	public String getSize() {
		return size;
	}
	public void setSize(String size) {
		this.size = size;
	}
	public String getModified() {
		return modified;
	}
	public void setModified(String modified) {
		this.modified = modified;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	public boolean getDelete() {
		return delete;
	}
	public void setDelete(boolean delete) {
		this.delete = delete;
	}

	public String getCursor() {
		return cursor;
	}

	public void setCursor(String cursor) {
		this.cursor = cursor;
	}
	
}
