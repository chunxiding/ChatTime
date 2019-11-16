package com.example.chattime;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.chattime.Common.Common;
import com.example.chattime.Holder.QBUsersHolder;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBRestChatService;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.content.QBContent;
import com.quickblox.content.model.QBFile;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.request.QBRequestUpdateBuilder;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class UserProfileActivity extends AppCompatActivity {

    EditText editNewPassword, editOldPassword, editFullName, editEmail, editPhone;
    Button buttonUpdate, buttonCancel;

    ImageView user_avatar;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.user_update_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.user_update_log_out: {
                logOut();
                break;
            }
            default:
                break;

        }
        return true;
    }

    private void logOut() {
        QBUsers.signOut().performAsync(new QBEntityCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid, Bundle bundle) {
                QBChatService.getInstance().logout(new QBEntityCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid, Bundle bundle) {
                        Toast.makeText(UserProfileActivity.this, "Logged Out", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(UserProfileActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(QBResponseException e) {

                    }
                });
            }

            @Override
            public void onError(QBResponseException e) {

            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        Toolbar toolbar = (Toolbar)findViewById(R.id.user_update_toolbar);
        toolbar.setTitle("ChatTime");
        setSupportActionBar(toolbar);

        initViews();
        loadUserProfile();

        user_avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent selectImage = new Intent();
                selectImage.setType("image/*");
                selectImage.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(selectImage, "Select Image"), Common.SELECT_IMAGE);
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        buttonUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newPassword = editNewPassword.getText().toString();
                String oldPassword = editOldPassword.getText().toString();
                String fullName = editFullName.getText().toString();
                String phone = editPhone.getText().toString();
                String email = editEmail.getText().toString();

                QBUser user = new QBUser();
                user.setId(QBChatService.getInstance().getUser().getId());
                if(!Common.isNullOrEmptyString(oldPassword)) {
                    user.setOldPassword(oldPassword);
                }
                if(!Common.isNullOrEmptyString(newPassword)) {
                    user.setPassword(newPassword);
                }
                if(!Common.isNullOrEmptyString(fullName)) {
                    user.setFullName(fullName);
                }
                if(!Common.isNullOrEmptyString(phone)) {
                    user.setPhone(phone);
                }
                if(!Common.isNullOrEmptyString(email)) {
                    user.setEmail(email);
                }

                final ProgressDialog mDialog = new ProgressDialog(UserProfileActivity.this);

                mDialog.setMessage("Please Wait...");
                mDialog.show();

                QBUsers.updateUser(user).performAsync(new QBEntityCallback<QBUser>() {
                    @Override
                    public void onSuccess(QBUser qbUser, Bundle bundle) {
                        Toast.makeText(UserProfileActivity.this, "User: " + qbUser.getLogin() + " updated", Toast.LENGTH_SHORT).show();
                        mDialog.dismiss();
                    }

                    @Override
                    public void onError(QBResponseException e) {
                        Toast.makeText(UserProfileActivity.this, "ERROR: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e("A", String.valueOf(requestCode));
        if (resultCode == RESULT_OK) {
            // Log.e("A", "herepic1");
            if (requestCode == Common.SELECT_IMAGE) {
                // Log.e("A", "herepic2");
                Uri selectImageUrl = data.getData();
                final ProgressDialog mDialog = new ProgressDialog(UserProfileActivity.this);
                mDialog.setMessage("Please wait...");
                mDialog.setCancelable(false);
                mDialog.show();

                try {
                    InputStream in = (InputStream) getContentResolver().openInputStream(selectImageUrl);
                    final Bitmap bitmap = BitmapFactory.decodeStream(in);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
                    File file = new File(Environment.getExternalStorageDirectory() + "/image.png");
                    FileOutputStream fileOut = new FileOutputStream(file);
                    fileOut.write(bos.toByteArray());
                    fileOut.flush();
                    fileOut.close();

                    int imageSizeKb = (int) file.length() / 1024;
                    if (imageSizeKb >= (1024 * 100)) {
                        Toast.makeText(this, "Image too large", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    QBContent.uploadFileTask(file, true, null).performAsync(new QBEntityCallback<QBFile>() {
                        @Override
                        public void onSuccess(QBFile qbFile, Bundle bundle) {
                            QBUser user = new QBUser();
                            user.setId(QBChatService.getInstance().getUser().getId());
                            user.setFileId(Integer.parseInt(qbFile.getId().toString()));

                            QBUsers.updateUser(user).performAsync(new QBEntityCallback<QBUser>() {
                                @Override
                                public void onSuccess(QBUser qbUser, Bundle bundle) {
                                    mDialog.dismiss();
                                    user_avatar.setImageBitmap(bitmap);
                                }

                                @Override
                                public void onError(QBResponseException e) {

                                }
                            });
                        }

                        @Override
                        public void onError(QBResponseException e) {

                        }

                    });
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        }
    }

    private void loadUserProfile() {


        int id = QBChatService.getInstance().getUser().getId();
        QBUsers.getUser(id).performAsync(new QBEntityCallback<QBUser>() {
            @Override
            public void onSuccess(QBUser qbUser, Bundle bundle) {
                String fullName = qbUser.getFullName();
                String email = qbUser.getEmail();
                String phone = qbUser.getPhone();
                editPhone.setText(phone);
                editFullName.setText(fullName);
                editEmail.setText(email);

                QBUsersHolder.getInstance().putUser(qbUser);
                if (qbUser.getFileId() != null) {
                    int profilePicId = qbUser.getFileId();
                    QBContent.getFile(profilePicId).performAsync(new QBEntityCallback<QBFile>() {
                        @Override
                        public void onSuccess(QBFile qbFile, Bundle bundle) {
                            String fileUrl = qbFile.getPublicUrl();
                            Picasso.with(getBaseContext()).load(fileUrl).into(user_avatar);
                        }

                        @Override
                        public void onError(QBResponseException e) {

                        }
                    });
                }
            }

            @Override
            public void onError(QBResponseException e) {

            }
        });

//        Log.e("A", "" + phone);
//        Log.e("A", editPhone.getText().toString());
    }

    private void initViews() {
        buttonCancel = (Button) findViewById(R.id.update_user_button_cancel);
        buttonUpdate = (Button) findViewById(R.id.update_user_button_update);

        editEmail = (EditText) findViewById(R.id.update_edit_email);
        editFullName = (EditText) findViewById(R.id.update_edit_full_name);
        editNewPassword = (EditText) findViewById(R.id.update_edit_new_password);
        editOldPassword = (EditText) findViewById(R.id.update_edit_old_password);
        editPhone = (EditText) findViewById(R.id.update_edit_phone);

        user_avatar = (ImageView) findViewById(R.id.user_avatar);
    }
}
