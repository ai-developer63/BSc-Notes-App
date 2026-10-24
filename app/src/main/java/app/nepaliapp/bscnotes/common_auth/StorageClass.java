package app.nepaliapp.bscnotes.common_auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class StorageClass {

    private static final String SECURE_PREFS = "secure_prefs";
    private final SharedPreferences securePrefs;

    public StorageClass(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            securePrefs = EncryptedSharedPreferences.create(
                    context,
                    SECURE_PREFS,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encrypted storage", e);
        }
    }

    /* ===================== JWT TOKEN ===================== */

    public void updateJwtToken(String jwtToken) {
        securePrefs.edit()
                .putString("jwtToken", jwtToken)
                .apply();
    }

    public String getJwtToken() {
        return securePrefs.getString("jwtToken", null);
    }

    /* ===================== REFRESH TOKEN ===================== */

    public void updateRefreshToken(String refreshToken) {
        securePrefs.edit()
                .putString("refreshToken", refreshToken)
                .commit();
    }

    public String getRefreshToken() {
        return securePrefs.getString("refreshToken", null);
    }

    /* ===================== DEVICE ID ===================== */

    public void updateDeviceUniqueID(String deviceId) {
        securePrefs.edit()
                .putString("deviceId", deviceId)
                .apply();
    }

    public String getDeviceUniqueID() {
        return securePrefs.getString("deviceId", null);
    }



    /* ===================== PublicId TOKEN ===================== */

    public void updateUserPublicId(String publicId) {
        securePrefs.edit()
                .putString("publicId", publicId)
                .commit();
    }

    public String getUserPublicId() {
        return securePrefs.getString("publicId", null);
    }

    /* ===================== CLEAR (LOGOUT) ===================== */



    public void clearAll() {
        securePrefs.edit().clear().apply();
    }

    public void logout() {
        SharedPreferences.Editor editor = securePrefs.edit();

        // Map of all keys currently in the storage
        java.util.Map<String, ?> allEntries = securePrefs.getAll();

        for (String key : allEntries.keySet()) {
            // Remove everything EXCEPT the device ID
            if (!key.equals("deviceId")) {
                editor.remove(key);
            }
        }

        editor.apply();
    }
}