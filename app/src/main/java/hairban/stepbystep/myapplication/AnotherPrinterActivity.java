package hairban.stepbystep.myapplication;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.printservice.PrintService;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class AnotherPrinterActivity extends AppCompatActivity {
    private UsbManager mUsbManager;
    private UsbDevice mDevice;
    private UsbDeviceConnection mConnection;
    private UsbInterface mInterface;
    private UsbEndpoint mEndPoint;
    private PendingIntent mPermissionIntent;
    private static final String ACTION_USB_PERMISSION = "hairban.stepbystep.myapplication.USB_PERMISSION";
    private static Boolean forceCLaim = true;
    Button detectPrint;
    HashMap<String, UsbDevice> mDeviceList;
    Iterator<UsbDevice> mDeviceIterator;
    byte[] testBytes;
    WifiLevelReceiver wifiLevelReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main1);


        Button print = (Button) findViewById(R.id.print);
        final EditText txt_product_id = (EditText) findViewById(R.id.txt_product_id);
        detectPrint = (Button) findViewById(R.id.detectPrint);
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        txt_product_id.setText(sharedPref.getString("vendor_id", ""));

        if (checkPermission()) {
            startService();
        } else
            requestPermission();
        wifiLevelReceiver = new WifiLevelReceiver();
        registerReceiver(wifiLevelReceiver, new IntentFilter("SENDTOPRINTER"));

        print.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                print(mConnection, mInterface);
            }
        });
        detectPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (txt_product_id.getText().toString().isEmpty()) {
                    Toast.makeText(AnotherPrinterActivity.this, "Please Enter Product Id", Toast.LENGTH_SHORT).show();
                } else {
                    SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("vendor_id", txt_product_id.getText().toString());
                    editor.apply();
                    detectDevice();
                }
            }
        });
    }

    private void detectDevice() {
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mDeviceList = mUsbManager.getDeviceList();

        if (mDeviceList.size() > 0) {
            mDeviceIterator = mDeviceList.values().iterator();

            Toast.makeText(this, "Device List Size: " + String.valueOf(mDeviceList.size()), Toast.LENGTH_SHORT).show();

            TextView textView = (TextView) findViewById(R.id.usbDevice);
            String usbDevice = "";
            while (mDeviceIterator.hasNext()) {
                UsbDevice usbDevice1 = mDeviceIterator.next();
                usbDevice += "\n" +
                        "DeviceID: " + usbDevice1.getDeviceId() + "\n" +
                        "DeviceName: " + usbDevice1.getDeviceName() + "\n" +
                        "Protocol: " + usbDevice1.getDeviceProtocol() + "\n" +
//                        "Product Name: " + usbDevice1.getProductName() + "\n" +
//                        "Manufacturer Name: " + usbDevice1.getManufacturerName() + "\n" +
                        "DeviceClass: " + usbDevice1.getDeviceClass() + " - " + translateDeviceClass(usbDevice1.getDeviceClass()) + "\n" +
                        "DeviceSubClass: " + usbDevice1.getDeviceSubclass() + "\n" +
                        "VendorID: " + usbDevice1.getVendorId() + "\n" +
//                        "Serial nuber: " + usbDevice1.getSerialNumber() + "\n" +
                        "ProductID: " + usbDevice1.getProductId() + "\n";


                int interfaceCount = usbDevice1.getInterfaceCount();
                Toast.makeText(this, "INTERFACE COUNT: " + String.valueOf(interfaceCount), Toast.LENGTH_SHORT).show();

                if (usbDevice1.getVendorId() == Integer.parseInt(detectPrint.getText().toString())) {
                    mDevice = usbDevice1;

                    mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
                    registerReceiver(mUsbReceiver, filter);

                    mUsbManager.requestPermission(mDevice, mPermissionIntent);
                    Toast.makeText(this, "Printer is detected", Toast.LENGTH_SHORT).show();
                }
                textView.setText(usbDevice);
            }


        } else {
            Toast.makeText(this, "Please attach printer via USB", Toast.LENGTH_SHORT).show();
        }
    }

    private void print(final UsbDeviceConnection connection, final UsbInterface usbInterface) {
        final String test = "THIS IS A PRINT TEST\r\n";
        testBytes = test.getBytes();

        if (usbInterface == null) {
            Toast.makeText(this, "INTERFACE IS NULL", Toast.LENGTH_SHORT).show();
        } else if (connection == null) {
            Toast.makeText(this, "CONNECTION IS NULL", Toast.LENGTH_SHORT).show();
        } else if (forceCLaim == null) {
            Toast.makeText(this, "FORCE CLAIM IS NULL", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Sending data", Toast.LENGTH_SHORT).show();
            connection.claimInterface(usbInterface, forceCLaim);

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    byte[] cut_paper = {0x1D, 0x56, 0x41, 0x10};
                    connection.bulkTransfer(mEndPoint, testBytes, testBytes.length, 0);
                    connection.bulkTransfer(mEndPoint, cut_paper, cut_paper.length, 0);
                }
            });
            thread.run();
        }
    }

    class WifiLevelReceiver extends BroadcastReceiver {

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals("SENDTOPRINTER")) {
                String level = intent.getStringExtra("LEVEL_DATA");
                try {
                    String parsedText = "";
                    PdfReader reader = new PdfReader(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/" + level).getPath());
                    int n = reader.getNumberOfPages();
                    for (int i = 0; i < n; i++) {
                        parsedText = parsedText + PdfTextExtractor.getTextFromPage(reader, i + 1).trim() + "\n"; //Extracting the content from the different pages
                    }
                    System.out.println(parsedText);
                    reader.close();
                    String lines[] = parsedText.split("\\r?\\n");
                    String finalPrintData ="";
                    for(int i=0;i<lines.length;i++){
                        finalPrintData =  finalPrintData.concat("    ".concat(lines[i]).concat("\n"));
                    }
                    final String test = finalPrintData.concat("\r\n");
                    Log.d("Data printing", test);
                    testBytes = test.getBytes();
                    Toast.makeText(AnotherPrinterActivity.this, "File sent to printer", Toast.LENGTH_SHORT).show();

                    if (mInterface == null) {
                        Toast.makeText(AnotherPrinterActivity.this, "INTERFACE IS NULL", Toast.LENGTH_SHORT).show();
                    } else if (mConnection == null) {
                        Toast.makeText(AnotherPrinterActivity.this, "CONNECTION IS NULL", Toast.LENGTH_SHORT).show();
                    } else if (forceCLaim == null) {
                        Toast.makeText(AnotherPrinterActivity.this, "FORCE CLAIM IS NULL", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AnotherPrinterActivity.this, "Sending data", Toast.LENGTH_SHORT).show();
                        mConnection.claimInterface(mInterface, forceCLaim);

                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                byte[] cut_paper = {0x1D, 0x56, 0x41, 0x10};
                                mConnection.bulkTransfer(mEndPoint, testBytes, testBytes.length, 0);
                                mConnection.bulkTransfer(mEndPoint, cut_paper, cut_paper.length, 0);
                            }
                        });
                        thread.run();
                    }
                } catch (Exception e) {
                    System.out.println(e);
                }

            }
        }

    }

    final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {

                            //call method to set up device communication
                            mInterface = device.getInterface(0);
                            mEndPoint = mInterface.getEndpoint(0);// 0 IN and  1 OUT to printer.
                            mConnection = mUsbManager.openDevice(device);

                        }
                    } else {
                        Toast.makeText(context, "PERMISSION DENIED FOR THIS DEVICE", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };


    private String translateDeviceClass(int deviceClass) {

        switch (deviceClass) {

            case UsbConstants.USB_CLASS_APP_SPEC:
                return "Application specific USB class";

            case UsbConstants.USB_CLASS_AUDIO:
                return "USB class for audio devices";

            case UsbConstants.USB_CLASS_CDC_DATA:
                return "USB class for CDC devices (communications device class)";

            case UsbConstants.USB_CLASS_COMM:
                return "USB class for communication devices";

            case UsbConstants.USB_CLASS_CONTENT_SEC:
                return "USB class for content security devices";

            case UsbConstants.USB_CLASS_CSCID:
                return "USB class for content smart card devices";

            case UsbConstants.USB_CLASS_HID:
                return "USB class for human interface devices (for example, mice and keyboards)";

            case UsbConstants.USB_CLASS_HUB:
                return "USB class for USB hubs";

            case UsbConstants.USB_CLASS_MASS_STORAGE:
                return "USB class for mass storage devices";

            case UsbConstants.USB_CLASS_MISC:
                return "USB class for wireless miscellaneous devices";

            case UsbConstants.USB_CLASS_PER_INTERFACE:
                return "USB class indicating that the class is determined on a per-interface basis";

            case UsbConstants.USB_CLASS_PHYSICA:
                return "USB class for physical devices";

            case UsbConstants.USB_CLASS_PRINTER:
                return "USB class for printers";

            case UsbConstants.USB_CLASS_STILL_IMAGE:
                return "USB class for still image devices (digital cameras)";

            case UsbConstants.USB_CLASS_VENDOR_SPEC:
                return "Vendor specific USB class";

            case UsbConstants.USB_CLASS_VIDEO:
                return "USB class for video devices";

            case UsbConstants.USB_CLASS_WIRELESS_CONTROLLER:
                return "USB class for wireless controller devices";

            default:
                return "Unknown USB class!";
        }
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE);

        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {

        ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, 1);

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    startService();

                }


                break;
        }
    }

    private void startService() {
        Intent intent = new Intent(this, FileObserverService.class);
        intent.putExtra("file", "download");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.startForegroundService(intent);
        } else {
            this.startService(intent);
        }
    }

    public void stopService() {
        Intent serviceIntent = new Intent(this, FileObserverService.class);
        stopService(serviceIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(wifiLevelReceiver);
        stopService();

    }

}
