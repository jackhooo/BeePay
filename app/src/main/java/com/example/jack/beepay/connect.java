package com.example.jack.beepay;

import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by jack on 2017/11/26.
 */

public class connect {
    private String getUrl;
    public static String check;
    public static String pub1;
    public static String pri1;
    public static String pub2;
    public static  String pri2;
    public static  String id;

    public void login(String url){
        this.getUrl = url;
        new Thread() {

            @Override
            public void run() {
                //建立HttpClient物件
                HttpClient httpClient = new DefaultHttpClient();
                //建立Http Get，並給予要連線的Url
                HttpGet get = new HttpGet(getUrl);
                //透過Get跟Http Server連線並取回傳值，並將傳值透過Log顯示出來
                try {

                    HttpResponse response = httpClient.execute(get);
                    HttpEntity resEntity = response.getEntity();
                    String mJsonText = EntityUtils.toString(response.getEntity());
                    pub1 = (String) new JSONObject(mJsonText).get("publickey1");
                    pri1 = (String) new JSONObject(mJsonText).get("privatekey1");
                    pub2 = (String) new JSONObject(mJsonText).get("publickey2");
                    pri2 = (String) new JSONObject(mJsonText).get("privatekey2");
                    check = (String) new JSONObject(mJsonText).get("check");
                    id = new JSONObject(mJsonText).get("id").toString();
                    Log.d("test",check);
                    Log.d("Response of login request", mJsonText);


                } catch (ClientProtocolException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (JSONException e) {
                    Log.e("MYAPP", "unexpected JSON exception", e);
                    // Do something to recover ... or kill the app.
                    e.printStackTrace();
                }

            }

        }.start();
// ).start();
    }
    public  String getxcheck() {
        return check;
    }
    public  synchronized String getcheck(){
        return check;
    }
    public  synchronized String getPub2(){
        return pub2;
    }
    public  synchronized String getPri1(){
        return pri1;
    }
    public  synchronized String getPri2(){
        return pri2;
    }
    public  synchronized String getid(){
        return id;
    }
    public  synchronized String getPub(){
        return pub1;
    }

}