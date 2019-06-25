package in.chalktool.auth;

import androidx.annotation.ColorRes;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.okta.appauth.android.OktaAppAuth;

import net.openid.appauth.AuthorizationException;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final String EXTRA_FAILED = "failed";

    private OktaAppAuth mOktaAppAuth;
    private ConstraintLayout mContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mOktaAppAuth = OktaAppAuth.getInstance(this);

        setContentView(R.layout.activity_login);

        mContainer = findViewById(R.id.loginLayout);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getIntent().getBooleanExtra(EXTRA_FAILED, false)) {
            showMessage(getString(R.string.auth_canceled));
        }

        initializeOktaAuth();
    }

    @Override
    protected void onDestroy() {
        if (mOktaAppAuth != null) {
            mOktaAppAuth.dispose();
            mOktaAppAuth = null;
        }
        super.onDestroy();
    }

    public void loginClickListener(View view) {
        startAuth();
    }

    @MainThread
    private void initializeOktaAuth() {
        Log.i(TAG, "Initializing OktaAppAuth");
        displayLoading(getString(R.string.loading_initializing));

        mOktaAppAuth.init(
                this,
                new OktaAppAuth.OktaAuthListener() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            if (mOktaAppAuth.isUserLoggedIn()) {
                                Log.i(TAG, "User is already authenticated, proceeding " +
                                        "to token activity");
                                startActivity(new Intent(LoginActivity.this,
                                        MainActivity.class));
                                finish();
                            } else {
                                Log.i(TAG, "Login activity setup finished");
                                displayAuthOptions();
                            }
                        });
                    }

                    @Override
                    public void onTokenFailure(@NonNull AuthorizationException ex) {
                        runOnUiThread(() -> showMessage(getString(R.string.init_failure)
                                + ":"
                                + ex.errorDescription));
                    }
                },
                getColorCompat(R.color.primaryColor));
    }


    @MainThread
    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @MainThread
    private void startAuth() {
        displayLoading(getString(R.string.loading_authorizing));

        Intent completionIntent = new Intent(this, MainActivity.class);
        Intent cancelIntent = new Intent(this, LoginActivity.class);
        cancelIntent.putExtra(EXTRA_FAILED, true);
        cancelIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        mOktaAppAuth.login(
                this,
                PendingIntent.getActivity(this, 0, completionIntent, 0),
                PendingIntent.getActivity(this, 0, cancelIntent, 0)
        );
    }

    private void displayLoading(String loadingMessage) {
        findViewById(R.id.loading_container).setVisibility(View.VISIBLE);
        mContainer.setVisibility(View.GONE);

    }

    private void displayAuthOptions() {
        mContainer.setVisibility(View.VISIBLE);

        findViewById(R.id.loading_container).setVisibility(View.GONE);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @SuppressWarnings("deprecation")
    private int getColorCompat(@ColorRes int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getColor(color);
        } else {
            return getResources().getColor(color);
        }
    }
}
