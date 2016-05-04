package ru.steam;

import java.text.DateFormat;
import java.util.Calendar;

public class SettingInfo {
    public AccessRight m_access;
    public String m_defaultValue;
    public Object m_extraData;
    public String m_key;
    public UpdateListener m_pUpdateListener;
    public int m_resid;
    public int m_resid_detailed;
    public SettingType m_type;

    public enum AccessRight {
        NONE,
        VALID_ACCOUNT,
        USER,
        CODE
    }


    public static class DateConverter {
        public static Calendar makeCalendar(String value) {
            Calendar myDOB = Calendar.getInstance();
            if (!(value == null || value.equals(""))) {
                int intValue = Integer.valueOf(value).intValue();
                myDOB.set(intValue / 10000, (intValue - ((intValue / 10000) * 10000)) / 100, intValue % 100);
            }
            return myDOB;
        }

        public static String formatDate(String value) {
            return DateFormat.getDateInstance(0).format(makeCalendar(value).getTime());
        }

        public static String makeValue(int year, int monthOfYear, int dayOfMonth) {
            return "" + (((year * 10000) + (monthOfYear * 100)) + dayOfMonth);
        }

        public static String makeUnixTime(String value) {
            return "" + (makeCalendar(value).getTimeInMillis() / 1000);
        }
    }

    public static class RadioSelectorItem {
        public int resid_text;
        public int value;
    }

    public enum SettingType {
        SECTION,
        INFO,
        CHECK,
        DATE,
        URI,
        MARKET,
        UNREADMSG,
        RADIOSELECTOR,
        RINGTONESELECTOR
    }

    public static class Transaction {
        private boolean m_bCookiesMarkedForSync;



        public Transaction(Context context) {
            this.m_bCookiesMarkedForSync = false;
            String steamID = LoggedInUserAccountInfo.getLoginSteamID();
        }

        public void setValue(SettingInfo setting, String value) {
//            if (this.m_editor != null) {
//                this.m_editor.putString(setting.m_key, value);
//            }
            if (setting.m_pUpdateListener != null) {
                setting.m_pUpdateListener.OnSettingInfoValueUpdate(setting, value, this);
            }
        }

        public void commit() {
            if (this.m_bCookiesMarkedForSync) {
                LoggedInUserAccountInfo.syncAllCookies();
                this.m_bCookiesMarkedForSync = false;
            }
        }

        public void markCookiesForSync() {
            this.m_bCookiesMarkedForSync = true;
        }
    }

    public interface UpdateListener {
        void OnSettingInfoValueUpdate(SettingInfo settingInfo, String str, Transaction transaction);
    }

    public SettingInfo() {
        this.m_resid = 0;
        this.m_resid_detailed = 0;
        this.m_key = null;
        this.m_type = SettingType.SECTION;
        this.m_access = AccessRight.NONE;
        this.m_defaultValue = null;
        this.m_extraData = null;
//        this.m_pUpdateListener = null;
    }

    public String getValue(Context context) {
        String steamID = LoggedInUserAccountInfo.getLoginSteamID();
        if (steamID == null || steamID.equals("")) {
            return this.m_defaultValue;
        }
        return context.getSharedPreferences("steam.settings." + steamID, 0).getString(this.m_key, this.m_defaultValue);
    }

    public boolean getBooleanValue(Context context) {
        String val = getValue(context);
        return (val == null || val.equals("")) ? false : true;
    }

    public int getIntegerValue(Context context) {
        try {
            return Integer.parseInt(getValue(context));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public RadioSelectorItem getRadioSelectorItemValue(Context context) {
        RadioSelectorItem[] radios = null;
        try {
            radios = (RadioSelectorItem[]) this.m_extraData;
        } catch (Exception e) {
        }
        if (radios == null) {
            return null;
        }
        RadioSelectorItem res;
        try {
            res = findRadioSelectorItemByValue(Integer.valueOf(getValue(context)).intValue(), radios);
            if (res != null) {
                return res;
            }
        } catch (Exception e2) {
        }
        try {
            res = findRadioSelectorItemByValue(Integer.valueOf(this.m_defaultValue).intValue(), radios);
            if (res != null) {
                return res;
            }
        } catch (Exception e3) {
        }
        try {
            return radios[0];
        } catch (Exception e4) {
            return null;
        }
    }

    public void setValueAndCommit(Context context, String value) {
        Transaction tr = new Transaction(context);
        tr.setValue(this, value);
        tr.commit();
    }

    public static RadioSelectorItem findRadioSelectorItemByValue(int value, RadioSelectorItem[] radios) {
        for (RadioSelectorItem radio : radios) {
            if (radio.value == value) {
                return radio;
            }
        }
        return null;
    }
}
