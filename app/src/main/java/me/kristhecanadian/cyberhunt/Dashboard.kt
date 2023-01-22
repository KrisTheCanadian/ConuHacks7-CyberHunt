package me.kristhecanadian.cyberhunt

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import me.kristhecanadian.cyberhunt.databinding.ActivityDashboardBinding


class Dashboard : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // create a binding with fragment home
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController: NavController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_dashboard)

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        // Build a GoogleSignInClient with the options specified by gso.
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.signout.setOnClickListener {
            // sign out the user
            googleSignInClient.signOut()
                .addOnCompleteListener(this) {
                    // update the UI
                    updateUI(null)
                }
        }

        // check if the user is signed in
        if (GoogleSignIn.getLastSignedInAccount(this) != null) {
            // make the sign out button visible
            binding.signout.visibility = View.VISIBLE

            binding.signout.setOnClickListener {
                // sign out the user
                googleSignInClient.signOut()
                    .addOnCompleteListener(this) {
                        // update the UI
                        updateUI(null)
                    }
            }

        }

        // TODO REMOVE
        binding.signout.visibility = View.INVISIBLE

    }

    private fun updateUI(account: GoogleSignInAccount?) {
        if (account != null) {
            val intent = Intent(this, Dashboard::class.java)
            startActivity(intent)
        } else {
            val intent = Intent(this, Landing::class.java)
            startActivity(intent)
        }
    }
}