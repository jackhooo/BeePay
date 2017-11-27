package com.example.jack.beepay;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class register extends AppCompatActivity {
    private String msg = null;
    private String msg2 = null;
    private String msg3 = null;
    private EditText account_email;
    private EditText account;
    private EditText Password;
    private Button getBtn;
    private Button getBtn2;
    private ItemDAO itemDAO;
    connect sumbit;
    connect2 sumbit2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);
        sumbit = new connect();
        sumbit2= new connect2();
        account = (EditText) findViewById(R.id.editText2);
        Password = (EditText) findViewById(R.id.editText3);
        account_email = (EditText) findViewById(R.id.editText4);
    }
    public void submitClick(View view) {
        TextView result = (TextView)findViewById(R.id.text);
        int i=0;
        msg = account.getEditableText().toString();
        msg2 = Password.getEditableText().toString();
        msg3=account_email.getEditableText().toString();
        sumbit2.Get("http://140.119.163.23:8080/BLE_Transaction/services/RegisterApi?user_email="+msg3+"&password="+msg2+"&user_account="+msg);
        String checkstring=sumbit2.getCheckregister();
        result.setText(checkstring);
        Intent goMainIntent = new Intent(register.this, LoginActivity.class);
        startActivity(goMainIntent);

    }
    public void backClick(View view) {
        Intent goMainIntent = new Intent(register.this, LoginActivity.class);
        startActivity(goMainIntent);
    }
}