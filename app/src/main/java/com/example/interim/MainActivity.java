package com.example.interim;

import android.os.Bundle;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private Button profileButton, cameraButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.viewPager);
        profileButton = findViewById(R.id.profileButton);
        cameraButton = findViewById(R.id.cameraButton);

        // Set up ViewPager Adapter
        List<androidx.fragment.app.Fragment> fragments = new ArrayList<>();
        fragments.add(new CameraFragment());
        fragments.add(new ProfileFragment());

        ViewPagerAdapter adapter = new ViewPagerAdapter(this, fragments);
        viewPager.setAdapter(adapter);

        // Disable swipe (so user only switches via toggle button)
        viewPager.setUserInputEnabled(false);

        // Toggle Button Click Listeners
        profileButton.setOnClickListener(v -> switchTab(1));
        cameraButton.setOnClickListener(v -> switchTab(0));

        // Listen for page changes to update button styles
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == 0) { // Camera is active
                    cameraButton.setBackgroundResource(R.drawable.toggle_active);
                    cameraButton.setTextColor(getResources().getColor(android.R.color.white));

                    profileButton.setBackgroundResource(R.drawable.toggle_inactive);
                    profileButton.setTextColor(getResources().getColor(android.R.color.black));
                } else { // Profile is active
                    profileButton.setBackgroundResource(R.drawable.toggle_active);
                    profileButton.setTextColor(getResources().getColor(android.R.color.white));

                    cameraButton.setBackgroundResource(R.drawable.toggle_inactive);
                    cameraButton.setTextColor(getResources().getColor(android.R.color.black));
                }
            }
        });
    }

    private void switchTab(int index) {
        viewPager.setCurrentItem(index, true);
    }
}
