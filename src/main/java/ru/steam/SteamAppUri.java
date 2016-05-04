package ru.steam;

import ru.steam.config.Config;

import java.net.URI;
import java.net.URL;

public class SteamAppUri {
    public static final String CART;
    public static final String CONFIRMATION_WEB;
    public static final String STEAMGUARD_CHANGE;
    public static final String STEAMGUARD_HELP;
    public static final String STEAMGUARD_PRECHANGE;
    public static final String STEAMGUARD_RCODE;
    public static final String STEAM_NEWS;
    public static final String STEAM_NOTIFICATIONS_SETTINGS;
    public static final String URL_CURRENT_USER_PROFILE_BASE_CUSTOMURL;
    public static final String URL_CURRENT_USER_PROFILE_BASE_GENERIC;
    public static final String URL_CURRENT_USER_PROFILE_BASE_STEAMID;

    static {
        URL_CURRENT_USER_PROFILE_BASE_GENERIC = Config.URL_COMMUNITY_BASE + "/my";
        URL_CURRENT_USER_PROFILE_BASE_STEAMID = Config.URL_COMMUNITY_BASE_INSECURE + "/profiles";
        URL_CURRENT_USER_PROFILE_BASE_CUSTOMURL = Config.URL_COMMUNITY_BASE_INSECURE + "/id";
        CART = Config.URL_STORE_BASE_INSECURE + "/cart/";
        STEAM_NEWS = Config.URL_STORE_BASE_INSECURE + "/news/";
        STEAMGUARD_HELP = Config.URL_COMMUNITY_BASE + "/steamguard/help";
        STEAMGUARD_CHANGE = Config.URL_COMMUNITY_BASE + "/steamguard/change";
        STEAMGUARD_RCODE = Config.URL_COMMUNITY_BASE + "/steamguard/twofactor_recoverycode?countdown=0";
        STEAMGUARD_PRECHANGE = Config.URL_COMMUNITY_BASE + "/steamguard/prechange";
        CONFIRMATION_WEB = Config.URL_COMMUNITY_BASE + "/mobileconf/conf";
        STEAM_NOTIFICATIONS_SETTINGS = Config.URL_COMMUNITY_BASE_INSECURE + "/mobilesettings/GetManifest/v0001";
    }

    public static URI createChatUri(String steamId) {
        return URI.create("steammobile://chat?steamid=" + steamId);
    }

    public static URI createCurrentUserProfileUri(String path) {
        if (LoggedInUserAccountInfo.isLoggedIn()) {
            return createSteamAppWebUri(URL_CURRENT_USER_PROFILE_BASE_STEAMID + "/" + LoggedInUserAccountInfo.getLoginSteamID() + path);
        }
        return createSteamAppWebUri(URL_CURRENT_USER_PROFILE_BASE_GENERIC + path);
    }

    public static URI createFriendsSearchUri(String query) {
        return createSearchUri(query, "friends");
    }

    public static URI createGroupsSearchUri(String query) {
        return createSearchUri(query, "groups");
    }

    public static URI library() {
        return createCurrentUserProfileUri("/games/?tab=all");
    }

    public static URI settings() {
        return createUri("steammobile://", "appsettings");
    }

    private static URI createSearchUri(String query, String resourceString) {
        return URI.create("steammobile://" + resourceString + "?" + "search" + "=" + query);
    }

    public static URI groupWebPage(String groupProfileUrl) {
        return createSteamAppWebUri(Config.URL_COMMUNITY_BASE_INSECURE + groupProfileUrl);
    }

    public static URI wishlist() {
        return createCurrentUserProfileUri("/wishlist/");
    }

    public static URI shoppingCart() {
        return createSteamAppWebUri(CART);
    }

    public static URI steamNews() {
        return createSteamAppWebUri(STEAM_NEWS);
    }

    public static URI friendActivity() {
        return createCurrentUserProfileUri("/home/");
    }

    public static URI createVisitProfileUri(String steamId) {
        return createSteamAppWebUri(Config.URL_COMMUNITY_BASE_INSECURE + "/profiles/" + steamId);
    }

    public static URI createSteamAppWebUri(String url) {
        return URI.create("steammobile://openurl?url=" + url);
    }

    public static URI searchSteam() {
        return createSteamAppWebUri(Config.URL_STORE_BASE_INSECURE + "/search/");
    }

    public static URI catalog() {
        return createSteamAppWebUri(Config.URL_STORE_BASE_INSECURE);
    }

    public static URI accountDetails() {
        return createSteamAppWebUri(Config.URL_STORE_BASE + "/account/");
    }

    public static URI steamGuard() {
        return createUri("steammobile://", "steamguard");
    }

    public static URI friendsList() {
        return createUri("steammobile://", "friends");
    }

    public static URI groupsList() {
        return createUri("steammobile://", "groups");
    }

    public static URI confirmationResource() {
        return createUri("steammobile://", "confirmation");
    }

    private static URI createUri(String scheme, String resource) {
        return URI.create(scheme + resource);
    }

    public static URI login() {
        return createUri("steammobile://", "login");
    }

    public static URI deleteNotification() {
        return createUri("steammobile://", "deletenotification");
    }

    public static URI notificationComments() {
        return createCurrentUserProfileUri("/commentnotifications");
    }

    public static URI notificationItems() {
        return createCurrentUserProfileUri("/inventory");
    }

    public static URI notificationInvites() {
        return createCurrentUserProfileUri("/home/invites");
    }

    public static URI notificationGifts() {
        return createCurrentUserProfileUri("/inventory#pending_gifts");
    }

    public static URI notificationTradeOffers() {
        return createCurrentUserProfileUri("/tradeoffers");
    }

    public static URI notificationAsyncGame() {
        return createCurrentUserProfileUri("/gamenotifications");
    }

    public static URI notificationModeratorMessage() {
        return createCurrentUserProfileUri("/moderatormessages");
    }

    public static String steamHelpUriPrefix() {
        return Config.URL_COMMUNITY_BASE + "/mobilelogin/help";
    }

    public static String steamSubscriberAgreementUriPrefix() {
        return Config.URL_STORE_BASE + "/mobilecheckout/ssapopup";
    }

    public static String steamPrivacyPolicyUriPrefix() {
        return Config.URL_STORE_BASE + "/mobilelogin/privacy_agreement";
    }
}
