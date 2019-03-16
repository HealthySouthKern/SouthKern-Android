package com.eddierangel.southkern.android.models;

import java.util.Objects;

/**
 *  User Object
 *  Uses components from SendBird and Firebase
 * */
public class User {

    private String uid;
    private String name;
    private String organization;
    private String userId;
    private String photo;
    private String type;
    private String position;
    private String firebaseToken;
    private String sendbirdToken;

    /**
     * The User object is a representation of the several components needed in the Mobile App
     * This should be used to manage the User state.
     * @param uid
     * @param name
     * @param organization
     * @param userId
     * @param photo
     * @param type
     * @param position
     * @param firebaseToken
     * @param sendbirdToken
     */
    public User(String uid, String name, String organization, String userId, String photo, String type, String position, String firebaseToken, String sendbirdToken) {
        this.uid = uid;
        this.name = name;
        this.organization = organization;
        this.userId = userId;
        this.photo = photo;
        this.type = type;
        this.position = position;
        this.firebaseToken = firebaseToken;
        this.sendbirdToken = sendbirdToken;
    }

    /**
     * Default Constructor of User for Healthy SouthKern
     */
    public User() {
        this(null,null,null,null,null,null,null,null,null);
    }

    public User(String uid){
        this(uid,null,null,null,null,null,null,null,null);
    }

    public String getFirebaseToken() {
        return firebaseToken;
    }

    public void setFirebaseToken(String firebaseToken) {
        this.firebaseToken = firebaseToken;
    }

    public String getSendbirdToken() {
        return sendbirdToken;
    }

    public void setSendbirdToken(String sendbirdToken) {
        this.sendbirdToken = sendbirdToken;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }


    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", organization='" + organization + '\'' +
                ", userId='" + userId + '\'' +
                ", position='" + position + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(getUid(), user.getUid());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getUid());
    }
}
