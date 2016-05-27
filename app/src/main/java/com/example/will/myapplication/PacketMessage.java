package com.example.will.myapplication;

import java.io.Serializable;

/**
 * Created by Will on 04/08/2015.
 */
public class PacketMessage implements Serializable {


    private static final long serialVersionUID = 7526472295622776147L;

    public static final int GROUP_MESSAGE = 1;
    public static final int PERSONAL_MESSAGE = 2;
    public static final int SEARCH_PACKET=3;
    public static final int ACKNOWLEDGE_PACKET = 4;
    public static final int MESSAGE_FAIL= 5;


    //public static final int RREQ_FULL_DUMP_OUT = 3;  //request packet
    //public static final int DSDV_FULL_DUMP_IN = 4;
    //public static final int DSDV_INCREMENT_OUT = 5;
    public static final int DSDV_INCREMENT_IN = 6;



    private int messageType;                //This is auto generated
    private String originalSenderName;      //use original NAME
    private String originalSenderMAC;       //use original MAC
    private int messageID = 0;
    private String lastSenderMAC;              //use MAC
    private String destinationMAC;             //use MAC
    private String messageContent;
    private int numberOfHops;
    private String nextHopNode;
    private String failedNodeRoute;
    private String addressToAddToTable;


    /**
     * there will be multiple different constructeurs, depending on the message type, this will enable serialisable
     * and avoid making the message unnessesarly large
     *
     * This will be the GROUP MESSAGE constructeur
     *
     */
    public PacketMessage(String originalSenderName, int messageID, String messageContent) {
        this.messageType=GROUP_MESSAGE;
        this.originalSenderName=originalSenderName;
        this.messageID=messageID;
        this.messageContent=messageContent;
    }

    /**
     * This will be the PERSONAL MESSAGE constructeur
     */
    public PacketMessage(String originalSenderName, String originalSenderMAC,
                            String lastSenderMAC, String destinationMAC, String messageContent) {
        this.messageType=PERSONAL_MESSAGE;
        this.originalSenderName=originalSenderName;
        this.originalSenderMAC=originalSenderMAC;
        this.lastSenderMAC=lastSenderMAC;
        this.destinationMAC=destinationMAC;
        this.messageContent=messageContent;
    }



    /*
        This will be for the SEARCH device packet
     */
    public PacketMessage(String originalSenderMAC, String originalSenderName, int messageID, String lastHopMAC){ //number of hops is used to
        this.messageType=SEARCH_PACKET;
        this.originalSenderMAC=originalSenderMAC;
        this.originalSenderName=originalSenderName;
        this.messageID=messageID;
        this.lastSenderMAC=lastHopMAC;
        this.numberOfHops=0;        //As this message was just created so by defult it is 0

    }

    /*
    This is for acknowledgement of discovery, so this will be sent back
     */
    public PacketMessage(String destinationMAC, String currentDeviceMAC,String currentDeviceName,String lastSenderMAC, String nextHopNode, int numberOfHops){
        this.messageType=ACKNOWLEDGE_PACKET;
        this.destinationMAC=destinationMAC; //the original sender becomes the new destination
        this.originalSenderMAC=currentDeviceMAC;//the original sender is now the device that should be added to the routing table
        this.originalSenderName=currentDeviceName; //the name of this device should be send aswell
        this.nextHopNode=nextHopNode;
        this.lastSenderMAC=lastSenderMAC; //the last sender should become the new route
        this.numberOfHops=numberOfHops; //the number of hops will be used to calculate distance

    }

    /*
    This is for message Fail
     */
    public PacketMessage(String unreachedDestinationMAC, String originalSenderMAC, String currentDevice ){ //current device is n
        this.messageType=MESSAGE_FAIL;

        //Note that we use inconsistent workfing for the table, unreachable destination becomes last sender
        //original sender becomes the destionat
        this.originalSenderMAC=currentDevice; //Beware of inconsistent NAMES
        this.failedNodeRoute=unreachedDestinationMAC;
        this.destinationMAC=originalSenderMAC; //beware of names

        //we do not use original sender name, but it used for consutrcuter overiding

    }


    public String getMessageContent(){
        return messageContent;
    }

    public int getMessageType(){
        return messageType;
    }

    public String getOriginalSenderMAC(){
        return originalSenderMAC;
    }

    public String getOriginalSenderName(){
        return originalSenderName;
    }

    public String getLastSenderMAC(){
        return lastSenderMAC;
    }

    public void setLastSenderMAC(String currentDevice){
        lastSenderMAC=currentDevice;
    }

    public String getDestinationMAC(){
        return destinationMAC;
    }

    public int getMessageID(){
        return messageID;
    }

    public void addHop(){
        numberOfHops=numberOfHops+1;
    }

    public void removeHop(){
        numberOfHops=numberOfHops-1;
    }
    public int getNumberOfHops(){
        return numberOfHops;
    }

    public String getFailedNodeRoute(){
        return failedNodeRoute;
    }

}
