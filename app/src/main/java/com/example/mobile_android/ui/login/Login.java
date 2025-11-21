package com.example.mobile_android.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.cardview.widget.CardView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobile_android.MainActivity;
import com.example.mobile_android.R;
import com.example.mobile_android.fcm.FcmTokenManager;
import com.example.mobile_android.model.UserResponse;
import com.example.mobile_android.network.ApiClient;
import com.example.mobile_android.network.AuthApi;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.messaging.FirebaseMessaging;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class Login extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    FirebaseAuth auth;
    GoogleSignInClient googleSignInClient;

    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == RESULT_OK) {
                Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount signInAccount = accountTask.getResult(ApiException.class);
                    AuthCredential authCredential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(), null);
                    auth.signInWithCredential(authCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Firebase 로그인 성공");

                                GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(Login.this);
                                String idToken = account.getIdToken();

                                if (idToken == null) {
                                    Toast.makeText(Login.this, "ID Token 가져오기 실패", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                String bearer = "Bearer " + idToken;
                                Log.d(TAG, "백엔드 API 호출 시작");

                                Retrofit retrofit = ApiClient.getClient();
                                AuthApi authApi = retrofit.create(AuthApi.class);

                                authApi.googleLogin(bearer).enqueue(new Callback<UserResponse>() {
                                    @Override
                                    public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                                        Log.d(TAG, "백엔드 응답 코드: " + response.code());
                                        if (response.isSuccessful()) {
                                            Log.d(TAG, "백엔드 로그인 성공, MainActivity로 이동");
                                            FirebaseMessaging.getInstance().getToken()
                                                    .addOnCompleteListener(task -> {
                                                        if (task.isSuccessful() && task.getResult() != null) {
                                                            FcmTokenManager.handleNewToken(getApplicationContext(), task.getResult());
                                                        } else {
                                                            Log.w(TAG, "Failed to fetch FCM token", task.getException());
                                                            FcmTokenManager.registerTokenIfPossible(getApplicationContext());
                                                        }
                                                    });

                                            Intent intent = new Intent(Login.this, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Log.e(TAG, "백엔드 로그인 실패: " + response.code() + " - " + response.message());
                                            try {
                                                Log.e(TAG, "에러 바디: " + response.errorBody().string());
                                            } catch (Exception e) {
                                                Log.e(TAG, "에러 바디 읽기 실패");
                                            }
                                            // 백엔드 실패 시 Firebase 로그아웃
                                            auth.signOut();
                                            googleSignInClient.signOut();
                                            Toast.makeText(Login.this, "백엔드 로그인 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<UserResponse> call, Throwable t) {
                                        Log.e(TAG, "서버 요청 실패", t);
                                        // 서버 요청 실패 시 Firebase 로그아웃
                                        auth.signOut();
                                        googleSignInClient.signOut();
                                        Toast.makeText(Login.this, "서버 요청 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                Log.e(TAG, "Firebase 로그인 실패", task.getException());
                                Toast.makeText(Login.this, "Firebase 로그인 실패", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (ApiException e) {
                    Log.e(TAG, "Google 로그인 ApiException", e);
                    e.printStackTrace();
                }
            }
        }
    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();

        // 이미 로그인되어 있다면 바로 MainActivity로 이동
        if (auth.getCurrentUser() != null) {
            FcmTokenManager.registerTokenIfPossible(getApplicationContext());
            Intent intent = new Intent(Login.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        setContentView(R.layout.activity_login);

        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, options);
        auth = FirebaseAuth.getInstance();

        androidx.cardview.widget.CardView googleLoginButton = findViewById(R.id.google_login_button);
        googleLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = googleSignInClient.getSignInIntent();
                activityResultLauncher.launch(intent);
            }
        });
    }
}
