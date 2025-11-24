package net.eqozqq.mcpeproxy;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ServerStorage {
    private static final String PREFS_NAME = "ServerListPrefs";
    private static final String PREF_SERVER_LIST = "serverList";

    public static List<Server> loadServers(Context context) {
        List<Server> servers = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String jsonList = prefs.getString(PREF_SERVER_LIST, "[]");

        try {
            JSONArray jsonArray = new JSONArray(jsonList);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String address = jsonObject.getString("address");
                int port = jsonObject.getInt("port");
                servers.add(new Server(address, port));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return servers;
    }

    public static void saveServers(Context context, List<Server> servers) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        JSONArray jsonArray = new JSONArray();

        try {
            for (Server server : servers) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("address", server.getAddress());
                jsonObject.put("port", server.getPort());
                jsonArray.put(jsonObject);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        editor.putString(PREF_SERVER_LIST, jsonArray.toString());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            editor.apply();
        } else {
            editor.commit();
        }
    }
}