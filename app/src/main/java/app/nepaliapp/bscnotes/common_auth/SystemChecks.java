package app.nepaliapp.bscnotes.common_auth;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

public class SystemChecks {



    public static String checkUpVPN(Context context) {

        boolean vpnDetected =
                isVpnInterfaceActive()
                        || isVpnTransportActive(context);

        if (vpnDetected) {
            return "VD";//VPN DETECTED
        }

        return "NC"; //NETWORK_CLEAN
    }


    public static String getNetworkType(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return "UNKNOWN";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            if (caps == null) return "OFFLINE";

            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "WIFI";
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "MOBILE_DATA";
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "ETHERNET";
        } else {
            // Fallback for older Android versions
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnected()) {
                if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) return "WIFI";
                if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) return "MOBILE_DATA";
            }
        }
        return "UNKNOWN";
    }






    private static boolean isVpnInterfaceActive() {
        try {
            Enumeration<NetworkInterface> interfaces =
                    NetworkInterface.getNetworkInterfaces();

            if (interfaces == null) return false;

            for (NetworkInterface nif : Collections.list(interfaces)) {

                if (!nif.isUp() || nif.isLoopback()) {
                    continue;
                }

                String name = nif.getName().toLowerCase();

                for (String vpn : VPN_INTERFACES) {
                    if (name.contains(vpn)) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) { }

        return false;
    }

    private static boolean isVpnTransportActive(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false;

        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(
                        Context.CONNECTIVITY_SERVICE);

        if (cm == null) return false;

        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        if (caps == null) return false;

        return caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
    }

//    private static boolean suspiciousIpFound() {
//        try {
//            Enumeration<NetworkInterface> interfaces =
//                    NetworkInterface.getNetworkInterfaces();
//
//            while (interfaces.hasMoreElements()) {
//                NetworkInterface nif = interfaces.nextElement();
//
//                Enumeration<InetAddress> addresses = nif.getInetAddresses();
//                while (addresses.hasMoreElements()) {
//                    InetAddress addr = addresses.nextElement();
//
//                    if (addr.isLoopbackAddress()
//                            || addr.isSiteLocalAddress()) {
//                        continue;
//                    }
//
//                    return true;
//                }
//            }
//        } catch (Exception ignored) { }
//
//        return false;
//    }

    private static final String[] VPN_INTERFACES = {
            "tun",
            "ppp",
            "wg",
            "ipsec",
            "tap"
    };
}
