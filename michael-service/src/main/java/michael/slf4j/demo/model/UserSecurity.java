package michael.slf4j.demo.model;

import java.io.Serializable;

public class UserSecurity implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private long userId;
	private long userSecurityId;
	private String username;
	private String password;
	public long getUserId() {
		return userId;
	}
	public void setUserId(long userId) {
		this.userId = userId;
	}
	public long getUserSecurityId() {
		return userSecurityId;
	}
	public void setUserSecurityId(long userSecurityId) {
		this.userSecurityId = userSecurityId;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}

}
