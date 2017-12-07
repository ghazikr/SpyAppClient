package com.example.ghazi.sms;

/**
 * Created by ghazi on 30-Nov-17.
 */

public class Notification {
    private String type,content,receivedTime,userSender;

    public Notification(String type, String content, String receivedTime, String userSender) {
        this.type = type;
        this.content = content;
        this.receivedTime = receivedTime;
        this.userSender = userSender;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "type='" + type + '\'' +
                ", content='" + content + '\'' +
                ", receivedTime='" + receivedTime + '\'' +
                ", userSender='" + userSender + '\'' +
                '}';
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getReceivedTime() {
        return receivedTime;
    }

    public void setReceivedTime(String receivedTime) {
        this.receivedTime = receivedTime;
    }

    public String getUserSender() {
        return userSender;
    }

    public void setUserSender(String userSender) {
        this.userSender = userSender;
    }
}
