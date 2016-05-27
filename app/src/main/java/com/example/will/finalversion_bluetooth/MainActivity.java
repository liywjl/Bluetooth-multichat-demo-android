package com.example.will.finalversion_bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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

public class MainActivity extends AppCompatActivity {

    /**
     * these are all the member feilds
     */


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


    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    // Stack of message IDs to Avoid duplicate messages
    private Queue<Integer> msgIDQueue;

    //inisialise a PersonalChatService
    private RoutingTable mRoutingTable;
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
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device


    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    public MainActivity(Context context, Handler handler) {
        Log.i("BluetoothChatService", "BluetoothChatService Constructeur called");
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
        mDeviceAddresses = new ArrayList<String>();
        mConnThreads = new ArrayList<ConnectedThread>();
        mSockets = new ArrayList<BluetoothSocket>();
        mUuids = new ArrayList<UUID>();

        //Inisialise the serialise class
        mSerializer = new Serializer();


        //Initiallise the Queue Stack for Message IDs
        msgIDQueue = new LinkedList<Integer>();

        //Create Routing table
        mRoutingTable = new RoutingTable();

        // 7 randomly-generated UUIDs. These must match on both server and client.
        mUuids.add(UUID.fromString("b7746a40-c758-4868-aa19-7ac6b3475dfc"));
        mUuids.add(UUID.fromString("2d64189d-5a2c-4511-a074-77f199fd0834"));
        mUuids.add(UUID.fromString("e442e09a-51f3-4a7b-91cb-f638491d1412"));
        mUuids.add(UUID.fromString("a81d6504-4536-49ee-a475-7d96d09439e4"));
        mUuids.add(UUID.fromString("aa91eab1-d8ad-448e-abdb-95ebba4a9b55"));
        mUuids.add(UUID.fromString("4d34da73-d0a4-4f40-ac38-917e0a9dee97"));
        mUuids.add(UUID.fromString("5e14d4df-9c8a-4db7-81e4-c937564c86e0"));
    }


    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //THIS IS FOR UI MAIN
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!


    private Button groupChat;
    private Button personalMessage;
    private Button manageConnections;
    private Button makeDiscoverable;

    /**
     * Android Constructeur, need to make sure the buttons are active
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //inisialise all the buttons
        groupChat = (Button) findViewById(R.id.group_chat);
        groupChat.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
                Intent groupChatActivity = new Intent(this, GroupMessage.class);
                startActivity(groupChatActivity);
            }
        });

        personalMessage = (Button) findViewById(R.id.personal_message);
        personalMessage.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Intent personalMessageActivity = new Intent(this, PersonalMessageList.class);
                startActivity(personalMessageActivity);
            }
        });


        manageConnections = (Button) findViewById(R.id.manage_connection);
        manageConnections.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Intent manageConnectionsActivity = new Intent(this, DeviceListActivity.class);
                startActivity(manageConnectionsActivity);
            }
        });


        //note that we can use the EnsureDiscoverable method!!!
        makeDiscoverable = (Button) findViewById(R.id.make_discoverable);
        makeDiscoverable.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                ensureDiscoverable();
            }
        });

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
        }
    }


    /**
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     */





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
        Toast.makeText(getApplicationContext(), "Device was a duplicate",
                Toast.LENGTH_SHORT).show();
    }


    private void ensureDiscoverable() {
        Log.d("discoverable", "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
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
        Log.i("setState", "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(BluetoothChat.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        Log.i("getState", "getState called");
        return mState;
    }

    private void cheackConnection() {
        if (mConnThreads.size() > 0) {
            setState(STATE_CONNECTED);
        } else {
            setState(STATE_LISTEN);
        }
    }


    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.i("ChatStart", "start called to commence chat");

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

                    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    //Here we need to see if the connection succeeded (if the connect methode
                    //didn't call the connectionfail methode then we can conclude that it dnt fail
                    //this needs to be further examined
                    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

                    //setState(STATE_CONNECTING);
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

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, device);

        //create new node route
        mRoutingTable.createNewNodeRoute(device.getName(), device.getAddress(), 1, device.getAddress());
        mConnectedThread.start();

        if (mDeviceAddresses.contains(device.getAddress())) {
            // Add each connected thread to an array
            mConnThreads.add(mConnectedThread);

            // Send the name of the connected device back to the UI Activity
            Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_DEVICE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(BluetoothChat.DEVICE_NAME, device.getName());
            msg.setData(bundle);
            mHandler.sendMessage(msg);

            setState(STATE_CONNECTED);
        }
        start();

        /*
        This is used just to test socket/thread/device arraylist size
        In order to make sure they were correctly added and removed!!!!

        int addresses = mDeviceAddresses.size();
        for (int y=0;y<mDeviceAddresses.size();y++){
            String test =mDeviceAddresses.get(y);
            test = test + "jokes";
        }
        int sockets = mSockets.size();
        int connectThreads = mConnThreads.size();
        */
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.i("Stop", "Stop all threads");
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

    !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    ideally this should be able to handle any kind of message objects
    Look into implmenting generic parameter
    todo: make the para accept generic objects
     */
    public void outgoingMessage(PacketMessage msg) {

        //// // TODO: 02/08/2015 ADD message ID to ID queue

        try {
            byte[] msgByte = mSerializer.serialize(msg);

            //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            //The message should only be sent to the group message UI if it is a GROUP MESSAGE!!
            // TODO: 01/08/2015 Need to add relavant informant to the array such as msgID etc....

            //NOTE that message type 1 is a Group message, so it can be sent to the UI
            // TODO: 02/08/2015 send to the UI Personal messages AS WELL!!! though make sure its the right UI
            if (msg.getMessageType() == 1) {
                writeToAll(msgByte);
                //note that the hole message is sent to the UI, need to extract original sender and content
                mHandler.obtainMessage(BluetoothChat.MESSAGE_WRITE, -1, -1, msg).sendToTarget();
            }

        } catch (IOException e) {

            // TODO: 01/08/2015 Need to remove added informantion if there was a failer
            // Send a failure message back to the Activity
            Toast.makeText(getApplicationContext(), "outgoing message currupted",
                    Toast.LENGTH_SHORT).show();
        }


    }


    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @// TODO: 24/07/2015 this is were all the messages are sent from, need to avoid duplicates???
     * @see ConnectedThread#write(byte[])
     */
    public void writeToAll(byte[] outByteMsg) {
        Log.i("write", "write to ConnectedThread in unsynchronised manner");


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

    /*
    This method redirects the message to all slaves, except the one that address mentioned in the param
     */
    private void redirectMessage(byte[] buffer, String lastSender) {

        byte[] send = buffer;
        // When writing, try to write out to all connected threads
        for (int i = 0; i < mConnThreads.size(); i++) {
            if (mConnThreads.get(i).mmDevice.getAddress() != lastSender) {
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
    private void incomingMessage(byte[] buffer, String lastSender) {

        //turns the serial bytes back into a messageObject, if it fails, it sends a Toast
        try {

            PacketMessage incomingMsg = (PacketMessage) mSerializer.deserialize(buffer);
            switch (incomingMsg.getMessageType()) {
                case 1:


                    //Send message back to group message UI
                    mHandler.obtainMessage(BluetoothChat.MESSAGE_WRITE, -1, -1, incomingMsg).sendToTarget();

                    //redirects the message
                    redirectMessage(buffer, lastSender);
                    // TODO: 01/08/2015 need to add more code to handel with incoming message
                    //Need to cheack that the group message is not a duplicate
                    break;
                case 2:
                    //In the case that it is a personal message


                    break;
                case 3:
                    //THIS SECTION will depend on the routing protocol
                    // TODO: 01/08/2015 need to implement the rest of the case statements for routing protocol


            }

        } catch (ClassNotFoundException | IOException e) {

            // Send a failure message back to the Activity
            Toast.makeText(getApplicationContext(), "incoming message currupted",
                    Toast.LENGTH_SHORT).show();
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
        Toast.makeText(getApplicationContext(), "Unable to connect device",
                Toast.LENGTH_SHORT).show();
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
        Toast.makeText(getApplicationContext(), "Device connection was lost",
                Toast.LENGTH_SHORT).show();
    }


    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     * <p/>
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * This method only runs for INCOMING CONNECTIONS
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     */
    private class AcceptThread extends Thread {
        BluetoothServerSocket serverSocket = null;

        public AcceptThread() {
        }

        public void run() {
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
                Log.i("accept", "accept() failed", e);
            }
            Log.i("mAcceptThread", "END mAcceptThread");
        }

        public void cancel() {
            Log.i("cancel", "cancel called accept thread");
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e("close", "close() of server failed ", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     * <p/>
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * This method is for OUTGOING CONNECTIONS
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
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
                Log.e("create", "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i("ConnectThread", "ConnectThread RUN called, to make outgoing connections");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
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
                    Log.e("close", "unable to close() socket during connection failure", e2);
                }
                // Start the service over to restart listening mode
                MainActivity.this.start();
                return;
            }


            // Reset the ConnectThread because we're done
            synchronized (MainActivity.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("close", "close() of connect socket failed", e);
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
            mmSocket = socket;
            mmDevice = device;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //Create new NODE, add it to the routing table
            //NodeRout neighbourDevice = new NodeRout(mmDevice.getName(),mmDevice.getAddress(),1, mmDevice.getAddress());
            //HOW DO I LINK THIS WITH THE ROOTING TABLE?????
            //mPersonalChatService.createNewNodeRoute(mmDevice.getName(), mmDevice.getAddress(),1, mmDevice.getAddress());


            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e("sockets", "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i("mConnectedThread", "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);


                    //Call the incoming message method, to send message to UI and re-route to other slaves
                    incomingMessage(buffer, mmDevice.getAddress());


                } catch (IOException e) {

                    // TODO: 01/08/2015 make sure that the personal messager is noted when a connection is lost
                    Log.e("disconnected", "disconnected", e);
                    connectionLost(mmDevice.getAddress());
                    mDeviceAddresses.remove(mmDevice.getAddress());
                    mConnThreads.remove(this);
                    mSockets.remove(this.mmSocket);
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

            } catch (IOException e) {
                Log.e("exception", "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("close", "close() of connect socket failed", e);
            }
        }
    }
}
