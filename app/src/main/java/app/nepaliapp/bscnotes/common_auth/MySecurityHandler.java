package app.nepaliapp.bscnotes.common_auth;

import androidx.annotation.NonNull;
import org.json.JSONObject;
import com.aheaditec.talsec_security.security.api.SuspiciousAppInfo;
import com.aheaditec.talsec_security.security.api.ThreatListener;

import java.util.List;

public class MySecurityHandler implements ThreatListener.ThreatDetected {

    // Boolean flags for data collection
    public static boolean isRooted = false;
    public static boolean isDebuggerPresent = false;
    public static boolean isEmulator = false;
    public static boolean isTampered = false;
    public static boolean isHooked = false;
    public static boolean isMalwarePresent = false;
    public static boolean isUntrustedSource = false;

    // Callbacks update the flags
    @Override
    public void onRootDetected()
    { isRooted = true; }
    @Override public void onDebuggerDetected()
    { isDebuggerPresent = true; }
    @Override public void onEmulatorDetected()
    { isEmulator = true; }
    @Override public void onTamperDetected()
    { isTampered = true; }
    @Override public void onHookDetected()
    { isHooked = true; }

    @Override
    public void onDeviceBindingDetected() {

    }

    @Override
    public void onObfuscationIssuesDetected() {

    }

    @Override public void onMalwareDetected(@NonNull List<SuspiciousAppInfo> list)
    { isMalwarePresent = true; }

    @Override
    public void onScreenshotDetected() {

    }

    @Override
    public void onScreenRecordingDetected() {

    }

    @Override
    public void onMultiInstanceDetected() {

    }

    @Override
    public void onUnsecureWifiDetected() {

    }

    @Override
    public void onTimeSpoofingDetected() {

    }

    @Override
    public void onLocationSpoofingDetected() {

    }

    @Override public void onUntrustedInstallationSourceDetected()
    { isUntrustedSource = true; }

    public static JSONObject getSecurityJson() {
        JSONObject json = new JSONObject();
        try {
            // Security Data (using short keys for stealth)
            json.put("r", isRooted);       // rooted
            json.put("d", isDebuggerPresent); // debugger
            json.put("e", isEmulator);     // emulator
            json.put("t", isTampered);     // tampered
            json.put("h", isHooked);       // hooked
            json.put("m", isMalwarePresent); // malware
            json.put("u", isUntrustedSource); // untrusted


            // Device Data (No permissions required)
            json.put("model", android.os.Build.MODEL);
            json.put("brand", android.os.Build.MANUFACTURER);

        } catch (Exception e) {
            // "Play Safe" - Don't crash if JSON fails
        }
        return json;
    }

    // ... Implement other mandatory empty methods ...
}
