package danelli.mario.dropbox.db.bean;

public class UserBean {

	private int id = 0;
	private String email = null;
	private String key = null;
	private String secret = null;
	private String cursor = null;
	
	public UserBean(int id, String email, String key, String secret, String cursor){
		this.id = id;
		this.email = email;
		this.key = key;
		this.secret = secret;
		this.cursor = cursor;
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getSecret() {
		return secret;
	}
	public void setSecret(String secret) {
		this.secret = secret;
	}
	public String getCursor() {
		return cursor;
	}
	public void setCursor(String cursor) {
		this.cursor = cursor;
	}
	
}
