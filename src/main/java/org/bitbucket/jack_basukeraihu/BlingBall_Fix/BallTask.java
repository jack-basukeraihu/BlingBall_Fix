package org.bitbucket.jack_basukeraihu.BlingBall_Fix;

import org.bukkit.scheduler.BukkitRunnable;

public class BallTask extends BukkitRunnable {
	private MainClass plugin;

	public BallTask(final MainClass plugin) {
		this.plugin = plugin;
	}

	public void run() {
		this.plugin.doBallPhysics();
	}
}