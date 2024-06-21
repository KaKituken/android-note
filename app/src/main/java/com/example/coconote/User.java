package com.example.coconote;

public class User {

    private static User instance;
    private String username;
    private int userId;
    private String userEmail;
    private String userNickname;
    private String userSignature;
    private String userAvatar;

    private User() {}

    public static synchronized User getInstance() {
        if (instance == null) {
            instance = new User();
        }
        return instance;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    public String getUserAvatar() {return userAvatar;}
    public void setUserAvatar(String userAvatar) {this.userAvatar = userAvatar;}

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserNickname(){
        return userNickname;
    }

    public void setUserNickname(String userNickname){
        this.userNickname = userNickname;
    }

    public String getUserSignature(){
        return userSignature;
    }

    public void setUserSignature(String userSignature){
        this.userSignature = userSignature;
    }

    public void signOut(){
        this.username = "";
        this.userId = -1;
        this.userSignature = "";
        this.userEmail = "";
        this.userNickname = "";
        this.userAvatar = "";
    }
}
