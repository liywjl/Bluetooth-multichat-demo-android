package com.example.will.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.will.myapplication.routing.NodeRout;
import com.example.will.myapplication.routing.RoutingTable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

/**
 * Created by Will on 04/08/2015.
 */

public class BluetoothChatService {
    // Debugging
    private static final String TAG = "BluetoothChatService";
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    private static final String NAME = "BluetoothChatMulti";

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    private Serializer mSerializer;

    private ArrayList<String> mDeviceAddresses;         //addresses of devices

    private ArrayList<ConnectedThread> mConnThreads;   //array of outgoing connections
    private ArrayList<BluetoothSocket> mSockets;       //array of incoming connections

    //private ArrayList<MessageObject> mMessageObject;

    // Stack of message IDs to Avoid duplicate messages
    private Queue<Integer> msgIDQueue;

    //inisialise a PersonalChatService
    private RoutingTable mRoutingTable;

    //hold the current MAC address for routing
    private String deviceMAC;
    private String deviceName;
    /**
     * A bluetooth piconet can support up to 7 connections. This array holds 7 unique UUIDs.
     * When attempting to make a connection, the UUID on the client must match one that the server
     * is listening for. When accepting incoming connections server listens for all 7 UUIDs.
     * When trying to form an outgoing connection, the client tries each UUID one at a time.
     */
    private ArrayList<UUID> mUuids;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    public BluetoothChatService(Context context, Handler handler, String deviceMAC, String deviceName) {
        Log.i("BluetoothChatService", "BluetoothChatService Constructeur called");
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;

        //inisialise the MAC address of this device
        this.deviceMAC=deviceMAC;
        this.deviceName=deviceName;

        mDeviceAddresses = new ArrayList<String>();
        mConnThreads = new ArrayList<ConnectedThread>();
        mSockets = new ArrayList<BluetoothSocket>();
        mUuids = new ArrayList<UUID>();

        //Inisialise the serialise class
        mSerializer = new Serializer();


        // TODO: 02/08/2015 need to see if we need to create a PM object
        //Create personal chat object????
        mRoutingTable = new RoutingTable();

        //Initiallise the Queue Stack for Message IDs
        msgIDQueue = new LinkedList<Integer>();

        // 7 randomly-generated UUIDs. These must match on both server and client.
        mUuids.add(UUID.fromString("b7746a40-c758-4868-aa19-7ac6b3475dfc"));
        mUuids.add(UUID.fromString("2d64189d-5a2c-4511-a074-77f199fd0834"));
        mUuids.add(UUID.fromString("e442e09a-51f3-4a7b-91cb-f638491d1412"));
        mUuids.add(UUID.fromString("a81d6504-4536-49ee-a475-7d96d09439e4"));
        mUuids.add(UUID.fromString("aa91eab1-d8ad-448e-abdb-95ebba4a9b55"));
        mUuids.add(UUID.fromString("4d34da73-d0a4-4f40-ac38-917e0a9dee97"));
        mUuids.add(UUID.fromString("5e14d4df-9c8a-4db7-81e4-c937564c86e0"));


    }

    /*
    will remove node links that were dependent on a specific connection
     */
    public void removeNodeRoutes(String deviceConnectionLost){
        //here we will remove all the paths that used this device for next hop
        mRoutingTable.removeNodeRout(deviceConnectionLost);;

    }



    public void updateTableRoute(String route){
        mRoutingTable.setRouteHops(route);
    }

    /*
    checks the Array of device address and returns a boolean if the address of
    the param is a duplicate
     */
    private boolean isDuplicateDevice(String address) {
        boolean checkDuplicate = false;
        //checking that there is no duplicate connection
        for (int y = 0; y < mDeviceAddresses.size(); y++) {
            if (mDeviceAddresses.get(y).equals(address)) {
                checkDuplicate = true;
            }
        }
        return checkDuplicate;
    }

    /*
    Sends a toast to the UI to notify the user is trying to connect to an already connected device
     */
    private void sendDuplicateDeviceToast() {
        // Send a duplication warning message back to the Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Device was a duplicate");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /*
    Adds a message ID to the Queue, MsgID. It will add the message if the queue is empty, or if
    the id is a non duplicate. If the stack size is bigger than 10, it will remove the first
    item of the Queue. This is to make sure that only the last 10 msg ID are compaired.
    Having only 10 msg ID means that the probability of colliding IDs is 10/8000
     */
    private void addMessageID(Integer id) {
        if (msgIDQueue.isEmpty()) {
            msgIDQueue.offer(id);
        } else {
            if (!msgIDQueue.contains(id)) {
                msgIDQueue.offer(id);
                if (msgIDQueue.size() >= 10) {
                    msgIDQueue.poll();
                }
            }
        }
    }

    /*
    checks that the message ID is not a duplicate and returns a boolean.
     */
    private boolean checkIDDuplicate(Integer id) {
        boolean isDuplicate = false;
        if (msgIDQueue.contains(id)) {
            isDuplicate = true;
        }
        return isDuplicate;
    }


    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        Log.i(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        Log.i("getState", "getState called");
        return mState;
    }

    /**
     * This could be a duplicate method
     */
    private void cheackConnection() {
        if (mConnThreads.size() > 0) {
            setState(STATE_CONNECTED);
        } else {
            setState(STATE_LISTEN);
        }
    }

    /**
     * this is to make s
     * @return
     */
    public boolean cheackIfConnected(){
        boolean connected;
        if (mConnThreads.size() > 0) {
            connected=true;
        } else {
            connected=false;
        }
        return connected;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.i("ChatStart", "start called to commence chat");
        if (D) Log.d(TAG, "start");

        //make sure to close the AcceptThread element if it is not null
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
        }

        //make the AcceptThread to restart listining to incoming connections
        mAcceptThread = null;

        // Start the thread to listen on a BluetoothServerSocket
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
        //setState(STATE_LISTEN);
        cheackConnection();

    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        Log.i("connect", "connect called to start chat thread");
        if (D) Log.d(TAG, "connect to: " + device);

        //check for duplicates
        boolean checkDuplicate = isDuplicateDevice(device.getAddress());
        if (!checkDuplicate) {

            // Create a new thread and attempt to connect to each UUID one-by-one.
            loopUUID:
            for (int i = 0; i < 7; i++) {
                try {
                    Log.i("loopUUID", "went through UUID loop to link the chat thread");
                    mConnectThread = new ConnectThread(device, mUuids.get(i));

                    mConnectThread.start();

                    cheackConnection();
                } catch (Exception e) {

                }
                boolean success = isDuplicateDevice(device.getAddress());
                if (success) {

                    break loopUUID;
                }
            }
        } else {
            // Send a duplication message back to the Activity
            sendDuplicateDeviceToast();
        }
    }


    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.i("connectED", "ConnectedThread to begin managing bluetooth connections");
        if (D) Log.d(TAG, "connected");

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, device);

        //create new node route
        mRoutingTable.createNewNodeRoute(device.getName(), device.getAddress(),1, device.getAddress());
        mConnectedThread.start();

        if (mDeviceAddresses.contains(device.getAddress())) {
            // Add each connected thread to an array
            mConnThreads.add(mConnectedThread);

            // Send the name of the connected device back to the UI Activity
            Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(MainActivity.DEVICE_NAME, device.getName());
            msg.setData(bundle);
            mHandler.sendMessage(msg);

            setState(STATE_CONNECTED);
        }
        start();
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.i("Stop", "Stop all threads");
        if (D) Log.d(TAG, "stop");
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        cheackConnection();
    }

    /*
    convert the object to and from its byte form to be able to be transmitted
     */
    public static class Serializer {
        public byte[] serialize(Object obj) throws IOException {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            ObjectOutputStream o = new ObjectOutputStream(b);
            o.writeObject(obj);
            return b.toByteArray();
        }

        public Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
            ByteArrayInputStream b = new ByteArrayInputStream(bytes);
            ObjectInputStream o = new ObjectInputStream(b);
            return o.readObject();
        }
    }


    /*
    This method takes that object, adds relavant infromantion to the arrays, addresses
    It then calls the serialiser to create a byte array to be then sent off the the write methode
     */
    public void outgoingMessage(PacketMessage msg) {

        if (msg.getMessageID()!=0){
            msgIDQueue.offer(msg.getMessageID());
        }

        try {
            byte[] msgByte;

            msgByte = mSerializer.serialize(msg);

            switch (msg.getMessageType()){
                case 1: //GROUP MESSAGE

                    //msgByte = mSerializer.serialize(msg);
                    writeToAll(msgByte);
                    //Should send message to UI through this to make sure that message has been sent
                    //note that the hole message is sent to the UI, need to extract original sender and content
                    //mHandler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, msg).sendToTarget();
                    break;

                case 2: //PERSONAL MESSAGE

                    //if (!msg.getDestinationMAC().equals(deviceMAC)){
                        //get the message desintation
                        String desination = msg.getDestinationMAC();
                        //find the next hop for that destination

                        //do we really need all of this?????
                        //as this method is called once th emessage is created, so needing to
                        //reinstate all the variables
                        // TODO: 08/08/2015 Need to see if i can edit this out

                        String nextHopNode = mRoutingTable.getRout(desination);
                        //change the last hope address to this device
                        //msg.setLastSenderMAC(deviceMAC);
                        //serialise the message
                        //msgByte = mSerializer.serialize(msg);
                        //send to the method who deals with this
                        sendToSpecificDevice(nextHopNode, msgByte);
                    //} else {
                        //this shoudl not happen as this is an outgoing message
                    //}
                    break;

                case 3: //SEARCH PACKET

                    //could this be removed???????//!!!!!!!!!!!!
                    //!!!!!!!!!!!!!!!!!!!!!!!

                    //serialise the message
                    //msgByte = mSerializer.serialize(msg);

                    //This is a search message, so it will be sent to all, if it is a new message
                    //and only redirected if it comes from another node
                    if (msg.getNumberOfHops()>0){
                        redirectMessageToAll(msgByte,msg.getLastSenderMAC());
                    }else {
                        writeToAll(msgByte);
                    }
                    break;
                // TODO: 09/08/2015 there is an issue here!!!!!!, it sends the search packet
                //There should be also other cases developed here!!!!!

                case 4: //ACKNOWLEDGE PACKET

                    //This is for sending an achknowledge message
                    //we assume that hops, and direction have already been stated

                    //destination(original sender)
                    //original sender (current devive (both name and MAC))
                    //have the number of hops aswell
                    String destinationNodeHop = mRoutingTable.getRout(msg.getDestinationMAC());

                    msgByte = mSerializer.serialize(msg);

                    sendToSpecificDevice(destinationNodeHop, msgByte);
                    break;

                case 5: //MESSAGE FAIL

                    //this is for failed message, if the device connection has been lost
                    //need to inform original sender
                    //make sure that the disconnected node is correctly edited in routing table

                    //get direction of original sender
                    //send to previous node

                    String destinationNodeNextHop = msg.getDestinationMAC();

                    msgByte = mSerializer.serialize(msg);

                    sendToSpecificDevice(destinationNodeNextHop, msgByte);
            }
        } catch (IOException e) {

            // TODO: 01/08/2015 Need to remove added informantion if there was a failer
            // Send a failure message back to the Activity
            Message msgUI = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(MainActivity.TOAST, "outgoing message currupted");
            msgUI.setData(bundle);
            mHandler.sendMessage(msgUI);
        }
    }



    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @// TODO: 24/07/2015 this is were all the messages are sent from, need to avoid duplicates???
     * @see ConnectedThread#write(byte[])
     */
    public void writeToAll(byte[] outByteMsg) {
        Log.i("write Group", "write to ConnectedThread in unsynchronised manner");


        // When writing, try to write out to all connected threads
        for (int i = 0; i < mConnThreads.size(); i++) {
            try {
                // Create temporary object
                ConnectedThread r;
                // Synchronize a copy of the ConnectedThread
                synchronized (this) {
                    if (mState != STATE_CONNECTED) return;
                    r = mConnThreads.get(i);
                }
                // Perform the write unsynchronized
                r.write(outByteMsg);
            } catch (Exception e) {
            }
        }

    }

    /**
     * this sends the message to a specific device, it can be a personal message
     * or it can be a RREQ or other table realted message
     * @param targetDeviceMAC
     * @param outByteMsg
     */
    public void sendToSpecificDevice(String targetDeviceMAC, byte[] outByteMsg){
        Log.i("write PM", "write to ConnectedThread in unsynchronised manner");

        findDeviceLoop:for (int y=0;y<mConnThreads.size();y++){
            String connectedDevicesMAC =mConnThreads.get(y).mmDevice.getAddress();
            if (connectedDevicesMAC.equals(targetDeviceMAC)){
                try {
                    ConnectedThread r = mConnThreads.get(y);
                    r.write(outByteMsg);
                    break findDeviceLoop;
                }catch (Exception e) {
                    //can throw error
                }
            }
        }

    }


    /*
    This method redirects the message to all slaves, except the one that address mentioned in the param
     */
    private void redirectMessageToAll(byte[] buffer, String lastSender) {

        byte[] send = buffer;
        // When writing, try to write out to all connected threads
        for (int i = 0; i < mConnThreads.size(); i++) {
            if (!mConnThreads.get(i).mmDevice.getAddress().equals(lastSender)) {
                try {
                    // Create temporary object
                    ConnectedThread r;
                    // Synchronize a copy of the ConnectedThread
                    synchronized (this) {
                        if (mState != STATE_CONNECTED) return;
                        r = mConnThreads.get(i);
                    }
                    // Perform the write unsynchronized
                    r.write(send);
                } catch (Exception e) {
                }
            }
        }
    }





    /*
    This method is called for incoming messages, called by the ConnectedThread. It converts the
    bytes into a String and sends them to the UI
    todo be able to handle different message types!!!!!!!!
     */
    private void incomingMessage(byte[] buffer,String lastSender) {

        //turns the serial bytes back into a messageObject, if it fails, it sends a Toast
        try {

            PacketMessage incomingMsg = (PacketMessage) mSerializer.deserialize(buffer);


            switch (incomingMsg.getMessageType()){
                case 1: //GROUP MESSAGE
                    //check that it is not a duplicate message
                    if (!msgIDQueue.contains(incomingMsg.getMessageID())){
                        //if the msg ID was not a duplicate, add ID to quie
                        addMessageID(incomingMsg.getMessageID());

                        //send message to UI
                        mHandler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, incomingMsg).sendToTarget();
                        //redirect message
                        redirectMessageToAll(buffer, lastSender);

                    } else {
                        Message msgUI = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
                        Bundle bundle = new Bundle();
                        bundle.putString(MainActivity.TOAST, "Incoming message was a duplicate");
                        msgUI.setData(bundle);
                        mHandler.sendMessage(msgUI);
                    }
                    break;

                case 2: //PERSONAL MESSAGE

                    //check if message is for this device
                    if (incomingMsg.getDestinationMAC().equals(deviceMAC)){
                        mHandler.obtainMessage(MainActivity.PERSONAL_MESSAGE, -1, -1, incomingMsg).sendToTarget();
                    }else {
                        String targetDevice =incomingMsg.getDestinationMAC();
                        String nextHopDevice =mRoutingTable.getRout(targetDevice);

                        //int jokes = mRoutingTable.getRoutHops(targetDevice);
                        //int lololol = mRoutingTable.getRoutHops(nextHopDevice);

                        //check to see if rout exists
                        if (nextHopDevice.equals(null)||mRoutingTable.getRoutHops(nextHopDevice)==-1
                                ||mRoutingTable.getRoutHops(targetDevice)==0){

                            mHandler.obtainMessage(MainActivity.MESSAGE_FAIL, -1, -1, incomingMsg).sendToTarget();
                            //need to send an error message
                            // TODO: 08/08/2015 SEND AN ERROR MESSAGE BACK TO THE DEVICE

                        } else {
                            sendToSpecificDevice(nextHopDevice,buffer);
                        }
                    }


                    break;
                case 3: //SEARCH PACKET
                    if (!checkIDDuplicate(incomingMsg.getMessageID())){
                        //get all the infomation form the search packet to add to table
                        String nodeMAC = incomingMsg.getOriginalSenderMAC();
                        String nodeName = incomingMsg.getOriginalSenderName();


                        String nextHopAddress= incomingMsg.getLastSenderMAC(); //used for acknowledge message


                        int numberOfHops = (incomingMsg.getNumberOfHops())+1; //add 1 to this

                        //this will create the new node address, and the be filtered throught addNote route method
                        //to check that is it not a duplicate, and if it is a quicker address, add it to the list
                        mRoutingTable.createNewNodeRoute(nodeName, nodeMAC, numberOfHops, nextHopAddress);

                        //Add route to the PMChat device select UI
                        mHandler.obtainMessage(MainActivity.NEW_ROUTE_DISCOVERED, -1, -1, incomingMsg).sendToTarget();

                        //need to add an acknowledge packet back to the original sender
                        PacketMessage acknowledgeMessage = new PacketMessage(nodeMAC,deviceMAC,deviceName,deviceMAC,nextHopAddress,numberOfHops);
                        //send message back to the original sender to acknowledge it
                        outgoingMessage(acknowledgeMessage);

                        //add last sender to this
                        incomingMsg.setLastSenderMAC(deviceMAC);
                        //add a hop to the packet
                        incomingMsg.addHop();

                        //serialise the message, and send it off to all other devices (except last)
                        byte[] msgByte = mSerializer.serialize(incomingMsg);


                        //send to all devices
                        redirectMessageToAll(msgByte, nextHopAddress);


                    } else{
                        // Send a failure message back to the Activity
                        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
                        Bundle bundle = new Bundle();
                        bundle.putString(MainActivity.TOAST, "Duplicate search message ignored");
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                    }


                    break;
                case 4: //ACKNOWLEDGE PACKET

                    //get all the relavant infromantion
                    String previousHopMAC = incomingMsg.getLastSenderMAC(); //last sender MAC
                    String originalSenderMAC = incomingMsg.getOriginalSenderMAC(); //get the original sender
                    String originalSenderName = incomingMsg.getOriginalSenderName();
                    String packetDirection = incomingMsg.getDestinationMAC(); //
                    int numberOfHops = incomingMsg.getNumberOfHops();

                    //make a new route, to add tot he route table
                    mRoutingTable.createNewNodeRoute(originalSenderName,originalSenderMAC,numberOfHops,previousHopMAC);

                    //Add route to the PMChat device select UI
                    mHandler.obtainMessage(MainActivity.NEW_ROUTE_DISCOVERED, -1, -1, incomingMsg).sendToTarget();

                    //if the current device is not the targeted device, send the message onwards
                    if (!incomingMsg.getDestinationMAC().equals(deviceMAC)){
                        incomingMsg.removeHop();
                        incomingMsg.setLastSenderMAC(deviceMAC);
                        outgoingMessage(incomingMsg);

                    }


                    break;

                case 5: // FAIL PACKET

                    String unreachableDevice = incomingMsg.getFailedNodeRoute();
                    mRoutingTable.removeNodeRout(unreachableDevice);

                    String destinationDevice = incomingMsg.getDestinationMAC();
                    if (!destinationDevice.equals(deviceMAC)){
                        outgoingMessage(incomingMsg);
                    }

                    //Remove the failed node route from the list of selectable device
                    mHandler.obtainMessage(MainActivity.REMOVE_FAILED_DEVICE_FROM_LIST, -1, -1, incomingMsg).sendToTarget();
                    break;


            }

        } catch (ClassNotFoundException | IOException e) {


            //removed for the main test!!!!!!!!!!
            /*
            // Send a failure message back to the Activity
            Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(MainActivity.TOAST, "incoming message currupted");
            msg.setData(bundle);
            mHandler.sendMessage(msg);
            */

        }

    }




    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed(String address) {
        Log.i("ConnectionFail", "Indicate the connection attempt failed, notify UI");

        mDeviceAddresses.remove(address);
        cheackConnection();

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost(String address) {
        Log.i("connectionLost", "Indicate that the connection was lost (notify the UI)");
        //setState(STATE_LISTEN);
        mDeviceAddresses.remove(address);
        cheackConnection();

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }


    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        BluetoothServerSocket serverSocket = null;
        public AcceptThread() {
        }
        public void run() {
            if (D) Log.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");
            BluetoothSocket socket = null;
            try {
                // Listen for all 7 UUIDs
                Log.i("AcceptThread_Run", "This thread runs while listening for incoming connections.");
                boolean success = false;
                mAcpSuccess:
                for (int i = 0; i < 7; i++) {
                    serverSocket = mAdapter.listenUsingRfcommWithServiceRecord(NAME, mUuids.get(i));
                    socket = serverSocket.accept();
                    if (socket != null) {
                        String address = socket.getRemoteDevice().getAddress();
                        //This calls the duplicate method
                        boolean checkDuplicate = isDuplicateDevice(address);
                        if (!checkDuplicate) {
                            mSockets.add(socket);
                            mDeviceAddresses.add(address);
                            success = true;
                            connected(socket, socket.getRemoteDevice());
                        } else {
                            // Send a duplication warning message back to the Activity
                            sendDuplicateDeviceToast();
                        }
                    }
                    if (success) {
                        break mAcpSuccess;
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "accept() failed", e);
            }
            if (D) Log.i(TAG, "END mAcceptThread");
        }

        public void cancel() {
            Log.i("cancel", "cancel called accept thread");
            if (D) Log.d(TAG, "cancel " + this);
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed ", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private UUID tempUuid;

        public ConnectThread(BluetoothDevice device, UUID uuidToTry) {
            Log.i("ConnectThread", "ConnectThread constructeur called, to make outgoing connections");
            mmDevice = device;
            BluetoothSocket tmp = null;
            tempUuid = uuidToTry;
            mDeviceAddresses.add(mmDevice.getAddress());
            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(uuidToTry);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }
        public void run() {
            Log.i("ConnectThread", "ConnectThread RUN called, to make outgoing connections");
            setName("ConnectThread");
            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();
            try {
                // This is a blocking call and will only return on a successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                if (tempUuid.toString().contentEquals(mUuids.get(6).toString())) {
                    mDeviceAddresses.remove(mmDevice.getAddress());
                    connectionFailed(mmDevice.getAddress());
                }
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                // Start the service over to restart listening mode
                BluetoothChatService.this.start();
                return;
            }
            // Reset the ConnectThread because we're done
            synchronized (BluetoothChatService.this) {
                mConnectThread = null;
            }
            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final BluetoothDevice mmDevice;

        public ConnectedThread(BluetoothSocket socket, BluetoothDevice device) {
            Log.i("ConnectedThread", "This thread runs during a connection with a remote device.");
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            mmDevice = device;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            //to add a new device to the PM chat service
            mHandler.obtainMessage(MainActivity.NEW_CONNECTABLE_DEVICE, -1, -1,mmDevice).sendToTarget();
            //Create new NODE, add it to the routing table
            mRoutingTable.createNewNodeRoute(mmDevice.getName(),mmDevice.getAddress(),1, mmDevice.getAddress());
            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;
            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    Log.e("MSGTFR", "INCOMING MESSAGE SIZE: " + buffer.length);
                    //Call the incoming message method, to send message to UI and re-route to other slaves
                    incomingMessage(buffer, mmDevice.getAddress());
                } catch (IOException e) {

                    //to update the fact that the connection has been lost
                    mHandler.obtainMessage(MainActivity.LOST_CONNECTION, -1, -1,mmDevice).sendToTarget();
                    //Removes the NODE ROUTE from the table
                    mRoutingTable.removeNodeRout(mmDevice.getAddress());
                    Log.e(TAG, "disconnected", e);
                    connectionLost(mmDevice.getAddress());
                    mDeviceAddresses.remove(mmDevice.getAddress());
                    mConnThreads.remove(this);
                    mSockets.remove(this.mmSocket);
                    //clear all selectable devices if there anre no more connectable device
                    if
                            (

                            mConnThreads.size()==0){
                        mHandler.obtainMessage(MainActivity.REMOVE_ALL_SELECTABLE_DEVICES, -1, -1).sendToTarget();
                    }
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                //Note that all UI displays and routing are managed by BluetoothChatService's write message
                mmOutStream.write(buffer);
                Log.e("MSGTFR", "Outgoing MESSAGE SIZE: "+ buffer.length);

            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
