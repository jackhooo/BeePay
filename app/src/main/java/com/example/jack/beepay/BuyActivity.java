package com.example.jack.beepay;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
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
import java.util.HashMap;
import java.util.List;

import javax.crypto.Cipher;

import static javax.crypto.Cipher.ENCRYPT_MODE;

public class BuyActivity extends AppCompatActivity {

    private final static String TAG = BuyActivity.class.getSimpleName();

    public static final String DEVICE_NAME = "DEVICE_NAME";
    public static final String DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String DEVICE_REC = "DEVICE_REC";
    public static final String DEVICE_MESSAGE = "DEVICE_MESSAGE";

    private String mDeviceName;
    private String mDeviceAddress;
    private String mDeviceRec;
    private String mDeviceMessage;

    private Button payButton;
    private Button confirmButton;

    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";


    Intent PayModeServiceIntent = null;
    Intent ConfirmModeServiceIntent = null;


    //////////////////////////////////// //掃描
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private ArrayList<BluetoothDevice> mBluetoothDevices = new ArrayList<BluetoothDevice>();
    private ArrayList<String> deviceName;
    private ArrayList<String> deviceScanRec;
    private ArrayList<String> devicesMessage;
    private ArrayList<String> adItem;

    private Device[] devices;

    private Handler mHandler; //該Handler用來搜尋Devices10秒後，自動停止搜尋

    //加密法
    private static String ALGORITHM = "RSA/ECB/NOPadding";
    private PublicKey publicKey1;
    private PrivateKey privateKey1;
    private PublicKey publicKey2;
    private PrivateKey privateKey2;

    private int mScanningMode = 3;

    private static final int SCAN_TIME = 60000;
    private static final int STOP_TIME = 500;

    private static final int REQUEST_ENABLE_BT = 1;

    //////////////////////////////////// //掃描

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Burger");  // provide compatibility to all the versions

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(DEVICE_ADDRESS);
        mDeviceRec = intent.getStringExtra(DEVICE_REC);
        mDeviceMessage = intent.getStringExtra(DEVICE_MESSAGE);

        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mGattServicesList.setVisibility(View.INVISIBLE);

        payButton = (Button) findViewById(R.id.PayButtonID);
        payButton.setVisibility(View.INVISIBLE);

        confirmButton = (Button) findViewById(R.id.ConfirmButtonID);
        confirmButton.setVisibility(View.INVISIBLE);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        PayModeServiceIntent = new Intent(BuyActivity.this, AdvertiserTwoService.class);
        ConfirmModeServiceIntent = new Intent(BuyActivity.this, AdvertiserThreeService.class);

        //////////////////////////////////// //掃描


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

        //初始化Device
        devices = new Device[10000];
        for (int i = 0; i < 10000; i += 1) {
            devices[i] = new Device();
        }

        mHandler = new Handler();

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


        //////////////////////////////////// //掃描

    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Intent myIntent = new Intent(getApplicationContext(), MainActivity.class);
        startActivityForResult(myIntent, 0);
        return true;
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Intent myIntent = new Intent(getApplicationContext(), MainActivity.class);
            startActivityForResult(myIntent, 0);
            mBluetoothLeService = null;
        }


    };


    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();

            //Toast.makeText(Main2Activity.this, action , Toast.LENGTH_LONG).show();

            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                //Show pay button
                payButton.setVisibility(View.VISIBLE);
                updateConnectionState(R.string.connected);
                //invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                //Hide pay button
                payButton.setEnabled(false);
                updateConnectionState(R.string.disconnected);
                //invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

                    if (mGattCharacteristics != null) {

                        final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();

                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }

                            //Toast.makeText(Main2Activity.this, Integer.toString(charaProp) , Toast.LENGTH_LONG).show();

                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                        }

                        return true;
                    }
                    return false;
                }
            };

    //按下付款
    public void payBtnClick(View v) throws UnsupportedEncodingException {

//        mHandler.postDelayed(new Runnable() { //啟動一個Handler，並使用postDelayed在10秒後自動執行此Runnable()
//            @Override
//            public void run() {
//                stopService(PayModeServiceIntent);
//                PayModeServiceIntent = new Intent(BuyActivity.this, AdvertiserTwoService.class);
//            }
//        }, 6000); //SCAN_TIME為 1分鐘 後要執行此Runnable


        //Already pay
        payButton.setEnabled(false);

        mBluetoothLeService.disconnect();
        //unregisterReceiver(mGattUpdateReceiver);

        String encodeDataToHex = bytesToHexString("Pay".getBytes("utf-8"));

        //Toast.makeText(getBaseContext(), convertHexToString(encodeDataToHex), Toast.LENGTH_SHORT).show();
        PayModeServiceIntent.putExtra(AdvertiserService.INPUT, encodeDataToHex);
        PayModeServiceIntent.putExtra(AdvertiserService.DEVICE_NUM, 6);
        startService(PayModeServiceIntent);



//        if (mNotifyCharacteristic != null) {
//
//            final BluetoothGattCharacteristic characteristic = mNotifyCharacteristic;
//
//            //Toast.makeText(BuyActivity.this, characteristic.getUuid().toString() , Toast.LENGTH_LONG).show();
//
//            final int charaProp = characteristic.getProperties();
//
//            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
//                // If there is an active notification on a characteristic, clear
//                // it first so it doesn't update the data field on the user interface.
//                if (characteristic != null) {
//                    mBluetoothLeService.setCharacteristicNotification(characteristic, false);
//                }
//
//                mBluetoothLeService.readCharacteristic(characteristic);
//            }
//
//            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
//                mNotifyCharacteristic = characteristic;
//                mBluetoothLeService.setCharacteristicNotification(characteristic, true);
//            }
//
//            //Already pay
//            payButton.setEnabled(false);
//        }

    }

    //按下確認
    public void confirmBtnClick(View v) throws UnsupportedEncodingException {

        String encodeDataToHex = bytesToHexString("Con".getBytes("utf-8"));

        //Toast.makeText(getBaseContext(), convertHexToString(encodeDataToHex), Toast.LENGTH_SHORT).show();
        ConfirmModeServiceIntent.putExtra(AdvertiserService.INPUT, encodeDataToHex);
        ConfirmModeServiceIntent.putExtra(AdvertiserService.DEVICE_NUM, 6);
        startService(ConfirmModeServiceIntent);

        //Already pay
        confirmButton.setEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }

        //一般來說，只要使用到mBluetoothAdapter.isEnabled()就可以將BL開啟了，但此部分添加一個Result Intent
        //跳出詢問視窗是否開啟BL，因此該Intent為BluetoothAdapter.ACTION.REQUEST_ENABLE
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT); //再利用startActivityForResult啟動該Intent
        }

        ScanFunction(true); //使用ScanFunction(true) 開啟BLE搜尋功能，該Function在下面部分
    }

    @Override
    protected void onPause() {
        super.onPause();
        //mBluetoothLeService.disconnect();
        unregisterReceiver(mGattUpdateReceiver);

        stopService(ConfirmModeServiceIntent);
        //ConfirmModeServiceIntent = new Intent(BuyActivity.this, AdvertiserTwoService.class);

        stopService(PayModeServiceIntent);
        //PayModeServiceIntent = new Intent(BuyActivity.this, AdvertiserTwoService.class);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(mServiceConnection);
        mBluetoothLeService = null;

        mScanningMode = 3;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        //mDataField.setText(R.string.no_data);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                //mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            // mDataField.setText(data);
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();

                if ("0000aaaa-0000-1000-8000-00805f9b34fb".equals(uuid)) {
                    mNotifyCharacteristic = gattCharacteristic;
                }

                currentCharaData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }

            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2},
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2}
        );

        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


    //////////////////////////////////// //掃描

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

                        if (manufacturerID.equals("WW")) {

                            //Toast.makeText(getBaseContext(), "見證者", Toast.LENGTH_SHORT).show();

                        } else if (manufacturerID.equals("CS")) {

                            //Toast.makeText(getBaseContext(), "新CS設備", Toast.LENGTH_SHORT).show();
                            mBluetoothDevices.add(device);//如沒重複則添加到bluetoothDevices中

                            int hexPackageNumStart = 14;
                            int hexPackageMessageStart = 18;
                            String packageStartMessage = bytesToHexString(scanRecord).substring(hexPackageNumStart, hexPackageNumStart + 4);

                            int packageInt = Integer.parseInt(packageStartMessage, 16);
                            int packageNum = packageInt % 10;
                            int recieveDeviceNum = (packageInt - packageNum) / 10;

                            manufacturerID += Integer.toString(recieveDeviceNum);

                            String name = convertHexToString(bytesToHexString(scanRecord).substring(hexPackageMessageStart, hexPackageMessageStart + 14));

                            if (name.equals("reciept")){
                                deviceName.add(name);
                                confirmButton.setVisibility(View.VISIBLE);
                            }



                            if (packageNum == 1) {
                                devices[recieveDeviceNum].hexMessage1 = bytesToHexString(scanRecord).substring(hexPackageMessageStart, hexPackageMessageStart + 44);

                                if (devices[recieveDeviceNum].checkIfAllMessageReceive()) {
                                    devices[recieveDeviceNum].setEncodedHex();
                                    try {
                                        byte[] decode1 = rsaDecode(hexStringToByteArray(devices[recieveDeviceNum].encodedHex), privateKey2);
                                        byte[] decode2 = rsaDecode(decode1, privateKey1);
                                        adItem.add(Integer.toString(recieveDeviceNum) + " " + new String(decode2, "UTF-8"));
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                    devices[recieveDeviceNum].cleanMessage();
                                }
                            } else if (packageNum == 2) {
                                devices[recieveDeviceNum].hexMessage2 = bytesToHexString(scanRecord).substring(hexPackageMessageStart, hexPackageMessageStart + 44);

                                if (devices[recieveDeviceNum].checkIfAllMessageReceive()) {
                                    devices[recieveDeviceNum].setEncodedHex();
                                    try {
                                        byte[] decode1 = rsaDecode(hexStringToByteArray(devices[recieveDeviceNum].encodedHex), privateKey2);
                                        byte[] decode2 = rsaDecode(decode1, privateKey1);
                                        adItem.add(Integer.toString(recieveDeviceNum) + " " + new String(decode2, "UTF-8"));
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                    devices[recieveDeviceNum].cleanMessage();
                                }
                            } else if (packageNum == 3) {
                                devices[recieveDeviceNum].hexMessage3 = bytesToHexString(scanRecord).substring(hexPackageMessageStart, hexPackageMessageStart + 40);

                                if (devices[recieveDeviceNum].checkIfAllMessageReceive()) {
                                    devices[recieveDeviceNum].setEncodedHex();
                                    try {
                                        byte[] decode1 = rsaDecode(hexStringToByteArray(devices[recieveDeviceNum].encodedHex), privateKey2);
                                        byte[] decode2 = rsaDecode(decode1, privateKey1);
                                        adItem.add(Integer.toString(recieveDeviceNum) + " " + new String(decode2, "UTF-8"));
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                    devices[recieveDeviceNum].cleanMessage();
                                }
                            }
                        }

                        deviceScanRec.add(bytesToHexString(scanRecord));
                        devicesMessage.add(manufacturerID + "  " + manufacturerMessage);
                        //deviceName.add(manufacturerID + " rssi:" + rssi + "\r\n" + device.getAddress()); //將device的Name、rssi、address裝到此ArrayList<String>中
                    }
                }
            });
        }
    };

    //////////////////////////////////// //掃描

    public KeyPair LoadKeyPair1(String algorithm) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

        InputStream publicFis = getAssets().open("public.key");
        int publicSize = publicFis.available();
        byte[] encodedPublicKey = new byte[publicSize];
        publicFis.read(encodedPublicKey);
        publicFis.close();

        InputStream privateFis = getAssets().open("private.key");
        int privateSize = privateFis.available();
        byte[] encodedPrivateKey = new byte[privateSize];
        privateFis.read(encodedPrivateKey);
        privateFis.close();

        // Generate KeyPair.
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedPublicKey);
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

        return new KeyPair(publicKey, privateKey);
    }

    public KeyPair LoadKeyPair2(String algorithm) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

        InputStream publicFis = getAssets().open("public2.key");
        int publicSize = publicFis.available();
        byte[] encodedPublicKey = new byte[publicSize];
        publicFis.read(encodedPublicKey);
        publicFis.close();

        InputStream privateFis = getAssets().open("private2.key");
        int privateSize = privateFis.available();
        byte[] encodedPrivateKey = new byte[privateSize];
        privateFis.read(encodedPrivateKey);
        privateFis.close();

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
