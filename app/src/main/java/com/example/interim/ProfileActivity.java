package com.example.interim;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends AppCompatActivity {

    private Button profileButton, cameraButton;
    private LinearLayout logoutButton;
    private TextView profileName;
    private DatabaseReference reference;
    private TextView profileAnalysisText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        profileButton = findViewById(R.id.profileButton);
        cameraButton = findViewById(R.id.cameraButton);
        logoutButton = findViewById(R.id.logout_button);
        profileName = findViewById(R.id.profile_name);
        reference = FirebaseDatabase.getInstance().getReference("users");
        profileAnalysisText = findViewById(R.id.profile_analysis_text);

        // Retrieve username from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("USER_DATA", MODE_PRIVATE);
        String username = sharedPreferences.getString("USERNAME", null);
        String skinTone = getIntent().getStringExtra("selectedSkinTone"); // Pass from SkintoneSelectionActivity
        String season = getIntent().getStringExtra("selectedSeason"); // Pass from ColorAnalysisActivity


        if (username != null) {
            fetchUserName(username);
        } else {
            profileName.setText("User");
        }


        // Set initial active/inactive state
        setActiveButton(profileButton, cameraButton);

        // Camera Button Click Listener (Switch to CameraActivity)
        cameraButton.setOnClickListener(v -> {
            animateButtonSwitch(cameraButton, profileButton); // Button animation
            Intent intent = new Intent(ProfileActivity.this, CameraActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out); // Activity transition animation
            finish();
        });

        logoutButton.setOnClickListener(v -> {
            // Clear stored username on logout
            sharedPreferences.edit().remove("USERNAME").apply();

            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        OpenAIHelper.generateProfileText(skinTone, season, new OpenAIHelper.OpenAIResponse() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> profileAnalysisText.setText(response));
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> profileAnalysisText.setText("Error: " + error));
            }
        });
        String selectedSkinTone = sharedPreferences.getString("selectedSkinTone", "Default Skin Tone");
        String selectedSeason = sharedPreferences.getString("selectedSeason", "Default Season");

// Update UI
        TextView profileSkinTone = findViewById(R.id.profile_skin_tone);
        TextView profileSeason = findViewById(R.id.profile_season);

        profileSkinTone.setText("Skin tone: " + selectedSkinTone);
        profileSeason.setText("Season: " + selectedSeason);
    }


    private void fetchUserName(String username) {
        reference.child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    if (name != null) {
                        profileName.setText(name);
                    } else {
                        profileName.setText("User");
                    }
                } else {
                    Toast.makeText(ProfileActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                    profileName.setText("User");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void animateButtonSwitch(Button active, Button inactive) {
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(inactive, "alpha", 1f, 0.5f);
        fadeOut.setDuration(300);
        fadeOut.start();

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(active, "alpha", 0.5f, 1f);
        fadeIn.setDuration(300);
        fadeIn.start();
    }

    private void setActiveButton(Button active, Button inactive) {
        active.setBackgroundResource(R.drawable.toggle_active);
        active.setTextColor(getResources().getColor(android.R.color.white));
        inactive.setBackgroundResource(R.drawable.toggle_inactive);
        inactive.setTextColor(getResources().getColor(android.R.color.black));
    }
}
