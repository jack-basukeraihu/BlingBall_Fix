package org.bitbucket.jack_basukeraihu.BlingBall_Fix;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class MainClass extends JavaPlugin implements Listener {
	private HashSet<Slime> balls;
	private HashSet<UUID> ballIds;
	private HashMap<UUID, Vector> velocities;

	public MainClass() {
		this.balls = new HashSet<Slime>();
		this.ballIds = new HashSet<UUID>();
		this.velocities = new HashMap<UUID, Vector>();
	}

	public HashSet<Slime> getBalls() {
		return balls;
	}

	//プラグインロード
	public void onEnable() {
		this.getLogger().info("BlingBall has been loaded.");
		this.getServer().getPluginManager().registerEvents((Listener) this, (Plugin) this);
		this.ballIds = new HashSet<UUID>();
		this.balls = new HashSet<Slime>();

		//Crackshotロードチェック
		if (Bukkit.getPluginManager().isPluginEnabled("CrackShot")) {
			PluginManager pm = getServer().getPluginManager();
			pm.registerEvents(new CrackShotListener(this), this);
		}

		//Entityデータ確認、ロードできればBall配置
		try {
			final BufferedReader load = new BufferedReader(new FileReader("BlingBalls.dat"));
			String line;
			while ((line = load.readLine()) != null) {
				try {
					this.ballIds.add(UUID.fromString(line));
				} catch (IllegalArgumentException ex) {
				}
			}
			load.close();
		} catch (FileNotFoundException e) {
			this.getLogger().info("No BlingBalls.dat found.");
		} catch (IOException e2) {
			this.getLogger().info("Error while reading BlingBalls.dat. Some balls may not be loaded properly.");
		}
		for (final World world : this.getServer().getWorlds()) {
			for (final Entity entity : world.getEntities()) {
				if (entity instanceof Slime && this.ballIds.contains(entity.getUniqueId())) {
					this.balls.add((Slime) entity);
					this.ballIds.remove(entity.getUniqueId());
				}
			}
		}
		this.getServer().getScheduler().scheduleSyncRepeatingTask((Plugin) this, (Runnable) new BallTask(this), 1L, 1L);
	}

	//アンロード
	public void onDisable() {
		this.getLogger().info("Blingball has been unloaded.");
		if (this.balls.size() > 0) {
			try {
				final BufferedWriter save = new BufferedWriter(new FileWriter("BlingBalls.dat"));
				for (final Slime ball : this.balls) {
					save.write(ball.getUniqueId().toString());
					save.write(10);
				}
				for (final UUID id : this.ballIds) {
					save.write(id.toString());
					save.write(10);
				}
				save.close();
			} catch (FileNotFoundException e) {
				this.getLogger().info("Unable to create BlingBalls.dat. Data will not be saved.");
			} catch (Exception e2) {
				this.getLogger().info("Unable to write BlingBalls.dat. Data will not be saved.");
			}
		}
	}

	public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
		if (cmd.getName().equalsIgnoreCase("ball")) {
			if (sender instanceof Player) {
				final Player player = (Player) sender;
				final Location pos = player.getLocation();
				final Slime ball = (Slime) player.getWorld().spawnEntity(pos.add(pos.getDirection().add(new Vector(0, 1, 0))), EntityType.SLIME);
				ball.setRemoveWhenFarAway(false);
				ball.setSize(1);
				this.balls.add(ball);
			} else {
				sender.sendMessage("Only players can create balls.");
			}
			return true;
		}
		if (cmd.getName().equalsIgnoreCase("clearballs")) {
			for (final Slime slime : this.balls) {
				slime.setHealth(0.0);
			}
			if (this.balls.size() == 0) {
				sender.sendMessage("No balls to clear. Use /ball to create a ball.");
			} else if (this.balls.size() == 1) {
				sender.sendMessage("Cleared 1 currently loaded ball.");
			} else if (this.balls.size() == 2) {
				sender.sendMessage("Cleared both currently loaded balls.");
			} else {
				sender.sendMessage("Destroyed all " + this.balls.size() + " currently loaded balls");
			}
			this.balls.clear();
			return true;
		}
		return false;
	}

	//チャンクロード時にチャンク内に居るスライムをボールに設定
	@EventHandler(priority = EventPriority.MONITOR)
	public void eventLoadChunk(final ChunkLoadEvent e) {
		Entity[] entities;
		for (int length = (entities = e.getChunk().getEntities()).length, i = 0; i < length; ++i) {
			final Entity entity = entities[i];
			final UUID id = entity.getUniqueId();
			if (this.ballIds.contains(id) && entity instanceof Slime) {
				this.ballIds.remove(id);
				this.balls.add((Slime) entity);
			}
		}
	}

	//チャンクアンロード時にボールから削除
	@EventHandler(priority = EventPriority.MONITOR)
	public void eventUnloadChunk(final ChunkUnloadEvent e) {
		Entity[] entities;
		for (int length = (entities = e.getChunk().getEntities()).length, i = 0; i < length; ++i) {
			final Entity entity = entities[i];
			if (entity instanceof Slime && this.balls.contains(entity)) {
				this.ballIds.add(entity.getUniqueId());
				this.balls.remove(entity);
			}
		}
	}

	//スライムのダメージを無効化する
	@EventHandler(priority = EventPriority.HIGH)
	public void eventDamage(final EntityDamageEvent e) {
		if (e.getEntity() instanceof Slime && this.balls.contains(e.getEntity()) && e.getCause() == EntityDamageEvent.DamageCause.FALL) {
			e.setCancelled(true);
		}
	}

	//スラムを右クリックした時に上向きに飛ばす
	@EventHandler(priority = EventPriority.MONITOR)
	public void eventRightClick(final PlayerInteractEntityEvent e) {
		final Entity entity = e.getRightClicked();
		if (entity instanceof Slime && this.balls.contains(entity)) {
			final Slime slime = (Slime) entity;
			slime.setVelocity(slime.getVelocity().add(new Vector(0, 0.4, 0).add(e.getPlayer().getLocation().getDirection().normalize().multiply(0.3).setY(0))));
			slime.getWorld().playSound(slime.getLocation(), Sound.ENTITY_SMALL_SLIME_HURT, 1.0f, 1.0f);
		}
	}

	//殴った時に追加のベクトルを掛ける
	@EventHandler
	public void entityDamageByEntity(final EntityDamageByEntityEvent e) {
		final Entity entity = e.getEntity();
		if (entity instanceof Slime && this.balls.contains(entity)) {
			if (e.getDamager() instanceof Snowball) {
				final Slime slime = (Slime) entity;
				slime.getWorld().playSound(slime.getLocation(), Sound.ENTITY_SMALL_SLIME_HURT, 1.0f, 1.0f);
				slime.setNoDamageTicks(0);
			}

			if (e.getDamager() instanceof Player) {
				Player p = (Player) e.getDamager();
				if (p.getInventory().getItemInMainHand() != null) {
					ItemStack im = p.getInventory().getItemInMainHand();
					if (im.getEnchantments() != null) {
						for (Enchantment enchant : im.getEnchantments().keySet()) {
							if (enchant == Enchantment.KNOCKBACK) { //ノックバックエンチャが付いてたらそっちの処理に任せる
								return;
							}
						}
					}
				}

				final Slime slime = (Slime) entity;
				slime.setVelocity(slime.getVelocity().add(new Vector(0, 0, 0).add(p.getLocation().getDirection().normalize().multiply(1.5))));
				slime.getWorld().playSound(slime.getLocation(), Sound.ENTITY_SMALL_SLIME_HURT, 1.0f, 1.0f);
			}
		}
	}

	@SuppressWarnings("deprecation")
	public void doBallPhysics() {
		for (final Slime slime : this.balls) {
			final UUID id = slime.getUniqueId();
			Vector velocity = slime.getVelocity();
			if (this.velocities.containsKey(id)) {
				velocity = this.velocities.get(id);
			}
			if (slime.isDead()) {
				continue;
			}
			Boolean bounceSound = false;
			final Vector newv = slime.getVelocity();
			if (newv.getX() == 0.0) {
				newv.setX(-velocity.getX() * 0.8);
				if (Math.abs(velocity.getX()) > 0.3) {
					bounceSound = true;
				}
			} else if (Math.abs(velocity.getX() - newv.getX()) < 0.1) {
				newv.setX(velocity.getX() * 0.98);
			}
			if (newv.getY() == -0.0784000015258789 && velocity.getY() < -0.1) {
				newv.setY(-velocity.getY() * 0.8);
				if (Math.abs(velocity.getY()) > 0.3) {
					bounceSound = true;
				}
			}
			if (newv.getZ() == 0.0) {
				newv.setZ(-velocity.getZ() * 0.8);
				if (Math.abs(velocity.getZ()) > 0.3) {
					bounceSound = true;
				}
			} else if (Math.abs(velocity.getZ() - newv.getZ()) < 0.1) {
				newv.setZ(velocity.getZ() * 0.98);
			}
			if (bounceSound) {
				slime.getWorld().playSound(slime.getLocation(), Sound.ENTITY_SMALL_SLIME_HURT, 1.0f, 1.0f);
			}
			slime.setMaxHealth(20.0);
			slime.setHealth(20.0);
			slime.setVelocity(newv);
			slime.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 10, -3, true), true);
			slime.playEffect(EntityEffect.HURT);
			this.velocities.put(id, newv);
		}
	}
}