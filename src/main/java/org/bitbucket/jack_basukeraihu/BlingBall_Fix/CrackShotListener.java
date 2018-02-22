package org.bitbucket.jack_basukeraihu.BlingBall_Fix;

import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.shampaggon.crackshot.events.WeaponDamageEntityEvent;

public class CrackShotListener implements Listener {

	public static MainClass plugin;

	public CrackShotListener(MainClass instance) {
		plugin = instance;
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onWeaponDamageEntity(WeaponDamageEntityEvent e) {
		if (e.getVictim() instanceof Slime && plugin.getBalls().contains(e.getVictim())) {
			e.setDamage(0);
			final Slime slime = (Slime) e.getVictim();
			slime.setHealth(slime.getMaxHealth());
		}
	}

}
