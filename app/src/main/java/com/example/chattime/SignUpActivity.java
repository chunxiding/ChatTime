package com.example.chattime;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.quickblox.auth.QBAuth;
import com.quickblox.auth.session.QBSession;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

public class SignUpActivity extends AppCompatActivity {

    Button buttonSignUp, buttonCancel;
    EditText editUsername, editPassword, editFullname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        registerSession();

        buttonSignUp = (Button) findViewById(R.id.signUp_buttonSignUp);
        buttonCancel = (Button) findViewById(R.id.signUp_buttonCancel);

        editPassword = (EditText) findViewById(R.id.signUp_editPassword);
        editUsername = (EditText) findViewById(R.id.signUp_editLogin);
        editFullname = (EditText) findViewById(R.id.signUp_editFullName);

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String user = editUsername.getText().toString();
                String password = editPassword.getText().toString();
                String fullname = editFullname.getText().toString();

                QBUser qbUser = new QBUser(user, password);

                qbUser.setFullName(fullname);

                QBUsers.signUp(qbUser).performAsync(new QBEntityCallback<QBUser>() {
                    @Override
                    public void onSuccess(QBUser qbUser, Bundle bundle) {
                        Toast.makeText(getBaseContext(),"Sign Up Successful!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(QBResponseException e) {
                        Toast.makeText(getBaseContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void registerSession() {
        QBAuth.createSession().performAsync(new QBEntityCallback<QBSession>() {
            @Override
            public void onSuccess(QBSession qbSession, Bundle bundle) {

            }

            @Override
            public void onError(QBResponseException e) {
                Log.e("ERROR",e.getMessage());
            }
        });
    }
}
