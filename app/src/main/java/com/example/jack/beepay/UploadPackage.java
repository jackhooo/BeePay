package com.example.jack.beepay;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;

/**
 * Created by jack on 2017/11/29.
 */

public class UploadPackage {
    private String getUrl;

    public void upload(String url) {
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

                } catch (ClientProtocolException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

        }.start();
// ).start();
    }
}
