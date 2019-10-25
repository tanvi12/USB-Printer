package hairban.stepbystep.myapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.os.FileObserver;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

public class FileObserverService extends Service {
    private FileObserver mFileObserver;
    String fileToScan;
    String lastFileScan = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, AnotherPrinterActivity.class), 0);

        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, "hairban.stepbystep.myapplication")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Printer App")
                .setContentIntent(pendingIntent).build();

        startForeground(1337, notification);


        if (intent.hasExtra("file")) {
            mFileObserver = new FileObserver(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath(), FileObserver.CREATE) {
                @Override
                public void onEvent(int event, String path) {
                    // If an event happens we can do stuff inside here
                    // for example we can send a broadcast message with the event-id
                    if (path != null && (path.startsWith("Token_DCo") || path.startsWith("Bill") || path.startsWith("Token_Path") || path.startsWith("Token_Radiology") || path.startsWith("Token_PHC") || path.startsWith("Token_billTest"))) {
                        if (!path.contains("crdownload")) {
                            Log.d("File redirected", path);
                            printFile(path.substring(0, path.indexOf(".pdf") + 4));
                        }
                    }
                }
            };
            mFileObserver.startWatching(); // The FileObserver starts watching
        }

        return Service.START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("hairban.stepbystep.myapplication", "Printer", importance);
            channel.setDescription("printer app");
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void printFile(String filename) {

        try {
            SharedPreferences sp = getSharedPreferences("app", Context.MODE_PRIVATE);
            String lastFile = sp.getString("last_file", "");
            fileToScan = filename;
            if (lastFile.length() == 0) {
                fileToScan = filename;
            } else {
                if (Integer.parseInt(filename.replaceAll("[^0-9]", "")) > Integer.parseInt(lastFile.replaceAll("[^0-9]", ""))) {
                    fileToScan = filename;
                } else {
                    fileToScan = null;
                }
            }

            if ((lastFileScan == null || !lastFileScan.equalsIgnoreCase(fileToScan)) && fileToScan != null) {
                lastFileScan = fileToScan;
                Intent sendLevel = new Intent();
                sendLevel.setAction("SENDTOPRINTER");
                sendLevel.putExtra("LEVEL_DATA", fileToScan);
                sendBroadcast(sendLevel);

            }
        } catch (Exception e) {
            e.printStackTrace();

        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}