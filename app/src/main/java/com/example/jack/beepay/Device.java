package com.example.jack.beepay;

/**
 * Created by jack on 2017/10/20.
 */

public class Device {

    String encodedHex = "";
    String hexMessage1 = "";
    String hexMessage2 = "";

    public void setEncodedHex(){

        encodedHex = hexMessage1 + hexMessage2 ;
    }

    public boolean checkIfAllMessageReceive(){
        if(hexMessage1.length()!=0 && hexMessage2.length()!=0){
            return true;
        }
        else {
            return false;
        }
    }

    public void cleanMessage(){
        hexMessage1 = "";
        hexMessage2 = "";
        encodedHex = "";
    }
}

