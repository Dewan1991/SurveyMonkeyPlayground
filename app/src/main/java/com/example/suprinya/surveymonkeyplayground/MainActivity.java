package com.example.suprinya.surveymonkeyplayground;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;
import com.example.suprinya.surveymonkeyplayground.databinding.ActivityMainBinding;
import com.linecorp.linesdk.api.LineApiClient;
import com.linecorp.linesdk.api.LineApiClientBuilder;
import com.linecorp.linesdk.auth.LineLoginApi;
import com.linecorp.linesdk.auth.LineLoginResult;
import com.surveymonkey.surveymonkeyandroidsdk.SurveyMonkey;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import org.json.JSONException;
import org.json.JSONObject;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {
    public static final int SM_REQUEST_CODE = 0;
    public static final int SM_REQUEST_CUSTOM_CODE = 1;
    public static final int LINE_REQUEST_CODE = 2;
    public static final String SURVEY_HASH = "CYPKXHT";
    public static final String CUSTOM_HASH = "JWJ7MB8";
    public static final String SURVEY_LINK = "https://www.surveymonkey.com/r/26Q3LKN";
    public static final String APP_NAME = "Survey Monkey Playground";
    public static final String USER_ID = "user_id";
    public static final String USER_TYPE = "user_type";

    public static final String SM_RESPONDENT = "smRespondent";
    public static final String SM_ERROR = "smError";
    public static final String RESPONSES = "responses";
    public static final String QUESTION_ID = "question_id";
    public static final String FEEDBACK_QUESTION_ID = "813797519";
    public static final String ANSWERS = "answers";
    public static final String ROW_ID = "row_id";
    public static final String FEEDBACK_FIVE_STARS_ROW_ID = "9082377273";
    public static final String FEEDBACK_POSITIVE_ROW_ID_2 = "9082377274";

    public static final String LINE_CHANNEL_ID = "1562047318";
    private static LineApiClient mLineApiClient;
    private String mAccessToken;

    private ActivityMainBinding mBinding;
    private SurveyMonkey mSurveyMonkey = new SurveyMonkey();
    private String mUserId = "000";
    private String mUserType = "line";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        LineApiClientBuilder apiClientBuilder =
                new LineApiClientBuilder(getApplicationContext(), LINE_CHANNEL_ID);
        mLineApiClient = apiClientBuilder.build();

        setButtonEvent();

        startSurveyMonkey();
    }

    private void setButtonEvent() {
        mBinding.btnFeedbackTemplate.setOnClickListener(v -> takeTemplateSurvey());

        mBinding.btnFeedbackCustom.setOnClickListener(v -> takeCustomSurvey());

        mBinding.btnFeedbackLink.setOnClickListener(v -> takeLinkSurvey());

        mBinding.btnLineLogin.setOnClickListener(v -> {
            if (mAccessToken == null) {
                lineLogin(v);
            } else {
                lineLogout();
            }
        });
    }

    private void startSurveyMonkey() {
        JSONObject user = new JSONObject();
        try {
            user.put(USER_ID, mUserId);
            user.put(USER_TYPE, mUserType);

        } catch (JSONException e) {
            Timber.e(e.getMessage());
        }

        mSurveyMonkey.onStart(this, APP_NAME, SM_REQUEST_CODE, SURVEY_HASH, user);
        mSurveyMonkey.onStart(this, APP_NAME, SM_REQUEST_CUSTOM_CODE, CUSTOM_HASH, user);
    }

    private void takeTemplateSurvey() {
        mBinding.btnFeedbackTemplate.setEnabled(false);
        mSurveyMonkey.startSMFeedbackActivityForResult(this, SM_REQUEST_CODE, SURVEY_HASH);
    }

    private void takeCustomSurvey() {
        mBinding.btnFeedbackCustom.setEnabled(false);
        mSurveyMonkey.startSMFeedbackActivityForResult(this, SM_REQUEST_CUSTOM_CODE, CUSTOM_HASH);
    }

    private void takeLinkSurvey() {
        startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(SURVEY_LINK)));
    }

    private void lineLogin(View view) {
        try {
            // App-to-app login
            Intent loginIntent = LineLoginApi.getLoginIntent(view.getContext(), LINE_CHANNEL_ID);
            startActivityForResult(loginIntent, LINE_REQUEST_CODE);

        } catch (Exception e) {
            Timber.e(e.toString());
        }
    }

    private void lineLogout() {
        mAccessToken = null;
        mBinding.text.setText(R.string.hello_monkey_survey);
        mBinding.btnLineLogin.setText(getString(R.string.line_login));
        Completable.fromAction(() -> mLineApiClient.logout())
                .subscribeOn(Schedulers.io());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        mBinding.btnFeedbackTemplate.setEnabled(true);
        mBinding.btnFeedbackCustom.setEnabled(true);
        //if (resultCode == RESULT_OK) {
        if (requestCode == LINE_REQUEST_CODE) {
            loginLineSuccess(LineLoginApi.getLoginResultFromIntent(intent));

        } else if (requestCode == SM_REQUEST_CODE || requestCode == SM_REQUEST_CUSTOM_CODE) {
            //region useless code
                /*boolean isPromoter = false;
                try {
                    String respondent = intent.getStringExtra(SM_RESPONDENT);
                    Timber.d(respondent);
                    JSONObject surveyResponse = new JSONObject(respondent);
                    JSONArray responsesList = surveyResponse.getJSONArray(RESPONSES);
                    JSONObject response;
                    JSONArray answers;
                    JSONObject currentAnswer;
                    for (int i = 0; i < responsesList.length(); i++) {
                        response = responsesList.getJSONObject(i);
                        if (response.getString(QUESTION_ID)
                                .equals(FEEDBACK_QUESTION_ID)) {
                            answers = response.getJSONArray(ANSWERS);
                            for (int j = 0; j < answers.length(); j++) {
                                currentAnswer = answers.getJSONObject(j);
                                if (currentAnswer.getString(ROW_ID)
                                        .equals(FEEDBACK_FIVE_STARS_ROW_ID)
                                        || currentAnswer.getString(ROW_ID)
                                        .equals(FEEDBACK_POSITIVE_ROW_ID_2)) {
                                    isPromoter = true;
                                    break;
                                }
                            }
                            if (isPromoter) {
                                break;
                            }
                        }
                    }
                } catch (JSONException e) {
                    Log.getStackTraceString(e);
                }
                if (isPromoter) {
                    Toast t = Toast.makeText(this,
                            getString(R.string.promoter_prompt),
                            Toast.LENGTH_LONG);
                    t.show();
                } else {
                    Toast t = Toast.makeText(this,
                            getString(R.string.detractor_prompt),
                            Toast.LENGTH_LONG);
                    t.show();
                }*/
            //endregion
        }

    }

    private void loginLineSuccess(LineLoginResult result) {
        switch (result.getResponseCode()) {
            case SUCCESS:
                // Login successful
                try {
                    mAccessToken = result.getLineCredential()
                            .getAccessToken()
                            .getAccessToken();
                    mUserId = result.getLineProfile()
                            .getUserId();

                    String msg = "Hello " + result.getLineProfile()
                            .getDisplayName() + "\nUser Id : " + result.getLineProfile()
                            .getUserId();
                    mBinding.text.setText(msg);
                    mBinding.btnLineLogin.setText(getString(R.string.line_logout));
                    Toast.makeText(this, "Login successful", Toast.LENGTH_LONG)
                            .show();
                } catch (Exception e) {
                    Timber.e(e.getMessage());
                }
                break;

            case CANCEL:
                // Login canceled by user
                Timber.e("LINE Login Canceled by user!!");
                break;

            default:
                // Login canceled due to other error
                Timber.e("Login FAILED!");
                Timber.e(result.getErrorData()
                        .toString());
        }
    }
}
