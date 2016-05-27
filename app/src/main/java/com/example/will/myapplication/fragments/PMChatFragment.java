package com.example.will.myapplication.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

import com.example.will.myapplication.MainActivity;
import com.example.will.myapplication.PMConversation;
import com.example.will.myapplication.R;

import java.util.ArrayList;

public class PMChatFragment extends Fragment {
    PersonalMessageSelectedListener personalMessageCallBack;

    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;

    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;

    private ArrayList<String> mConversationArrayList;

    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer = new StringBuffer("");



    public PMChatFragment(){
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setRetainInstance(true);

        final View view = inflater.inflate(R.layout.personal_message_chat_fragment, container, false);
        mConversationView = (ListView)view.findViewById(R.id.in);

        mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);
        mConversationView.setAdapter(mConversationArrayAdapter);


        if (savedInstanceState==null|| !savedInstanceState.containsKey("conversation")){
            mConversationArrayList =new ArrayList<String>();
        } else {
            mConversationArrayList = savedInstanceState.getStringArrayList("conversation");
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
                // Send a message using content of the edit text widget
                TextView viewText = (TextView) view.findViewById(R.id.edit_text_out);
                String message = viewText.getText().toString();

                ((MainActivity)getActivity()).sendPersonalMessage(message);

                //addMessageToChat("Me", message);


                // Reset out string buffer to zero and clear the edit text field
                mOutStringBuffer.setLength(0);
                mOutEditText.setText(mOutStringBuffer);
            }
        });

        return view;

    }


    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("conversation", mConversationArrayList);
    }




    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
            new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                    // If the action is a key-up event on the return key, send the message
                    if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                        String message = view.getText().toString();
                        sendPersonalMessage(message);
                    }
                    return true;
                }
            };




    /*
    Called when a new device is selected, and removes the conversation, and displayes the new conversation
     */
    public void deviceSelectedChange(PMConversation previousConversation){
        //need to remove all items from array adapter
        mConversationArrayAdapter.clear();

        //add conversation to arraylist
        mConversationArrayList=previousConversation.getConversations();

        //put conversation into arrayadapter
        for (int i=0;i<mConversationArrayList.size();i++){
            mConversationArrayAdapter.add(mConversationArrayList.get(i));
        }
    }


    public void addMessageToChat(String sender, String messageContent) {
        String message = sender + ": " + messageContent;
        //mConversationArrayList.add(message);
        mConversationArrayAdapter.add(message);

    }


    public void sendPersonalMessage(String message){
        //This section will send the string back to the MainActivity
        //(MainActivity)getActivity().
    }











    /**
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * Interface to communicate to the Main activity
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     */
    //This is in order to send a string message back to the MainActivity
    public interface PersonalMessageSelectedListener{
        void PersonalMessageSelectedListener(String personalMessage);
    }

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try{
            personalMessageCallBack = (PersonalMessageSelectedListener) activity;
        } catch (ClassCastException e){
            throw new ClassCastException((activity.toString() + " must implement PersonalMessageSelectedListener"));
        }
    }
}