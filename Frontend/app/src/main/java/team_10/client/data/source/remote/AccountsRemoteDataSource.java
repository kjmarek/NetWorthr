package team_10.client.data.source.remote;

import android.support.annotation.NonNull;
import android.widget.Toast;

import com.android.volley.Response;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import team_10.client.MainActivity;
import team_10.client.constant.PERIOD;
import team_10.client.constant.TYPE;
import team_10.client.constant.URL;
import team_10.client.data.User;
import team_10.client.data.models.Account;
import team_10.client.data.models.Transaction;
import team_10.client.data.source.AccountsDataSource;
import team_10.client.data.source.AccountsRepository;
import team_10.client.utility.adapter.AbstractAccountAdapter;
import team_10.client.utility.adapter.LocalDateAdapter;
import team_10.client.utility.io.AppExecutors;
import team_10.client.utility.io.HostReachableCallback;
import team_10.client.utility.io.IO;
import team_10.client.utility.io.SharedPreferencesManager;
import team_10.client.utility.io.VolleyPostRequest;

public class AccountsRemoteDataSource implements AccountsDataSource {

    private static volatile AccountsRemoteDataSource INSTANCE;

    private AppExecutors mAppExecutors;

    // Prevent direct instantiation.
    private AccountsRemoteDataSource(@NonNull AppExecutors appExecutors) {
        mAppExecutors = appExecutors;
    }

    public static AccountsRemoteDataSource getInstance(@NonNull AppExecutors appExecutors) {
        if (INSTANCE == null) {
            synchronized (AccountsRemoteDataSource.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AccountsRemoteDataSource(appExecutors);
                }
            }
        }
        return INSTANCE;
    }

    @Override
    public void getAccounts(@NonNull LoadAccountsCallback callback) {
        List<Account> returnVal = new ArrayList<>();
        String requestBody = null;

        try {
            JsonObject userAccountsRequest = new JsonObject();
            userAccountsRequest.addProperty("id", Integer.toString(SharedPreferencesManager.getUser().getID()));
            requestBody = userAccountsRequest.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        final String finalRequestBody = requestBody;
        VolleyPostRequest request = new VolleyPostRequest(finalRequestBody, URL.URL_GET_ACCOUNTS,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {

                            System.out.println(response);

                            JSONObject object = new JSONObject(response);

                            if (object.has("accounts")) {
                                List<Account> accounts = IO.deserializeAccounts(response);

                                System.out.println("Fetched from server:\n" + IO.serializeAccounts(accounts));

                                callback.onAccountsLoaded(accounts);

                            } else if (object.has("error") &&
                                    object.getBoolean("error") && User.getToken() != "") {

                                String message = object.getString("message");
                                Toast.makeText(MainActivity.myContext, "Error: " + message, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

        request.execute(new HostReachableCallback() {
            @Override
            public void onHostReachable() {

            }

            @Override
            public void onHostUnreachable() {
                callback.onDataNotAvailable();
            }
        });
    }


    @Override
    public void getAccount(@NonNull String accountID, @NonNull GetAccountCallback callback) {

    }

    @Override
    public void newAccount(@NonNull TYPE type, @NonNull GetAccountCallback callback) {

    }

    @Override
    public void getAccountCopy(@NonNull String accountID, @NonNull GetAccountCallback callback) {

    }

    @Override
    public void saveAccount(@NonNull Account account, @NonNull SaveAccountCallback callback) {

        // Sending edited accounts to server requires specific URL
        String URL = (AccountsRepository.getInstance().getCachedAccounts().containsKey(account.getID()))
                ? team_10.client.constant.URL.URL_EDIT_ACCOUNTS
                : team_10.client.constant.URL.URL_ADD_ACCOUNTS;

        System.out.println(URL);

        String requestBody = null;
        try {
            ArrayList<Account> accountsList = new ArrayList<>();
            accountsList.add(account);
            requestBody = IO.serializeAccounts(accountsList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String finalRequestBody = requestBody;
        VolleyPostRequest request = new VolleyPostRequest(requestBody, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject object = new JSONObject(response);

                    if (object.has("error") &&
                            object.getBoolean("error")) {  // handle request specific errors

                        String message = object.getString("message");

                        Toast.makeText(MainActivity.myContext, "Error: " + message, Toast.LENGTH_SHORT).show();

                    } else {
                        System.out.println("Successfully sent to server:\n" + finalRequestBody);

                        callback.onAccountSaved();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        request.execute(new HostReachableCallback() {
            @Override
            public void onHostReachable() {

            }

            @Override
            public void onHostUnreachable() {
                callback.onDataNotAvailable();
            }
        });
    }

    @Override
    public void disableAccount(@NonNull String accountID) {

    }

    @Override
    public void deleteAllAccounts() {

    }

    @Override
    public void refreshAccounts() {

    }

    @Override
    public void getValues(@NonNull PERIOD period, GetValuesCallback callback) {
        GsonBuilder b = new GsonBuilder();
        b.registerTypeAdapter(Account.class, new AbstractAccountAdapter());
        b.registerTypeAdapter(LocalDate.class, new LocalDateAdapter());
        b.setPrettyPrinting();
        Gson g = b.create();

        VolleyPostRequest request = new VolleyPostRequest(null, URL.URL_FETCH_ACCOUNTS, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                System.out.println(response);

                try {
                    JSONObject object = new JSONObject(response);
                    if (object.has("error") &&
                            object.getBoolean("error")) {  // handle request specific errors

                        String message = object.getString("message");

                        Toast.makeText(MainActivity.myContext, "Error: " + message, Toast.LENGTH_SHORT).show();

                        callback.onDataNotAvailable();

                    } else {
                        Type mapType = new TypeToken<Map<String, Map<LocalDate, Double>>>() {
                        }.getType();
                        Map<String, Map<LocalDate, Double>> wrapper = g.fromJson(response, mapType);
                        if (wrapper != null)
                            updateTransactions(wrapper);
                        else
                            return;

                        List<Account> cachedAccounts = new ArrayList<>(
                                AccountsRepository.getInstance().getCachedAccounts().values());

                        Map<LocalDate, Double> values = new HashMap<>();

                        LocalDate date = LocalDate.now().minusDays(
                                period.getDaysPerPeriod() * period.getPeriodsOnGraph());

                        while (date.isBefore(LocalDate.now())) {
                            for (Account account : cachedAccounts) {
                                if (TYPE.getType(account.getClass()).isValueOnNetwork()) {

                                    int assetOrLiabilityMuliplier = (TYPE.getType(account.getClass()).isAsset()) ? 1 : -1;
                                    Double value = account.getValue(date) * assetOrLiabilityMuliplier;

                                    if (values.containsKey(date)) {
                                        Double cachedValue = values.get(date);

                                        values.put(date, value + cachedValue);
                                    } else {
                                        values.put(date, value);
                                    }

                                }
                            }

                            date = date.plusDays(period.getDaysPerPeriod());
                        }

                        System.out.println("Successfully fetched data from the server:\n" + wrapper);

                        callback.onValuesLoaded(values);

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        request.execute(new HostReachableCallback() {
            @Override
            public void onHostReachable() {

            }

            @Override
            public void onHostUnreachable() {
                callback.onDataNotAvailable();
            }
        });
    }

    private void updateTransactions(@NonNull Map<String, Map<LocalDate, Double>> values) {
        final Map<String, Account> cachedAccounts = AccountsRepository.getInstance().getCachedAccounts();

        for (String accountID : values.keySet()) {

            if (!cachedAccounts.containsKey(accountID))
                return;

            Account account = cachedAccounts.get(accountID);

            for (LocalDate date : values.get(accountID).keySet()) {

                Double value = values.get(accountID).get(date);

                Transaction t = account.getTransaction(date);

                t.setValue(value);

                if (date.isEqual(LocalDate.now()))
                    continue;

                AccountsRepository.getInstance().saveAccount(account, new SaveAccountCallback() {
                    @Override
                    public void onAccountSaved() {

                    }

                    @Override
                    public void onDataNotAvailable() {

                    }
                });

            }
        }
    }
}