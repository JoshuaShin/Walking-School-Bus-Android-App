package com.thewalkingschoolbus.thewalkingschoolbus;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.thewalkingschoolbus.thewalkingschoolbus.Interface.OnTaskComplete;
import com.thewalkingschoolbus.thewalkingschoolbus.api_binding.GetUserAsyncTask;
import com.thewalkingschoolbus.thewalkingschoolbus.Models.User;

import static com.thewalkingschoolbus.thewalkingschoolbus.MainMenuActivity.USER_LOGSTATUS;
import static com.thewalkingschoolbus.thewalkingschoolbus.api_binding.GetUserAsyncTask.functionType.*;


/**
 * MainActivity
 * Description here.
 */
public class MainActivity extends AppCompatActivity {

    public static final String REGISTER_EMAIL = "registerEmail";
    public static final String LOGIN_NAME = "loginName";
    public static final String LOGIN_PASSWORD = "loginPassword";
    public static final String EMAIL_AND_PASSWORD_REQUIRED_EMPTY_MESSAGE = "email and password are required";
    public static final String USERNAME_EMAIL_AND_PASSWORD_REQUIRED_EMPTY_MESSAGE = "name email and password are required";
    public static final String ACCOUNT_HAS_BEEN_REGISTERED_MESSAGE = "this account has been registered";
    public static final String REGISTER_SUCCESSFULLY_MESSAGE = "register succesfully";
    public static final String ACCOUNT_DOES_NOT_EXIST_MESSAGE = "that account does not exist";
    public static final String PASSWORD_AND_NAME_NOT_CORRECT_MESSAGE = "password and name both are not correct";
    public static final String REGISTER_FAIL_MESSAGE = "register failed";
    public static final String LOGIN_FAIL_MESSAGE = "login failed";
    public static final String SUCCESSFUL_LOGIN_MESSAGE = "login successfully";
    EditText emailET;
    EditText passwordET;
    String loginPassword;
    String registerEmail;
    public static final String AppStates = "UUERLOGIN";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupTextviews();
        setupLoginButton();
        setupRegisterButton();
        //getUserLastState(getApplicationContext());
    }


    private void setupTextviews() {
        emailET = (EditText)findViewById(R.id.emailid);
        passwordET = (EditText) findViewById(R.id.passwordid);
    }




    private void setupRegisterButton() {
        Button registerButton = (Button) findViewById(R.id.registerid);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = RegisterActivity.makeIntent(MainActivity.this);
                startActivity(intent);
            }
        });
    }





    // Temporary - delete after proper login is written.
    private void setupLoginButton() {
        final Button loginButton = (Button) findViewById(R.id.loginid);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerEmail = emailET.getText().toString();
                loginPassword = passwordET.getText().toString();

                if(registerEmail.isEmpty()|| loginPassword.isEmpty()){
                    Toast.makeText(getApplicationContext(), EMAIL_AND_PASSWORD_REQUIRED_EMPTY_MESSAGE, Toast.LENGTH_SHORT)
                            .show();
                } else {
                    User.setLoginUser(new User());
                    User.getLoginUser().setEmail(registerEmail);
                    User.getLoginUser().setPassword(loginPassword);
                    login();
                }

            }
        });
    }

    private void login() {
        new GetUserAsyncTask(LOGIN_REQUEST, User.getLoginUser(),null, null, null,new OnTaskComplete() {
            @Override
            public void onSuccess(Object result) {
                if(result == null){
                    Toast.makeText(getApplicationContext(),LOGIN_FAIL_MESSAGE, Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Toast.makeText(getApplicationContext(),SUCCESSFUL_LOGIN_MESSAGE, Toast.LENGTH_SHORT)
                            .show();
                    storeUserInfoToSharePreferences();
                    setLoginUser(User.getLoginUser());
                }
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getApplicationContext(), "ERROR: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }).execute();
    }

    public void setLoginUser(User user){
        new GetUserAsyncTask(GET_USER_BY_EMAIL, user, null, null, null,new OnTaskComplete() {
            @Override
            public void onSuccess(Object result) {
                if(result != null){
                    User.setLoginUser((User)result);
                    Intent intent = MainMenuActivity.makeIntent(MainActivity.this);
                    startActivity(intent);
                    finish();

                    SharedPreferences preferences = getSharedPreferences(AppStates, MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();

                    editor.putBoolean(USER_LOGSTATUS, true);
                    editor.commit();
                }
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(MainActivity.this,"Error :" + e.getMessage() , Toast.LENGTH_SHORT).show();
            }
        }).execute();
    }



    private void storeUserInfoToSharePreferences() {
        SharedPreferences preferences = getSharedPreferences(AppStates, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(REGISTER_EMAIL, registerEmail );
        editor.putString(LOGIN_PASSWORD, loginPassword );
        editor.commit();

    }

    public static Intent makeIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        return intent;
    }

}
