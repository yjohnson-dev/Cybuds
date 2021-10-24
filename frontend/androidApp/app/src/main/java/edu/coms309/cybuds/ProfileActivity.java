package edu.coms309.cybuds;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import edu.coms309.cybuds.databinding.ActivityProfileBinding;
import edu.coms309.cybuds.model.User;

public class ProfileActivity extends AppCompatActivity {

	private ActivityProfileBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_profile);

		/*binding = ActivityProfileBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		BottomNavigationView navView = findViewById(R.id.nav_view);
		// Passing each menu ID as a set of Ids because each
		// menu should be considered as top level destinations.
		AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
				R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
				.build();
		NavHostFragment navHostFragment =
				(NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_profile);
		assert navHostFragment != null;
		NavController navController = navHostFragment.getNavController();
		NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
		NavigationUI.setupWithNavController(binding.navView, navController);

		Intent i = getIntent();
		User user = (User) i.getSerializableExtra("currentUserProfile");

		TextView t = findViewById(R.id.textView_profileHeader);
		t.setText(String.format("%s %s", user.getFirstName(), user.getLastName()));
		t = findViewById(R.id.textView_MainBody);
		t.setText(user.printable());*/
	}


}