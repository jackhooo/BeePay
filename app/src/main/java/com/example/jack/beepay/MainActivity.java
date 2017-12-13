package com.example.jack.beepay;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;

import javax.crypto.Cipher;

import static javax.crypto.Cipher.ENCRYPT_MODE;

public class MainActivity extends AppCompatActivity implements ProfileFragment.OnFragmentInteractionListener, HistoryFragment.OnFragmentInteractionListener {

    private final static String TAG = MainActivity.class.getSimpleName();

    private static final int MY_PERMISSION_RESPONSE = 42;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private ArrayList<BluetoothDevice> mBluetoothDevices = new ArrayList<BluetoothDevice>();
    private ArrayList<String> deviceName;
    private ArrayList<Integer> IdArray;
    private ArrayList<String> deviceScanRec;
    private ArrayList<String> devicesMessage;
    private ArrayList<String> adItem;
    private ArrayList<String> ConfirmadItem;

    private ArrayList<String> payment;
    private ArrayList<Integer> paymentId;
    private ArrayList<String> paymentMessage;
    private ListView paymentList;
    private ListAdapter paymentListAdapter;

    private ArrayList<String> confirm;
    private ArrayList<Integer> confirmId;
    private ArrayList<String> confirmMessage;
    private ListView confirmList;
    private ListAdapter confirmListAdapter;

    private ArrayList<String> dataToUploadList;

    private Device[] devices;
    private Device[] confirmDevices;

    private ListView scanList;
    private ListAdapter listAdapter;

    private Handler mHandler; //該Handler用來搜尋Devices10秒後，自動停止搜尋

    //加密法
    private static String ALGORITHM = "RSA/ECB/NOPadding";
    private PublicKey publicKey1;
    private PrivateKey privateKey1;
    private PublicKey publicKey2;
    private PrivateKey privateKey2;

    private int mScanningMode = 3;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int SCAN_TIME = 60000;
    private static final int STOP_TIME = 500;

    Intent ShopModeServiceIntent = null;
    Intent RecieptModeServiceIntent = null;
    Intent RecieptModeServiceIntent2 = null;

    Intent ConnectServer = null;

    private ViewFlipper mViewFlipper;


    Intent WitnessServiceIntent = null;
    Intent WitnessServiceIntent2 = null;

    private ArrayList<String> witnessList;

    private int witnessTime = 0;
    private int witnessCount = 0;

    private int[] witnessNum;

    private int myId;
    private int otherId;

    private String othersPriv2 = "";
    private String othersPub1 = "";
    private String keymessage = "";

    UploadPackage upload;

    private ArrayList<String> witnessGetList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setSelectedItemId(R.id.navigation_nearby);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        if (!getPackageManager().hasSystemFeature(getPackageManager().FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(getBaseContext(), R.string.No_sup_ble, Toast.LENGTH_SHORT).show();
            finish();
        }//利用getPackageManager().hasSystemFeature()檢查手機是否支援BLE設備，否則利用finish()關閉程式。

        //試著取得BluetoothAdapter，如果BluetoothAdapter==null，則該手機不支援Bluetooth
        //取得Adapter之前，需先使用BluetoothManager，此為系統層級需使用getSystemService
        mBluetoothManager = (BluetoothManager) this.getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(getBaseContext(), R.string.No_sup_Bluetooth, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }//如果==null，利用finish()取消程式。

        //存取scan的結果
        deviceScanRec = new ArrayList<String>();
        deviceName = new ArrayList<String>();   //此ArrayList屬性為String，用來裝Devices Name
        devicesMessage = new ArrayList<>();
        adItem = new ArrayList<String>();
        ConfirmadItem = new ArrayList<String>();
        IdArray = new ArrayList<Integer>();

        //商家的paymentlist
        payment = new ArrayList<String>();
        paymentId = new ArrayList<Integer>();

        paymentMessage = new ArrayList<String>();
        paymentList = (ListView) findViewById(R.id.payList);
        paymentListAdapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_expandable_list_item_1, payment);//ListView使用的Adapter，
        paymentList.setAdapter(paymentListAdapter);//將listView綁上Adapter
        paymentList.setOnItemClickListener(new onPaymentClickListener());

        //商家的confirmlist
        confirm = new ArrayList<String>();
        confirmId = new ArrayList<Integer>();
        confirmMessage = new ArrayList<String>();

        confirmList = (ListView) findViewById(R.id.confirmList);
        confirmListAdapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_expandable_list_item_1, confirm);//ListView使用的Adapter，
        confirmList.setAdapter(confirmListAdapter);//將listView綁上Adapter
        confirmList.setOnItemClickListener(new onConfirmClickListener());

        dataToUploadList = new ArrayList<String>();

        //初始化Device
        devices = new Device[1000];
        for (int i = 0; i < 1000; i += 1) {
            devices[i] = new Device();
        }

        confirmDevices = new Device[1000];
        for (int i = 0; i < 1000; i += 1) {
            confirmDevices[i] = new Device();
        }

//        witnessNum = new int[10000000];
//        for (int i = 0; i < 10000000; i += 1) {
//            witnessNum[i] = 0;
//        }

        scanList = (ListView) findViewById(R.id.scanlistID);
        listAdapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_expandable_list_item_1, deviceName);//ListView使用的Adapter，
        scanList.setAdapter(listAdapter);//將listView綁上Adapter
        scanList.setOnItemClickListener(new onItemClickListener()); //綁上OnItemClickListener，設定ListView點擊觸發事件

        mHandler = new Handler();

        // Prompt for permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("BleActivity", "Location access not granted!");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSION_RESPONSE);
        }

        ShopModeServiceIntent = new Intent(MainActivity.this, AdvertiserService.class);

        RecieptModeServiceIntent = new Intent(MainActivity.this, AdvertiserTwoService.class);
        RecieptModeServiceIntent2 = new Intent(MainActivity.this, AdvertiserThreeService.class);

        //用於連接
        ConnectServer = new Intent(MainActivity.this, ServerService.class);

        mViewFlipper = (ViewFlipper) this.findViewById(R.id.view_flipper);

        SharedPreferences spref = getSharedPreferences("dada", Context.MODE_PRIVATE);
        myId = Integer.parseInt(spref.getString("id", "0"));

        otherId = 0;

        WitnessServiceIntent = new Intent(MainActivity.this, WitnessAdvertiserService.class);
        WitnessServiceIntent2 = new Intent(MainActivity.this, WitnessAdvertiserServiceTwo.class);

        witnessList = new ArrayList<String>();

        witnessGetList = new ArrayList<String>();

        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("my-event"));

        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(mPubMessageReceiver,
                new IntentFilter("event"));

        try {
            KeyPair loadedKeyPair1 = LoadKeyPair1("RSA");
            publicKey1 = loadedKeyPair1.getPublic();
            privateKey1 = loadedKeyPair1.getPrivate();
            KeyPair loadedKeyPair2 = LoadKeyPair2("RSA");
            publicKey2 = loadedKeyPair2.getPublic();
            privateKey2 = loadedKeyPair2.getPrivate();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    // handler for received Intents for the "my-event" event
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.getStringExtra("key") != null){
                Log.i("GetKey", "priv" + intent.getStringExtra("key"));
                //othersPriv2 += intent.getStringExtra("key");
                if(othersPriv2.length() > 220){
                    Log.i("GetKey", "priv" + othersPriv2);
                }
                othersPriv2 = "";
            }
        }
    };

    // handler for received Intents for the "my-event" event
    private BroadcastReceiver mPubMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.getStringExtra("pub") != null){
                Log.i("GetKey", "pub" + intent.getStringExtra("pub"));
                othersPub1 += intent.getStringExtra("pub");
                if(othersPub1.length() == 88){
                    Log.i("GetKey","pub" + othersPub1);
                }
                othersPub1 = "";
            }
        }
    };


    //此為ScanFunction，輸入函數為boolean，如果true則開始搜尋，false則停止搜尋
    private void ScanFunction(boolean enable) {
        if (enable) {

            mHandler.postDelayed(new Runnable() { //啟動一個Handler，並使用postDelayed在10秒後自動執行此Runnable()
                @Override
                public void run() {
                    if (mScanningMode != 3) {
                        ScanFunction(false);
                        Log.d(TAG, "ScanFunction():Stop Scan");
                    }
                }
            }, SCAN_TIME); //SCAN_TIME為 1分鐘 後要執行此Runnable

            mScanningMode = 1; //搜尋旗標設為true
            mBluetoothAdapter.startLeScan(mLeScanCallback);//開始搜尋BLE設備
            //textView.setText("Scanning");
            Log.d(TAG, "Start Scan");

        } else {

            mHandler.postDelayed(new Runnable() { //啟動一個Handler，並使用postDelayed在10秒後自動執行此Runnable()
                @Override
                public void run() {
                    if (mScanningMode != 3) {
                        ScanFunction(true);
                        Log.d(TAG, "ScanFunction():Start Scan");
                    }
                }
            }, STOP_TIME); //STOP_TIME為 0.5秒 後要執行此Runnable

            mScanningMode = 2;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            //textView.setText("Stop Scan");
        }
    }

    //注意，在此enable==true中的Runnable是在10秒後才會執行，因此是先startLeScan，10秒後才會執行Runnable內的stopLeScan
//在BLE Devices Scan中，使用的方法為startLeScan()與stopLeScan()，兩個方法都需填入callback，當搜尋到設備時，都會跳到
//callback的方法中

    //建立一個BLAdapter的Callback，當使用startLeScan或stopLeScan時，每搜尋到一次設備都會跳到此callback
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {

            runOnUiThread(new Runnable() {
                //使用runOnUiThread方法，其功能等同於WorkThread透過Handler將資訊傳到MainThread(UiThread)中，
                //詳細可進到runOnUiThread中觀察
                @Override
                public void run() {
                    if (!mBluetoothDevices.contains(device)) {//利用contains判斷是否有搜尋到重複的device
                        //mBluetoothDevices.add(device);//如沒重複則添加到bluetoothDevices中

                        int manufacturerIDStart = 5;

                        if (device.getName() != null) {
                            manufacturerIDStart += device.getName().length() + 2;
                        }

                        String scanMessage = convertHexToString(bytesToHexString(scanRecord));
                        String manufacturerID = scanMessage.substring(manufacturerIDStart, manufacturerIDStart + 2);
                        int messageStart = manufacturerIDStart + 2;
                        String manufacturerMessage = scanMessage.substring(messageStart);

                        Log.i(TAG, manufacturerID );

                        //店家名稱廣播
                        if (manufacturerID.equals("CC")) {

                            mBluetoothDevices.add(device);//如沒重複則添加到bluetoothDevices中

                            int hexPackageNumStart = 14;
                            int hexPackageMessageStart = 18;
                            String packageStartMessage = bytesToHexString(scanRecord).substring(hexPackageNumStart, hexPackageNumStart + 4);
                            int packageInt = Integer.parseInt(packageStartMessage, 16);
                            int packageNum = packageInt % 10;
                            int recieveDeviceNum = (packageInt - packageNum) / 10;

                            IdArray.add(recieveDeviceNum);

                            Log.i("DeviceNum", Integer.toString(recieveDeviceNum));

                            manufacturerID += Integer.toString(recieveDeviceNum);

                            deviceName.add(convertHexToString(bytesToHexString(scanRecord).substring(hexPackageMessageStart, hexPackageMessageStart + 44)));

                            ((BaseAdapter) listAdapter).notifyDataSetChanged();//使用notifyDataSetChanger()更新listAdapter的內容
                        }
//                        else if (manufacturerID.equals("WW")) {
//                            if( !witnessGetList.contains(device.getAddress()) ) {
//                                witnessGetList.add(device.getAddress());
//                                Log.i(TAG, "收到見證");
//                                //Toast.makeText(getBaseContext(), "見證者", Toast.LENGTH_SHORT).show();
//                            }
//                        }
                        else if (manufacturerID.equals("CS") || manufacturerID.equals("WW")) {

                            int hexPackageNumStart = 14;
                            int hexPackageMessageStart = 18;
                            //String packageStartMessage = bytesToHexString(scanRecord).substring(hexPackageNumStart, hexPackageNumStart + 4);

                            //int packageInt = Integer.parseInt(packageStartMessage, 16);
                            int packageNum = 0;

                            //int recieveDeviceNum = (packageInt - packageNum) / 10;
                            //manufacturerID += Integer.toString(recieveDeviceNum);

                            String data = bytesToHexString(scanRecord).substring(hexPackageMessageStart, hexPackageMessageStart + 40);

                            String payeeIdHex = data.substring(0,6);
                            int payeeId = Integer.parseInt(payeeIdHex, 16);

                            String sellerIdHex = data.substring(6,12);
                            int sellerId = Integer.parseInt(sellerIdHex, 16);

                            String paymentTimeHex = data.substring(12,22);
                            long paymentTime = Long.parseLong(paymentTimeHex,16);

                            String whichData = data.substring(22,24);
                            int whichDataNum = Integer.parseInt(whichData, 16);

                            String oneOrTwo = data.substring(24,26);
                            packageNum = Integer.parseInt(oneOrTwo, 16);

                            String encodeData = data.substring(26);

                            Log.i("DATaaa",data);

                            if(!dataToUploadList.contains(data)){
                                dataToUploadList.add(data);
                                store_package_number(data);
                            }

                            if( manufacturerID.equals("WW") && !witnessGetList.contains(device.getAddress()) ) {
                                witnessGetList.add(device.getAddress());
                                Log.i("Witness", "收到見證" + data);
                                //witnessNum[(int)paymentTime - 562500000] += 1;
                                //Toast.makeText(getBaseContext(), "見證者", Toast.LENGTH_SHORT).show();
                            }

                            if ( sellerId == myId && whichDataNum == 1 && !payment.contains("送出收據" + Integer.toString(payeeId) + " " + Long.toString(paymentTime))){

                                if (packageNum == 1) {
                                    devices[payeeId].hexMessage1 = encodeData;

                                    if (devices[payeeId].checkIfAllMessageReceive()) {
                                        devices[payeeId].setEncodedHex();
                                        try {
                                            byte[] decode1 = rsaDecode(hexStringToByteArray(devices[payeeId].encodedHex), privateKey2);
                                            byte[] decode2 = rsaDecode(decode1, privateKey1);

                                            if ( !adItem.contains(bytesToHexString(decode2))) {
                                                adItem.add(bytesToHexString(decode2));
                                                if( checkMessage(bytesToHexString(decode2)) ) {
                                                    paymentId.add(payeeId);
                                                    paymentMessage.add(bytesToHexString(decode2));
                                                    payment.add("送出收據" + Integer.toString(payeeId) + " " + Long.toString(paymentTime));
                                                    Log.i("Payment", bytesToHexString(decode2));
                                                }
                                            }

                                        } catch (UnsupportedEncodingException e) {
                                            e.printStackTrace();
                                        }
                                        devices[payeeId].cleanMessage();
                                    }
                                } else if (packageNum == 2) {
                                    devices[payeeId].hexMessage2 = encodeData;

                                    if (devices[payeeId].checkIfAllMessageReceive()) {
                                        devices[payeeId].setEncodedHex();
                                        try {
                                            byte[] decode1 = rsaDecode(hexStringToByteArray(devices[payeeId].encodedHex), privateKey2);
                                            byte[] decode2 = rsaDecode(decode1, privateKey1);

                                            if ( !adItem.contains(bytesToHexString(decode2))) {
                                                adItem.add(bytesToHexString(decode2));
                                                if( checkMessage(bytesToHexString(decode2)) ) {
                                                    paymentId.add(payeeId);
                                                    paymentMessage.add(bytesToHexString(decode2));
                                                    payment.add("送出收據" + Integer.toString(payeeId) + " " + Long.toString(paymentTime));
                                                    Log.i("Payment", bytesToHexString(decode2));
                                                }
                                            }

                                        } catch (UnsupportedEncodingException e) {
                                            e.printStackTrace();
                                        }
                                        devices[payeeId].cleanMessage();
                                    }
                                }
                            }else  if ( sellerId == myId && whichDataNum == 3 && !confirm.contains("確認交易" + Integer.toString(payeeId) + " " + Long.toString(paymentTime))){

                                if (packageNum == 1) {
                                    confirmDevices[payeeId].hexMessage1 = encodeData;

                                    if (confirmDevices[payeeId].checkIfAllMessageReceive()) {
                                        confirmDevices[payeeId].setEncodedHex();
                                        try {
                                            byte[] decode1 = rsaDecode(hexStringToByteArray(confirmDevices[payeeId].encodedHex), privateKey2);
                                            byte[] decode2 = rsaDecode(decode1, privateKey1);

                                            if ( !ConfirmadItem.contains(bytesToHexString(decode2))) {
                                                ConfirmadItem.add(bytesToHexString(decode2));
                                                if( checkMessage(bytesToHexString(decode2)) ) {
                                                    stopService(RecieptModeServiceIntent);
                                                    stopService(RecieptModeServiceIntent2);
                                                    confirmId.add(payeeId);
                                                    confirmMessage.add(bytesToHexString(decode2));
                                                    confirm.add("確認交易" + Integer.toString(payeeId) + " " + Long.toString(paymentTime));
                                                    Log.i("Confirm", bytesToHexString(decode2));
                                                    String show =  "確認：" + bytesToHexString(decode2);
                                                    Toast.makeText(getBaseContext(), show, Toast.LENGTH_LONG).show();
                                                }
                                            }

                                        } catch (UnsupportedEncodingException e) {
                                            e.printStackTrace();
                                        }
                                        confirmDevices[payeeId].cleanMessage();
                                    }
                                } else if (packageNum == 2) {
                                    confirmDevices[payeeId].hexMessage2 = encodeData;

                                    if (confirmDevices[payeeId].checkIfAllMessageReceive()) {
                                        confirmDevices[payeeId].setEncodedHex();
                                        try {
                                            byte[] decode1 = rsaDecode(hexStringToByteArray(confirmDevices[payeeId].encodedHex), privateKey2);
                                            byte[] decode2 = rsaDecode(decode1, privateKey1);

                                            if ( !ConfirmadItem.contains(bytesToHexString(decode2))) {
                                                ConfirmadItem.add(bytesToHexString(decode2));
                                                if( checkMessage(bytesToHexString(decode2)) ) {
                                                    stopService(RecieptModeServiceIntent);
                                                    stopService(RecieptModeServiceIntent2);
                                                    confirmId.add(payeeId);
                                                    confirmMessage.add(bytesToHexString(decode2));
                                                    confirm.add("確認交易" + Integer.toString(payeeId) + " " + Long.toString(paymentTime));
                                                    Log.i("Confirm", bytesToHexString(decode2));
                                                    String show =  "確認：" + bytesToHexString(decode2);
                                                    Toast.makeText(getBaseContext(), show, Toast.LENGTH_LONG).show();
                                                }
                                            }

                                        } catch (UnsupportedEncodingException e) {
                                            e.printStackTrace();
                                        }
                                        confirmDevices[payeeId].cleanMessage();
                                    }
                                }
                            }

                            if( !witnessList.contains(data) ) {
                                if(payeeId != myId && sellerId != myId) {
                                    if (packageNum == 1) {
                                        Log.i(TAG, "見證");
                                        witnessList.add(data);
                                        witnessTime += 1;
                                        stopService(WitnessServiceIntent);
                                        WitnessServiceIntent = new Intent(MainActivity.this, WitnessAdvertiserService.class);
                                        WitnessServiceIntent.putExtra(WitnessAdvertiserService.INPUT, data);
                                        WitnessServiceIntent.putExtra(WitnessAdvertiserService.DEVICE_NUM, myId);
                                        startService(WitnessServiceIntent);
                                    } else if (packageNum == 2) {
                                        Log.i(TAG, "見證");
                                        witnessList.add(data);
                                        witnessTime += 1;
                                        stopService(WitnessServiceIntent2);
                                        WitnessServiceIntent2 = new Intent(MainActivity.this, WitnessAdvertiserServiceTwo.class);
                                        WitnessServiceIntent2.putExtra(WitnessAdvertiserServiceTwo.INPUT, data);
                                        WitnessServiceIntent2.putExtra(WitnessAdvertiserServiceTwo.DEVICE_NUM, myId);
                                        startService(WitnessServiceIntent2);
                                    }
                                }
                            }
                        }


                        deviceScanRec.add(bytesToHexString(scanRecord));
                        devicesMessage.add(manufacturerID + "  " + manufacturerMessage);
                        //deviceName.add(manufacturerID + " rssi:" + rssi + "\r\n" + device.getAddress()); //將device的Name、rssi、address裝到此ArrayList<String>中

                        ((BaseAdapter) confirmListAdapter).notifyDataSetChanged();//使用notifyDataSetChanger()更新listAdapter的內容
                        ((BaseAdapter) paymentListAdapter).notifyDataSetChanged();//使用notifyDataSetChanger()更新listAdapter的內容
                        ((BaseAdapter) listAdapter).notifyDataSetChanged();//使用notifyDataSetChanger()更新listAdapter的內容
                    }
                }
            });
        }
    };

    private boolean checkMessage(String data){

        Boolean result = false;

        String payeeIdHex = data.substring(0,6);
        int payeeId = Integer.parseInt(payeeIdHex, 16);

        String sellerIdHex = data.substring(6,12);
        int sellerId = Integer.parseInt(sellerIdHex, 16);

        String paymentTimeHex = data.substring(12,22);
        long paymentTime = Long.parseLong(paymentTimeHex,16);

        String money = data.substring(22);
        int moneyNum = Integer.parseInt(money, 16);

        if ( sellerId == myId ){
            result = true;
        }

        return  result;
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        Fragment nowFrag = null;

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_nearby:

                    if (nowFrag != null) {
                        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                        ft.remove(nowFrag);
                        ft.commit();
                    }
                    return true;

                case R.id.navigation_profile:
                    Fragment frag2 = ProfileFragment.newInstance("個人資料", Integer.toString(witnessTime));
                    if (frag2 != null) {
                        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                        ft.replace(R.id.container, frag2, frag2.getTag());
                        nowFrag = frag2;
                        ft.commit();
                    }
                    return true;

                case R.id.navigation_transactionRecord:
                    Fragment frag3 = HistoryFragment.newInstance("交易資料", "");
                    Bundle bundle = new Bundle();
                    bundle.putStringArrayList(HistoryFragment.AD_LIST, confirm);
                    frag3.setArguments(bundle);

                    if (frag3 != null) {
                        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                        ft.replace(R.id.container, frag3, frag3.getTag());
                        nowFrag = frag3;
                        ft.commit();
                    }
                    return true;
            }
            return false;
        }
    };

    private class onConfirmClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        }
    }

    private class onPaymentClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            String data = paymentMessage.get(position);

            String payeeIdHex = data.substring(0,6);
            int payeeId = Integer.parseInt(payeeIdHex, 16);

            String sellerIdHex = data.substring(6,12);
            int sellerId = Integer.parseInt(sellerIdHex, 16);

            String paymentTimeHex = data.substring(12,22);
            long paymentTime = Long.parseLong(paymentTimeHex,16);

            String money = data.substring(22);
            int moneyNum = Integer.parseInt(money, 16);

            //Show witness
            //Toast.makeText(getBaseContext(), Integer.toString(witnessNum[(int)paymentTime - 562500000]), Toast.LENGTH_SHORT).show();

            String notEncodeData1 = fillInt(paymentId.get(position), 6) + fillInt(myId, 6) + fillLong(paymentTime,10) + fillInt(2,2) + fillInt(1,2);
            String notEncodeData2 = fillInt(paymentId.get(position), 6) + fillInt(myId, 6) + fillLong(paymentTime,10) + fillInt(2,2) + fillInt(2,2);

            final byte[] encodeData1 = rsaEncode(hexStringToByteArray(data),publicKey1);
            final byte[] encodeData2 = rsaEncode(encodeData1,publicKey2);

            String ADData1 = notEncodeData1 + bytesToHexString(encodeData2).substring(0,14);

            String ADData2 = notEncodeData2 + bytesToHexString(encodeData2).substring(14);

            RecieptModeServiceIntent.putExtra(AdvertiserService.INPUT, ADData1);
            RecieptModeServiceIntent.putExtra(AdvertiserService.DEVICE_NUM, myId);
            startService(RecieptModeServiceIntent);

            RecieptModeServiceIntent2.putExtra(AdvertiserService.INPUT, ADData2);
            RecieptModeServiceIntent2.putExtra(AdvertiserService.DEVICE_NUM, myId);
            startService(RecieptModeServiceIntent2);

            String show =  "收據明碼：" + notEncodeData1 + "\n" + "收據加密：" + bytesToHexString(encodeData2);
            Toast.makeText(getBaseContext(), show, Toast.LENGTH_LONG).show();

        }
    }

    //以下為ListView ItemClick的Listener，當按下Item時，將該Item的BLE Name與Address包起來，將送到另一
    //Activity中建立連線
    private class onItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            //mBluetoothDevices為一個陣列資料ArrayList<BluetoothDevices>，使用.get(position)取得
            //Item位置上的BluetoothDevice
            final BluetoothDevice mBluetoothDevice = mBluetoothDevices.get(position);

            //建立一個Intent，將從此Activity進到ControlActivity中
            //在ControlActivity中將與BLE Device連線，並互相溝通
            Intent goControlIntent = new Intent(MainActivity.this, BuyActivity.class);

            //將device Name與address存到ControlActivity的DEVICE_NAME與ADDRESS，以供ControlActivity使用

//            if (mBluetoothDevice.getName() == null) {
//                goControlIntent.putExtra(BuyActivity.DEVICE_NAME, mBluetoothDevice.getName());
//            } else {
//                goControlIntent.putExtra(BuyActivity.DEVICE_NAME, mBluetoothDevice.getName() + "  Hex: " + asciiToHex(mBluetoothDevice.getName()));
//            }

            goControlIntent.putExtra(BuyActivity.DEVICE_NAME, deviceName.get(position));
            goControlIntent.putExtra(BuyActivity.DEVICE_ADDRESS, mBluetoothDevice.getAddress());
            goControlIntent.putExtra(BuyActivity.DEVICE_REC, deviceScanRec.get(position));
            goControlIntent.putExtra(BuyActivity.DEVICE_MESSAGE, devicesMessage.get(position));
            goControlIntent.putExtra(BuyActivity.DEVICE_NUM, IdArray.get(position));

            if (mScanningMode == 1) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mScanningMode = 3;
            }

            startActivity(goControlIntent);
        }
    }

    public void logoutClick(View view) {
        SharedPreferences spref = getSharedPreferences(
                "dada", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = spref.edit();
        editor.clear();
        editor.commit();
        Intent goMainIntent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(goMainIntent);
    }

    public void uploadPackage(View view){

        SharedPreferences spref = getSharedPreferences("dada", Context.MODE_PRIVATE);

        Log.i("pack", ""+spref.getInt("countpackage",0));
        Log.i("pack", spref.getString("countid",null));

        Log.i("pack", spref.getString("0",null));
        Log.i("pack", spref.getString("1",null));

        upload_package();
    }

    //按下店家模式button
    public void shopBtnClick(View v) throws UnsupportedEncodingException {

        SharedPreferences spref = getSharedPreferences("dada", Context.MODE_PRIVATE);
        String myEmail = spref.getString("email",null);

        String encodeDataToHex = bytesToHexString(myEmail.getBytes("utf-8"));

        switch (v.getId()) {
            case R.id.shopBtn:

                if (mViewFlipper.getCurrentView().getId() == R.id.user) {
                    mViewFlipper.showNext();
                }

                //Toast.makeText(getBaseContext(), convertHexToString(encodeDataToHex), Toast.LENGTH_SHORT).show();
                ShopModeServiceIntent.putExtra(AdvertiserService.INPUT, encodeDataToHex);
                ShopModeServiceIntent.putExtra(AdvertiserService.DEVICE_NUM, myId);
                startService(ShopModeServiceIntent);
                //For connect
                startService(ConnectServer);

                break;
            case R.id.userBtn:

                if (mViewFlipper.getCurrentView().getId() == R.id.shop) {
                    mViewFlipper.showPrevious();
                }

                stopService(RecieptModeServiceIntent);
                stopService(RecieptModeServiceIntent2);
                //stopService(ShopModeServiceIntent);
                stopService(WitnessServiceIntent);
                stopService(WitnessServiceIntent2);

                stopService(ShopModeServiceIntent);
                ShopModeServiceIntent = new Intent(MainActivity.this, AdvertiserService.class);
                //For connect
                stopService(ConnectServer);
                ConnectServer = new Intent(MainActivity.this, ServerService.class);
                //For connect
                //startService(ConnectServer);

                break;
        }
    }

    //需要注意的是，需加入一個stopLeScan在onPause()中，當按返回鍵或關閉程式時，需停止搜尋BLE
    //否則下次開啟程式時會影響到搜尋BLE device
    @Override
    protected void onPause() {
        super.onPause();

        Log.d(TAG, "onPause():Stop Scan");
        //mScanningMode = 3;
        //mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

        stopService(RecieptModeServiceIntent);
        stopService(RecieptModeServiceIntent2);
        stopService(ShopModeServiceIntent);
        stopService(WitnessServiceIntent);
        stopService(WitnessServiceIntent2);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //一般來說，只要使用到mBluetoothAdapter.isEnabled()就可以將BL開啟了，但此部分添加一個Result Intent
        //跳出詢問視窗是否開啟BL，因此該Intent為BluetoothAdapter.ACTION.REQUEST_ENABLE
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT); //再利用startActivityForResult啟動該Intent
        }

        paymentListAdapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_expandable_list_item_1, payment);//ListView使用的Adapter，
        paymentList.setAdapter(paymentListAdapter);//將listView綁上Adapter

        listAdapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_expandable_list_item_1, deviceName);//ListView使用的Adapter，
        scanList.setAdapter(listAdapter);//將listView綁上Adapter

        ScanFunction(true); //使用ScanFunction(true) 開啟BLE搜尋功能，該Function在下面部分
    }

    //這個Override Function是因為在onResume中使用了ActivityForResult，當使用者按了取消或確定鍵時，結果會
    //返回到此onActivityResult中，在判別requestCode判別是否==RESULT_CANCELED，如果是則finish()程式
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (REQUEST_ENABLE_BT == 1 && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void store_package_number(String Package){
        //這裡的packge需為明碼和加密一起傳進來,共20bytes;
        SharedPreferences spref = getSharedPreferences(
                "dada", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = spref.edit();
        editor.putString(spref.getString("countid",null),Package);
        int temp=spref.getInt("countpackage",0)+1;
        editor.commit();
        editor.putString("countid",Integer.toString(temp));
        editor.putInt("countpackage",temp);
        editor.commit();
    }

    public void upload_package(){
        SharedPreferences spref = getSharedPreferences(
                "dada", Context.MODE_PRIVATE);
        upload=new UploadPackage();
        for(int i=spref.getInt("whichload",0);i<spref.getInt("countpackage",0);i++){
            String temp=Integer.toString(i);
            String temp_package=spref.getString(temp,null);
            String cut_tran_data=temp_package.substring(26,40);
            String cut_open_data=temp_package.substring(0,26);
            String upload_id=spref.getString("id",null);
            String user_id=spref.getString("id",null);
            int usrid=Integer.parseInt(user_id,16);
            user_id=fillInt(usrid,6);
            Log.i("pack", user_id);
            Log.i("pack", cut_open_data);
            Log.i("pack", cut_tran_data);
            upload.upload("http://140.119.163.23:8080/BLE_Transaction/services/TransactionApi?transactiondata="+cut_tran_data+"&opendata="+cut_open_data+"&echonum=1&uploader="+user_id);

        }
        SharedPreferences.Editor editor = spref.edit();
        editor.putInt("whichload",spref.getInt("countpackage",0));
        editor.commit();

    }

    public String fillInt(int uncode, int number){
        String encode = null;
        encode = Integer.toHexString(uncode);
        while (encode.length() < number){
            encode = "0" + encode;
        }
        return encode;
    }

    public String fillLong(long uncode, int number){
        String encode = null;
        encode = Long.toHexString(uncode);
        while (encode.length() < number){
            encode = "0" + encode;
        }
        return encode;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    public KeyPair LoadKeyPair1(String algorithm) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

//        InputStream publicFis = getAssets().open("public.key");
//        int publicSize = publicFis.available();
//        byte[] encodedPublicKey = new byte[publicSize];
//        publicFis.read(encodedPublicKey);
//        publicFis.close();
//
//        InputStream privateFis = getAssets().open("private.key");
//        int privateSize = privateFis.available();
//        byte[] encodedPrivateKey = new byte[privateSize];
//        privateFis.read(encodedPrivateKey);
//        privateFis.close();

        String publicString = "302a300d06092a864886f70d01010105000319003016020f00a461c31e6f8aa88b58c0e6d353730203010001";
        byte[] encodedPublicKey=hexStringToByteArray(publicString);

        String privateString="306f020100300d06092a864886f70d0101010500045b3059020100020f00a461c31e6f8aa88b58c0e6d353730203010001020e411c22037faddd323462157d3b1d020800e3aa174ea7cde5020800b8d7584a0e4677020749ea8992b20cb902073a5c23901e39b9020800b8f10b432910e6";
        byte[] encodedPrivateKey=hexStringToByteArray(privateString);

        // Generate KeyPair.
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedPublicKey);
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

        return new KeyPair(publicKey, privateKey);
    }

    public KeyPair LoadKeyPair2(String algorithm) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

//        InputStream publicFis = getAssets().open("public2.key");
//        int publicSize = publicFis.available();
//        byte[] encodedPublicKey = new byte[publicSize];
//        publicFis.read(encodedPublicKey);
//        publicFis.close();
//
//        InputStream privateFis = getAssets().open("private2.key");
//        int privateSize = privateFis.available();
//        byte[] encodedPrivateKey = new byte[privateSize];
//        privateFis.read(encodedPrivateKey);
//        privateFis.close();

        String publicString = "302a300d06092a864886f70d01010105000319003016020f00a461c31e6f8aa88b58c0e6d353730203010001";
        byte[] encodedPublicKey=hexStringToByteArray(publicString);

        String privateString="306f020100300d06092a864886f70d0101010500045b3059020100020f00a461c31e6f8aa88b58c0e6d353730203010001020e411c22037faddd323462157d3b1d020800e3aa174ea7cde5020800b8d7584a0e4677020749ea8992b20cb902073a5c23901e39b9020800b8f10b432910e6";
        byte[] encodedPrivateKey=hexStringToByteArray(privateString);

        // Generate KeyPair.
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedPublicKey);
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

        return new KeyPair(publicKey, privateKey);
    }

    public byte[] rsaEncode(byte[] plainText, PublicKey publicKey) {
        Encryption encryption = new Encryption();
        byte[] encryptedResult = "0".getBytes();
        try {
            encryptedResult = encryption.cryptByRSA(plainText, publicKey, ALGORITHM, ENCRYPT_MODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encryptedResult;
    }

    public byte[] rsaDecode(byte[] result, PrivateKey privateKey) throws UnsupportedEncodingException {
        Encryption encryption = new Encryption();
        byte[] decryptResult = "0".getBytes();
        try {
            decryptResult = encryption.cryptByRSA(result, privateKey, ALGORITHM, Cipher.DECRYPT_MODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decryptResult;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String bytesToHexString(byte[] bytes) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            buffer.append(String.format("%02x", bytes[i]));
        }
        return buffer.toString();
    }

    private static String asciiToHex(String asciiValue) {
        char[] chars = asciiValue.toCharArray();
        StringBuffer hex = new StringBuffer();
        for (int i = 0; i < chars.length; i++) {
            hex.append(Integer.toHexString((int) chars[i]));
        }
        return hex.toString();
    }

    public String convertHexToString(String hex) {

        StringBuilder sb = new StringBuilder();
        StringBuilder temp = new StringBuilder();

        //49204c6f7665204a617661 split into two characters 49, 20, 4c...
        for (int i = 0; i < hex.length() - 1; i += 2) {

            //grab the hex in pairs
            String output = hex.substring(i, (i + 2));
            //convert hex to decimal
            int decimal = Integer.parseInt(output, 16);
            //convert the decimal to character
            sb.append((char) decimal);

            temp.append(decimal);
        }
        System.out.println("Decimal : " + temp.toString());

        return sb.toString();
    }
}
