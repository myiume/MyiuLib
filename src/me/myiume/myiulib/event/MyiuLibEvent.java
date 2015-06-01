package me.myiume.myiulib.event;

import me.myiume.myiulib.MGUtil;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * The base event type for the MyiuLib.
 */
public class MyiuLibEvent extends Event {

	private static HandlerList handlers = new HandlerList();

	protected String plugin;

	/**
	 * Creates a new instance of this event.
	 *
	 * @param plugin the name of the plugin involved in this {@link MyiuLibEvent}
	 * @since 0.1.0
	 */
	public MyiuLibEvent(String plugin) {
		this.plugin = plugin;
	}

	/**
	 * Retrieves the name of the plugin involved in this {@link MyiuLibEvent}.
	 *
	 * @return the name of the plugin involved in this {@link MyiuLibEvent}
	 * @since 0.1.0
	 */
	public String getPlugin() {
		return plugin;
	}

	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	/**
	 * Unsets all static objects in this class.
	 *
	 * @throws UnsupportedOperationException if MyiuLib is not currently disabling
	 *
	 * @since 0.1.0
	 */
	public static void uninitialize() {
		MGUtil.verifyDisablingStatus();
		handlers = null;
	}

}
