package me.myiume.myiulib;

import me.myiume.myiulib.api.Locale;
import me.myiume.myiulib.api.LogLevel;
import me.myiume.myiulib.api.Minigame;
import me.myiume.myiulib.api.Round;
import me.myiume.myiulib.event.MyiuLibEvent;
import me.myiume.myiulib.util.NmsUtil;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * MyiuLib's primary (central) class.
 *
 * @version 0.4.0
 * @since 0.1.0
 */
public class MyiuLib extends JavaPlugin {

	/**
	 * MyiuLib's logger.
	 *
	 * <p><strong>This is for use within the library; please do not use this in
	 * your plugin or you'll confuse the server owner.</strong></p>
	 *
	 * @since 0.1.0
	 */
	public static Logger log;

	/**
	 * Whether block changes should be logged immediately.
	 */
	public static boolean IMMEDIATE_LOGGING;

	/**
	 * The minimum level at which messages should be logged.
	 */
	public static LogLevel LOGGING_LEVEL;

	/**
	 * Whether vanilla spectating is globally disabled.
	 */
	private static boolean VANILLA_SPECTATING_DISABLED;

	/**
	 * The locale for MyiuLib itself.
	 */
	public static Locale locale;

	private static boolean disabling = false;

	/**
	 * Standard {@link JavaPlugin#onEnable()} override.
	 *
	 * @since 0.1.0
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void onEnable() {

		MGUtil.plugin = this;
		log = getLogger();
		Bukkit.getPluginManager().registerEvents(new MGListener(), this);
		saveDefaultConfig();
		IMMEDIATE_LOGGING = getConfig().getBoolean("immediate-logging");
		LOGGING_LEVEL = LogLevel.valueOf(getConfig().getString("logging-level").toUpperCase());
		if (LOGGING_LEVEL == null) {
			LOGGING_LEVEL = LogLevel.WARNING;
			MyiuLib.log("The configured logging level is invalid!", LogLevel.WARNING);
		}
		VANILLA_SPECTATING_DISABLED = getConfig().getBoolean("disable-vanilla-spectating");

		locale = new Locale("MyiuLib");
		locale.initialize();

		if (this.getDescription().getVersion().contains("dev")) {
			log.warning(locale.getMessage("plugin.alert.dev-build"));
		}

		// store UUIDs of online players
		List<String> names = new ArrayList<String>();
		for (Player pl : NmsUtil.getOnlinePlayers()) {
			names.add(pl.getName());
		}
		try {
			new UUIDFetcher(names).call();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			MyiuLib.log.severe(locale.getMessage("plugin.alert.uuid-fail"));
		}

		log.info(locale.getMessage("plugin.event.enable", this.toString()));
	}

	/**
	 * Standard {@link JavaPlugin#onDisable()} override.
	 *
	 * @since 0.1.0
	 */
	@Override
	public void onDisable() {
		disabling = true;
		Bukkit.broadcastMessage(ChatColor.DARK_PURPLE + "[MyiuLib] " + locale.getMessage("plugin.event.restart"));
		for (Minigame mg : Minigame.getMinigameInstances()) {
			for (Round r : mg.getRoundList()) {
				r.end(false);
			}
		}
		Minigame.uninitialize();
		MyiuLibEvent.uninitialize();
		NmsUtil.uninitialize();
		UUIDFetcher.uninitialize();
		log.info(locale.getMessage("plugin.event.disable", this.toString()));
		MyiuLib.uninitialize();
	}

	/**
	 * <p>This method should not be called from your plugin. So don't use it.
	 * Please.</p>
	 *
	 * @param plugin the name of the plugin to register worlds for
	 */
	public static void registerWorlds(String plugin) {
		MGListener.addWorlds(plugin);
	}

	private static void uninitialize() {
		log = null;
		MGUtil.plugin = null;
	}

	/**
	 * Internal convenience method for logging. <strong>Please do not call this
	 * from your plugin.</strong>
	 *
	 * @param message the message to log.
	 * @param level   the {@link LogLevel level} at which to log the message
	 * @since 0.3.0
	 */
	public static void log(String message, LogLevel level) {
		MGUtil.log(message, "MyiuLib", level);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (label.equalsIgnoreCase("mglib")) {
			sender.sendMessage(ChatColor.LIGHT_PURPLE +
					locale.getMessage("plugin.event.info", getDescription().getVersion(), "Myiume"));
			return true;
		}
		return false;
	}

	/**
	 * Retrieves worlds registered with MyiuLib's event listener for the given
	 * plugin.
	 *
	 * @param plugin the plugin to retrieve worlds for
	 * @return worlds registered with MyiuLib's event listener for the given
	 * plugin
	 * @since 0.4.0
	 */
	public static List<String> getWorlds(String plugin) {
		return MGListener.getWorlds();
	}

	/**
	 * Retrieves a hashmap mapping the names of online players to their
	 * respective UUIDs.
	 *
	 * @return a hashmap mapping the names of online players to their
	 * respective UUIDs
	 * @since 0.3.0
	 */
	public static HashMap<String, UUID> getOnlineUUIDs() {
		return UUIDFetcher.uuids;
	}

	/**
	 * Retrieves whether vanilla spectating has been globally disabled by
	 * MyiuLib's config.yml file.
	 *
	 * @return whether vanilla spectating has been globally disabled by MyiuLib's
	 * config.yml file
	 * @since 0.3.0
	 */
	public static boolean isVanillaSpectatingDisabled() {
		return VANILLA_SPECTATING_DISABLED;
	}

	/**
	 * Determines whether MyiuLib is in the process of disabling.
	 * This is to provide security when unsetting static objects.
	 *
	 * @return whether MyiuLib is in the process of disabling.
	 * @since 0.4.0
	 */
	public static boolean isDisabling() {
		return disabling;
	}

}
