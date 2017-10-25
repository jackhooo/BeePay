package com.example.jack.beepay;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
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

import javax.crypto.Cipher;

import static javax.crypto.Cipher.ENCRYPT_MODE;

public class MainActivity extends AppCompatActivity implements ProfileFragment.OnFragmentInteractionListener, HistoryFragment.OnFragmentInteractionListener{

    private final static String TAG = MainActivity.class.getSimpleName();

    private ListView scanList;

    private static final int MY_PERMISSION_RESPONSE = 42;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BluetoothDevice> mBluetoothDevices = new ArrayList<BluetoothDevice>();

    private ArrayList<String> deviceName;
    private ArrayList<String> deviceScanRec;
    private ArrayList<String> devicesMessage;
    private ArrayList<String> adItem;
    private Device[] devices;
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

        //初始化Device
        devices = new Device[10000];
        for (int i = 0; i < 10000; i += 1) {
            devices[i] = new Device();
        }
//        deviceNum = random.nextInt(4095 - 0 + 1) + 0;//random.nextInt(max - min + 1) + min

        scanList = (ListView) findViewById(R.id.scanlistID);
        listAdapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_expandable_list_item_1, deviceName);//ListView使用的Adapter，
        scanList.setAdapter(listAdapter);//將listView綁上Adapter
        scanList.setOnItemClickListener(new onItemClickListener()); //綁上OnItemClickListener，設定ListView點擊觸發事件
        mHandler = new Handler();

        // Prompt for permissions
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("BleActivity", "Location access not granted!");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSION_RESPONSE);
        }

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
                        mBluetoothDevices.add(device);//如沒重複則添加到bluetoothDevices中

                        int manufacturerIDStart = 5;

                        if (device.getName() != null) {
                            manufacturerIDStart += device.getName().length() + 2;
                        }

                        String scanMessage = convertHexToString(bytesToHexString(scanRecord));
                        String manufacturerID = scanMessage.substring(manufacturerIDStart, manufacturerIDStart + 2);
                        int messageStart = manufacturerIDStart + 2;
                        String manufacturerMessage = scanMessage.substring(messageStart);

                        if (manufacturerID.equals("CS")) {

                            Toast.makeText(getBaseContext(), "新CS設備", Toast.LENGTH_SHORT).show();

                            int hexPackageNumStart = 14;
                            int hexPackageMessageStart = 18;
                            String packageStartMessage = bytesToHexString(scanRecord).substring(hexPackageNumStart,hexPackageNumStart+4);

                            int packageInt = Integer.parseInt(packageStartMessage,16);
                            int packageNum = packageInt%10;
                            int recieveDeviceNum = (packageInt - packageNum) / 10;

                            manufacturerID += Integer.toString(recieveDeviceNum);

                            // Toast.makeText(getBaseContext(),Integer.toString(recieveDeviceNum), Toast.LENGTH_SHORT).show();

                            if(packageNum == 1){
                                devices[recieveDeviceNum].hexMessage1 = bytesToHexString(scanRecord).substring(hexPackageMessageStart,hexPackageMessageStart+44);

                                if(devices[recieveDeviceNum].checkIfAllMessageReceive()){
                                    devices[recieveDeviceNum].setEncodedHex();
                                    try {
                                        byte[] decode1 = rsaDecode(hexStringToByteArray(devices[recieveDeviceNum].encodedHex),privateKey2);
                                        byte[] decode2 = rsaDecode(decode1,privateKey1);
                                        adItem.add(Integer.toString(recieveDeviceNum) + " " + new String(decode2, "UTF-8"));
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                    devices[recieveDeviceNum].cleanMessage();
                                }
                            }
                            else if(packageNum == 2){
                                devices[recieveDeviceNum].hexMessage2 = bytesToHexString(scanRecord).substring(hexPackageMessageStart,hexPackageMessageStart+44);

                                if(devices[recieveDeviceNum].checkIfAllMessageReceive()){
                                    devices[recieveDeviceNum].setEncodedHex();
                                    try {
                                        byte[] decode1 = rsaDecode(hexStringToByteArray(devices[recieveDeviceNum].encodedHex),privateKey2);
                                        byte[] decode2 = rsaDecode(decode1,privateKey1);
                                        adItem.add(Integer.toString(recieveDeviceNum) + " " + new String(decode2, "UTF-8"));
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                    devices[recieveDeviceNum].cleanMessage();
                                }
                            }
                            else if(packageNum == 3){
                                devices[recieveDeviceNum].hexMessage3 = bytesToHexString(scanRecord).substring(hexPackageMessageStart,hexPackageMessageStart+40);

                                if(devices[recieveDeviceNum].checkIfAllMessageReceive()){
                                    devices[recieveDeviceNum].setEncodedHex();
                                    try {
                                        byte[] decode1 = rsaDecode(hexStringToByteArray(devices[recieveDeviceNum].encodedHex),privateKey2);
                                        byte[] decode2 = rsaDecode(decode1,privateKey1);
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
                        deviceName.add(manufacturerID + " rssi:" + rssi + "\r\n" + device.getAddress()); //將device的Name、rssi、address裝到此ArrayList<String>中

                        ((BaseAdapter) listAdapter).notifyDataSetChanged();//使用notifyDataSetChanger()更新listAdapter的內容
                    }
                }
            });
        }
    };


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
                    Fragment frag2 = ProfileFragment.newInstance("個人資料","");
                    if (frag2 != null) {
                        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                        ft.replace(R.id.container, frag2, frag2.getTag());
                        nowFrag = frag2;
                        ft.commit();
                    }
                    return true;

                case R.id.navigation_transactionRecord:
                    Fragment frag3 = HistoryFragment.newInstance("交易資料","");
                    Bundle bundle=new Bundle();
                    bundle.putStringArrayList(HistoryFragment.AD_LIST, adItem);
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

    //以下為ListView ItemClick的Listener，當按下Item時，將該Item的BLE Name與Address包起來，將送到另一
    //Activity中建立連線
    private class onItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        }
    }

    //分別按下搜尋予停止搜尋button時的功能，分別為開始搜尋與停止搜尋
    public void btnClick(View v) {
//        switch (v.getId()) {
//            case R.id.scanbtnID:
//                ScanFunction(true);
//                break;
//            case R.id.stopbtnID:
//                mScanningMode = 3;
//                mBluetoothAdapter.stopLeScan(mLeScanCallback);
//                textView.setText("Stop Scan");
//                break;
//        }
    }

    //需要注意的是，需加入一個stopLeScan在onPause()中，當按返回鍵或關閉程式時，需停止搜尋BLE
    //否則下次開啟程式時會影響到搜尋BLE device
    @Override
    protected void onPause() {
        super.onPause();

        Log.d(TAG, "onPause():Stop Scan");
        mScanningMode = 3;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
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

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

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

    public byte[] rsaEncode(byte[] plainText,PublicKey publicKey) {
        Encryption encryption = new Encryption();
        byte[] encryptedResult = "0".getBytes();
        try {
            encryptedResult = encryption.cryptByRSA(plainText, publicKey, ALGORITHM, ENCRYPT_MODE);
        } catch (Exception e) {
            e.printStackTrace();}
        return encryptedResult;
    }

    public byte[] rsaDecode(byte[] result, PrivateKey privateKey) throws UnsupportedEncodingException {
        Encryption encryption = new Encryption();
        byte[] decryptResult = "0".getBytes();
        try {
            decryptResult = encryption.cryptByRSA(result, privateKey, ALGORITHM, Cipher.DECRYPT_MODE);
        } catch (Exception e) {
            e.printStackTrace();}
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
