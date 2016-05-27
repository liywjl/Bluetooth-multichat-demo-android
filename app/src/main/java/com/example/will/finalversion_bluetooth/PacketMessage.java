package com.example.will.finalversion_bluetooth;

import java.io.Serializable;

/**
 * Created by Will on 03/08/2015.
 */
public class PacketMessage implements Serializable {


    public static final int GROUP_MESSAGE = 1;
    public static final int PERSONAL_MESSAGE = 2;
    public static final int RREQ_FULL_DUMP_OUT = 3;  //request packet
    public static final int DSDV_FULL_DUMP_IN = 4;
    public static final int DSDV_INCREMENT_OUT = 5;
    public static final int DSDV_INCREMENT_IN = 6;


    private int messageType;
    private String originalSender;
    private int messageID;
    private String lastSender;
    private String destination;
    private String messageContent;


    /**
     * there will be multiple different constructeurs, depending on the message type, this will enable serialisable
     * and avoid making the message unnessesarly large
     *
     * This will be the GROUP MESSAGE constructeur
     *
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * should we add last sender?????????
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     */
    public PacketMessage(String originalSender, int messageID, String messageContent) {
        this.messageType=GROUP_MESSAGE;
        this.originalSender=originalSender;
        this.messageID=messageID;
        this.messageContent=messageContent;
    }

    /**
     * Thos will be the PERSONAL MESSAGE constructeur
     * @param originalSender
     * @param messageID
     * @param lastSender
     * @param destination
     * @param messageContent
     */
    public PacketMessage(String originalSender, int messageID, String lastSender, String destination, String messageContent) {
        this.messageType=PERSONAL_MESSAGE;
        this.originalSender=originalSender;
        this.messageID=messageID;
        this.lastSender=lastSender;
        this.destination=destination;
        this.messageContent=messageContent;
    }



    public String getMessageContent(){
        return messageContent;
    }

    public int getMessageType(){
        return messageType;
    }

    public String getOriginalSender(){
        return originalSender;
    }

}
