package com.example.chattime;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.quickblox.auth.session.QBSettings;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.QBSettingsSaver;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

public class MainActivity extends AppCompatActivity {
    static final String APP_ID = "";
    static final String AUTH_KEY = "";
    static final String AUTH_SECRET = "";
    static final String ACCOUNT_KEY = "";

    Button buttonLogin, buttonSignUp;
    EditText editUsername, editPassword;

    static final int REQUEST_CODE = 1000;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestRuntimePermissions();
        
        initializeFramework();

        buttonLogin = (Button) findViewById(R.id.main_buttonLogin);
        buttonSignUp = (Button) findViewById(R.id.main_buttonSignUp);

        editPassword = (EditText) findViewById(R.id.main_editPassword);
        editUsername = (EditText) findViewById(R.id.main_editLogin);

        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SignUpActivity.class));
            }
        });

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String user = editUsername.getText().toString();
                final String password = editPassword.getText().toString();

                QBUser qbUser = new QBUser(user, password);

                QBUsers.signIn(qbUser).performAsync(new QBEntityCallback<QBUser>() {
                    @Override
                    public void onSuccess(QBUser qbUser, Bundle bundle) {
                        Toast.makeText(getBaseContext(), "Login Successful!",Toast.LENGTH_SHORT).show();

                        // go to chat dialogs page after login success
                        Intent intent = new Intent(MainActivity.this, ChatDialogsActivity.class);
                        // pass user and password info using intent extra
                        intent.putExtra("user", user);
                        intent.putExtra("password", password);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(QBResponseException e) {
                        Toast.makeText(getBaseContext(), ""+e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestRuntimePermissions() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
        checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {
                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getBaseContext(), "Permission Granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getBaseContext(), "Permission Denied", Toast.LENGTH_SHORT).show();

                }
                break;
            }
        }
    }

    private void initializeFramework() {
        QBSettings.getInstance().init(getApplicationContext(), APP_ID, AUTH_KEY, AUTH_SECRET);
        QBSettings.getInstance().setAccountKey(ACCOUNT_KEY);
    }
}
