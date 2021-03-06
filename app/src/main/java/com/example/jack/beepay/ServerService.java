package com.example.jack.beepay;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.util.UUID;

public class ServerService extends Service {

    private static final String TAG = ServerService.class.getSimpleName();

    public static boolean running = false;

    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    private BluetoothGattServer gattServer;

    private static final String SERVICE_UUID_YOU_CAN_CHANGE = "0000180a-0000-1000-8000-00805f9b34fb";//Device Information Service
    private static final String CHAR_UUID_YOU_CAN_CHANGE = "0000aaaa-0000-1000-8000-00805f9b34fb";
    private static final String CHAR2_UUID_YOU_CAN_CHANGE = "0000bbbb-0000-1000-8000-00805f9b34fb";

    private static int NOTIFICATION_ID = 0;


    String address = "0";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        Log.v(null, "onStartCommand");

        running = true;

        initialize();

        startServer();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        /**
         * Note that onDestroy is not guaranteed to be called quickly or at all. Services exist at
         * the whim of the system, and onDestroy can be delayed or skipped entirely if memory need
         * is critical.
         */
        running = false;
        stopServer();
        //stopForeground(true);
        super.onDestroy();
    }

    /**
     * Required for extending service, but this will be a Started Service only, so no need for
     * binding.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Get references to system Bluetooth objects if we don't have them already.
     */
    private void initialize() {

        if (mBluetoothLeAdvertiser == null) {

            BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

            if (mBluetoothManager != null) {

                BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();

                gattServer = mBluetoothManager.openGattServer(this,serverCallback);

                if (mBluetoothAdapter != null) {
                    mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
                } else {
                    Toast.makeText(ServerService.this, "null", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(ServerService.this, "null", Toast.LENGTH_LONG).show();
            }
        }

    }

    private void sendNotification(String message){
        NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(getString(R.string.app_name))
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                        .setAutoCancel(true)
                        .setContentText(message);
        Notification note = mBuilder.build();
        note.defaults |= Notification.DEFAULT_VIBRATE;
        note.defaults |= Notification.DEFAULT_SOUND;
        mNotificationManager.notify(NOTIFICATION_ID++, note);
    }

    // Send an Intent with an action named "my-event".
    private void sendMessage(String message) {
        Intent intent = new Intent("my-event");
        // add data
        intent.putExtra("key", message);
        //intent.putExtra("pub", "");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // Send an Intent with an action named "my-event".
    private void sendPubMessage(String message) {
        Intent intent = new Intent("event");
        // add data
        //intent.putExtra("key", "");
        intent.putExtra("pub", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private BluetoothGattServerCallback serverCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            if(newState == BluetoothProfile.STATE_CONNECTED) {
                sendNotification("Client connected: " + device.getAddress());
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                sendNotification("Client disconnected");
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            gattServer.sendResponse(device, requestId, 0, offset, characteristic.getValue());
        }


        //Get price and send back
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            byte[] bytes = value;
            String message = new String(bytes);

            if(CHAR_UUID_YOU_CAN_CHANGE.equals(characteristic.getUuid().toString())){
                //Log.i("GetKey", "aaaa");
                sendMessage(message);
            }else if(CHAR2_UUID_YOU_CAN_CHANGE.equals(characteristic.getUuid().toString())){
                //Log.i("GetKey", "bbbb");
                sendPubMessage(message);
            }

            //sendNotification(message);
            gattServer.sendResponse(device, requestId, 0, offset, value);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);

        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            byte[] bytes = value;
            String message = new String(bytes);
            sendNotification(bytesToHexString(value));
            gattServer.sendResponse(device, requestId, 0, offset, value);
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            super.onExecuteWrite(device, requestId, execute);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
        }
    };

    //UUIDを設定
    private void setUuid() {

        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor
                (UUID.fromString(CHAR_UUID_YOU_CAN_CHANGE), BluetoothGattDescriptor.PERMISSION_READ);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

        BluetoothGattDescriptor descriptor2 = new BluetoothGattDescriptor
                (UUID.fromString(CHAR2_UUID_YOU_CAN_CHANGE), BluetoothGattDescriptor.PERMISSION_READ);
        descriptor2.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

        //serviceUUIDを設定
        BluetoothGattService service = new BluetoothGattService(
                UUID.fromString(SERVICE_UUID_YOU_CAN_CHANGE),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        //SharedPreferences spref = getSharedPreferences("dada", Context.MODE_PRIVATE);
        //String myPub1 = spref.getString("pub1", null);
        //String myPriv2 = spref.getString("priv2", null);


        //characteristicUUIDを設定
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                UUID.fromString(CHAR_UUID_YOU_CAN_CHANGE),
                BluetoothGattCharacteristic.PROPERTY_READ |
                        BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ |
                        BluetoothGattCharacteristic.PERMISSION_WRITE);

        characteristic.setValue("");

        //characteristicUUIDを設定
        BluetoothGattCharacteristic characteristic2 = new BluetoothGattCharacteristic(
                UUID.fromString(CHAR2_UUID_YOU_CAN_CHANGE),
                BluetoothGattCharacteristic.PROPERTY_READ |
                        BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ |
                        BluetoothGattCharacteristic.PERMISSION_WRITE);

        characteristic2.setValue("hiiii");

        //characteristicUUIDをserviceUUIDにのせる
        service.addCharacteristic(characteristic);
        service.addCharacteristic(characteristic2);

        //serviceUUIDをサーバーにのせる
        gattServer.addService(service);
    }

    private void startServer() {

        Log.d(TAG, "Service: Starting Server");

        setUuid();
    }

    private void stopServer() {
        Log.d(TAG, "Service: Stopping Server");

        if (gattServer != null) {
            gattServer.clearServices();
            gattServer.close();
            gattServer = null;
        }
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
