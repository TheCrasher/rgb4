package de.fhtrier.gdig.demos.jumpnrun.common.network;

import de.fhtrier.gdig.demos.jumpnrun.common.gamelogic.player.states.identifiers.PlayerActionState;

public class PlayerData extends NetworkData {

	public PlayerData(int id) {
		super(id);
	}

	private static final long serialVersionUID = -3776997774121122797L;

	public PlayerActionState state;

	public PlayerActionState getState() {
		return state;
	}

}
