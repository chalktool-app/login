package in.chalktool.auth;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.okta.appauth.android.OktaAppAuth;

import net.openid.appauth.AuthorizationException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicReference;

import static com.okta.appauth.android.OktaAppAuth.getInstance;

public class AccountFragment extends Fragment {

    private static final String TAG = "AccountFragment";

    private static final String KEY_USER_INFO = "userInfo";
    private static final String EXTRA_FAILED = "failed";

    private OktaAppAuth mOktaAppAuth;
    private final AtomicReference<JSONObject> mUserInfoJson = new AtomicReference<>();

    public AccountFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mOktaAppAuth = getInstance(getContext());
        if (!mOktaAppAuth.isUserLoggedIn()) {
            Log.d(TAG, "No logged in user found. Finishing session");

            getActivity().finish();
        }

        if (savedInstanceState != null) {
            try {
                mUserInfoJson.set(new JSONObject(savedInstanceState.getString(KEY_USER_INFO)));
            } catch (JSONException ex) {
                Log.e(TAG, "Failed to parse saved user info JSON, discarding", ex);
            }
        }
    }

    private void displayAuthorizationInfo(View view) {
        view.findViewById(R.id.authorized).setVisibility(View.VISIBLE);
        view.findViewById(R.id.account_loading_container).setVisibility(View.GONE);

        JSONObject userInfo = mUserInfoJson.get();
        Log.i(TAG,"mess: " + userInfo.toString());
        try {
            String firstName = "???";
            if (userInfo.has("given_name")) {
                firstName = userInfo.getString("given_name");
            }
            ((TextView) getView().findViewById(R.id.inputFirstName)).setText(firstName);

            String lastName = "???";
            if (userInfo.has("family_name")) {
                lastName = userInfo.getString("family_name");
            }
            ((TextView) getView().findViewById(R.id.inputLastName)).setText(lastName);

            String email = "???";
            if (userInfo.has("preferred_username")) {
                email = userInfo.getString("preferred_username");
            }
            ((TextView) getView().findViewById(R.id.inputEmail)).setText(email);

            ((TextView) getView().findViewById(R.id.logoutBtn)).setOnClickListener((logoutView -> signOut()));

        } catch (JSONException ex) {
            Log.e(TAG, "Failed to read userinfo JSON", ex);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        if (mOktaAppAuth.isUserLoggedIn()) {
            displayLoading(view);
            fetchUserInfo(view);

        } else {
            Log.i(TAG, "No authorization state retained - reauthorization required");
            startActivity(new Intent(getContext(), LoginActivity.class));
            getActivity().finish();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @MainThread
    private void displayLoading(View view) {
        view.findViewById(R.id.account_loading_container).setVisibility(View.VISIBLE);
        view.findViewById(R.id.authorized).setVisibility(View.GONE);
    }

    @MainThread
    private void fetchUserInfo(View view) {
        displayLoading(view);
        mOktaAppAuth.getUserInfo(new OktaAppAuth.OktaAuthActionCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                // Do whatever you need to do with the user info data
                mUserInfoJson.set(response);
                getActivity().runOnUiThread(() -> displayAuthorizationInfo(view));
            }

            @Override
            public void onTokenFailure(@NonNull AuthorizationException ex) {
                // Handle an error with the Okta authorization and tokens
                mUserInfoJson.set(null);
                Log.i(TAG,mUserInfoJson.toString());
                getActivity().runOnUiThread(() -> {
                    showSnackbar(getString(R.string.token_failure_message));
                });
            }

            @Override
            public void onFailure(int httpResponseCode, Exception ex) {
                // Handle a network error when fetching the user info data
                mUserInfoJson.set(null);
                Log.i(TAG,mUserInfoJson.toString());
                getActivity().runOnUiThread(() -> {
                    showSnackbar(getString(R.string.network_failure_message));
                });
            }
        });
    }

    @MainThread
    private void showSnackbar(String message) {
        getActivity().getWindow().getDecorView().post(() -> Snackbar.make(getView().findViewById(R.id.account_fragment),
                message,
                Snackbar.LENGTH_SHORT)
                .show());
    }

    @MainThread
    private void signOut() {

        Intent completionIntent = new Intent(getContext(), LoginActivity.class);
        Intent cancelIntent = new Intent(getContext(), MainActivity.class);
        cancelIntent.putExtra(EXTRA_FAILED, true);
        cancelIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        completionIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

        mOktaAppAuth.signOutFromOkta(getContext(),
                PendingIntent.getActivity(getContext(), 0, completionIntent, 0),
                PendingIntent.getActivity(getContext(), 0, cancelIntent, 0)
        );

        mOktaAppAuth.clearSession();

    }

}