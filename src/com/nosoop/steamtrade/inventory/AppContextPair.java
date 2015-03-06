package com.nosoop.steamtrade.inventory;

/**
 * Represents an integer/long id pair to identify a player's inventory.
 * Optionally allows a name.
 *
 * @author nosoop
 */
public class AppContextPair {

	int appid;
	long contextid;
	String name;

	/**
	 * Creates an app-context pair.
	 *
	 * @param appid
	 * @param contextid
	 */
	public AppContextPair(int appid, long contextid) {
		this.appid = appid;
		this.contextid = contextid;
		this.name = null;
	}

	/**
	 * Creates an app-context pair with a name.
	 *
	 * @param appid
	 * @param contextid
	 * @param name
	 */
	public AppContextPair(int appid, long contextid, String name) {
		this(appid, contextid);
		this.name = name;
	}

	/**
	 * @return Integer containing the game's inventory-specific contextid.
	 */
	public long getContextid() {
		return contextid;
	}

	/**
	 * @return Integer containing the game-specific appid.
	 */
	public int getAppid() {
		return appid;
	}

	/**
	 * @return Name of the AppContextPair, or null if there is none.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns a String representing the AppContextPair object.
	 *
	 * @return String with the game name (ex: "Team Fortress 2"). If null,
	 * returns a generic identifier (ex: "[APPCONTEXT] 440_2") instead.
	 */
	@Override
	public String toString() {
		if (name != null) {
			return name;
		} else {
			return String.format("[APPCONTEXT] %d_%d", appid, contextid);
		}
	}

	@Override
	public int hashCode() {
		return 38 * appid + Long.valueOf(contextid).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final AppContextPair other = (AppContextPair) obj;
		if (this.appid != other.appid) {
			return false;
		}
		if (this.contextid != other.contextid) {
			return false;
		}
		return true;
	}
}
