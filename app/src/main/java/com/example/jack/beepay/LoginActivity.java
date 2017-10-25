package com.example.jack.beepay;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }

    //按下登入
    public void btnClick(View view) {
        Intent goMainIntent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(goMainIntent);
    }
}
