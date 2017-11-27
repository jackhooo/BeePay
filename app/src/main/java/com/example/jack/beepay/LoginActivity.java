package com.example.jack.beepay;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class LoginActivity extends AppCompatActivity {
    private EditText account;
    private EditText Password;
    private ItemDAO itemDAO;
    private String msg = null;
    private String msg2 = null;
    public static final String KEY = "dada";
    String CHECK;
    connect submit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        submit = new connect();
        account = (EditText) findViewById(R.id.account);
        Password = (EditText) findViewById(R.id.password);
        itemDAO = new ItemDAO(getApplicationContext());
        Context context = getApplication();
        SharedPreferences spref = context.getSharedPreferences(
                KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = spref.edit();
//        editor.clear();
//        editor.commit();
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
        String check ;

        msg = account.getEditableText().toString();
        msg2 = Password.getEditableText().toString();

        Context context = getApplication();
        SharedPreferences spref = context.getSharedPreferences(
                KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = spref.edit();
        submit = new connect();
        submit.login("http://140.119.163.23:8080/BLE_Transaction/services/LoginApi?username="+msg+"&password="+msg2);
        String id=submit.getid();
        String pub=submit.getPub();
        String pub2=submit.getPub2();
        String pri=submit.getPri1();
        String pri2=submit.getPri2();
        check=submit.getcheck();
        String test="1";
//        TextView result = (TextView) findViewById(R.id.tv1);
//        result.setText(check);
        if(test.equals(check)) {
            editor.putString("email", msg);
            editor.putString("pub1",pub);
            editor.putString("pub2",pub2);
            editor.putString("pri",pri);
            editor.putString("priv2",pri2);
            editor.putString("id", id);
            editor.clear();
            editor.apply();
            editor.commit();
//            result.setText(spref.getString("msg",null));

            Intent intent2 = new Intent();
            intent2.setClass(LoginActivity.this, MainActivity.class);
            startActivity(intent2);
        }
        else {
            TextView result = (TextView) findViewById(R.id.tv1);
            result.setText("帳號或密碼錯誤");
        }
    }
    public void registerClick(View view) {
        Intent goMainIntent = new Intent(LoginActivity.this, register.class);
        startActivity(goMainIntent);
    }
}