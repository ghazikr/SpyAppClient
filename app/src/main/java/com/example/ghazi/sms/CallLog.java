package com.example.ghazi.sms;

import java.util.Date;

/**
 * Created by ghazi on 25-Nov-17.
 */

public class CallLog {
    private String phoneNumber;
    private String callType;
    private Date callDate;
    private String callDurationSeconds;

    public CallLog(String phoneNumber, String callType, Date callDate, String callDurationSeconds) {
        this.phoneNumber = phoneNumber;
        this.callType = callType;
        this.callDate = callDate;
        this.callDurationSeconds = callDurationSeconds;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCallType() {
        return callType;
    }

    public void setCallType(String callType) {
        this.callType = callType;
    }

    public Date getCallDate() {
        return callDate;
    }

    public void setCallDate(Date callDate) {
        this.callDate = callDate;
    }

    public String getCallDurationSeconds() {
        return callDurationSeconds;
    }

    public void setCallDurationSeconds(String callDurationSeconds) {
        this.callDurationSeconds = callDurationSeconds;
    }
}
