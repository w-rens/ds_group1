package distributed.systems.das.units;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import distributed.systems.core.IMessageReceivedHandler;
import distributed.systems.core.Message;
import distributed.systems.core.ServerAndPorts;
import distributed.systems.das.BattleField;
import distributed.systems.das.GameState;
import distributed.systems.das.MessageRequest;
import distributed.systems.das.units.extra.Coordinate;
import distributed.systems.das.units.extra.UnitType;
import distributed.systems.executors.ClientExecutor;
import distributed.systems.executors.ServerExecutor;

/**
 * Base class for all players whom can participate in the DAS game. All
 * properties of the units (hitpoints, attackpoints) are initialized in this
 * class.
 * 
 * @author Pieter Anemaet, Boaz Pat-El
 */
public abstract class Unit implements Serializable, IMessageReceivedHandler
{
	private static final long serialVersionUID = -4550572524008491160L;

	// Position of the unit
	protected int x, y;

	// Health
	private int maxHitPoints;
	protected int hitPoints;

	// Attack points
	protected int attackPoints;

	// Identifier of the unit
	private int unitID;

	// The communication socket between this client and the board
	//	protected Socket clientSocket;

	// Map messages from their ids
	private Map<Integer, Message> messageList;
	// Is used for mapping an unique id to a message sent by this unit
	private int localMessageCounter = 0;

	// If this is set to false, the unit will return its run()-method and disconnect from the server
	protected boolean running;

	/* The thread that is used to make the unit run in a separate thread.
	 * We need to remember this thread to make sure that Java exits cleanly.
	 * (See stopRunnerThread())
	 */
	protected Thread runnerThread;

	private transient ServerExecutor serverExecutor;
	private transient ClientExecutor clientExecutor;
	private transient ServerAndPorts sp;
	
	/**
	 * Create a new unit and specify the number of hitpoints. Units hitpoints
	 * are initialized to the maxHitPoints.
	 * 
	 * @param maxHealth
	 *            is the maximum health of this specific unit.
	 */
	//include url + port
	public Unit(int maxHealth, int attackPoints)
	{
		//		Socket localSocket = new Socket();

		messageList = new HashMap<Integer, Message>();

		// Initialize the max health and health
		hitPoints = maxHitPoints = maxHealth;

		// Initialize the attack points
		this.attackPoints = attackPoints;
		
		// Get a new unit id
		unitID = BattleField.getBattleField().getNewUnitID();

		// Create a new socket
		//		clientSocket = new SynchronizedSocket(localSocket);
		serverExecutor = BattleField.getBattleField().getServerExecutor();
		sp = serverExecutor.getServerPortData();
		
//		try
//		{
//			// Try to register the socket
//			clientSocket.register("D" + unitID);
//		}
//		catch (AlreadyAssignedIDException e)
//		{
//			System.err.println("Socket \"D" + unitID + "\" was already registered.");
//		}
//
//		clientSocket.addMessageReceiveHandler(this);
	}
	
	public void setClientExecutor(ClientExecutor ce) {
		this.clientExecutor = ce;
	}
	
	public ClientExecutor getClientExecutor() {
		return this.clientExecutor;
	}

	/**
	 * Adjust the hitpoints to a certain level. Useful for healing or dying
	 * purposes.
	 * 
	 * @param modifier
	 *            is to be added to the hitpoint count.
	 */
	public synchronized void adjustHitPoints(int modifier)
	{
		if (hitPoints <= 0)
			return;

		hitPoints += modifier;

		if (hitPoints > maxHitPoints)
			hitPoints = maxHitPoints;

		if (hitPoints <= 0)
			removeUnit(x, y);
	}

	public void dealDamage(int x, int y, int damage)
	{
		/* Create a new message, notifying the board
		 * that a unit has been dealt damage.
		 */
		int id;
		Message damageMessage;
		synchronized (this)
		{
			id = localMessageCounter++;

			damageMessage = new Message();
			damageMessage.put("request", MessageRequest.dealDamage);
			damageMessage.put("x", x);
			damageMessage.put("y", y);
			damageMessage.put("damage", damage);
			damageMessage.put("id", id);
			damageMessage.put("port",clientExecutor.getPort() );
		}

		// Send a spawn message
		//		clientSocket.sendMessage(damageMessage, "localsocket://" + BattleField.serverID);
		//this.serverExecutor.sendMessageToMany(damageMessage);
		System.out.println("Unit:dealDamage");
		ServerExecutor.sendMessageToServer(sp,damageMessage);
	}

	public void healDamage(int x, int y, int healed)
	{
		/* Create a new message, notifying the board
		 * that a unit has been healed.
		 */
		int id;
		Message healMessage;
		synchronized (this)
		{
			id = localMessageCounter++;

			healMessage = new Message();
			healMessage.put("request", MessageRequest.healDamage);
			healMessage.put("x", x);
			healMessage.put("y", y);
			healMessage.put("healed", healed);
			healMessage.put("id", id);
			healMessage.put("port", clientExecutor.getPort());
		}

		// Send a spawn message
		//		clientSocket.sendMessage(healMessage, "localsocket://" + BattleField.serverID);
		System.out.println("Unit:healDamage");
		ServerExecutor.sendMessageToServer(sp,healMessage);
		//this.serverExecutor.sendMessageToMany(healMessage); This is wrong
	}

	/**
	 * @return the maximum number of hitpoints.
	 */
	public int getMaxHitPoints()
	{
		return maxHitPoints;
	}

	/**
	 * @return the unique unit identifier.
	 */
	public int getUnitID()
	{
		return unitID;
	}

	/**
	 * Set the position of the unit.
	 * 
	 * @param x
	 *            is the new x coordinate
	 * @param y
	 *            is the new y coordinate
	 */
	public void setPosition(int x, int y)
	{
		this.x = x;
		this.y = y;
	}

	/**
	 * @return the x position
	 */
	public int getX()
	{
		return x;
	}

	/**
	 * @return the y position
	 */
	public int getY()
	{
		return y;
	}

	/**
	 * @return the current number of hitpoints.
	 */
	public int getHitPoints()
	{
		return hitPoints;
	}

	/**
	 * @return the attack points
	 */
	public int getAttackPoints()
	{
		return attackPoints;
	}

	/**
	 * Tries to make the unit spawn at a certain location on the battlefield
	 * 
	 * @param x
	 *            x-coordinate of the spawn location
	 * @param y
	 *            y-coordinate of the spawn location
	 * @return true if the unit could spawn at the location on the battlefield
	 */
	protected boolean spawn(int x, int y)
	{
		/* Create a new message, notifying the board
		 * the unit has actually spawned at the
		 * designated position. 
		 */
		int id = localMessageCounter++;
		Message spawnMessage = new Message();
		spawnMessage.put("request", MessageRequest.spawnUnit);
		spawnMessage.put("x", x);
		spawnMessage.put("y", y);
		spawnMessage.put("unit", this);
		spawnMessage.put("id", id);
		spawnMessage.put("port", clientExecutor.getPort());
		//clientSocket.sendMessage(spawnMessage, "localsocket://" + BattleField.serverID);
		//this.serverExecutor.sendMessageToMany(spawnMessage);
		System.out.println("Unit:spawn");
		ServerExecutor.sendMessageToServer(sp,spawnMessage);
		// Wait for the unit to be placed
		getUnit(x, y);

		return true;
	}

	/**
	 * Returns whether the indicated square contains a player, a dragon or
	 * nothing.
	 * 
	 * @param x:
	 *            x coordinate
	 * @param y:
	 *            y coordinate
	 * @return UnitType: the indicated square contains a player, a dragon or
	 *         nothing.
	 */
	protected UnitType getType(int x, int y)
	{
		Message getMessage = new Message(), result;
		int id = localMessageCounter++;
		getMessage.put("request", MessageRequest.getType);
		getMessage.put("x", x);
		getMessage.put("y", y);
		getMessage.put("id", id);
		getMessage.put("port", clientExecutor.getPort());
		
		// Send the getUnit message
		//		clientSocket.sendMessage(getMessage, "localsocket://" + BattleField.serverID);
		//this.serverExecutor.sendMessageToMany(getMessage);
		System.out.println("Unit:getType");
		ServerExecutor.sendMessageToServer(sp,getMessage);

		// Wait for the reply
		while (!messageList.containsKey(id))
		{
			try
			{
				Thread.sleep(50);
			}
			catch (InterruptedException e)
			{
			}

			// Quit if the game window has closed
			if (!GameState.getRunningState())
				return UnitType.undefined;
		}

		result = messageList.get(id);
		if (result == null) // Could happen if the game window had closed
			return UnitType.undefined;
		messageList.put(id, null);

		return (UnitType)result.get("type");

	}

	protected Unit getUnit(int x, int y)
	{
		Message getMessage = new Message(), result;
		int id = localMessageCounter++;
		getMessage.put("request", MessageRequest.getUnit);
		getMessage.put("x", x);
		getMessage.put("y", y);
		getMessage.put("id", id);
		getMessage.put("port", clientExecutor.getPort());
		// Send the getUnit message
		//		clientSocket.sendMessage(getMessage, "localsocket://" + BattleField.serverID);
		//this.serverExecutor.sendMessageToMany(getMessage);
		System.out.println("Unit:getUnit");
		ServerExecutor.sendMessageToServer(sp,getMessage);
		// Wait for the reply
		while (!messageList.containsKey(id))
		{
			try
			{
				Thread.sleep(50);
			}
			catch (InterruptedException e)
			{
			}

			// Quit if the game window has closed
			if (!GameState.getRunningState())
				return null;
		}

		result = messageList.get(id);
		messageList.put(id, null);

		return (Unit)result.get("unit");
	}

	protected void removeUnit(int x, int y)
	{
		Message removeMessage = new Message();
		int id = localMessageCounter++;
		removeMessage.put("request", MessageRequest.removeUnit);
		removeMessage.put("x", x);
		removeMessage.put("y", y);
		removeMessage.put("id", id);
		removeMessage.put("port", clientExecutor.getPort());
		// Send the removeUnit message
		//		clientSocket.sendMessage(removeMessage, "localsocket://" + BattleField.serverID);
		//this.serverExecutor.sendMessageToMany(removeMessage);
		System.out.println("Unit:removeUnit");
		ServerExecutor.sendMessageToServer(sp,removeMessage);
	}

	protected void moveUnit(int x, int y)
	{
		Message moveMessage = new Message();
		int id = localMessageCounter++;
		moveMessage.put("request", MessageRequest.moveUnit);
		moveMessage.put("x", x);
		moveMessage.put("y", y);
		moveMessage.put("id", id);
		moveMessage.put("unit", this);
		moveMessage.put("port", clientExecutor.getPort() );
		// Send the getUnit message
		//		clientSocket.sendMessage(moveMessage, "localsocket://" + BattleField.serverID);
		//this.serverExecutor.sendMessageToMany(moveMessage);
		System.out.println("Unit:moveUnit");
		ServerExecutor.sendMessageToServer(sp, moveMessage);
		// Wait for the reply
		while (!messageList.containsKey(id))
		{
			try
			{
				Thread.sleep(50);
			}
			catch (InterruptedException e)
			{
			}

			// Quit if the game window has closed
			if (!GameState.getRunningState())
				return;
		}

		// Remove the result from the messageList
		messageList.put(id, null);
	}

	public void onMessageReceived(Message message)
	{
		messageList.put((Integer)message.get("id"), message);
	}

	// Disconnects the unit from the battlefield by exiting its run-state
	public void disconnect()
	{
		running = false;
	}

	/**
	 * Stop the running thread. This has to be called explicitly to make sure
	 * the program terminates cleanly.
	 */
	public void stopRunnerThread()
	{
		try
		{
			runnerThread.join();
		}
		catch (InterruptedException ex)
		{
			assert (false) : "Unit stopRunnerThread was interrupted";
		}

	}

	public Coordinate getPosition()
	{
		return new Coordinate(x, y);
	}
}
