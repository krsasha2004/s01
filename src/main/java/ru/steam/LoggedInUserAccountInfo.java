package ru.steam;

import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import com.valvesoftware.android.steam.community.SettingInfo.DateConverter;
import com.valvesoftware.android.steam.community.views.SteamWebView;
import com.valvesoftware.android.steam.community.webrequests.Endpoints;
import com.valvesoftware.android.steam.community.webrequests.RequestBuilder;
import com.valvesoftware.android.steam.community.webrequests.RequestErrorInfo;
import com.valvesoftware.android.steam.community.webrequests.ResponseListener;
import org.json.JSONException;
import org.json.JSONObject;
import ru.steam.config.Config;
import ru.steam.webrequests.Endpoints;
import ru.steam.webrequests.RequestBuilder;
import ru.steam.webrequests.RequestErrorInfo;
import ru.steam.webrequests.ResponseListener;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class LoggedInUserAccountInfo {
    private static boolean dontLoginToChat;
    private static LoginInformation loginInformation;
    private static boolean s_bCookiesAreOutOfDate;
    private static final Map<String, Map<String, CWebCookie>> s_cookiesConfiguration;
    private static long s_lLastWGTokenFailureMS;
    private static String s_strLanguage;

    /* renamed from: com.valvesoftware.android.steam.community.LoggedInUserAccountInfo.1 */
    static class C01461 extends ResponseListener {


        C01461() {

        }

        public void onSuccess(JSONObject response) {
            String wgtoken = response.optString("token", "");
            String wgtoken_secure = response.optString("token_secure", "");
            if (wgtoken.length() == 0 || wgtoken_secure.length() == 0) {
                LoggedInUserAccountInfo.logOut();
                LoggedInUserAccountInfo.s_lLastWGTokenFailureMS = 0;
                return;
            }
            LoggedInUserAccountInfo.updateWGToken(wgtoken, wgtoken_secure);
            LoggedInUserAccountInfo.s_lLastWGTokenFailureMS = System.currentTimeMillis();

        }

        public void onError(RequestErrorInfo errorInfo) {
        }
    }

    private static class CWebCookie {
        public boolean m_httponly;
        public boolean m_secure;
        public String m_value;

        CWebCookie(String value, boolean secure, boolean httponly) {
            this.m_secure = false;
            this.m_httponly = false;
            this.m_value = value;
            this.m_secure = secure;
            this.m_httponly = httponly;
        }

        public boolean equals(CWebCookie other) {
            if (this.m_value == null) {
                if (other.m_value != null) {
                    return false;
                }
            } else if (!this.m_value.equals(other.m_value)) {
                return false;
            }
            if (this.m_secure == other.m_secure && this.m_httponly == other.m_httponly) {
                return true;
            }
            return false;
        }
    }

    public static class LoginInformation {
        public String accessToken;
        public LoginState loginState;
        public String steamId;
        public String wgtoken;
        public String wgtokenSecure;

        public LoginInformation() {
            LogOut();
        }

        public LoginInformation(JSONObject jsonDoc) {
            DeserializeFromJSONDoc(jsonDoc);
        }

        void LogOut() {
            this.loginState = LoginState.RequireUsernameAndPassword;
            this.steamId = null;
            this.accessToken = null;
            this.wgtoken = null;
            this.wgtokenSecure = null;
        }

        void SerializeToJSONDoc(JSONObject jsonDoc) throws JSONException {
            if (this.loginState == LoginState.LoggedIn) {
                jsonDoc.put("access_token", this.accessToken);
                jsonDoc.put("x_steamid", this.steamId);
                jsonDoc.put("wgtoken", this.wgtoken);
                jsonDoc.put("wgtoken_secure", this.wgtokenSecure);
            }
        }

        void DeserializeFromJSONDoc(JSONObject jsonDoc) {
            LogOut();
            this.accessToken = jsonDoc.optString("access_token");
            this.steamId = jsonDoc.optString("x_steamid");
            this.wgtoken = jsonDoc.optString("wgtoken");
            this.wgtokenSecure = jsonDoc.optString("wgtoken_secure");
            if (this.accessToken != null && this.accessToken.length() != 0) {
                this.loginState = LoginState.LoggedIn;
            }
        }
    }

    public enum LoginState {
        RequireUsernameAndPassword,
        RequireSteamGuardEmailToken,
        LoggedIn
    }

    static {
        dontLoginToChat = false;
        loginInformation = new LoginInformation();
        s_cookiesConfiguration = new HashMap();
        s_bCookiesAreOutOfDate = false;
        s_strLanguage = "";
        s_lLastWGTokenFailureMS = 0;
    }

    public static boolean isLoggedIn() {
        return loginInformation.loginState == LoginState.LoggedIn;
    }

    public static String getLoginSteamID() {
        return loginInformation.steamId;
    }

    public static String getAccessToken() {
        return loginInformation.accessToken;
    }

    public static void logOut() {
        //SteamCommunityApplication.GetInstance().GetDiskCacheIndefinite().Delete("login.json");
        loginInformation.LogOut();
        setLoginInformation(loginInformation);
    }

    public static LoginInformation getLoginInformation() {
        return loginInformation;
    }

    public static void setLoginInformation(LoginInformation loginInfo) {
        String sDOBcookie = null;
        loginInformation = loginInfo;
        resetAllCookies();
        setCookie2("steamLogin", loginInformation.steamId + "||" + loginInformation.wgtoken, false, true);
        setCookie2("steamLoginSecure", loginInformation.steamId + "||" + loginInformation.wgtokenSecure, true, true);
        SettingInfo settingInfo = SteamCommunityApplication.GetInstance().GetSettingInfoDB().m_settingDOB;
        String sDOBvalue = settingInfo == null ? null : settingInfo.getValue(SteamCommunityApplication.GetInstance().getApplicationContext());
        if (!(sDOBvalue == null || sDOBvalue.equals(""))) {
            sDOBcookie = SettingInfo.DateConverter.makeUnixTime(sDOBvalue);
        }
        setCookie2("dob", sDOBcookie);
        syncAllCookies();
    }

    public static void updateWGToken(String wgtoken, String wgtoken_secure) {
        loginInformation.wgtoken = wgtoken;
        loginInformation.wgtokenSecure = wgtoken_secure;
        setCookie2("steamLogin", loginInformation.steamId + "||" + loginInformation.wgtoken, false, true);
        setCookie2("steamLoginSecure", loginInformation.steamId + "||" + loginInformation.wgtokenSecure, true, true);
        SteamCommunityApplication.GetInstance().UpdateCachedLoginInformation();
        s_bCookiesAreOutOfDate = true;
        syncAllCookies();
    }

    public static void setCookie2(String cookieName, String cookieValue) {
        setCookie2(cookieName, cookieValue, false, false);
    }

    public static void setCookie2(String cookieName, String cookieValue, boolean secure, boolean httponly) {
        String url = Config.URL_COMMUNITY_BASE;
        int iProtocol = url.indexOf("://", 0);
        int iSubDir = url.indexOf("/", iProtocol + 3);
        (iSubDir < 0 ? url : url.substring(0, iSubDir)).substring(iProtocol + 3);
        url = "";
        if (!s_cookiesConfiguration.containsKey(url)) {
            s_cookiesConfiguration.put(url, new HashMap());
        }
        CWebCookie Cookie = new CWebCookie(cookieValue, secure, httponly);
        Map<String, CWebCookie> urlCookies = (Map) s_cookiesConfiguration.get(url);
        if (urlCookies.containsKey(cookieName)) {
            if (cookieValue != null) {
                CWebCookie OldCookie = (CWebCookie) urlCookies.get(cookieName);
                if (OldCookie == null || !Cookie.equals(OldCookie)) {
                    s_bCookiesAreOutOfDate = true;
                }
            } else if (urlCookies.get(cookieName) != null) {
                s_bCookiesAreOutOfDate = true;
            }
            urlCookies.remove(cookieName);
            urlCookies.put(cookieName, Cookie);
            return;
        }
        if (cookieValue != null) {
            s_bCookiesAreOutOfDate = true;
        }
        urlCookies.put(cookieName, Cookie);
    }

    public static void resetAllCookies() {
        s_cookiesConfiguration.clear();
        setCookie2("mobileClient", "android");
        setCookie2("mobileClientVersion", "" + Config.APP_VERSION_ID + " (" + Config.APP_VERSION + ")");
        s_strLanguage = "russian";
        setCookie2("Steam_Language", s_strLanguage);
    }

    public static void updateLanguage() {
        String strSelectedLanguage = "russian";
        if (!s_strLanguage.equals(strSelectedLanguage)) {
            s_strLanguage = strSelectedLanguage;
            setCookie2("Steam_Language", strSelectedLanguage);
            syncAllCookies();
        }
    }

    public static void syncAllCookies() {
        if (s_bCookiesAreOutOfDate) {
            s_bCookiesAreOutOfDate = false;
            CookieManager cm = CookieManager.getInstance();
            for (Entry<String, Map<String, CWebCookie>> urlEntry : s_cookiesConfiguration.entrySet()) {
                for (Entry<String, CWebCookie> entry : ((Map) urlEntry.getValue()).entrySet()) {
                    CWebCookie cookie = (CWebCookie) entry.getValue();
                    try {
                        String sEncodedValue = cookie.m_value != null ? URLEncoder.encode(cookie.m_value, "ISO-8859-1") : "";
                        StringBuilder cookieString = new StringBuilder();
                        cookieString.append((String) entry.getKey());
                        cookieString.append("=");
                        cookieString.append(sEncodedValue);
                        if (cookie.m_secure) {
                            cookieString.append("; secure");
                        }
                        if (cookie.m_httponly) {
                            cookieString.append("; HttpOnly");
                        }
                        for (String url : Config.WG_AUTH_DOMAINS) {
                            cm.setCookie(url, cookieString.toString());
                        }
                    } catch (UnsupportedEncodingException e) {
                    }
                }
            }
            CookieSyncManager.getInstance().sync();
        }
    }

    public static boolean dontLoginToChat() {
        return dontLoginToChat;
    }

    public static void setDontLoginToChat(boolean dontLoginToChat) {
        if (dontLoginToChat != dontLoginToChat) {
            dontLoginToChat = dontLoginToChat;
        }
    }

    public static void reacquireWGTokenFromServer(SteamWebView steamWebView) {
        if (!isLoggedIn()) {
            return;
        }
        if (s_lLastWGTokenFailureMS <= 0 || s_lLastWGTokenFailureMS + 5000 <= System.currentTimeMillis()) {
            s_lLastWGTokenFailureMS = System.currentTimeMillis();
            RequestBuilder requestBuilder = Endpoints.getWGToken();
            requestBuilder.setResponseListener(new C01461());
            SteamCommunityApplication.GetInstance().sendRequest(requestBuilder);
        }
    }
}
