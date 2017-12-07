package com.example.ghazi.sms;

/**
 * Created by ghazi on 25-Nov-17.
 */

public class Sms {
    private String address;
    private String body;

    public Sms(String address, String body) {
        this.address = address;
        this.body = body;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
