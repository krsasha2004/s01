package ru.steam.steamguard;


import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import ru.steam.*;
import ru.steam.utils.TimeCorrector;
import ru.steam.webrequests.Endpoints;
import ru.steam.webrequests.RequestBuilder;
import ru.steam.webrequests.RequestErrorInfo;
import ru.steam.webrequests.ResponseListener;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Slf4j
public class SteamguardState {
    private static Context sContext;
    private static ArrayList<SteamguardState> sSteamGuardStates;
    private static boolean sbLoadedSteamguardStates;
    private boolean mCommitted;
    private JSONObject mInfo;
    private JSONObject mTwoFactorStatus;
    private TwoFactorToken mTwoFactorToken;

    /* renamed from: com.valvesoftware.android.steam.community.SteamguardState.1 */
    static class C01571 implements Comparator<SteamguardState> {
        C01571() {
        }

        public int compare(SteamguardState sg1, SteamguardState sg2) {
            return sg1.getAccountName().compareToIgnoreCase(sg2.getAccountName());
        }
    }

    /* renamed from: com.valvesoftware.android.steam.community.SteamguardState.2 */
    class C01582 extends ResponseListener {
        C01582() {
        }

        public void onSuccess(JSONObject json) {
            SteamguardState.this.handleTwoFactorStatus(json);
        }

        public void onError(RequestErrorInfo errorInfo) {
        }
    }

    /* renamed from: com.valvesoftware.android.steam.community.SteamguardState.3 */
    class C01593 extends ResponseListener {
        final /* synthetic */ Completion val$completion;
        final /* synthetic */ FinalizeTwoFactorState val$finalizeTwoFactorState;

        C01593(FinalizeTwoFactorState finalizeTwoFactorState, Completion completion) {
            this.val$finalizeTwoFactorState = finalizeTwoFactorState;
            this.val$completion = completion;
        }

        public void onSuccess(JSONObject json) {
            if (!json.optBoolean("success")) {
                SteamguardState.this.finalizeHelperErrorCleanup(this.val$finalizeTwoFactorState);
                if (json.optInt("status") == 89) {
                    this.val$completion.failure(json.optInt("status"), "SteamMobile_Steamguard_BadCode");
                    return;
                }
                this.val$completion.failure(json.optInt("status"), null);
            } else if (!json.optBoolean("want_more")) {
//                SteamguardState.broadcastSteamguardStateAdded(SteamguardState.this.getTokenGID());
                this.val$completion.success();
            } else if (this.val$finalizeTwoFactorState.retriesRemaining <= 0) {
                SteamguardState.this.finalizeHelperErrorCleanup(this.val$finalizeTwoFactorState);
                this.val$completion.failure(-1, null);
            } else {
                if (this.val$finalizeTwoFactorState.bSentActivationCode) {
                    FinalizeTwoFactorState finalizeTwoFactorState = this.val$finalizeTwoFactorState;
                    finalizeTwoFactorState.timeOffset += 30;
                    finalizeTwoFactorState = this.val$finalizeTwoFactorState;
                    finalizeTwoFactorState.retriesRemaining--;
                } else {
                    this.val$finalizeTwoFactorState.bSentActivationCode = true;
                }
                SteamguardState.this.finalizeAddTwoFactorHelper(this.val$finalizeTwoFactorState, this.val$completion);
            }
        }

        public void onError(RequestErrorInfo errorInfo) {
            if (this.val$finalizeTwoFactorState.retriesRemaining > 0) {
                FinalizeTwoFactorState finalizeTwoFactorState = this.val$finalizeTwoFactorState;
                finalizeTwoFactorState.timeOffset += 30;
                finalizeTwoFactorState = this.val$finalizeTwoFactorState;
                finalizeTwoFactorState.retriesRemaining--;
                SteamguardState.this.finalizeAddTwoFactorHelper(this.val$finalizeTwoFactorState, this.val$completion);
            } else if (this.val$finalizeTwoFactorState.nSentAuthCodeCount > 0) {
//                SteamguardState.broadcastSteamguardStateAdded(SteamguardState.this.getTokenGID());
                this.val$completion.success();
            } else {
                this.val$completion.failure(-1, null);
            }
        }
    }

    /* renamed from: com.valvesoftware.android.steam.community.SteamguardState.4 */
    class C01604 extends ResponseListener {
        final /* synthetic */ Completion val$completion;

        C01604(Completion completion) {
            this.val$completion = completion;
        }

        public void onSuccess(JSONObject json) {
            SteamguardState.mergeJson(SteamguardState.this.mInfo, json);
            SteamguardState.this.mTwoFactorToken = new TwoFactorToken(SteamguardState.this.mInfo);
            if (json.has("shared_secret")) {
                SteamguardState.this.sendActivationCodeEmail();
                this.val$completion.success();
                return;
            }
            this.val$completion.failure(-1, null);
        }

        public void onError(RequestErrorInfo errorInfo) {
            this.val$completion.failure(-1, null);
        }
    }

    /* renamed from: com.valvesoftware.android.steam.community.SteamguardState.5 */
    class C01615 extends ResponseListener {
        C01615() {
        }

        public void onSuccess(JSONObject response) {
            response.toString();
            response.toString();
        }

        public void onError(RequestErrorInfo errorInfo) {
            errorInfo.toString();
            errorInfo.toString();
        }
    }

    /* renamed from: com.valvesoftware.android.steam.community.SteamguardState.6 */
    class C01626 extends ResponseListener {
        final /* synthetic */ Completion val$completion;
        final /* synthetic */ Scheme val$newScheme;
        final /* synthetic */ String val$oldScheme;

        C01626(String str, Scheme scheme, Completion completion) {
            this.val$oldScheme = str;
            this.val$newScheme = scheme;
            this.val$completion = completion;
        }

        public void onSuccess(JSONObject json) {
            if (json.optBoolean("success")) {
                if (this.val$oldScheme.equals(Integer.toString(Scheme.TwoFactor.ordinal()))) {
                    SteamguardState.this.sendRemovalEmail();
                }
                String gid = SteamguardState.this.getTokenGID();
                SteamguardState.this.sanitize();
                SteamguardState.this.internalSetScheme(this.val$newScheme);
                SteamguardState.this.saveToFile();
//                SteamguardState.broadcastSteamguardStateRemoved(gid);
                this.val$completion.success();
                return;
            }
            this.val$completion.failure(-1, null);
        }

        public void onError(RequestErrorInfo errorInfo) {
            errorInfo.toString();
            this.val$completion.failure(-1, null);
        }
    }

    public static abstract class Completion {
        public abstract void failure(int i, String str);

        public abstract void success();
    }

    private enum ETwoFactorEmailType {
        k_ETwoFactorEmailType_None,
        k_ETwoFactorEmailType_Signup,
        k_ETwoFactorEmailType_Added,
        k_ETwoFactorEmailType_Removed
    }

    private enum ETwoFactorTokenType {
        k_ETwoFactorTokenType_None,
        k_ETwoFactorTokenType_ValveMobileApp,
        k_ETwoFactorTokenType_ThirdParty
    }

    class FinalizeTwoFactorState {
        public String activationCode;
        public boolean bSentActivationCode;
        public int nSentAuthCodeCount;
        public Scheme oldScheme;
        public int retriesRemaining;
        public long timeOffset;

        public FinalizeTwoFactorState(String activationCode) {
            this.activationCode = activationCode;
            this.timeOffset = -40;
            this.retriesRemaining = 10;
            this.bSentActivationCode = false;
            this.nSentAuthCodeCount = 0;
            this.oldScheme = SteamguardState.this.getScheme();
        }
    }

    public enum Scheme {
        None,
        Email,
        TwoFactor
    }

    static {
        sSteamGuardStates = new ArrayList();
        sbLoadedSteamguardStates = false;
    }

 //   public static void initializeSteamguardState(Context context) {
 //       sContext = context;
    public static void initializeSteamguardState() {
        sContext = new Context();
        getSteamGuardStates();
    }

    public static ArrayList<SteamguardState> getSteamGuardStates() {
        if (!sbLoadedSteamguardStates) {
            sbLoadedSteamguardStates = true;
            sSteamGuardStates = loadExistingSteamguardStates();
        }
        return sSteamGuardStates;
    }

    public static boolean hasLiveSteamguardStates() {
        Iterator i$ = getSteamGuardStates().iterator();
        while (i$.hasNext()) {
            if (((SteamguardState) i$.next()).getTwoFactorToken() != null) {
                return true;
            }
        }
        return false;
    }

    public static ArrayList<SteamguardState> getTwoFactorSteamGuardStates() {
        ArrayList<SteamguardState> result = new ArrayList();
        Iterator i$ = getSteamGuardStates().iterator();
        while (i$.hasNext()) {
            SteamguardState sgState = (SteamguardState) i$.next();
            if (sgState.getTwoFactorToken() != null) {
                result.add(sgState);
            }
        }
        return result;
    }

    public static ArrayList<SteamguardState> getSortedTwoFactorSteamGuardStates() {
        ArrayList<SteamguardState> sgStates = getTwoFactorSteamGuardStates();
        Collections.sort(sgStates, new C01571());
        return sgStates;
    }

    public static SteamguardState steamguardStateForLoggedInUser() {
        return steamguardStateForSteamID(LoggedInUserAccountInfo.getLoginSteamID());
    }

    public static SteamguardState steamguardStateForSteamID(String steamID) {
        if (steamID == null) {
            return null;
        }
        Iterator i$ = sSteamGuardStates.iterator();
        while (i$.hasNext()) {
            SteamguardState steamguardState = (SteamguardState) i$.next();
            if (steamguardState.getSteamId().equals(steamID)) {
                return steamguardState;
            }
        }
        SteamguardState sgState = new SteamguardState(steamID);
        sSteamGuardStates.add(sgState);
        return sgState;
    }

    public static SteamguardState steamguardStateForGID(String gidString) {
        if (gidString == null) {
            return null;
        }
        Iterator i$ = sSteamGuardStates.iterator();
        while (i$.hasNext()) {
            SteamguardState steamguardState = (SteamguardState) i$.next();
            if (steamguardState.getTokenGID().equals(gidString)) {
                return steamguardState;
            }
        }
        return null;
    }

    public static Scheme stringToScheme(String schemeName) {
        if (schemeName == null) {
            return null;
        }
        for (Scheme scheme : Scheme.values()) {
            if (schemeName.equalsIgnoreCase(scheme.toString())) {
                return scheme;
            }
        }
        return null;
    }

    static Scheme schemeNumberStringToScheme(String s) {
        if (s == null) {
            return null;
        }
        int n = Integer.parseInt(s);
        for (Scheme sch : Scheme.values()) {
            if (sch.ordinal() == n) {
                return sch;
            }
        }
        return null;
    }

    public static int installSecret(String base64String) {
        String jsonString = null;
        try {
            jsonString = new String(ru.steam.crypt.Base64.decode(base64String.getBytes(), 0), "UTF-8");
        } catch (Exception e) {
        }
        if (jsonString == null) {
            return -1;
        }
        JSONObject json = null;
        try {
            json = new JSONObject(jsonString);
        } catch (Exception e2) {
        }
        if (json == null) {
            return -1;
        }
        String steamID = json.optString("steamid");
        if (steamID == null) {
            return -1;
        }
        SteamguardState sgState = steamguardStateForSteamID(steamID);
        if (sgState == null) {
            return -1;
        }
        sgState.updateFromJSON(json);
        return 0;
    }

//    public static void broadcastSteamguardStateAdded(String gid) {
//        Intent intent = new Intent();
//        intent.setAction("TWOFACTORCODES_CHANGED");
//        intent.putExtra("com.valve.community.added", gid);
//        sContext.sendBroadcast(intent);
//    }
//
//    public static void broadcastSteamguardStateRemoved(String gid) {
//        Intent intent = new Intent();
//        intent.setAction("TWOFACTORCODES_CHANGED");
//        intent.putExtra("com.valve.community.removed", gid);
//        sContext.sendBroadcast(intent);
//    }

    public SteamguardState(String steamId) {
        this.mInfo = new JSONObject();
        try {
            this.mInfo.put("steamid", steamId);
        } catch (JSONException e) {
        }
        this.mCommitted = true;
    }

    public SteamguardState(JSONObject properties) {
        this.mInfo = properties;
        this.mCommitted = true;
    }

    public String getSteamId() {
        return this.mInfo.optString("steamid");
    }

    public String getAccountName() {
        return this.mInfo.optString("account_name");
    }

    public TwoFactorToken getTwoFactorToken() {
        if (getScheme() != Scheme.TwoFactor) {
            return null;
        }
        if (this.mTwoFactorToken == null) {
            this.mTwoFactorToken = new TwoFactorToken(this.mInfo);
        }
        return this.mTwoFactorToken;
    }

    public String getTokenGID() {
        return this.mInfo.optString("token_gid");
    }

    public String getRevocationCode() {
        return this.mInfo.optString("revocation_code");
    }

    public Scheme getScheme() {
        return schemeNumberStringToScheme(this.mInfo.optString("steamguard_scheme", "0"));
    }

    public boolean saveToFile() {
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(stateFileForSteamId(getSteamId()),false);
            outputStream.write(this.mInfo.toString().getBytes());
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                }
            }
            return true;
        } catch (FileNotFoundException e2) {
            if (outputStream == null) {
                return false;
            }
            try {
                outputStream.close();
                return false;
            } catch (IOException e3) {
                return false;
            }
        } catch (IOException e4) {
            if (outputStream == null) {
                return false;
            }
            try {
                outputStream.close();
                return false;
            } catch (IOException e5) {
                return false;
            }
        } catch (Throwable th) {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e6) {
                }
            }
        }
        return false;
    }

    private static SteamguardState loadFromFile(Context context, String filename) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filename);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer, 0, inputStream.available());
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(new String(buffer));
            } catch (JSONException e2) {
            }
            if (jsonObject != null) {
                return new SteamguardState(jsonObject);
            }
            return null;
        } catch (IOException e3) {
            if (inputStream == null) {
                return null;
            }
            try {
                inputStream.close();
                return null;
            } catch (IOException e4) {
                return null;
            }
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e5) {
                }
            }
        }
        return null;
    }

    private static ArrayList<SteamguardState> loadExistingSteamguardStates() {
        ArrayList<SteamguardState> ar = new ArrayList();
        File dir = new File(".");

        for (File file : dir.listFiles()) {
            if (file.getName() != null && file.getName().startsWith("Steamguard-")) {
                SteamguardState steamguardState = loadFromFile(sContext, file.getName());
                if (steamguardState != null) {
                    ar.add(steamguardState);
                }
            }
        }
        return ar;
    }

    public void updateFromJSON(JSONObject properties) {
        this.mInfo = properties;
        this.mCommitted = true;
        this.mTwoFactorToken = null;
        saveToFile();
//        if (getTokenGID() != null) {
//            broadcastSteamguardStateAdded(getTokenGID());
//        }
    }

    private static String stateFileForSteamId(String steamId) {
        return String.format("%s%s", new Object[]{"Steamguard-", steamId});
    }

    public void sanitize() {
        String[] saveProperties = new String[]{"steamguard_scheme", "steamid", "account_name"};
        JSONObject newInfo = new JSONObject();
        for (String property : saveProperties) {
            if (this.mInfo.has(property)) {
                try {
                    newInfo.put(property, this.mInfo.optString(property));
                } catch (JSONException e) {
                }
            }
        }
        this.mInfo = newInfo;
    }

    public void startGetTwoFactorStatus() {
        RequestBuilder requestBuilder = Endpoints.getTwoFactorQueryStatusRequestBuilder(LoggedInUserAccountInfo.getLoginSteamID());
        requestBuilder.setResponseListener(new C01582());
        sendRequest(requestBuilder);
    }

    public void startSetScheme(Scheme newScheme, String smsPhoneId, Completion completion) {
        if (newScheme == Scheme.TwoFactor && getScheme() == Scheme.TwoFactor) {
            String tokenGID = getTokenGID();
            internalSetScheme(Scheme.None);
            sanitize();
            saveToFile();
//            broadcastSteamguardStateRemoved(tokenGID);
            startAddTwoFactor(smsPhoneId, completion);
        } else if (newScheme == Scheme.TwoFactor) {
            startAddTwoFactor(smsPhoneId, completion);
        } else {
            internalStartSetScheme(newScheme, completion);
        }
    }

    private void sendRequest(RequestBuilder requestBuilder) {
        SteamCommunityApplication.GetInstance().sendRequest(requestBuilder);
    }

    private void finalizeHelperErrorCleanup(FinalizeTwoFactorState finalizeTwoFactorState) {
        if (finalizeTwoFactorState.nSentAuthCodeCount > 0) {
            internalSetScheme(finalizeTwoFactorState.oldScheme);
            sanitize();
            saveToFile();
            this.mCommitted = false;
        }
    }

    private void finalizeAddTwoFactorHelper(FinalizeTwoFactorState finalizeTwoFactorState, Completion completion) {
        RequestBuilder requestBuilder;
        long authenticator_time = this.mTwoFactorToken.currentTime() + finalizeTwoFactorState.timeOffset;
        if (finalizeTwoFactorState.bSentActivationCode) {
            requestBuilder = Endpoints.getTwoFactorFinalizeAddAuthenticatorRequestBuilder(LoggedInUserAccountInfo.getLoginSteamID(), null, this.mTwoFactorToken.generateSteamGuardCodeForTime(authenticator_time), authenticator_time);
            if (finalizeTwoFactorState.nSentAuthCodeCount == 0) {
                this.mCommitted = true;
                internalSetScheme(Scheme.TwoFactor);
                saveToFile();
            }
            finalizeTwoFactorState.nSentAuthCodeCount++;
        } else {
            requestBuilder = Endpoints.getTwoFactorFinalizeAddAuthenticatorRequestBuilder(LoggedInUserAccountInfo.getLoginSteamID(), finalizeTwoFactorState.activationCode, null, authenticator_time);
        }
        requestBuilder.setResponseListener(new C01593(finalizeTwoFactorState, completion));
        sendRequest(requestBuilder);
    }

    public void finalizeAddTwoFactor(String activationCode, Completion completion) {
        finalizeAddTwoFactorHelper(new FinalizeTwoFactorState(activationCode), completion);
    }

    private static void mergeJson(JSONObject dest, JSONObject obj) {
        Iterator<String> iter = obj.keys();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            try {
                dest.put(key, obj.get(key));
            } catch (JSONException e) {
            }
        }
    }

    private void startAddTwoFactor(String smsPhoneId, Completion completion) {
        RequestBuilder requestBuilder = Endpoints.getAddAuthenticatorRequestBuilder(LoggedInUserAccountInfo.getLoginSteamID(), Integer.toString(ETwoFactorTokenType.k_ETwoFactorTokenType_ValveMobileApp.ordinal()), getUniqueIdForPhone());
        if (smsPhoneId != null) {
            requestBuilder.appendKeyValue("sms_phone_id", smsPhoneId);
        }
        requestBuilder.setResponseListener(new C01604(completion));
        sendRequest(requestBuilder);
    }

    public static String getUniqueIdForPhone() {
        SharedPreferences sharedPreferences = sContext.getSharedPreferences("steam.uuid", 0);
        String savedId = sharedPreferences.getString("uuidKey", "");
        if (savedId.length() > 0) {
            return savedId;
        }
        String generatedId = null;
        try {
            generatedId = UUID.randomUUID().toString();
        } catch (Exception ex) {
            log.error("RandomUUID", ex.toString());
        }
        if (generatedId == null) {
            generatedId = String.format("%d", new Object[]{Long.valueOf(new GregorianCalendar().getTimeInMillis())});
        }
        String result = String.format("android:%s", new Object[]{generatedId});
        sharedPreferences.edit().putString("uuidKey", result).commit();
        return result;
    }

    public void sendActivationCodeEmail() {
        sendEmailOfType(ETwoFactorEmailType.k_ETwoFactorEmailType_Signup, "include_activation_code", "1");
    }

    private void sendRemovalEmail() {
        sendEmailOfType(ETwoFactorEmailType.k_ETwoFactorEmailType_Removed, new String[0]);
    }

    private void sendEmailOfType(ETwoFactorEmailType emailType, String... arguments) {
        RequestBuilder requestBuilder = Endpoints.getTwoFactorSendEmailRequestBuilder(LoggedInUserAccountInfo.getLoginSteamID(), emailType.ordinal());
        for (int i = 0; i + 1 < arguments.length; i += 2) {
            requestBuilder.appendKeyValue(arguments[i], arguments[i + 1]);
        }
        requestBuilder.setResponseListener(new C01615());
        sendRequest(requestBuilder);
    }

    public void removeTwoFactorForScheme(Scheme newScheme) {
        if (newScheme != null && newScheme != Scheme.TwoFactor) {
            String gid = getTokenGID();
            internalSetScheme(newScheme);
            sanitize();
            saveToFile();
//            broadcastSteamguardStateRemoved(gid);
        }
    }

    private void handleTwoFactorStatus(JSONObject json) {
        this.mTwoFactorStatus = json;
        if (getScheme() == Scheme.TwoFactor) {
            Scheme serverScheme = schemeNumberStringToScheme(json.optString("steamguard_scheme"));
            String serverGid = json.optString("token_gid");
            if (serverScheme == null) {
                return;
            }
            if (serverScheme != Scheme.TwoFactor || serverGid != null) {
                if (getScheme() == Scheme.TwoFactor) {
                    removeTwoFactorForScheme(serverScheme);
                } else if (getTokenGID() != null && serverGid.compareTo(getTokenGID()) != 0) {
                    removeTwoFactorForScheme(Scheme.Email);
                }
            }
        }
    }

    private void internalStartSetScheme(Scheme newScheme, Completion completion) {
        String oldScheme = this.mInfo.optString("steamguard_scheme");
        RequestBuilder requestBuilder = Endpoints.getRemoveAuthenticatorRequestBuilder(LoggedInUserAccountInfo.getLoginSteamID(), Integer.toString(newScheme.ordinal()), this.mInfo.optString("revocation_code", null));
        requestBuilder.setResponseListener(new C01626(oldScheme, newScheme, completion));
        sendRequest(requestBuilder);
    }

    private void internalSetScheme(Scheme newScheme) {
        try {
            this.mInfo.put("steamguard_scheme", Integer.toString(newScheme.ordinal()));
        } catch (JSONException e) {
        }
    }

    static String percentEncodeUrlUnsafeChars(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '+' || c == '/' || c == '=') {
                sb.append(String.format("%%%02x", new Object[]{Integer.valueOf(c)}));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String base64encryptedConfirmationHash(long time, String tag) {
        String sSecret = this.mInfo.optString("identity_secret");
        if (sSecret == null) {
            return "";
        }
        byte[] secretBytes = ru.steam.crypt.Base64.decode(sSecret.getBytes(), 0);
        int dataLen = 8;
        if (tag != null) {
            if (tag.length() > 32) {
                dataLen = 8 + 32;
            } else {
                dataLen = 8 + tag.length();
            }
        }
        byte[] dataBytes = new byte[dataLen];
        int i = 8;
        while (true) {
            int i2 = i - 1;
            if (i <= 0) {
                break;
            }
            dataBytes[i2] = (byte) ((int) time);
            time >>>= 8;
            i = i2;
        }
        if (tag != null) {
            System.arraycopy(tag.getBytes(), 0, dataBytes, 8, dataLen - 8);
        }
        SecretKeySpec signKey = new SecretKeySpec(secretBytes, "HmacSHA1");
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signKey);
            return percentEncodeUrlUnsafeChars(ru.steam.crypt.Base64.encodeToString(mac.doFinal(dataBytes), 2));
        } catch (NoSuchAlgorithmException e) {
            return null;
        } catch (InvalidKeyException e2) {
            return null;
        }
    }

    public String getConfirmationUrl() {
        if (base64encryptedConfirmationHash(TimeCorrector.getInstance().currentTimeSeconds(), "conf") == null) {
            return "";
        }
        return String.format("%s?p=%s&a=%s&k=%s&t=%d&m=android&tag=%s", new Object[]{SteamAppUri.CONFIRMATION_WEB, getUniqueIdForPhone(), getSteamId(), base64encryptedConfirmationHash(TimeCorrector.getInstance().currentTimeSeconds(), "conf"), Long.valueOf(TimeCorrector.getInstance().currentTimeSeconds()), "conf"});
    }

    public String getTaggedConfirmationUrlParams(String tag) {
        if (base64encryptedConfirmationHash(TimeCorrector.getInstance().currentTimeSeconds(), tag) == null) {
            return "";
        }
        return String.format("p=%s&a=%s&k=%s&t=%d&m=android&tag=%s", new Object[]{getUniqueIdForPhone(), getSteamId(), base64encryptedConfirmationHash(TimeCorrector.getInstance().currentTimeSeconds(), tag), Long.valueOf(TimeCorrector.getInstance().currentTimeSeconds()), tag});
    }

    public static void handleTwoFactorRemovalNotification(String gidString, String newSchemeString) {
        if (gidString != null && newSchemeString != null) {
            SteamguardState sgState = steamguardStateForGID(gidString);
            if (sgState != null) {
                Scheme newScheme = stringToScheme(newSchemeString);
                if (newScheme != null) {
                    sgState.removeTwoFactorForScheme(newScheme);
                }
            }
        }
    }
}
