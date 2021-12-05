package edu.coms309.cybuds;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import edu.coms309.cybuds.model.User;

public class ProfileActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_user_profile);

		/*edu.coms309.cybuds.databinding.ActivityProfileBinding binding = ActivityProfileBinding.inflate(getLayoutInflater());
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
		NavigationUI.setupWithNavController(binding.navView, navController);*/

		Intent i = getIntent();
		User user = (User) i.getSerializableExtra("currentUserProfile");

		TextView t = findViewById(R.id.activity_user_profile_name);
		//t.setText("asdf".toCharArray(),0,1);

		t.setText(String.format("%s %s", user.getFirstName(), user.getLastName()));
		t = findViewById(R.id.activity_user_profile_bio);
		t.setText(user.printable());
	}


	public void activity_user_profile_bio_edit_onClick(View view) {
		//FrameLayout fl = findViewById(android.R.id.custom);
		//fl.addView(myView, new LayoutParams(MATCH_PARENT, WRAP_CONTENT));

	}

	public void userProfileGotoMatchUser_onClick(View view) {
		Intent toProfile = new Intent(getBaseContext(), SearchPeopleActivity.class);
		startActivity(toProfile);

	}

	public void btnUserProfile_menu_onClick(View view) {
		Toast.makeText( getBaseContext(), "Goto Feed!", Toast.LENGTH_LONG ).show();
	}


	public void btnUserProfile_interestCard_onClick(View view) {
		setContentView(R.layout.activity_search_interests);
	}

	public void btnUserProfile_groupCard_onClick(View view) {
		setContentView(R.layout.activity_search_groups);
	}


}