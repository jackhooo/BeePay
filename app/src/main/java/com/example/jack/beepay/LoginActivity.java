package com.example.jack.beepay;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

public class LoginActivity extends AppCompatActivity {
    private EditText account;
    private EditText Password;
    private ItemDAO itemDAO;
    private String msg = null;
    private String msg2 = null;
    public static final String KEY = "dada";
    String CHECK;
    connect submit;
    private Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        submit = new connect();
        account = (EditText) findViewById(R.id.account);
        Password = (EditText) findViewById(R.id.password);
        Context context = getApplication();
        SharedPreferences spref = context.getSharedPreferences(
                KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = spref.edit();
        CHECK = spref.getString("email", null);
        if(CHECK!=null) {
            Intent intent = new Intent();
            intent.setClass(LoginActivity.this,MainActivity.class);
            startActivity(intent);
        }
    }

    //按下登入
    public void btnClick(View view) {
        int i=0;

        msg = account.getEditableText().toString();
        msg2 = Password.getEditableText().toString();
        Context context = getApplication();
        SharedPreferences spref = context.getSharedPreferences(
                KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = spref.edit();
        submit = new connect();
        submit.login("http://140.119.163.23:8080/BLE_Transaction/services/LoginApi?username="+msg+"&password="+msg2);
        String check=submit.getcheck();
////            result.setText(spref.getString("msg",null));
        dialog = ProgressDialog.show(LoginActivity.this,
                "讀取中", "請等待3秒...",true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    String id=submit.getid();
                    String pub=submit.getPub();
                    String pub2=submit.getPub2();
                    String pri=submit.getPri1();
                    String pri2=submit.getPri2();
                    SharedPreferences spref = getSharedPreferences(
                            KEY, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = spref.edit();
                    editor.putString("email", msg);
                    editor.putString("pub1",pub);
                    editor.putString("pub2",pub2);
                    editor.putString("pri",pri);
                    editor.putString("priv2",pri2);
                    editor.putString("id", id);
                    editor.putInt("countpackage",0);
                    editor.putString("countid","0");
                    editor.putInt("whichupload",0);
                    editor.commit();
                    Thread.sleep(2000);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    String check=submit.getcheck();
                    dialog.dismiss();
                    String test="1";
                    if(test.equals(check)) {
//                        Log.d("code",id);
                        Intent intent2 = new Intent();
                        intent2.setClass(LoginActivity.this, MainActivity.class);
                        startActivity(intent2);
                        finish();
                    }
                }
            }
        }).start();

    }
    public void registerClick(View view) {
        Intent goMainIntent = new Intent(LoginActivity.this, register.class);
        startActivity(goMainIntent);
    }

}
