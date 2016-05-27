package com.example.will.myapplication;

import java.util.ArrayList;

/**
 * Created by Will on 07/08/2015.
 */
public class PMConversation {

    private ArrayList<String> mConversations;
    private String deviceMAC;

    PMConversation(String MACAddress){
        mConversations = new ArrayList<String>();
        this.deviceMAC=MACAddress;
    }

    public void addConversationMsg(String message){
        mConversations.add(message);
    }

    public String getMACAddress(){
        return deviceMAC;
    }

    public ArrayList<String> getConversations(){
        return mConversations;
    }
}
