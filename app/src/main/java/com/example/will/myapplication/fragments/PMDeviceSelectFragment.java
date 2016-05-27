package com.example.will.myapplication.fragments;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.will.myapplication.MainActivity;
import com.example.will.myapplication.R;

import java.util.ArrayList;

/**
 * Created by Will on 04/08/2015.
 */

public class PMDeviceSelectFragment extends Fragment {
    deviceSelectedListener deviceCallBack;

    //Inisialise the scan button to be able to search for more devices
    private Button mSearchDevices;
    //Inisialise the List view to display devices to communicate to
    private ListView mReachabelDevicesList;

    //Create a HashSet that will hold the list of connectable devices
    //This will avoid duplicate devices from being added
    private ArrayAdapter<String> mListDevices;

    //This will be a the arrayList that will save all the device
    private ArrayList<String> mArrayDevice;

    //
    ArrayList<String> mArrayDeviceMACaddress;


    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setRetainInstance(true);
        //return inflater.inflate(R.layout.personal_message_device_list, container, false);

        final View view = inflater.inflate(R.layout.personal_message_device_list, container, false);

        //inisialise the listView
        mReachabelDevicesList = (ListView) view.findViewById(R.id.connectable_devices);

        //The device will be displayed like a message, so might need to change this
        mListDevices = new ArrayAdapter<String>(getActivity(), R.layout.personal_message_device);

        //gets the saved array of MAC addresses
        if (savedInstanceState == null || !savedInstanceState.containsKey("MACList")) {
            mArrayDeviceMACaddress=new ArrayList<String>();
        } else {
            mArrayDeviceMACaddress=savedInstanceState.getStringArrayList("MACList");
        }

        //gets the saved array of deives
        if (savedInstanceState == null || !savedInstanceState.containsKey("devices")) {
            mArrayDevice = new ArrayList<String>();
        } else {
            for (int i = 0; i < mArrayDevice.size(); i++) {
                mListDevices.add(mArrayDevice.get(i));
            }
        }

        mSearchDevices = (Button) view.findViewById(R.id.scan_for_devices);
        mSearchDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Send a discovery message
                ((MainActivity) getActivity()).sendDiscoveryMessage();
            }
        });

        mReachabelDevicesList.setAdapter(mListDevices);

        //Set an onCLick Lister for each element on the listview

        mReachabelDevicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String fulladdress = mListDevices.getItem(position);
                String deviceName = fulladdress.substring(0, fulladdress.indexOf("\n"));
                String MACaddress = fulladdress.substring(fulladdress.indexOf("\n") + 1);

                ((MainActivity) getActivity()).devicePMSelected(deviceName, MACaddress);

            }
        });
        return view;

    }


    /**
     * this will be called to create a
     * @param position
     */
    public void onDeviceSelected(int position){


    }




    public void addDeviceToList(String deviceName, String deviceAddress) {
        if (!mArrayDeviceMACaddress.contains(deviceAddress)){
            String deviceListing = deviceName + "\n" + deviceAddress;
            mListDevices.add(deviceListing);
            mArrayDevice.add(deviceListing);
            mArrayDeviceMACaddress.add(deviceAddress);
        }
    }


    public void removeDeviceToList(String deviceAddress) {
        //Note that both the ArrayList and ArrayAdapter items will be removed
        //the issue is that we assume that bothe the ArrayList and ArrayAdapter are of the same size!!!
        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        String removeDevice = null;

        //remove the address from the list of MAC address
        mArrayDeviceMACaddress.remove(deviceAddress);




        //Find the device corresponding to the same MAC address, and remove it from both the ArrayList and ArrayAdapter
        deviceLoop:for (int i = 0; i < mArrayDevice.size(); i++) {
            if (mArrayDevice.get(i).toLowerCase().contains(deviceAddress.toLowerCase())) {

                //we remove the object that looks the same from ArrayLIst of Array Adapter
                mListDevices.remove(mArrayDevice.get(i));
                mArrayDevice.remove(i);
                //mListDevices.remove(mListDevices.getItem(i));
                //removeDevice =mListDevices.getItem(i);

                //Note sure if this works???????????
                //mListDevices.notifyDataSetChanged();
                break deviceLoop;
            }
        }
    }

    public void clearAllSelectableDevices(){
        mArrayDeviceMACaddress.clear();
        mArrayDevice.clear();
        mListDevices.clear();
    }



    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("devices", mArrayDevice);
        outState.putStringArrayList("MACList",mArrayDeviceMACaddress);
    }


    /**
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * Interface to communicate to the Main activity
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     */

    //This is in order to send a string message back to the MainActivity
    public interface deviceSelectedListener {
        void deviceSelectedListener(String deviceSelected);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception

        //this will call the MainActivity to creat a new fragment on device selected
        try {
            deviceCallBack = (deviceSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException((activity.toString() + " must implement deviceSelectedListener"));
        }
    }
}