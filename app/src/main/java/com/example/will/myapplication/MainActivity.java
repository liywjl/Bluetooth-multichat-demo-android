package com.example.will.myapplication;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.will.myapplication.fragments.GroupMessageFragment;
import com.example.will.myapplication.fragments.PMChatFragment;
import com.example.will.myapplication.fragments.PMDeviceSelectFragment;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements GroupMessageFragment.GroupMessageSelectedListener,
        PMChatFragment.PersonalMessageSelectedListener, PMDeviceSelectFragment.deviceSelectedListener {


    // Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int NEW_CONNECTABLE_DEVICE = 6;
    public static final int LOST_CONNECTION = 7;
    public static final int PERSONAL_MESSAGE = 8;
    public static final int MESSAGE_FAIL=9;
    public static final int NEW_ROUTE_DISCOVERED=10;
    public static final int REMOVE_FAILED_DEVICE_FROM_LIST=11;
    public static final int REMOVE_ALL_SELECTABLE_DEVICES=12;


    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_PERSONAL_MESSAGE_MENU = 3;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    //This can be used for PM

    private boolean isConnected;


    //This all deals with PM deaves
    private String pmDeviceSelectedName;
    private String pmDeviceSelectedMACAddress;

    //It would be better to implement a Map, though for simplicity we are using an arraylist
    private ArrayList<PMConversation> deviceConversations;


    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;

    private PagerAdapter adapter;

    /**
     * THIS SECTION IS WHAT WAS INISIALY CREATED
     *
     * @param savedInstanceState
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //this section was added from previous verions
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText("Group"));
        tabLayout.addTab(tabLayout.newTab().setText("PM Device"));
        tabLayout.addTab(tabLayout.newTab().setText("PM Chat"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);

        //note that adapter was final but this was changed in order to be able to reach the variable
        adapter = new PagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        //Create the instance of Bluetooth chat service in constructeur
        mChatService = new BluetoothChatService(this, mHandler,mBluetoothAdapter.getAddress(),mBluetoothAdapter.getName());

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
        }

        //create an array of PM coversations
        deviceConversations = new ArrayList<PMConversation>();
    }


    public void devicePMSelected(String deviceName, String MACAddress) {
        //Note in the case of lost connection, that this needs to be changed

        pmDeviceSelectedName = deviceName;
        pmDeviceSelectedMACAddress = MACAddress;

        //New item selected, need to update the PMChatFragment
        searchLoop:for (int i=0;i<deviceConversations.size();i++){
            String getConversation = deviceConversations.get(i).getMACAddress();
            if (getConversation.equals(MACAddress)){
                //If found send the PM chat tot he adapter to update the fragment
                adapter.onDeviceSelected(deviceConversations.get(i));
                break searchLoop;
            }
        }
    }


    public boolean checkConnection() {
        boolean connected = mChatService.cheackIfConnected();
        return connected;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scan:
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                return true;
            case R.id.discoverable:
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * END OF WHAT WAS INISIALLY CREATED
     */


    /**
     * this will be used to get the list of currenlty paired devices to the Connections fragment
     *
     * @return
     */
    public ArrayList<String> getBTDevices() {
        // Get the local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        ArrayList<String> mPairedDevicesArrayAdapter = new ArrayList<String>();

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }
        return mPairedDevicesArrayAdapter;
    }


    @Override
    public void onStart() {
        super.onStart();
        if (D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE || mChatService.getState() == BluetoothChatService.STATE_CONNECTED) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }


    /**
     * TODO: need to be changed as this sets up the chat window, and therefore can be disgarded
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");
/*
        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConversationView = (ListView) findViewById(R.id.in);
        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                sendGroupMessage(message);
            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
        */

    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if (D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if (D) Log.e(TAG, "--- ON DESTROY ---");
    }


    private void ensureDiscoverable() {
        if (D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message to all slaves.
     *
     * @param message A string of text to send.
     */
    public void sendGroupMessage(String message) {

        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        for (int y =0;y<161;y++){
            message = message + "l";
        }

        for(int i =0;i<1; i++){
            // Check that there's actually something to send
            if (message.length() > 0) {

                Log.e("OUTGOING MSG MENU", "OUT GOING PRESS");


                // Create a random four digit ID for the message
                int msgID = (int) (Math.random() * 9000) + 1000;

                // The device's name
                String deviceName = mBluetoothAdapter.getName();
                String deviceAddress = mBluetoothAdapter.getAddress();


                // Create new message object of type group (will be done by defult from the constructeur
                PacketMessage groupMessage = new PacketMessage(deviceName, msgID, message);

                // Send the message to the chat service outgoing message instance
                mChatService.outgoingMessage(groupMessage);
            }
        }

    }



    public void sendPersonalMessage(String message){
        if (pmDeviceSelectedMACAddress!=null){
            // Check that we're actually connected before trying anything
            if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
                Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
                return;
            }

            // Check that there's actually something to send
            if (message.length() > 0) {

                //add the message to the PMChatConversation Array
                messageLoop:for (int i=0;i<deviceConversations.size();i++){
                    if (pmDeviceSelectedMACAddress.equals(deviceConversations.get(i).getMACAddress())){
                        deviceConversations.get(i).addConversationMsg("Me: "+message);
                        break messageLoop;
                    }
                }

                // The device's name
                String deviceName = mBluetoothAdapter.getName();
                String deviceMACAddress = mBluetoothAdapter.getAddress();

                // Create new Personal Packet message
                PacketMessage personalMessage = new PacketMessage(deviceName,deviceMACAddress,deviceMACAddress, pmDeviceSelectedMACAddress, message);

                // Send the message to the chat service outgoing message instance
                mChatService.outgoingMessage(personalMessage);
                adapter.addPersonalMessage("Me",message);
            }
        } else{
            Toast.makeText(getApplicationContext(), "No Selected device, Please select a device " , Toast.LENGTH_SHORT).show();
        }


    }


    /**
     * Creates a new discover message to get more devices to have PM conversations
     */
    public void sendDiscoveryMessage(){

        // Create a random four digit ID for the message
        int msgID = (int) (Math.random() * 9000) + 1000;
        // Create search message packet

        PacketMessage search = new PacketMessage(mBluetoothAdapter.getAddress(),mBluetoothAdapter.getName(),msgID, mBluetoothAdapter.getAddress());
        // Send to Bluetooth service outgoing message method
        mChatService.outgoingMessage(search);

    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            //Could be interesting to develop later if needed to change the title name to show that device is connected
                            //mTitle.setText(R.string.title_connected_to);
                            //mTitle.append(mConnectedDeviceName);

                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            //Can be removed!!!!!!!!!
                            //mTitle.setText(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                            //Can be removed as the device is always listening
                        case BluetoothChatService.STATE_NONE:
                            //Can show that there is no connection
                            //mTitle.setText(R.string.title_not_connected);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    try {

                        //the object sent back is a String so can just display
                        PacketMessage incomingMessage = (PacketMessage) msg.obj;

                        String messageContent = incomingMessage.getMessageContent();
                        String senderName = incomingMessage.getOriginalSenderName();
                        //Display the message
                        adapter.addGroupMessage(senderName, messageContent);
                        break;
                    } catch (ClassCastException e) {
                        // TODO: 03/09/2017  Need to handle error
                    }


                case MESSAGE_READ:
                    // This method is never called!
                    try {
                        // Get the message from the BluetoothChatService Class, and cast it correctly
                        PacketMessage incomingMessage = (PacketMessage) msg.obj;

                        // Get message content
                        String messageContent = incomingMessage.getMessageContent();
                        // Get original sender name
                        String senderName = incomingMessage.getOriginalSenderName();


                        // Check that the message has content
                        if (messageContent.length() > 0) {
                            adapter.addGroupMessage(senderName, messageContent);
                        }


                    } catch (ClassCastException e) {
                        // TODO:Need to handle the error
                    }
                    break;
                case PERSONAL_MESSAGE:
                    //get the personal message
                    PacketMessage incomingMessage = (PacketMessage) msg.obj;

                    // Get all the relavant infromantion
                    String messageContent = incomingMessage.getMessageContent();
                    String originalSenderMAC = incomingMessage.getOriginalSenderMAC();
                    String originalSenderName = incomingMessage.getOriginalSenderName();
                    String conversationMAC= null;

                    // Checks if conversation exists
                    ConversationLoop:for (int i=0;i<deviceConversations.size();i++){
                        conversationMAC = deviceConversations.get(i).getMACAddress();
                        //If conversation exists, add message
                        if (conversationMAC.equals(originalSenderMAC)){
                            deviceConversations.get(i).addConversationMsg(originalSenderName+ ": "+messageContent);
                            break ConversationLoop;
                        }else {
                            //if conversation dsnt exist create new conversation
                            PMConversation newConversation = new PMConversation(originalSenderMAC);
                            //add it to the array
                            deviceConversations.add(newConversation);
                        }
                        // If the current conversation is selected, add it to the PM UI
                    }
                    if (conversationMAC.equals(pmDeviceSelectedMACAddress)){
                        adapter.addPersonalMessage(pmDeviceSelectedName,messageContent);
                    }
                    break;

                case MESSAGE_DEVICE_NAME:
                    // Save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();

                    break;
                case MESSAGE_TOAST:
                    // Display toast messages
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;


                // This section deals with adding and removing devices that can
                case NEW_CONNECTABLE_DEVICE:
                    // Add a new Device to the PM chat service
                    BluetoothDevice deviceConnectable = (BluetoothDevice) msg.obj;
                    String deviceName = deviceConnectable.getName();
                    String deviceAddress = deviceConnectable.getAddress();
                    adapter.addPMChatDevice(deviceName, deviceAddress);

                    // Cheack if conversation dsnt already exist, else create new
                    // could be better to use a hashMap for seeing if MAC address exits
                    boolean converstationExists = false;
                    converstationLoop: for (int i = 0; i < deviceConversations.size(); i++) {
                        String pmMAC = deviceConversations.get(i).getMACAddress();
                        if (pmMAC.equals(deviceAddress)) {
                            converstationExists = true;
                            mChatService.updateTableRoute(deviceAddress);
                            break converstationLoop;
                        }
                    }

                    // If the conversation doesnt already exist add a new one
                    if (!converstationExists) {
                        PMConversation newChat = new PMConversation(deviceAddress);
                        deviceConversations.add(newChat);
                    }
                    break;

                case LOST_CONNECTION:
                    //Remove the connection from the PM Device list
                    BluetoothDevice deviceConnectionLost = (BluetoothDevice) msg.obj;
                    String deviceAddressLost = deviceConnectionLost.getAddress();

                    mChatService.removeNodeRoutes(deviceAddressLost);

                    adapter.removePMCharDevice(deviceAddressLost);
                break;


                case MESSAGE_FAIL:
                    // Get all the information form the failed message
                    PacketMessage failedMessage = (PacketMessage) msg.obj;

                    //the new direction will be to the original sender
                    String failedDestinationMAC = failedMessage.getDestinationMAC();
                    String newDirectionMAC = failedMessage.getOriginalSenderMAC();
                    String currentDeviceMAC = mBluetoothAdapter.getAddress();

                    // Create new message
                    PacketMessage failMessageToBeSent = new PacketMessage(failedDestinationMAC,newDirectionMAC,currentDeviceMAC);
                    // Send it off to the outgoing message method in bluetooth sevice class
                    mChatService.outgoingMessage(failMessageToBeSent);

                    adapter.removePMCharDevice(failedDestinationMAC);

                    break;
                case NEW_ROUTE_DISCOVERED:
                    PacketMessage newRouteDiscovered = (PacketMessage) msg.obj;

                    String newDeviceName = newRouteDiscovered.getOriginalSenderName();
                    String newDeviceAddress = newRouteDiscovered.getOriginalSenderMAC();
                    boolean conversationExists = false;
                    for (int i=0;i<deviceConversations.size();i++){
                        if (deviceConversations.get(i).getMACAddress().equals(newDeviceAddress)){
                            conversationExists=true;
                        }
                    }

                    // This all depends on if the conversation exists
                    if (!conversationExists){
                        PMConversation newChat = new PMConversation(newDeviceAddress);
                        deviceConversations.add(newChat);
                    }
                    adapter.addPMChatDevice(newDeviceName, newDeviceAddress);

                    break;
                case REMOVE_FAILED_DEVICE_FROM_LIST:

                    PacketMessage failMsg = (PacketMessage) msg.obj;
                    String failedDestinationMACRemove = failMsg.getFailedNodeRoute();

                    adapter.removePMCharDevice(failedDestinationMACRemove);


                    String newDirectionMACFail = failMsg.getDestinationMAC();
                    String currentDeviceMACFail = mBluetoothAdapter.getAddress();

                    if(newDirectionMACFail.equals(currentDeviceMACFail)){
                        adapter.addPersonalMessage("Message Fail","The Device Route was lost");
                    }
                    break;

                case REMOVE_ALL_SELECTABLE_DEVICES:
                    adapter.clearAllSelectableDevices();

                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BluetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    mChatService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occured
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }

    }


    /**
     * This section is for the main activity to be able to communicate to each fragment!!!!!
     *
     * @param groupMessage
     */


    @Override
    public void onGroupMessageSent(String groupMessage) {
        //PagerAdapter groupMsgSender = (PagerAdapter) getSupportFragmentManager().findFragmentByTag(R.layout.group_chat_fragment)
    }

    @Override
    public void PersonalMessageSelectedListener(String personalMessage) {

    }

    @Override
    public void deviceSelectedListener(String deviceSelected) {

    }
}