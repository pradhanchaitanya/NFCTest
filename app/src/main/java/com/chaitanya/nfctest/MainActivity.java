package com.chaitanya.nfctest;

import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Parcelable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Adapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements NfcAdapter.CreateNdefMessageCallback, NfcAdapter.OnNdefPushCompleteCallback {

    private NfcAdapter mNfcAdapter;

    private ArrayList<String> messagesToSendArray = new ArrayList<>();
    private ArrayList<String> messagesReceivedArray = new ArrayList<>();

    private EditText mAddMessageEditText;
    private TextView mSendMessageTextView, mReceivedMessageTextView;
    private Button mAddMessageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAddMessageEditText = (EditText) findViewById(R.id.et_add_message);
        mSendMessageTextView = (TextView) findViewById(R.id.tv_send_message);
        mReceivedMessageTextView = (TextView) findViewById(R.id.tv_received_message);
        mAddMessageButton = (Button) findViewById(R.id.bt_add_message);

        mAddMessageButton.setText("Add Message");
        updateTextViews();

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        // handle NFC non-availability
        if (mNfcAdapter != null && mNfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC available", Toast.LENGTH_LONG).show();
            mNfcAdapter.setNdefPushMessageCallback(this, this);
            mNfcAdapter.setOnNdefPushCompleteCallback(this, this);
        } else {
            Toast.makeText(this, "NFC not available", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTextViews();
        handleNfcIntent(getIntent());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("messagesToSend", messagesToSendArray);
        outState.putStringArrayList("lastMessagesReceived", messagesReceivedArray);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        messagesToSendArray = savedInstanceState.getStringArrayList("messagesToSend");
        messagesReceivedArray = savedInstanceState.getStringArrayList("lastMessagesReceived");
    }

    public void addMessage(View view) {
        String newMessage = mAddMessageEditText.getText().toString();
        messagesToSendArray.add(newMessage);

        mAddMessageEditText.setText(null);
        updateTextViews();

        Toast.makeText(this, "Message added", Toast.LENGTH_LONG).show();
    }

    private void updateTextViews() {
        mSendMessageTextView.setText("Messages to send: \n");
        if (messagesToSendArray.size() > 0) {
            for (int i = 0; i < messagesToSendArray.size(); i++) {
                mSendMessageTextView.append(messagesToSendArray.get(i));
                mSendMessageTextView.append("\n");
            }
        }

        mReceivedMessageTextView.setText("Messages received: \n");
        if (messagesReceivedArray.size() > 0) {
            for (int i = 0; i < messagesReceivedArray.size(); i++) {
                mReceivedMessageTextView.append(messagesReceivedArray.get(i));
                mReceivedMessageTextView.append("\n");
            }
        }
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent nfcEvent) {
        if (messagesToSendArray.size() == 0) {
            return null;
        }

        NdefRecord[] recordsToAttach = createRecords();
        return new NdefMessage(recordsToAttach);
    }

    @Override
    public void onNdefPushComplete(NfcEvent nfcEvent) {
        messagesToSendArray.clear();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleNfcIntent(intent);
    }

    public NdefRecord[] createRecords() {
        NdefRecord[] records = new NdefRecord[messagesToSendArray.size()];

        for (int i = 0; i < messagesToSendArray.size(); i++) {
            byte[] payload = messagesToSendArray.get(i)
                    .getBytes(Charset.forName("UTF-8"));

            NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                    NdefRecord.RTD_TEXT,
                    new byte[0],
                    payload);

            records[i] = record;
        }

        records[messagesToSendArray.size()] = NdefRecord.createApplicationRecord(getPackageName());
        return records;
    }

    private void handleNfcIntent(Intent nfcIntent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(nfcIntent.getAction())) {
            Parcelable[] receivedArray = nfcIntent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (receivedArray != null) {
                messagesReceivedArray.clear();
                NdefMessage receivedMessage = (NdefMessage) receivedArray[0];
                NdefRecord[] attachedRecords = receivedMessage.getRecords();

                for (NdefRecord record : attachedRecords) {
                    String string = new String(record.getPayload());
                    if (string.equals(getPackageName())) {
                        continue;
                    }
                    messagesReceivedArray.add(string);
                }

                Toast.makeText(this, "Received " + messagesReceivedArray.size() + " Messages",
                        Toast.LENGTH_LONG).show();
                updateTextViews();
            } else {
                Toast.makeText(this, "Received blank parcel", Toast.LENGTH_LONG).show();
            }
        }
    }
}