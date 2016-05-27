package com.example.will.myapplication;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.example.will.myapplication.fragments.GroupMessageFragment;
import com.example.will.myapplication.fragments.PMChatFragment;
import com.example.will.myapplication.fragments.PMDeviceSelectFragment;

public class PagerAdapter extends FragmentStatePagerAdapter {
    int mNumOfTabs;

    GroupMessageFragment groupChat;
    PMDeviceSelectFragment devicePM;
    PMChatFragment chatPM;
    FragmentManager fm;


    public PagerAdapter(FragmentManager fm, int NumOfTabs) {
        super(fm);
        this.fm=fm;
        this.mNumOfTabs = NumOfTabs;
        groupChat = new GroupMessageFragment();
        devicePM = new PMDeviceSelectFragment();
        chatPM = new PMChatFragment();
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:

                //make sure that the fragment hasnt been already created
                if(groupChat==null){
                    groupChat = new GroupMessageFragment();

                    //trying to save the fragment
                    groupChat.setRetainInstance(true);
                }
                return groupChat;
            case 1:

                //make sure that the fragment hasnt been already created
                if (devicePM==null){
                    devicePM = new PMDeviceSelectFragment();

                    //trying to save the fragment
                    devicePM.setRetainInstance(true);
                }
                return devicePM;

            case 2:

                //make sure that the fragment hasnt been already created
                if (chatPM==null){
                    chatPM = new PMChatFragment();

                    //trying to save the fragment
                    chatPM.setRetainInstance(true);
                }
                return chatPM;
            default:
                return null;
        }
    }

    /**
     * This method is called to add a message to the Group converstation UI
     * @param senderName
     * @param messageContent
     */
    public void addGroupMessage(String senderName, String messageContent){
        groupChat.addMessageToChat(messageContent, senderName);
    }



    /*
    Add message to chat array
     */
    public void addPersonalMessage(String senderName, String messageContent){
        chatPM.addMessageToChat(senderName,messageContent);
    }
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //This section all deals with PM chat
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    /*
    Add a device to the PM Chat list
     */
    public void addPMChatDevice(String deviceName, String deviceAddress){
        devicePM.addDeviceToList(deviceName, deviceAddress);
    }

    /*
    Remove a device to the PM chat
     */
    public void removePMCharDevice(String deviceAddress){
        devicePM.removeDeviceToList(deviceAddress);
    }

    public void clearAllSelectableDevices(){
        devicePM.clearAllSelectableDevices();
    }


    /**
     * sends the selected conversation to the PM chat fragment
     * @param selectedConversation
     */
    public void onDeviceSelected(PMConversation selectedConversation){
        chatPM.deviceSelectedChange(selectedConversation);
    }








    /**
     * This will be called to create a fragment for a PMChat once an element is selected
     */
    public void createPMChatFragment(String deviceName, String adviceAddress){
        //the user selects a device from the PMDeviceSelectFragment

        if (chatPM==null){
            chatPM = new PMChatFragment();
        } else {

        }



    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}