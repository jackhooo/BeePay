package com.example.jack.beepay;

/**
 * Created by jack on 2017/11/26.
 */

public class Item implements java.io.Serializable {

    // 用戶id,用戶名,帳戶鑰匙兩對,帳戶餘額,
    private long id;
    private String acount_name;
    private String account_email;
    private String pub1;
    private String priv1;
    private String pub2;
    private String priv2;
    private int acount_money;
    private int acount_exist; //0or1
    public Item() {
    }

    public Item(long id, String acount_name,String account_email, String pub1, String  priv1, String pub2, String priv2, int acount_money, int acount_exist) {
        this.id = id;
        this.acount_name=acount_name;
        this.account_email=account_email;
        this.pub1=pub1;
        this.priv1=priv1;
        this.pub2=pub2;
        this.priv2=priv2;
        this.acount_money=acount_money;
        this.acount_exist=acount_exist;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAcount_name(){
        return  acount_name;
    }
    public  void setAcount_name(String acount_name){
        this.acount_name=acount_name;
    }
    public String getAccount_email(){
        return  account_email;
    }
    public  void setAccount_email(String account_email){
        this.account_email=account_email;
    }
    public String getPub1(){
        return  pub1;
    }
    public void setpub1(String pub1){
        this.pub1=pub1;
    }
    public String getPub2(){
        return  pub2;
    }
    public void setpub2(String pub2){
        this.pub2=pub2;
    }
    public String getPriv1(){
        return  priv1;
    }
    public void setPriv1(String priv1){
        this.priv1=priv1;
    }
    public String getPriv2(){
        return  priv2;
    }
    public void setPriv2(String priv2){
        this.priv2=priv2;
    }
    public int getAcount_money() {
        return acount_money;
    }

    public void setAcount_money(int acount_money) {
        this.acount_money = acount_money;
    }
    public int getAcount_exist() {
        return acount_exist;
    }

    public void setAcount_exist(int acount_exist) {
        this.acount_exist = acount_exist;
    }

}