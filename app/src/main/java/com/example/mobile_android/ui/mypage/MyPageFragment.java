package com.example.mobile_android.ui.mypage;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.mobile_android.R;
import com.example.mobile_android.ui.login.Login;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MyPageFragment extends Fragment {

    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;

    private ImageView profileImage;
    private TextView userName;
    private TextView userEmail;
    private TextView logoutButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mypage, container, false);

        profileImage = view.findViewById(R.id.profile_image);
        userName = view.findViewById(R.id.user_name);
        userEmail = view.findViewById(R.id.user_email);
        logoutButton = view.findViewById(R.id.logout_button);

        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        // Configure Google Sign In
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), options);

        if (currentUser != null) {
            Glide.with(this).load(currentUser.getPhotoUrl()).into(profileImage);
            userName.setText(currentUser.getDisplayName());
            userEmail.setText(currentUser.getEmail());
        }

        logoutButton.setOnClickListener(v -> {
            // Sign out from Firebase
            auth.signOut();
            // Sign out from Google
            googleSignInClient.signOut().addOnCompleteListener(requireActivity(), task -> {
                Toast.makeText(requireContext(), "로그아웃 하였습니다", Toast.LENGTH_SHORT).show();
                // Go back to Login activity
                Intent intent = new Intent(requireActivity(), Login.class);
                startActivity(intent);
                requireActivity().finish();
            });
        });

        return view;
    }
}
