package app.nepaliapp.bscnotes.common_auth;

import static android.content.Context.TELEPHONY_SERVICE;

import android.content.Context;
import android.telephony.TelephonyManager;

import org.json.JSONException;
import org.json.JSONObject;

import app.nepaliapp.bscnotes.common.CommonFunctions;


public class CommonSecurityClass {
    Context context;

    public CommonSecurityClass(Context context) {
        // Guard against memory leaks by saving the global application context
        this.context = context.getApplicationContext();
    }

    public JSONObject getSecurityWithVPNDataJSON(){
        JSONObject object= MySecurityHandler.getSecurityJson();
        TelephonyManager tm = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);

        try {
            String simCountry = tm != null ? tm.getSimCountryIso() : "";
            String simNetwork = tm != null ? tm.getNetworkCountryIso() : "";
            object.put("sm", simCountry);
            object.put("smn", simNetwork);
            object.put("v", SystemChecks.checkUpVPN(context));
            object.put("NetworkType", SystemChecks.getNetworkType(context));
            object.put("deviceId", CommonFunctions.getUniqueId(context));
            object.put("product", android.os.Build.PRODUCT);   // Returns "sdk_gphone64_arm64" on emulators
            object.put("hardware", android.os.Build.HARDWARE); // Returns "ranchu" or "goldfish" on emulators

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return object;
    }
}
