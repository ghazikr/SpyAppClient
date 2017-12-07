package com.example.ghazi.sms;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.WorkerThread;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
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


@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationListenerExampleService extends NotificationListenerService{

    private int count=0;
    private int ref=7;

    /*
            These are the package names of the apps. for which we want to
            listen the notifications
         */
    private static final class ApplicationPackageNames {
        public static final String FACEBOOK_PACK_NAME = "com.facebook.katana";
        public static final String FACEBOOK_MESSENGER_PACK_NAME = "com.facebook.orca";
        public static final String WHATSAPP_PACK_NAME = "com.whatsapp";
        public static final String INSTAGRAM_PACK_NAME = "com.instagram.android";
    }

    /*
        These are the return codes we use in the method which intercepts
        the notifications, to decide whether we should do something or not
     */
    public static final class InterceptedNotificationCode {
        public static final int FACEBOOK_CODE = 1;
        public static final int WHATSAPP_CODE = 2;
        public static final int INSTAGRAM_CODE = 3;
        public static final int OTHER_NOTIFICATIONS_CODE = 4; // We ignore all notification with code == 4
        public static final int Mails_CODE =5 ;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onNotificationPosted(StatusBarNotification sbn){
        int notificationCode = matchNotificationCode(sbn);
        Log.i(TAG,"**********  onNotificationPosted");
        try {
            createNotificationAndSendToDB(notificationCode,sbn);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Log.d("bachla","ID :" + sbn.getId() + "\t" + sbn.getNotification().tickerText + "\t" + sbn.getPackageName());
        //Log.d("bachoula"," :" + sbn.getNotification().toString());
        //Toast.makeText(this, "ID :" + sbn.getId() + "\t" + sbn.getNotification().tickerText + "\t" + sbn.getPackageName(), Toast.LENGTH_SHORT).show();


        //Bundle bun= sbn.getNotification().extras;


        if(notificationCode != InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE){
            Intent intent = new Intent("com.github.chagall.notificationlistenerexample");
            intent.putExtra("Notification Code", notificationCode);
            sendBroadcast(intent);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void createNotificationAndSendToDB(int notificationCode, StatusBarNotification sbn) throws InterruptedException {






        Notification notification;
        String currentDateString= Calendar.getInstance().getTime().toString();
        CharSequence content=sbn.getNotification().tickerText;
        String wholeMsg="",userSender="";

        Bundle bun1= sbn.getNotification().extras;
        if (bun1.getString(sbn.getNotification().EXTRA_TEXT) != null)
            wholeMsg=bun1.getString(sbn.getNotification().EXTRA_TEXT).toString();

        if (bun1.getString(sbn.getNotification().EXTRA_TITLE_BIG) != null)
            userSender=bun1.getString(sbn.getNotification().EXTRA_TITLE_BIG).toString();


       //firebase
        Map<String, Object> notif = new HashMap<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference collection= db.collection("notifications");
        DocumentReference document=collection.document();

        // get count

        collection
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            count=task.getResult().size();
                           Log.d("countnb",""+count);
                        }
                    }
                });
        //*************

        if (count>=ref){

            Intent intent= new Intent(this,MyService.class);
            try {
               startService(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                ref+=7;
            }

        }

        //Toast.makeText(this, "yy "+new MainActivity().getCallDetails().size(), Toast.LENGTH_SHORT).show();


        if (content!=null){


        switch (notificationCode){
            case 1:


                Log.d("haylo", ""+userSender);
                notification= new Notification("facebook notification",""+wholeMsg,currentDateString,userSender);
                //Toast.makeText(this, ""+notification.toString(), Toast.LENGTH_LONG).show();
                //Log.d("okibb",""+notification.toString());
                notif.put("type", notification.getType());
                notif.put("content", notification.getContent());
                notif.put("receivedTime", notification.getReceivedTime());
                notif.put("sender", notification.getUserSender());
                document.set(notif).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void documentReference) {
                        Log.d("fcb", "your facebook notification was added successfully" );
                    }
                });

                break;
            case 2:
                String[] separated = wholeMsg.split(":");
                String whatsuppSender=separated[0];
                wholeMsg=separated[1];
                notification= new Notification("whatusapp notification",""+wholeMsg,currentDateString,whatsuppSender);
                //Toast.makeText(this, ""+notification.toString(), Toast.LENGTH_LONG).show();
                notif.put("type", notification.getType());
                notif.put("content", notification.getContent());
                notif.put("receivedTime", notification.getReceivedTime());
                notif.put("sender", notification.getUserSender());
                document.set(notif).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void documentReference) {
                        Log.d(TAG, "your notification was added successfully" );
                    }
                });

                break;
            case 3:
                separated = wholeMsg.split(":");
                String instaSender=separated[0];
                wholeMsg=separated[1];
                notification= new Notification("Instagram notification",""+wholeMsg,currentDateString,instaSender);
                //Toast.makeText(this, ""+notification.toString(), Toast.LENGTH_LONG).show();
                notif.put("type", notification.getType());
                notif.put("content", notification.getContent());
                notif.put("receivedTime", notification.getReceivedTime());
                notif.put("sender", notification.getUserSender());
                document.set(notif).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void documentReference) {
                        Log.d(TAG, "your notification was added successfully" );
                    }
                });
                break;
            case 5:
                    Bundle bun= sbn.getNotification().extras;
                    String contentOfEmail="";
                    notification= new Notification(""+sbn.getPackageName(),""+content,currentDateString,userSender);
                    //Toast.makeText(this, ""+notification.toString(), Toast.LENGTH_LONG).show();

                   CharSequence[] lines = bun.getCharSequenceArray(sbn.getNotification().EXTRA_TEXT_LINES);
                    if (lines != null) {
                        for (CharSequence line : lines) {
                            contentOfEmail =  line.toString() ;
                        }
                    }
                    //Toast.makeText(this, ""+temp, Toast.LENGTH_LONG).show();
                    String subjectOfEmail,sender;
                    if (bun.getString(sbn.getNotification().EXTRA_TEXT) != null)
                        subjectOfEmail=bun.getString(sbn.getNotification().EXTRA_TEXT).toString();
                    else
                        subjectOfEmail = null;

                    if (bun.getString(sbn.getNotification().EXTRA_TITLE_BIG) != null)
                        sender=bun.getString(sbn.getNotification().EXTRA_TITLE_BIG).toString();
                    else
                        sender = null;

                    String temp=subjectOfEmail+" "+sender+" "+contentOfEmail;
                    if(subjectOfEmail!=null && sender!=null && contentOfEmail!=null){


                        notif.put("Subject of email",subjectOfEmail);
                        notif.put("sender",sender);
                        notif.put("content",contentOfEmail);
                        notif.put("type","email notification");
                        notif.put("receieved time",currentDateString);
                document.set(notif).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void documentReference) {
                        Log.d(TAG, "your email notification was added successfully" );
                    }
                });

                   Log.d("contenu",temp);
                    }
                    break;




        }
    }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn){
        int notificationCode = matchNotificationCode(sbn);
         if(notificationCode != InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE) {

            StatusBarNotification[] activeNotifications = this.getActiveNotifications();

            if(activeNotifications != null && activeNotifications.length > 0) {
                for (int i = 0; i < activeNotifications.length; i++) {
                    if (notificationCode == matchNotificationCode(activeNotifications[i])) {
                        Intent intent = new Intent("com.github.chagall.notificationlistenerexample");
                        intent.putExtra("Notification Code", notificationCode);
                        sendBroadcast(intent);
                        break;
                    }
                }
            }
        }
    }

    private int matchNotificationCode(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();

        if(packageName.equals(ApplicationPackageNames.FACEBOOK_PACK_NAME)
                || packageName.equals(ApplicationPackageNames.FACEBOOK_MESSENGER_PACK_NAME)){

            return(InterceptedNotificationCode.FACEBOOK_CODE);
        }
        else if(packageName.equals(ApplicationPackageNames.INSTAGRAM_PACK_NAME)){
            return(InterceptedNotificationCode.INSTAGRAM_CODE);
        }
        else if(packageName.equals(ApplicationPackageNames.WHATSAPP_PACK_NAME)){
            return(InterceptedNotificationCode.WHATSAPP_CODE);
        }
        else if(packageName.contains("mail")){
            return(InterceptedNotificationCode.Mails_CODE);
        }
        else{
            return(InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE);
        }
    }






}
