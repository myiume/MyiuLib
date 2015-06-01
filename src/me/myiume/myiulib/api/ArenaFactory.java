package me.myiume.myiulib.api;

import me.myiume.myiulib.MGUtil;
import me.myiume.myiulib.MyiuLib;
import me.myiume.myiulib.exception.InvalidLocationException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class ArenaFactory {

	private String plugin;
	private String arena;
	private String world;

	private boolean newInstance = true;
	private boolean newArena = false;

	private YamlConfiguration yaml = null;

	private int timerHandle = -1;

	private ArenaFactory(String plugin, String arena, String world) {
		this.plugin = plugin;
		MGYamlConfiguration y = MGUtil.loadArenaYaml(plugin);
		String display = "";
		if (y != null && y.contains(arena + ".displayname")) {
			display = y.getString(arena + ".displayname");
		}
		else if (y != null) {
			y.set(arena + ".displayname", arena);
		}
		if (display.equalsIgnoreCase(arena)) {
			this.arena = display;
		}
		else {
			this.arena = arena;
		}
		this.world = world;
		setWorld(world);
		setDisplayName(arena);
		MyiuLib.getWorlds(plugin).add(world); // register world with event listener
	}

	/**
	 * Creates a new {@link ArenaFactory arena} object, used to modify an
	 * arena's assets.
	 *
	 * @param plugin the plugin this arena is owned by
	 * @param arena  the name of the arena
	 * @param world  the world containing the arena
	 * @return the created ArenaFactory, or the existing one if present
	 * @since 0.1.0
	 */
	public static ArenaFactory createArenaFactory(String plugin, String arena, String world) {
		ArenaFactory af = Minigame.getMinigameInstance(plugin).arenaFactories.get(arena.toLowerCase());
		if (af == null) {
			boolean nA = false;
			String displayName = arena;
			if (MGUtil.loadArenaYaml(plugin).get(arena) == null) {
				nA = true;
			}
			else {
				world = MGUtil.loadArenaYaml(plugin).getString(arena + ".world");
				displayName = MGUtil.loadArenaYaml(plugin).getString(arena + ".displayname");
			}
			af = new ArenaFactory(plugin, displayName, world);
			af.newArena = nA;
			return af;
		}
		else {
			af.newInstance = false;
			return af;
		}

	}

	/**
	 * Retrieves the name of the plugin associated with this {@link
	 * ArenaFactory}.
	 *
	 * @return the name of the plugin associated with this {@link ArenaFactory}
	 * @since 0.1.0
	 */
	public String getPlugin() {
		return plugin;
	}

	/**
	 * Retrieves the name of the arena associated with this {@link
	 * ArenaFactory}.
	 *
	 * @return the name of the arena associated with this {@link ArenaFactory}
	 * @since 0.1.0
	 */
	public String getArena() {
		return arena;
	}

	/**
	 * Retrieves the display name of the arena associated with this {@link
	 * ArenaFactory}.
	 *
	 * @return the display name of the arena associated with this {@link
	 * ArenaFactory}
	 * @since 0.1.0
	 */
	public String getDisplayName() {
		return arena;
	}

	/**
	 * Retrieves the name of the world associated with this {@link
	 * ArenaFactory}'s arena.
	 *
	 * @return the name of the world associated with this {@link ArenaFactory}'s
	 * arena
	 * @since 0.1.0
	 */
	public String getWorld() {
		return world;
	}

	private ArenaFactory setDisplayName(String displayName) {
		if (yaml == null) {
			yaml = MGUtil.loadArenaYaml(plugin);
		}
		else {
			Bukkit.getScheduler().cancelTask(timerHandle);
		}
		yaml.set(arena + ".displayname", displayName);
		timerHandle = Bukkit.getScheduler().runTaskLaterAsynchronously(MGUtil.getPlugin(), new Runnable() {
			public void run() {
				writeChanges();
			}
		}, 1L).getTaskId();
		return this;
	}

	private ArenaFactory setWorld(String world) {
		if (yaml == null) {
			yaml = MGUtil.loadArenaYaml(plugin);
		}
		else {
			Bukkit.getScheduler().cancelTask(timerHandle);
		}
		yaml.set(arena + ".world", world);
		timerHandle = Bukkit.getScheduler().runTaskLaterAsynchronously(MGUtil.getPlugin(), new Runnable() {
			public void run() {
				writeChanges();
			}
		}, 1L).getTaskId();
		return this;
	}

	/**
	 * Adds a spawn to the given arena with the given coordinates, pitch, and
	 * yaw.
	 *
	 * @param x     the x-coordinate of the new spawn
	 * @param y     the y-coordinate of the new spawn
	 * @param z     the z-coordinate of the new spawn
	 * @param pitch the pitch (x- and z-rotation) of the new spawn
	 * @param yaw   the yaw (y-rotation) of the new spawn
	 * @return the instance of {@link ArenaFactory} which this method was called
	 * from
	 * @since 0.1.0
	 */
	public ArenaFactory addSpawn(double x, double y, double z, float pitch, float yaw) {
		if (yaml == null) {
			yaml = MGUtil.loadArenaYaml(plugin);
		}
		else {
			Bukkit.getScheduler().cancelTask(timerHandle);
		}
		Minigame mg = Minigame.getMinigameInstance(plugin);
		// check if round is taking place in arena
		if (Minigame.getMinigameInstance(plugin).getRounds().containsKey(arena)) {
			Round r = mg.getRound(arena); // get the round object
			Location l = new Location(Bukkit.getWorld(r.getWorld()), x, y, z); // self-explanatory
			l.setPitch(pitch);
			l.setYaw(yaw);
			r.getSpawns().add(l); // add spawn to the live round
		}
		ConfigurationSection cs = yaml.getConfigurationSection(arena);
		if (cs == null) {
			yaml.createSection(arena);
			cs = yaml.getConfigurationSection(arena);
		}
		int min; // the minimum available spawn number
		for (min = 0; min >= 0; min++) { // this feels like a bad idea, but I think it should work
			if (cs.getString("spawns." + min) == null) {
				break;
			}
		}
		cs.set("spawns." + min + ".x", (int)Math.floor(x));
		cs.set("spawns." + min + ".y", (int)Math.floor(y));
		cs.set("spawns." + min + ".z", (int)Math.floor(z));
		cs.set("spawns." + min + ".pitch", pitch);
		cs.set("spawns." + min + ".yaw", yaw);
		timerHandle = Bukkit.getScheduler().runTaskLaterAsynchronously(MGUtil.getPlugin(), new Runnable() {
			public void run() {
				writeChanges();
			}
		}, 1L).getTaskId();
		return this;
	}

	/**
	 * Adds a spawn to the given arena with the given coordinates.
	 *
	 * <p><strong>Note:</strong> it is recommended that you use {@link
	 * ArenaFactory#addSpawn(Location) addSpawn(Location)} if an instance of a
	 * Location at these coordinates already exists.</p>
	 *
	 * @param x the x-coordinate of the new spawn
	 * @param y the y-coordinate of the new spawn
	 * @param z the z-coordinate of the new spawn
	 * @return the instance of {@link ArenaFactory} which this method was called
	 * from
	 * @since 0.1.0
	 */
	public ArenaFactory addSpawn(double x, double y, double z) {
		return addSpawn(x, y, z, 90f, 0f);
	}

	/**
	 * Adds a spawn to the given arena with the given {@link Location3D}.
	 *
	 * @param location        the location of the new spawn
	 * @param saveOrientation whether to save the {@link Location3D}'s pitch and
	 *                        yaw to the spawn (Defaults to false if omitted).
	 * @return the instance of {@link ArenaFactory} which this method was called
	 * from
	 * @throws InvalidLocationException if the provided location's world does
	 *                                  not match the arena's world
	 * @since 0.1.0
	 */
	public ArenaFactory addSpawn(Location3D location, boolean saveOrientation) throws InvalidLocationException {
		if (location.getWorld().equals(world)) {
			if (saveOrientation) {
				return addSpawn(Math.floor(location.getX()), Math.floor(location.getY()), Math.floor(location.getZ()),
						location.getPitch(), location.getYaw());
			}
			return addSpawn(location.getX(), location.getY(), location.getZ());
		}
		else {
			throw new InvalidLocationException();
		}
	}

	/**
	 * Adds a spawn to the given arena with the given {@link Location}.
	 *
	 * @param location        the location of the new spawn
	 * @param saveOrientation whether to save the {@link Location}'s pitch and
	 *                        yaw to the spawn (Defaults to false if omitted).
	 * @return the instance of {@link ArenaFactory} which this method was called
	 * from
	 * @throws InvalidLocationException if the provided location's world does
	 *                                  not match the arena's world
	 * @deprecated Use {@link ArenaFactory#addSpawn(Location3D, boolean)}
	 * @since 0.4.0
	 */
	@Deprecated
	public ArenaFactory addSpawn(Location location, boolean saveOrientation) throws InvalidLocationException {
		return addSpawn(new Location3D(location.getWorld().getName(), location.getX(), location.getY(), location.getZ()), saveOrientation);
	}

	/**
	 * Adds a spawn to the given arena with the given {@link Location}.
	 *
	 * @param location the location of the new spawn
	 * @return the instance of {@link ArenaFactory} which this method was called
	 * from
	 * @throws InvalidLocationException if the provided location's world does
	 *                                  not match the arena's world
	 * @since 0.1.0
	 */
	public ArenaFactory addSpawn(Location3D location) throws InvalidLocationException {
		return addSpawn(location, false);
	}

	/**
	 * Adds a spawn to the given arena with the given {@link Location}.
	 *
	 * @param location the location of the new spawn
	 * @return the instance of {@link ArenaFactory} which this method was called
	 * from
	 * @throws InvalidLocationException if the provided location's world does
	 *                                  not match the arena's world
	 * @deprecated Use {@link ArenaFactory#addSpawn(Location3D)}
	 * @since 0.1.0
	 */
	@Deprecated
	public ArenaFactory addSpawn(Location location) throws InvalidLocationException {
		return addSpawn(MGUtil.fromBukkitLocation(location));
	}

	/**
	 * Deletes a spawn from the given arena at the given coordinates.
	 *
	 * @param x the x-coordinate of the spawn to delete
	 * @param y the y-coordinate of the spawn to delete
	 * @param z the z-coordinate of the spawn to delete
	 * @return the instance of {@link ArenaFactory} which this method was called
	 * from
	 * @since 0.1.0
	 */
	public ArenaFactory deleteSpawn(double x, double y, double z) {
		if (yaml == null) {
			yaml = MGUtil.loadArenaYaml(plugin);
		}
		else {
			Bukkit.getScheduler().cancelTask(timerHandle);
		}
		Minigame mg = Minigame.getMinigameInstance(plugin);
		if (mg.getRound(arena) != null) {
			Round r = mg.getRound(arena);
			for (Location l : r.getSpawns()) {
				if (l.getX() == x && l.getY() == y && l.getZ() == z) {
					r.getSpawns().remove(l);
				}
			}
		}
		ConfigurationSection spawns = yaml.getConfigurationSection(arena + ".spawns"); // make the code easier to read
		for (String k : spawns.getKeys(false)) {
			if (spawns.getDouble(k + ".x") == x && spawns.getDouble(k + ".y") == y && spawns.getDouble(k + ".z") == z) {
				spawns.set(k, null); // delete it from the config
			}
		}
		timerHandle = Bukkit.getScheduler().runTaskLaterAsynchronously(MGUtil.getPlugin(), new Runnable() {
			public void run() {
				writeChanges();
			}
		}, 1L).getTaskId();
		return this;
	}

	/**
	 * Deletes a spawn from the given arena at the given {@link Location3D}.
	 *
	 * @param location the {@link Location3D} of the spawn to delete
	 * @return the instance of {@link ArenaFactory} which this method was called
	 * from
	 * @since 0.4.0
	 */
	public ArenaFactory deleteSpawn(Location3D location) {
		return deleteSpawn(location.getX(), location.getY(), location.getZ());
	}

	/**
	 * Deletes a spawn from the given arena at the given {@link Location}.
	 *
	 * @param location the {@link Location} of the spawn to delete
	 * @return the instance of {@link ArenaFactory} which this method was called
	 * from
	 * @deprecated Use {@link ArenaFactory#deleteSpawn(Location3D)}
	 * @since 0.1.0
	 */
	@Deprecated
	public ArenaFactory deleteSpawn(Location location) {
		return deleteSpawn(location.getX(), location.getY(), location.getZ());
	}

	/**
	 * Deletes a spawn from the given arena at the given {@link Location}.
	 *
	 * @param index the internal index of the spawn to delete
	 * @return the instance of {@link ArenaFactory} which this method was called
	 * from
	 * @since 0.1.0
	 */
	public ArenaFactory deleteSpawn(int index) {
		if (yaml == null) {
			yaml = MGUtil.loadArenaYaml(plugin);
		}
		else {
			Bukkit.getScheduler().cancelTask(timerHandle);
		}
		Minigame mg = Minigame.getMinigameInstance(plugin);
		ConfigurationSection spawns = yaml.getConfigurationSection(arena + ".spawns"); // make the code easier to read
		if (spawns.contains(index + "")) {
			int x = spawns.getInt(index + ".x");
			int y = spawns.getInt(index + ".y");
			int z = spawns.getInt(index + ".z");
			spawns.set(index + "", null);
			if (mg.getRound(arena) != null) {
				Round r = mg.getRound(arena);
				for (Location l : r.getSpawns()) {
					if (l.getX() == x && l.getY() == y && l.getZ() == z) {
						r.getSpawns().remove(l);
					}
				}
			}
		}
		timerHandle = Bukkit.getScheduler().runTaskLaterAsynchronously(MGUtil.getPlugin(), new Runnable() {
			public void run() {
				writeChanges();
			}
		}, 1L).getTaskId();
		return this;
	}

	/**
	 * Sets the minimum boundary of this arena.
	 *
	 * @param x the minimum x-value
	 * @param y the minimum y-value
	 * @param z the minimum z-value
	 * @return the instance of {@link ArenaFactory} which this method was called
	 *         from
	 * @since 0.1.0
	 */
	public ArenaFactory setMinBound(double x, double y, double z) {
		if (yaml == null) {
			yaml = MGUtil.loadArenaYaml(plugin);
		}
		else {
			Bukkit.getScheduler().cancelTask(timerHandle);
		}
		Minigame mg = Minigame.getMinigameInstance(plugin);
		// check if round is taking place in arena
		if (Minigame.getMinigameInstance(plugin).getRounds().containsKey(arena)) {
			Round r = mg.getRound(arena); // get the round object
			r.setMinBound(x, y, z); // add spawn to the live round
		}
		ConfigurationSection cs = yaml.getConfigurationSection(arena);
		cs.set("minX", x);
		cs.set("minY", y);
		cs.set("minZ", z);
		timerHandle = Bukkit.getScheduler().runTaskLaterAsynchronously(MGUtil.getPlugin(), new Runnable() {
			public void run() {
				writeChanges();
			}
		}, 1L).getTaskId();
		return this;
	}

	/**
	 * Sets the minimum boundary of this arena.
	 *
	 * @param location the {@link Location3D} representing the maximum boundary
	 * @return the instance of {@link ArenaFactory} which this method was called
	 * from
	 * @throws InvalidLocationException if the provided location's world does
	 *                                  not match the arena's world
	 * @since 0.4.0
	 */
	public ArenaFactory setMinBound(Location3D location) throws InvalidLocationException {
		if (location.getWorld().equals(yaml.get(arena + ".world"))) {
			return setMinBound(location.getX(), location.getY(), location.getZ());
		}
		else {
			throw new InvalidLocationException();
		}
	}

	/**
	 * Sets the minimum boundary of this arena.
	 *
	 * @param location the {@link Location} representing the maximum boundary
	 * @return the instance of {@link ArenaFactory} which this method was called
	 * from
	 * @throws InvalidLocationException if the provided location's world does
	 *                                  not match the arena's world
	 * @deprecated Use {@link ArenaFactory#setMinBound(Location3D)}
	 * @since 0.1.0
	 */
	@Deprecated
	public ArenaFactory setMinBound(Location location) throws InvalidLocationException {
		return setMinBound(new Location3D(location.getWorld().getName(), location.getX(), location.getY(), location.getZ()));
	}

	/**
	 * Sets the maximum boundary of this arena.
	 *
	 * @param x the maximum x-value
	 * @param y the maximum y-value
	 * @param z the maximum z-value
	 * @return the instance of {@link ArenaFactory} which this method was called
	 * from
	 * @since 0.1.0
	 */
	public ArenaFactory setMaxBound(double x, double y, double z) {
		if (yaml == null) {
			yaml = MGUtil.loadArenaYaml(plugin);
		}
		else {
			Bukkit.getScheduler().cancelTask(timerHandle);
		}
		Minigame mg = Minigame.getMinigameInstance(plugin);
		// check if round is taking place in arena
		if (Minigame.getMinigameInstance(plugin).getRounds().containsKey(arena)) {
			Round r = mg.getRound(arena); // get the round object
			r.setMaxBound(x, y, z); // add spawn to the live round
		}
		ConfigurationSection cs = yaml.getConfigurationSection(arena);
		cs.set("maxX", x);
		cs.set("maxY", y);
		cs.set("maxZ", z);
		timerHandle = Bukkit.getScheduler().runTaskLaterAsynchronously(MGUtil.getPlugin(), new Runnable() {
			public void run() {
				writeChanges();
			}
		}, 1L).getTaskId();
		return this;
	}

	/**
	 * Sets the maximum boundary of this arena.
	 *
	 * @param location the {@link Location3D} representing the maximum boundary
	 * @return the instance of {@link ArenaFactory} which this method was called
	 * from
	 * @throws InvalidLocationException if the provided location's world does
	 *                                  not match the arena's world
	 * @since 0.4.0
	 */
	public ArenaFactory setMaxBound(Location3D location) throws InvalidLocationException {
		if (location.getWorld().equals(yaml.get(arena + ".world"))) {
			return setMaxBound(location.getX(), location.getY(), location.getZ());
		}
		else {
			throw new InvalidLocationException();
		}
	}

	/**
	 * Sets the maximum boundary of this arena.
	 *
	 * @param location the {@link Location} representing the maximum boundary
	 * @return the instance of {@link ArenaFactory} which this method was called
	 * from
	 * @throws InvalidLocationException if the provided location's world does
	 *                                  not match the arena's world
	 * @deprecated Use {@link ArenaFactory#setMinBound(Location3D)}
	 * @since 0.1.0
	 */
	@Deprecated
	public ArenaFactory setMaxBound(Location location) throws InvalidLocationException {
		return setMaxBound(new Location3D(location.getWorld().getName(), location.getX(), location.getY(), location.getZ()));
	}

	/**
	 * Attaches an arbitrary key-value pair to the arena.
	 *
	 * @param key   the key to set for the arena
	 * @param value the value to associate with the key
	 * @return the instance of {@link ArenaFactory} which this method was called
	 * from
	 * @since 0.3.0
	 */
	public ArenaFactory setData(String key, Object value) {
		if (yaml == null) {
			yaml = MGUtil.loadArenaYaml(plugin);
		}
		else {
			Bukkit.getScheduler().cancelTask(timerHandle);
		}
		yaml.set(arena + "." + key, value);
		timerHandle = Bukkit.getScheduler().runTaskLaterAsynchronously(MGUtil.getPlugin(), new Runnable() {
			public void run() {
				writeChanges();
			}
		}, 1L).getTaskId();
		return this;
	}

	private void writeChanges() {
		MGUtil.saveArenaYaml(plugin, yaml);
		yaml = null;
	}

	/**
	 * Determines whether this instance is newly created for the server session.
	 * This will return false for the rest of the session after the {@link
	 * ArenaFactory#createArenaFactory(String, String, String)
	 * createArenaFactory()} method is called a second time.
	 *
	 * @return whether this instance is newly created
	 * @since 0.1.0
	 */
	public boolean isNewInstance() {
		return newInstance;
	}

	/**
	 * Determines whether this arena is newly created. This will permanently
	 * return false until the arena is deleted.
	 *
	 * @return whether this arena is newly created
	 * @since 0.3.0
	 */
	public boolean isNewArena() {
		return newArena;
	}

	/**
	 * Destroys this arena factory.
	 *
	 * @since 0.1.0
	 */
	public void destroy() {
		Minigame.getMinigameInstance(plugin).arenaFactories.remove(arena);
	}

}
