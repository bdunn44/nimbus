package com.kbdunn.nimbus.desktop.sync.data;

import java.util.Map;
import java.util.prefs.Preferences;

import com.kbdunn.nimbus.desktop.model.SyncCredentials;

public class SyncPreferences {
	
	private static final String PREF_NODE = "/nimbus/sync";
	private static Preferences prefs;
	
	static {
		prefs = Preferences.userRoot().node(PREF_NODE);
	}
	
	private enum Key {
		CREDENTIALS,
		URL,
		NODE_NAME,
	}
	
	private static String getPreference(Key key) {
		return prefs.get(key.toString().toLowerCase(), "");
	}
	
	private static void setPreference(Key key, String value) {
		synchronized(prefs) {
			prefs.put(key.toString().toLowerCase(), value);
		}
	}
	
	public static SyncCredentials getCredentials() {
		return SyncCredentials.fromCompositeString(getPreference(Key.CREDENTIALS));
	}
	
	public static void setCredentials(SyncCredentials credentials) {
		setPreference(Key.CREDENTIALS, credentials.getCompositeString());
	}
	
	public static String getUrl() {
		return getPreference(Key.URL);
	}
	
	public static void setUrl(String url) {
		setPreference(Key.URL, url);
	}
	
	public static String getNodeName() {
		if (getPreference(Key.NODE_NAME) == null) 
			return getDefaultNodeName();
		return getPreference(Key.NODE_NAME);
	}
	
	public static String getDefaultNodeName() {
	    Map<String, String> env = System.getenv();
	    if (env.containsKey("COMPUTERNAME"))
	        return env.get("COMPUTERNAME");
	    else if (env.containsKey("HOSTNAME"))
	        return env.get("HOSTNAME");
	    else
	        return "Nimbus Sync Client";
	}
	
	public static void setNodeName(String nodeName) {
		if (nodeName == null || nodeName.isEmpty()) {
			nodeName = getDefaultNodeName();
		}
		setPreference(Key.NODE_NAME, nodeName);
	}
}
