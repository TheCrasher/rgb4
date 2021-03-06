package de.fhtrier.gdig.demos.jumpnrun.common.gamelogic.player;

import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.particles.ConfigurableEmitter;
import org.newdawn.slick.particles.ConfigurableEmitter.ColorRecord;
import org.newdawn.slick.particles.ParticleSystem;
import org.newdawn.slick.util.Log;

import de.fhtrier.gdig.demos.jumpnrun.client.input.InputControl;
import de.fhtrier.gdig.demos.jumpnrun.client.network.protocol.QueryAction;
import de.fhtrier.gdig.demos.jumpnrun.common.gamelogic.StateColor;
import de.fhtrier.gdig.demos.jumpnrun.common.gamelogic.Team;
import de.fhtrier.gdig.demos.jumpnrun.common.gamelogic.player.states.PlayerAssetState;
import de.fhtrier.gdig.demos.jumpnrun.common.gamelogic.player.states.PlayerFallingState;
import de.fhtrier.gdig.demos.jumpnrun.common.gamelogic.player.states.PlayerJumpingState;
import de.fhtrier.gdig.demos.jumpnrun.common.gamelogic.player.states.PlayerLandingState;
import de.fhtrier.gdig.demos.jumpnrun.common.gamelogic.player.states.PlayerRunningState;
import de.fhtrier.gdig.demos.jumpnrun.common.gamelogic.player.states.PlayerShootFallingState;
import de.fhtrier.gdig.demos.jumpnrun.common.gamelogic.player.states.PlayerShootJumpingState;
import de.fhtrier.gdig.demos.jumpnrun.common.gamelogic.player.states.PlayerShootRunningState;
import de.fhtrier.gdig.demos.jumpnrun.common.gamelogic.player.states.PlayerShootStandingState;
import de.fhtrier.gdig.demos.jumpnrun.common.gamelogic.player.states.PlayerStandingState;
import de.fhtrier.gdig.demos.jumpnrun.common.gamelogic.player.states.identifiers.PlayerActionState;
import de.fhtrier.gdig.demos.jumpnrun.common.gamelogic.player.states.identifiers.PlayerActions;
import de.fhtrier.gdig.demos.jumpnrun.common.network.NetworkData;
import de.fhtrier.gdig.demos.jumpnrun.common.network.PlayerData;
import de.fhtrier.gdig.demos.jumpnrun.common.physics.entities.LevelCollidableEntity;
import de.fhtrier.gdig.demos.jumpnrun.identifiers.Assets;
import de.fhtrier.gdig.demos.jumpnrun.identifiers.Constants;
import de.fhtrier.gdig.demos.jumpnrun.identifiers.Constants.GamePlayConstants;
import de.fhtrier.gdig.demos.jumpnrun.identifiers.EntityOrder;
import de.fhtrier.gdig.demos.jumpnrun.identifiers.EntityType;
import de.fhtrier.gdig.engine.gamelogic.Entity;
import de.fhtrier.gdig.engine.graphics.entities.ParticleEntity;
import de.fhtrier.gdig.engine.graphics.shader.Shader;
import de.fhtrier.gdig.engine.helpers.IFiniteStateMachineListener;
import de.fhtrier.gdig.engine.management.AssetMgr;
import de.fhtrier.gdig.engine.management.Factory;
import de.fhtrier.gdig.engine.network.NetworkComponent;
import de.fhtrier.gdig.engine.physics.CollisionManager;
import de.fhtrier.gdig.engine.sound.SoundManager;

public class Player extends LevelCollidableEntity implements
		IFiniteStateMachineListener<PlayerActionState> {

	// Shader stuff
	private static Image playerGlow = null;
	private static Image weaponGlow = null;
	private static Shader playerShader = null;

	// helpers
	private Factory factory;
	private float playerHalfWidth = 48;

	// "useful" members
	private PlayerCondition condition;
	private PlayerStats stats;

	// some states for gfx and a statemachine for logic
	private PlayerStandingState stateStanding;
	private PlayerShootStandingState stateShootStanding;
	private PlayerRunningState stateRunning;
	private PlayerShootRunningState stateShootRunning;
	private PlayerJumpingState stateJumping;
	private PlayerShootJumpingState stateShootJumping;
	private PlayerLandingState stateLanding;
	private PlayerFallingState stateFalling;
	private PlayerShootFallingState stateShootFalling;

	private PlayerActionFSM fsmAction;
	private PlayerOrientationFSM fsmOrientation;

	// carries current Asset
	private PlayerAssetState currentPlayerAsset;

	// old Stuff
	private Entity playerGroup;
	private ParticleEntity weaponParticles;
	private AssetMgr assets;

	// initialization
	public Player(int id, Factory factory) throws SlickException {
		super(id, EntityType.PLAYER);

		this.factory = factory;
		assets = new AssetMgr();

		initCondition();
		initStats();
		initGraphics();    
		initPhysics();
		initStates();

		// startup
		setState(stateStanding);
	}

	private void initCondition() {
		condition = new PlayerCondition();
		condition.name = "XXX";
		setConditions();
	}
	
	private void setConditions() {
		condition.teamId = 1;
		condition.health = 1.0f;
		condition.ammo = 1.0f;
		condition.damage = 0.2f;
		condition.color = StateColor.RED; // player gets default-color: red
		condition.weaponColor = StateColor.RED; // weapon of player get
		// default-color: red		
	}
	
	private void initStats() {
		stats = new PlayerStats();		
	}

	private void initStates() throws SlickException {

		// statemachines for logic
		this.fsmAction = new PlayerActionFSM();
		this.fsmAction.add(this);

		this.fsmOrientation = new PlayerOrientationFSM();
		this.fsmOrientation.add(this);

		// some states
		stateStanding = new PlayerStandingState(this, factory);
		stateShootStanding = new PlayerShootStandingState(this, factory);
		stateRunning = new PlayerRunningState(this, factory);
		stateShootRunning = new PlayerShootRunningState(this, factory);
		stateJumping = new PlayerJumpingState(this, factory);
		stateShootJumping = new PlayerShootJumpingState(this, factory);
		stateLanding = new PlayerLandingState(this, factory);
		stateFalling = new PlayerFallingState(this, factory);
		stateShootFalling = new PlayerShootFallingState(this, factory);
	}

	private void initGraphics() throws SlickException {
		
		// weapon
		/*assets.storeAnimation(Assets.WeaponImageId,Assets.WeaponAnimImagePath);
		this.weapon = factory.createAnimationEntity(Assets.WeaponImageId, Assets.WeaponImageId);
		this.playerGroup.add(this.weapon);
		weapon.setVisible(true);*/

		// particles
		assets.storeParticleSystem(Assets.WeaponParticleEffect,
				Assets.WeaponParticleEffectImgPath,
				Assets.WeaponParticleEffectCfgPath);
		weaponParticles = factory.createParticleEntity(
				Assets.WeaponParticleEffect, Assets.WeaponParticleEffect, assets);

		int groupId = factory.createEntity(EntityOrder.Player,
				EntityType.HELPER);

		playerGroup = factory.getEntity(groupId);
		playerGroup.add(weaponParticles);

		// Position correction for particleEffects
		weaponParticles.getData()[Entity.X] = 40;
		weaponParticles.getData()[Entity.Y] = 110;

		add(playerGroup);

		// weapon
		// TODO Weapon Image loading goes here
		// assets.storeAnimation(Assets.WeaponImageId,
		// Assets.BulletAnimImagePath);
		// this.weapon = factory.createAnimationEntity(Assets.WeaponImageId,
		// Assets.WeaponImageId);
		// this.playerGroup.add(this.weapon);
		// Position correction for weapon
		// weapon.getData()[Entity.X] += 20;
		// weapon.getData()[Entity.Y] += 95;
		// weapon.setVisible(true);

		// shader
		if (playerShader == null && Constants.Debug.shadersActive) {
			playerShader = new Shader(
					assets.makePathRelativeToAssetPath(Assets.PlayerVertexShaderPath),
					assets.makePathRelativeToAssetPath(Assets.PlayerPixelShaderPath));

			playerGlow = new Image(
					assets.makePathRelativeToAssetPath(Assets.PlayerGlowImagePath));
			weaponGlow = new Image(
					assets.makePathRelativeToAssetPath(Assets.WeaponGlowImagePath));
		}

		// make entities visible
		setVisible(true);
		weaponParticles.setVisible(true);

		// order
		this.setOrder(EntityOrder.Player);
	}

	private void initPhysics() {
		// initialize position, velocity and acceleration
		// X Y OX OY SY SY ROT

		// SpawnPoint randomSpawnPoint = level.getRandomSpawnPoint(1);

		initData(new float[] { 200, 200, 65, 70, 1, 1, 0 }); // pos +
																// center of
																// rotation +
																// scale +
																// rot

		setVel(new float[] { 0, 0, 0, 0, 0, 0, 0 }); // no speed
		setAcc(new float[] { 0, GamePlayConstants.gravity, 0, 0, 0, 0, 0 }); // gravity

		// make player collidable with other entities
		CollisionManager.addEntity(this);

		// set bounding box according to idle animation size
		setBounds(new Rectangle(35, 16, 58, 108)); // bounding box
	}

	// network
	@Override
	public void applyNetworkData(final NetworkData networkData) {
		super.applyNetworkData(networkData);

		// TODO refactoring artefact
		// HACK we assume we're getting PlayerData
		// this.forceState(((PlayerData) networkData).getState());
	}

	@Override
	protected NetworkData _createNetworkData() {
		return new PlayerData(getId());
	}

	@Override
	public NetworkData getNetworkData() {
		PlayerData result = (PlayerData) super.getNetworkData();
		// result.state = this.fsmAction.getCurrentState();

		return result;
	}

	public PlayerAssetState getCurrentPlayerAsset() {
		return currentPlayerAsset;
	}

	public void setState(PlayerAssetState state) {
		if (currentPlayerAsset != null) {
			currentPlayerAsset.leave();
		}
		currentPlayerAsset = state;
		if (currentPlayerAsset != null) {
			currentPlayerAsset.enter();
		}
	}

	public void applyAction(PlayerActions action) {
		this.fsmAction.apply(action);
	}

	// physics
	@Override
	public boolean handleCollisions() {
		if (!isActive()) {
			return false;
		}

		boolean result = super.handleCollisions();

		// HACK for debug only
		if (Constants.Debug.showCollisions) {
			if (CollisionManager.collidingEntities(this).size() != 0) {
				this.map.setTileId(0, 0, 0, 0);
			} else {
				this.map.setTileId(0, 0, 0, 13);
			}
		}

		return result;
	}

	// input
	@Override
	public void handleInput(final Input input) {
		if (this.isActive()) {
			if (!InputControl.isRefKeyDown(InputControl.REFWALKLEFT)
					&& !InputControl.isRefKeyDown(InputControl.REFWALKRIGHT)
					&& !InputControl.isRefKeyDown(InputControl.REFJUMP)) {
				getAcc()[Entity.X] = 0.0f;
			}

			if (InputControl.isRefKeyDown(InputControl.REFWALKLEFT)) {
				fsmOrientation.apply(PlayerActions.Left);
			}

			if (InputControl.isRefKeyDown(InputControl.REFWALKRIGHT)) {
				fsmOrientation.apply(PlayerActions.Right);
			}

			if (InputControl.isRefKeyPressed(InputControl.REFJUMP)) {
				if (this.isOnGround()) {
					getVel()[Entity.Y] = -Constants.GamePlayConstants.playerJumpSpeed;
					applyAction(PlayerActions.Jump);
					SoundManager.playSound(Assets.PlayerJumpSoundId, 1f, 0.2f);
				}
			}

			if (InputControl.isRefKeyPressed(InputControl.REFFIRE)) {

				// TODO tell server to create bullet
				// TODO refactor PlayerAction to PlayerNetworkAction
				NetworkComponent.getInstance().sendCommand(
						new QueryAction(PlayerNetworkAction.SHOOT));
				applyAction(PlayerActions.StartShooting);
			}

			PlayerCondition state = this.getPlayerCondition();

			// change player color
			if (InputControl.isRefKeyPressed(InputControl.REFCHANGECOLOR)) {
				NetworkComponent.getInstance().sendCommand(
						new QueryAction(PlayerNetworkAction.PLAYERCOLOR));
				SoundManager.playSound(Assets.PlayerChangeColorSoundID, 1f, 0.2f);
			}

			// change weapon color
			if (InputControl.isRefKeyPressed(InputControl.REFCHANGEWEAPON)) {
				NetworkComponent.getInstance().sendCommand(
						new QueryAction(PlayerNetworkAction.WEAPONCOLOR));
				
				SoundManager.playSound(Assets.WeaponChangeColorSoundID, 1f, 0.2f);

				ParticleSystem particleSystem = weaponParticles.Assets()
						.getParticleSystem(Assets.WeaponParticleEffect);
				ConfigurableEmitter emitter = (ConfigurableEmitter) particleSystem
						.getEmitter(0);
				ColorRecord cr = (ColorRecord) emitter.colors.get(2);

				cr.col = StateColor.constIntoColor(state.weaponColor);
			}
		}
		super.handleInput(input);
	}

	public void nextColor() {
		PlayerCondition state = this.getPlayerCondition();

		state.color = state.color << 1;
		if (state.color > StateColor.BLUE) {
			state.color = StateColor.RED;
		}
	}

	public void nextWeaponColor() {
		PlayerCondition state = this.getPlayerCondition();

		state.weaponColor = state.weaponColor << 1;
		if (state.weaponColor > StateColor.BLUE) {
			state.weaponColor = StateColor.RED;
		}
	}

	public void die() {
		// TODO: Implement dying animation etc.
		this.respawn(); // FIXME: Do we want to respawn immediately?
		this.stats.increaseDeaths();
		Team.getTeamById(this.getPlayerCondition().teamId).increaseDeaths();
	}

	public void respawn() {
		setConditions();

		initData(new float[] { 200, 200, 65, 70, 1, 1, 0 });

		// TODO Reset Player state
		// setState(PlayerActionState.RunRight);
		// setState(PlayerActionState.Idle);
	}

	@Override
	protected void preRender(Graphics graphicContext) {
		super.preRender(graphicContext);

		if (Constants.Debug.shadersActive) {
			Shader.pushShader(playerShader);
			Shader.activateAdditiveBlending();
			float weaponGlowSize = 0.2f + this.getPlayerCondition().ammo * 0.8f;
			float glowSize = 0.2f + this.getPlayerCondition().health * 0.8f;

			float weaponX = this.getData(CENTER_X);
			float weaponY = this.getData(CENTER_Y) - weaponGlow.getHeight()
					* weaponGlowSize / 2 + 40;

			int lookDirection = 1;

			if (this.getData()[Entity.SCALE_X] == 1) // left
				lookDirection = -1;

			playerShader.setValue("playercolor", StateColor.constIntoColor(this
					.getPlayerCondition().weaponColor));

			graphicContext.drawImage(weaponGlow, weaponX, weaponY, weaponX
					+ weaponGlow.getWidth() * lookDirection, weaponY
					+ weaponGlow.getHeight() * weaponGlowSize, 0, 0,
					weaponGlow.getWidth(), weaponGlow.getHeight());

			playerShader.setValue("playercolor",
					StateColor.constIntoColor(this.getPlayerCondition().color));
			graphicContext.drawImage(playerGlow, this.getData(CENTER_X)
					- playerGlow.getWidth() * glowSize / 2,
					this.getData(CENTER_Y) - playerGlow.getHeight() * glowSize
							/ 2, this.getData(CENTER_X) + playerGlow.getWidth()
							* glowSize / 2,
					this.getData(CENTER_Y) + playerGlow.getHeight() * glowSize
							/ 2, 0, 0, playerGlow.getWidth(),
					playerGlow.getHeight());

			Shader.activateDefaultBlending();
		}

	}

	// render
	@Override
	public void renderImpl(final Graphics g, Image frameBuffer) {

		currentPlayerAsset.render(g, frameBuffer);
	
		super.renderImpl(g, frameBuffer);
	}

	@Override
	protected void postRender(Graphics graphicContext) {

		// deactivate shader
		if (Constants.Debug.shadersActive) {
			Shader.popShader();
		}

		super.postRender(graphicContext);

		// render player infos
		if (this.condition.name != null) {
			float x = getData()[Entity.X] + playerHalfWidth
					- graphicContext.getFont().getWidth(condition.name) / 2.0f;

			float y = getData()[Entity.Y]
					- graphicContext.getFont().getHeight(condition.name);

			// colors the name of player with his color

			if (!Constants.Debug.shadersActive) {
				graphicContext.setColor(StateColor
						.constIntoColor(condition.color));
			} else {
				graphicContext
						.setColor(Constants.GamePlayConstants.DefaultPlayerTextColor);
			}
			graphicContext.drawString(condition.name, x, y);
		}

	}

	// update
	@Override
	public void update(final int deltaInMillis) {

		if (this.isActive()) {

			// set Drag
			if (isOnGround()) {
				setDrag(Constants.GamePlayConstants.playerGroundDrag);
			} else {
				setDrag(Constants.GamePlayConstants.playerAirDrag);
			}

			super.update(deltaInMillis); // calc physics

			// Handle Player Actions according to physics state after update
			getCurrentPlayerAsset().update();

			if (this.getVel()[Entity.X] > Constants.GamePlayConstants.playerMaxSpeed) {
				this.getVel()[Entity.X] = Constants.GamePlayConstants.playerMaxSpeed;
			}

			if (this.getVel()[Entity.Y] > Constants.GamePlayConstants.playerMaxJumpSpeed) {
				this.getVel()[Entity.Y] = Constants.GamePlayConstants.playerMaxJumpSpeed;
			}

			if (this.getVel()[Entity.X] < -Constants.GamePlayConstants.playerMaxSpeed) {
				this.getVel()[Entity.X] = -Constants.GamePlayConstants.playerMaxSpeed;
			}

			if (this.getVel()[Entity.Y] < -Constants.GamePlayConstants.playerMaxJumpSpeed) {
				this.getVel()[Entity.Y] = -Constants.GamePlayConstants.playerMaxJumpSpeed;
			}

			// TODO fix PlayerState
			// if (this._currentState == PlayerActionState.Standing
			// && Math.abs(this.getData()[Entity.X]
			// - this.getPrevPos()[Entity.X]) < Constants.EPSILON
			// && Math.abs(this.getData()[Entity.Y]
			// - this.getPrevPos()[Entity.Y]) < Constants.EPSILON) {
			// this.getVel()[Entity.X] = this.getVel()[Entity.Y] = 0.0f;
			// }
		}
	}

	// getters + setters
	public PlayerCondition getPlayerCondition() {
		return condition;
	}
	public PlayerStats getPlayerStats() {
		return stats;
	}

	public void setPlayerCondition(PlayerCondition playerCondition) {
		this.condition = playerCondition;

	}

	@Override
	public void leavingState(PlayerActionState state) {
		// TODO Auto-generated method stub

	}

	@Override
	public void enteringState(PlayerActionState state) {
		switch (state) {
		case Left:
			getAcc()[Entity.X] = -Constants.GamePlayConstants.playerWalkSpeed;
			getData()[Entity.SCALE_X] = 1.0f;
			applyAction(PlayerActions.StartRunning);
			break;
		case Right:
			getAcc()[Entity.X] = Constants.GamePlayConstants.playerWalkSpeed;
			getData()[Entity.SCALE_X] = -1.0f;
			applyAction(PlayerActions.StartRunning);
			break;
		case Standing:
			setState(stateStanding);
			break;
		case Running:
			setState(stateRunning);
			break;
		case ShootStanding:
			setState(stateShootStanding);
			break;
		case ShootRunning:
			setState(stateShootRunning);
			break;
		case Jumping:
			setState(stateJumping);
			break;
		case ShootJumping:
			setState(stateShootJumping);
			break;
		case Landing:
			setState(stateLanding);
			break;
		case Falling:
			setState(stateFalling);
			break;
		case FallShooting:
			setState(stateShootFalling);
			break;
		default:
			if (Constants.Debug.finiteStateMachineDebug) {
				Log.error("FSM: state " + state + "unhandled");
			}
		}
	}

	public AssetMgr getAssetMgr() {
		return assets;
	}
}
