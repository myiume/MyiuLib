package me.myiume.myiulib.api;

import me.myiume.myiulib.LobbyManager;
import me.myiume.myiulib.MGUtil;
import me.myiume.myiulib.MyiuLib;
import me.myiume.myiulib.RollbackManager;
import me.myiume.myiulib.exception.ArenaExistsException;
import me.myiume.myiulib.exception.InvalidLocationException;
import me.myiume.myiulib.exception.NoSuchArenaException;

import com.google.common.collect.Lists;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * The primary API class. Contains necessary methods to create a minigame plugin
 * from the library.
 *
 * @version 0.4.0
 * @since 0.1.0
 */
public class Minigame {

	private static HashMap<String, Minigame> registeredInstances = new HashMap<String, Minigame>();

	private JavaPlugin plugin;

	private HashMap<String, Round> rounds = new HashMap<String, Round>();

	private ConfigManager configManager;
	private RollbackManager rbManager;
	private LobbyManager lobbyManager;
	private Locale locale;

	HashMap<String, ArenaFactory> arenaFactories = new HashMap<String, ArenaFactory>();

	private static List<String> versions = Arrays.asList("0.1.0", "0.2.0", "0.3.0", "0.3.1", "0.4.0");

	boolean customRoundClass = false;
	boolean customPlayerClass = false;

	private Minigame(final JavaPlugin plugin) {
		if (!registeredInstances.containsKey(plugin.getName())) {
			this.plugin = plugin;
			registeredInstances.put(plugin.getName(), this); // list this instance for use in other parts of the API
		}
		else {
			String authors = "";
			for (String s : plugin.getDescription().getAuthors()) {
				authors += s + ", ";
			}
			if (authors.length() > 0) {
				authors = " (" + authors.substring(0, authors.length() - 2) + ")";
			}
			throw new IllegalStateException(plugin + " attempted to hook into MyiuLib while an instance of the API was " +
					"already registered. This is a bug, and should be reported this to the author(s) of the hooking " +
					"plugin" + authors);
		}
		configManager = new ConfigManager(plugin.getName());
		rbManager = new RollbackManager(plugin); // register rollback manager
		rbManager.checkRollbacks(); // roll back any arenas which were left un-rolled back
		lobbyManager = new LobbyManager(plugin.getName());
		lobbyManager.loadSigns();
		Bukkit.getScheduler().runTask(MGUtil.getPlugin(), new Runnable() {
			public void run() {
				lobbyManager.reset();
			}
		});
		MyiuLib.registerWorlds(plugin.getName()); // registers worlds containing arenas for use with the event listener
		locale = new Locale(plugin.getName());
		Bukkit.getScheduler().runTask(MGUtil.getPlugin(), new Runnable() {
			// add delay so that the plugin has a chance to change its default locale
			//TODO: locale messages are not available in the same method call that constructs this object
			public void run() {
				locale.initialize();
				MyiuLib.log.info(MyiuLib.locale.getMessage("plugin.event.hook", plugin.getName()));
			}
		});
	}

	/**
	 * Registers a plugin with the MyiuLib API.
	 *
	 * @param plugin an instance of your plugin (can be substituted with this if
	 *               called from your main class)
	 * @return a minigame object which may be used for most core API methods,
	 * with the exception of some pertaining exclusively to players or rounds.
	 * @since 0.1.0
	 */
	public static Minigame registerPlugin(JavaPlugin plugin) {
		return new Minigame(plugin);
	}

	/**
	 * Retrieves the {@link JavaPlugin} associated with this {@link Minigame}
	 * instance.
	 *
	 * @return the {@link JavaPlugin} associated with this {@link Minigame}
	 * instance
	 * @deprecated Depends on Bukkit
	 * @since 0.1.0
	 */
	@Deprecated
	public JavaPlugin getPlugin() {
		return plugin;
	}

	/**
	 * Retrieves a {@link List list} of all registered {@link Minigame minigame}
	 * instances.
	 *
	 * @return a {@link List list} of all registered {@link Minigame minigame}
	 * instances
	 * @since 0.1.0
	 */
	public static List<Minigame> getMinigameInstances() {
		return Lists.newArrayList(registeredInstances.values());
	}

	/**
	 * Finds the instance of the MyiuLib API associated with a given plugin.
	 *
	 * @param plugin the name of the plugin to search for
	 * @return the instance of the MyiuLib API (Minigame.class) associated with
	 * the given plugin
	 * @since 0.1.0
	 */
	public static Minigame getMinigameInstance(String plugin) {
		return registeredInstances.get(plugin);
	}

	/**
	 * Finds the instance of the MyiuLib API associated with a given plugin.
	 *
	 * @param plugin the plugin to search for
	 * @return the instance of the MyiuLib API (Minigame.class) associated with
	 * the given plugin
	 * @deprecated Use {@link Minigame#getMinigameInstance(String)}
	 * @since 0.1.0
	 */
	@Deprecated
	public static Minigame getMinigameInstance(JavaPlugin plugin) {
		return getMinigameInstance(plugin.getName());
	}

	/**
	 * Retrieves a hashmap containing all rounds associated with the instance
	 * which registered this API instance.
	 *
	 * @return a hashmap containing all rounds associated with the instance
	 * which registered this API instance
	 * @since 0.1.0
	 */
	public HashMap<String, Round> getRounds() {
		return rounds;
	}

	/**
	 * Retrieves a list containing all rounds associated with the instance which
	 * registered this API instance.
	 *
	 * @return a list containing all rounds associated with the instance which
	 * registered this API instance
	 * @since 0.1.0
	 */
	public List<Round> getRoundList() {
		return Lists.newArrayList(rounds.values());
	}

	/**
	 * Creates and stores a new round with the given parameters.
	 *
	 * @param arena the name of the arena to create the round in
	 * @return the created round
	 * @throws NoSuchArenaException if the given arena does not exist
	 * @since 0.1.0
	 */
	public Round createRound(String arena) throws NoSuchArenaException {
		Round r = null;
		if (this.customRoundClass) {
			try {
				@SuppressWarnings("deprecation")
				Constructor<?> con = getConfigManager().getRoundClass().getDeclaredConstructor(String.class, String.class);
				r = (Round)con.newInstance(plugin.getName(), arena.toLowerCase());
				r.setStage(Stage.WAITING); // default to waiting stage
				rounds.put(arena.toLowerCase(), r); // register arena with MyiuLib
			}
			catch (NoSuchMethodException ex) { // thrown when the required constructor does not exist
				MyiuLib.log.severe(locale.getMessage("plugin.alert.bad-constructor", "Round", plugin.getName()));
				ex.printStackTrace();
			}
			catch (InvocationTargetException ex) { // any exception thrown from the called constructor
				ex.getTargetException().printStackTrace();
			}
			catch (SecurityException ex) { // I have no idea why this would be possible
				ex.printStackTrace();
			}
			catch (InstantiationException ex) { // if this happens then the overriding plugin screwed something up
				MyiuLib.log.severe(locale.getMessage("plugin.alert.bad-constructor", "Round", plugin.getName()));
				ex.printStackTrace();
			}
			catch (IllegalAccessException ex) { // thrown if the called method from the overriding class is not public
				MyiuLib.log.severe(locale.getMessage("plugin.alert.invisible-constructor", "Round", plugin.getName()));
				ex.printStackTrace();
			}
		}
		else {
			r = new Round(plugin.getName(), arena.toLowerCase());
		}
		return r; // give the calling plugin the Round object
	}

	/**
	 * Retrieves the instance of the round associated with the given arena.
	 *
	 * @param name the name of the round to retrieve
	 * @return the instance of the round associated with the given arena, or
	 * null if it does not exist
	 * @since 0.1.0
	 */
	public Round getRound(String name) {
		return rounds.get(name.toLowerCase());
	}

	/**
	 * Creates an arena for use with MyiuLib.
	 *
	 * @param name    the name of the arena (used to identify it)
	 * @param spawn   the initial spawn point of the arena (more may be added
	 *                later)
	 * @param corner1 a corner of the arena
	 * @param corner2 the corner of the arena opposite <code>corner1</code>
	 * @return the new arena's {@link ArenaFactory}
	 * @throws InvalidLocationException if the given locations are not in the
	 *                                  same world
	 * @throws ArenaExistsException     if an arena of the same name already
	 *                                  exists
	 * @since 0.4.0
	 */
	public ArenaFactory createArena(String name, Location3D spawn, Location3D corner1, Location3D corner2)
			throws InvalidLocationException, ArenaExistsException {

		double minX = Double.NaN;
		double minY = Double.NaN;
		double minZ = Double.NaN;
		double maxX = Double.NaN;
		double maxY = Double.NaN;
		double maxZ = Double.NaN;
		double x1;
		double y1;
		double z1;
		double x2;
		double y2;
		double z2;

		if (corner1 != null && corner2 != null) {
			if (!spawn.getWorld().equals(corner1.getWorld())) {
				// spawn's in a different world than the first corner
				throw new InvalidLocationException();
			}
			if (!spawn.getWorld().equals(corner2.getWorld())) {
				// spawn's in a different world than the second corner
				throw new InvalidLocationException();
			}

			x1 = corner1.getX();
			y1 = corner1.getY();
			z1 = corner1.getZ();
			x2 = corner2.getX();
			y2 = corner2.getY();
			z2 = corner2.getZ();

			// this whole bit just determines which coords are the maxes and mins
			if (x1 < x2) {
				minX = x1;
				maxX = x2;
			}
			else {
				minX = x2;
				maxX = x1;
			}
			if (y1 < y2) {
				minY = y1;
				maxY = y2;
			}
			else {
				minY = y2;
				maxY = y1;
			}
			if (z1 < z2) {
				minZ = z1;
				maxZ = z2;
			}
			else {
				minZ = z2;
				maxZ = z1;
			}
		}

		ArenaFactory a = ArenaFactory.createArenaFactory(plugin.getName(), name, spawn.getWorld());
		if (!a.isNewArena()) {
			throw new ArenaExistsException();
		}
		a.addSpawn(spawn);
		if (minX == minX) {
			a.setMinBound(minX, minY, minZ).setMaxBound(maxX, maxY, maxZ);
		}
		return a;
	}

	/**
	 * Creates an arena for use with MyiuLib.
	 *
	 * @param name    the name of the arena (used to identify it)
	 * @param spawn   the initial spawn point of the arena (more may be added
	 *                later)
	 * @param corner1 a corner of the arena
	 * @param corner2 the corner of the arena opposite <code>corner1</code>
	 * @return the new arena's {@link ArenaFactory}
	 * @throws InvalidLocationException if the given locations are not in the
	 *                                  same world
	 * @throws ArenaExistsException     if an arena of the same name already
	 *                                  exists
	 * @deprecated Use
	 * {@link Minigame#createArena(String, Location3D, Location3D, Location3D)}
	 * @since 0.1.0
	 */
	@Deprecated
	public ArenaFactory createArena(String name, Location spawn, Location corner1, Location corner2)
			throws InvalidLocationException, ArenaExistsException {
		return createArena(
				name,
				MGUtil.fromBukkitLocation(spawn),
				MGUtil.fromBukkitLocation(corner1),
				MGUtil.fromBukkitLocation(corner2)
		);
	}

	/**
	 * Creates an arena for use with MyiuLib.
	 *
	 * @param name  the name of the arena (used to identify it)
	 * @param spawn the initial spawn point of the arena (more may be added
	 *              later)
	 * @throws ArenaExistsException if an arena of the same name already exists
	 * @since 0.4.0
	 */
	public void createArena(String name, Location3D spawn) throws ArenaExistsException {
		try {
			createArena(name, spawn, null, null);
		}
		catch (InvalidLocationException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Creates an arena for use with MyiuLib.
	 *
	 * @param name  the name of the arena (used to identify it)
	 * @param spawn the initial spawn point of the arena (more may be added
	 *              later)
	 * @throws ArenaExistsException if an arena of the same name already exists
	 * @deprecated Use {@link Minigame#createArena(String, Location3D)}
	 * @since 0.1.0
	 */
	@Deprecated
	public void createArena(String name, Location spawn) throws ArenaExistsException {
		createArena(name, MGUtil.fromBukkitLocation(spawn));
	}

	/**
	 * Removes an arena from the plugin's config, effectively deleting it.
	 *
	 * @param name the arena to delete
	 * @throws NoSuchArenaException if an arena by the specified name does not
	 *                              exist
	 * @since 0.1.0
	 */
	public void deleteArena(String name) throws NoSuchArenaException {
		YamlConfiguration y = MGUtil.loadArenaYaml(plugin.getName()); // convenience method for loading the YAML file
		if (!y.contains(name)) { // arena doesn't exist
			throw new NoSuchArenaException();
		}
		y.set(name, null); // remove the arena from the arenas.yml file
		MGUtil.saveArenaYaml(plugin.getName(), y);
		Round r = Minigame.getMinigameInstance(plugin.getName()).getRound(name); // get the Round object if it exists
		if (r != null) {
			r.end(); // end the round
			r.destroy(); // get rid of the object (or just its assets)
		}
	}

	/**
	 * Returns the {@link MGPlayer} associated with the given username.
	 *
	 * @param player the username to search for
	 * @return the {@link MGPlayer} associated with the given username, or
	 * <code>null</code> if none is found
	 * @since 0.1.0
	 */
	public MGPlayer getMGPlayer(String player) {
		for (Round r : rounds.values()) { // iterate registered rounds
			if (r.getMGPlayer(player) != null) { // check if the player is in the round
				return r.getMGPlayer(player);
			}
		}
		return null;
	}

	/**
	 * Convenience method for checking if an {@link MGPlayer} is associated with
	 * the given username.
	 *
	 * <p>This method simply checks if {@link Minigame#getMGPlayer(String)
	 * Minigame#getMGPlayer(p)} is <code>null</code>.</p>
	 *
	 * @param p the username to search for
	 * @return Whether an associated {@link MGPlayer} was found
	 * @since 0.1.0
	 */
	public boolean isPlayer(String p) {
		return getMGPlayer(p) != null;
	}

	/**
	 * Retrieves an {@link ArenaFactory} for the arena of the specified name.
	 *
	 * @param arena the name of the arena to retrieve an {@link ArenaFactory}
	 *              for
	 * @return the arena's {@link ArenaFactory}
	 * @throws NoSuchArenaException if the given arena does not exist. In this
	 *                              case, you should instead use {@link
	 *                              ArenaFactory#createArenaFactory(String,
	 *                              String, String)}.
	 * @since 0.1.0
	 */
	public ArenaFactory getArenaFactory(String arena) throws NoSuchArenaException {
		YamlConfiguration y = MGUtil.loadArenaYaml(plugin.getName());
		if (y.isSet(arena + ".world")) {
			return ArenaFactory.createArenaFactory(plugin.getName(), arena,
					MGUtil.loadArenaYaml(plugin.getName()).getString(arena + ".world"));
		}
		throw new NoSuchArenaException();
	}

	/**
	 * Retrieves this minigame's rollback manager.
	 *
	 * @return this minigame's rollback manager
	 * @since 0.1.0
	 */
	public RollbackManager getRollbackManager() {
		return rbManager;
	}

	/**
	 * Retrieves this minigame's lobby manager.
	 *
	 * @return this minigame's lobby manager
	 * @since 0.1.0
	 */
	public LobbyManager getLobbyManager() {
		return lobbyManager;
	}

	/**
	 * Retrieves this minigame's config manager.
	 *
	 * @return this minigame's config manager
	 * @since 0.1.0
	 */
	public ConfigManager getConfigManager() {
		return configManager;
	}

	/**
	 * Returns the {@link Locale} for this minigame.
	 *
	 * @return the {@link Locale} for this minigame
	 * @since 0.3.0
	 */
	public Locale getLocale() {
		return locale;
	}

	/**
	 * Logs the given message at the specified logging level.
	 *
	 * @param message the message to log
	 * @param level   the level at which to log the message
	 * @since 0.3.0
	 */
	public void log(String message, LogLevel level) {
		MGUtil.log(message, plugin.getName(), level);
	}

	/**
	 * Logs the given message at {@link LogLevel#SEVERE}.
	 *
	 * @param message the message to log
	 * @since 0.3.0
	 */
	public void severe(String message) {
		this.log(message, LogLevel.SEVERE);
	}

	/**
	 * Logs the given message at {@link LogLevel#WARNING}.
	 *
	 * @param message the message to log
	 * @since 0.3.0
	 */
	public void warning(String message) {
		this.log(message, LogLevel.WARNING);
	}

	/**
	 * Logs the given message at {@link LogLevel#INFO}.
	 *
	 * @param message the message to log
	 * @since 0.3.0
	 */
	public void info(String message) {
		this.log(message, LogLevel.INFO);
	}

	/**
	 * Logs the given message at {@link LogLevel#DEBUG}.
	 *
	 * @param message the message to log
	 * @since 0.3.0
	 */
	public void debug(String message) {
		this.log(message, LogLevel.DEBUG);
	}

	/**
	 * Logs the given message at {@link LogLevel#VERBOSE}.
	 *
	 * @param message the message to log
	 * @since 0.3.0
	 */
	public void verbose(String message) {
		this.log(message, LogLevel.VERBOSE);
	}

	/**
	 * Retrieves a hashmap mapping the names of online players to their
	 * respective UUIDs.
	 *
	 * @return a hashmap mapping the names of online players to their respective
	 * UUIDs
	 * @since 0.3.0
	 */
	public static HashMap<String, UUID> getOnlineUUIDs() {
		return MyiuLib.getOnlineUUIDs();
	}

	/**
	 * Unsets all static objects in this class.
	 *
	 * @throws UnsupportedOperationException if MyiuLib is not currently
	 *                                       disabling
	 * @since 0.1.0
	 */
	public static void uninitialize() {
		MGUtil.verifyDisablingStatus();
		for (Minigame mg : registeredInstances.values()) {
			mg = null;
		}
		registeredInstances = null;
	}

	/**
	 * Determines whether this version of MyiuLib is compatibile with the
	 * specified minimum required version.
	 *
	 * @param minReqVersion the minimum required version of MyiuLib
	 * @return whether this version of MyiuLib is compatibile with the specified
	 * minimum required version
	 * @since 0.3.0
	 */
	public static boolean isMGLibCompatible(String minReqVersion) {
		return versions.contains(minReqVersion);
	}

}
