package de.fhtrier.gdig.demos.jumpnrun.common.gamelogic.player.states;

import org.newdawn.slick.Animation;
import org.newdawn.slick.SlickException;

import de.fhtrier.gdig.demos.jumpnrun.common.gamelogic.player.Player;
import de.fhtrier.gdig.demos.jumpnrun.common.gamelogic.player.states.identifiers.PlayerActions;
import de.fhtrier.gdig.demos.jumpnrun.identifiers.Assets;
import de.fhtrier.gdig.demos.jumpnrun.identifiers.Constants;
import de.fhtrier.gdig.demos.jumpnrun.identifiers.EntityOrder;
import de.fhtrier.gdig.engine.gamelogic.Entity;
import de.fhtrier.gdig.engine.graphics.entities.AssetEntity;
import de.fhtrier.gdig.engine.management.Factory;
import de.fhtrier.gdig.engine.sound.SoundManager;

public class PlayerShootFallingState extends PlayerAssetState {
	
	private Animation anim;
	
	public PlayerShootFallingState(Player player, Factory factory)
			throws SlickException {
		super(player, Assets.PlayerFallShootingAnimId, Assets.PlayerFallShootingImagePath, EntityOrder.Player, factory);
	
		AssetEntity e = getGfxEntity();
		
		anim = e.Assets().getAnimation(e.getAssetId());
		anim.setLooping(false);
	}

	@Override
	public void enter() {
		if (anim.isStopped()) {
			anim.restart();
		}
		SoundManager.playSound(Assets.BulletSoundId, 1f, 0.2f);
	}

	@Override
	public void leave() {
	}

	@Override
	public void update() {	

		// check if vel < threshold --> stop falling
		if (getPlayer().isOnGround()) {
			getPlayer().applyAction(PlayerActions.Land);
		}
		
		if (anim.isStopped()) {
			getPlayer().applyAction(PlayerActions.StopShooting);
		}
	}
	
}
