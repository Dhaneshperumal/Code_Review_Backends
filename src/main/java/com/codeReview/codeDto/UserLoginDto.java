package com.codeReview.codeDto;

public class UserLoginDto {
	
	
    private String email;
    private String password;
    
    public UserLoginDto() {
		super();
		// TODO Auto-generated constructor stub
	}
    
	public UserLoginDto(String email, String password) {
		super();
		this.email = email;
		this.password = password;
	}
	// Getters and Setters
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}
