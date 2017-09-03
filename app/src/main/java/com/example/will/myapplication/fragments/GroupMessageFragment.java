package com.example.will.myapplication.fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.will.myapplication.MainActivity;
import com.example.will.myapplication.R;

import java.util.ArrayList;

public class GroupMessageFragment extends Fragment {
    GroupMessageSelectedListener groupCallBack;

    private boolean isConnected = false;

    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;

    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    private ArrayList<String> mConversationArrayList;

    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer = new StringBuffer("");


    public GroupMessageFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container,savedInstanceState);
        setRetainInstance(true);

        final View view = inflater.inflate(R.layout.group_chat_fragment, container, false);

        // Final View view = inflater.inflate(R.layout.group_chat_fragment, container, false);
        mConversationView = (ListView) view.findViewById(R.id.in);

        mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);
        mConversationView.setAdapter(mConversationArrayAdapter);

        if (savedInstanceState==null|| !savedInstanceState.containsKey("conversation")){
            mConversationArrayList =new ArrayList<String>();
        } else {
            mConversationArrayList = savedInstanceState.getStringArrayList("conversation");
            int jokes = mConversationArrayList.size();
            for (int i=0;i<mConversationArrayList.size();i++){

                mConversationArrayAdapter.add(mConversationArrayList.get(i));
            }
        }

        // Initialize the compose field with a listener for the return key
        mOutEditText = (EditText) view.findViewById(R.id.edit_text_out);
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton = (Button) view.findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            if (checkConnection()){
                // Send a message using content of the edit text widget
                TextView viewText = (TextView) view.findViewById(R.id.edit_text_out);
                String message = viewText.getText().toString();

                // Send the message to the MainActivity
                ((MainActivity)getActivity()).sendGroupMessage(message);

                // Displays the text to the screen, need to make sure that it goes to thr right
                mConversationArrayAdapter.add("Me:  " + message);

                // Add to the arraylist so it can be saved, if the fragment is stopped
                mConversationArrayList.add("Me:  " + message);
                // Reset out string buffer to zero and clear the edit text field
                mOutStringBuffer.setLength(0);
                mOutEditText.setText(mOutStringBuffer);
            } else {
                Toast.makeText(container.getContext(),"Device is not Connected", Toast.LENGTH_SHORT).show();
            }
            }
        });
        return view;
    }


    public boolean checkConnection(){
        boolean connected =((MainActivity)getActivity()).checkConnection();
        isConnected=connected;
        return connected;
    }

    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
        new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                addMessageToChat("Me",message);
            }
            return true;
            }
        };

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("conversation", mConversationArrayList);
    }

    public void addMessageToChat(String messageContent, String sender) {
        String message = sender + ": " + messageContent;
        mConversationArrayList.add(message);
        mConversationArrayAdapter.add(message);

    }

    /**
     * Interface to communicate to the Main activity
     */

    //This is in order to send a string message back to the MainActivity
    public interface GroupMessageSelectedListener {
        void onGroupMessageSent(String groupMessage);
    }

    @Override
    /**
     * TODO: Need to remove as is deprecated on latest version of Android
     */
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            groupCallBack = (GroupMessageSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException((activity.toString() + " must implement GroupMessageSelectedListener"));
        }
    }
}