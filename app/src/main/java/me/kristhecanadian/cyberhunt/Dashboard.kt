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

    companion object {
        private const val RC_SIGN_IN = 120
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController: NavController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_dashboard)

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications,
                //R.id.navigation_map
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

        binding.signin.setOnClickListener {
            signIn()
        }

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
            // the user is signed in
            binding.signin.visibility = View.GONE
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

        } else {
            // the user is not signed in
            binding.signin.visibility = View.VISIBLE
            // Configure sign-in to request the user's ID, email address, and basic


            binding.signin.setOnClickListener {
                signIn()
            }

            // Check for existing Google Sign In account, if the user is already signed in
            // the GoogleSignInAccount will be non-null.
            val account = GoogleSignIn.getLastSignedInAccount(this)
            updateUI(account)
        }

    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            // Signed in successfully, update UI with the signed-in user's information
            updateUI(account)
        } catch (e: ApiException) {
            // Sign in failed, update UI appropriately
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
            updateUI(null)
        }
    }

    private fun updateUI(account: GoogleSignInAccount?) {
        if (account != null) {
            val intent = Intent(this, Dashboard::class.java)
            // make the sign in button invisible
            binding.signin.visibility = View.INVISIBLE
            startActivity(intent)
        } else {
            // make the sign in button visible
            binding.signin.visibility = View.VISIBLE
            // make the sign out button invisible
            binding.signout.visibility = View.INVISIBLE
        }
    }
}