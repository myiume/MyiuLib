package me.myiume.myiulib;

import me.myiume.myiulib.api.Minigame;
import me.myiume.myiulib.api.Round;
import me.myiume.myiulib.api.Stage;
import me.myiume.myiulib.event.round.MinigameRoundRollbackEvent;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class RollbackManager {

	private static boolean logging;
	private File f = null;
	private YamlConfiguration y = null;
	private JavaPlugin plugin = null;

	EntityDamageEvent lastEvent;

	/**
	 * Creates a new rollback manager for the specified plugin.
	 *
	 * @param plugin the plugin to create the rollback manager for
	 * @since 0.1.0
	 */
	public RollbackManager(JavaPlugin plugin) {
		f = new File(plugin.getDataFolder(), "rollback.yml");
		if (!plugin.getDataFolder().exists()) {
			plugin.getDataFolder().mkdirs();
		}
		try {
			if (!f.exists()) {
				f.createNewFile();
			}
			y = new YamlConfiguration();
			y.load(f);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			MyiuLib.log.severe("An exception occurred while initializing the rollback manager for plugin " +
					plugin.getName());
		}
		logging = MGUtil.getPlugin().getConfig().getBoolean("rollback-logging");
	}

	/**
	 * Retrieves the plugin associated with this rollback manager.
	 *
	 * @return the plugin associated with this rollback manager
	 * @since 0.1.0
	 */
	public JavaPlugin getPlugin() {
		return plugin;
	}

	/**
	 * Logs a block change.
	 *
	 * @param block the block which was changed
	 * @param arena the arena in which the block is contained
	 * @since 0.1.0
	 */
	public void logBlockChange(Block block, String arena) {
		if (!y.isSet(arena + ".blockChanges." + block.getX() + "," + block.getY() + "," + block.getZ())) {
			y.createSection(arena + ".blockChanges." + block.getX() + "," + block.getY() + "," + block.getZ());
		}
		ConfigurationSection cs = y.getConfigurationSection(arena + ".blockChanges." +
				block.getX() + "," + block.getY() + "," + block.getZ());
		cs.set("world", block.getWorld().getName());
		if (!cs.isSet("type")) { // make sure it hasn't already been changed
			cs.set("type", block.getType().toString());
			@SuppressWarnings("deprecation")
			int data = block.getData();
			cs.set("data", data);
			if (block.getState() instanceof Sign) {
				for (int i = 0; i < 4; i++) {
					cs.set("sign-text-" + i, ((Sign)block.getState()).getLine(i));
				}
			}
		}
		if (logging) {
			try {
				y.save(f);
			}
			catch (Exception ex) {
				MyiuLib.log.severe("An exception occurred while saving data for arena " + arena);
			}
		}
	}

	/**
	 * Logs an inventory change.
	 *
	 * @param inventory the inventory to log
	 * @param block     the block containing the inventory
	 * @param arena     the arena in which the block is contained
	 * @since 0.1.0
	 */
	public void logInventoryChange(Inventory inventory, Block block, String arena) {
		if (!y.isSet(arena + ".inventoryChanges." + block.getX() + "," + block.getY() + "," + block.getZ())) {
			y.createSection(arena + ".inventoryChanges." + block.getX() + "," + block.getY() + "," + block.getZ());
		}
		ConfigurationSection cs = y.getConfigurationSection(arena + ".inventoryChanges." +
				block.getX() + "," + block.getY() + "," + block.getZ());
		cs.set("world", block.getWorld().getName());
		if (!cs.isSet("inventory")) { // make sure it hasn't already been changed
			for (int i = 0; i < inventory.getSize(); i++) {
				ItemStack is = inventory.getItem(i);
				if (is != null) {
					cs.set("inventory." + i, is);
				}
			}
		}
		if (logging) {
			try {
				y.save(f);
			}
			catch (Exception ex) {
				MyiuLib.log.severe("An exception occurred while saving data for arena " + arena);
			}
		}
	}

	/**
	 * Rolls back the given arena.
	 *
	 * <p>This method <strong>should not</strong> be called from your plugin unless
	 * you understand the implications.</p>
	 *
	 * @param arena the arena to roll back
	 * @since 0.1.0
	 */
	@SuppressWarnings("deprecation")
	public void rollback(String arena) {
		Round r = null;
		for (Minigame mg : Minigame.getMinigameInstances()) {
			r = mg.getRound(arena);
		}
		if (r != null) {
			r.setStage(Stage.RESETTING);
			MGUtil.callEvent(new MinigameRoundRollbackEvent(r));
		}
		ConfigurationSection cs = y.getConfigurationSection(arena + ".blockChanges");
		if (cs != null) {
			for (String k : cs.getKeys(false)) {
				String[] coords = k.split(",");
				double x = Double.NaN, y = Double.NaN, z = Double.NaN;
				if (MGUtil.isInteger(coords[0])) {
					x = Integer.parseInt(coords[0]);
				}
				if (MGUtil.isInteger(coords[1])) {
					y = Integer.parseInt(coords[1]);
				}
				if (MGUtil.isInteger(coords[2])) {
					z = Integer.parseInt(coords[2]);
				}
				World w = Bukkit.getWorld(cs.getString(k + ".world"));
				if (w != null && x == x && y == y && z == z) {
					Location l = new Location(w, x, y, z);
					if (l.getBlock().getState() instanceof InventoryHolder) {
						((InventoryHolder)l.getBlock().getState()).getInventory().setContents(new ItemStack[0]);
					}
					l.getBlock().setType(Material.getMaterial(cs.getString(k + ".type")));
					l.getBlock().setData(Byte.parseByte(cs.getString(k + ".data")));
					if (l.getBlock().getState() instanceof Sign) {
						for (int i = 0; i < 4; i++) {
							if (cs.isSet("sign-text-" + i)) {
								((Sign)l.getBlock().getState()).setLine(i, cs.getString("sign-text-" + i));
							}
						}
					}
				}
			}
		}
		ConfigurationSection cs2 = y.getConfigurationSection(arena + ".inventoryChanges");
		if (cs2 != null) {
			for (String k : cs2.getKeys(false)) {
				String[] coords = k.split(",");
				double x = Double.NaN, y = Double.NaN, z = Double.NaN;
				if (MGUtil.isInteger(coords[0])) {
					x = Integer.parseInt(coords[0]);
				}
				if (MGUtil.isInteger(coords[1])) {
					y = Integer.parseInt(coords[1]);
				}
				if (MGUtil.isInteger(coords[2])) {
					z = Integer.parseInt(coords[2]);
				}
				World w = Bukkit.getWorld(cs2.getString(k + ".world"));
				if (w != null && x == x && y == y && z == z) {
					Location l = new Location(w, x, y, z);
					if (l.getBlock().getState() instanceof InventoryHolder) {
						Inventory inv = ((InventoryHolder)l.getBlock().getState()).getInventory();
						ConfigurationSection ymlInv = cs2.getConfigurationSection(k + ".inventory");
						for (String kk : ymlInv.getKeys(false)) {
							if (MGUtil.isInteger(kk)) {
								int slot = Integer.parseInt(kk);
								inv.setItem(slot, ymlInv.getItemStack(kk));
							}
						}
					}
				}
			}
		}
		y.set(arena, null);
		if (logging) {
			try {
				y.save(f);
			}
			catch (Exception ex) {
				ex.printStackTrace();
				MyiuLib.log.severe(MyiuLib.locale.getMessage("plugin.alert.data.save", arena));
			}
		}
		if (r != null) {
			r.setStage(Stage.WAITING);
			r.getMinigame().getLobbyManager().update(arena);
		}
	}

	/**
	 * Rolls back arenas which have not been rolled back due to a crash or unclean shutdown.
	 *
	 * @since 0.1.0
	 */
	public void checkRollbacks() {
		for (String k : y.getKeys(false)) {
			rollback(k);
		}
	}

}
