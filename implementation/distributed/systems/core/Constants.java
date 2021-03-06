package distributed.systems.core;

public final class Constants{
	
	public static final String HOSTNAME="localhost";
	
	public static final int NUMBER_OF_SERVERS = 5;
	
	public static int[] TOM_PORTS = {2000,2001,2002,2003,2004};
	
	public static int[] RECEIVER_PORTS = {3000,3001,3002,3003,3004};
	
	public static int[] MULTICAST_PORTS = {4000,4001,4002,4003,4004};
	
	public static int[] PUBLISH_PORTS = {5000,5001,5002,5003,5004};
	
	public static ServerAndPorts[] SERVER_PORT = {
			new ServerAndPorts(0, Constants.HOSTNAME, 2000, 3000, 4000, 5000),
			new ServerAndPorts(1, Constants.HOSTNAME, 2001, 3100, 4001, 5001),
			new ServerAndPorts(2, Constants.HOSTNAME, 2002, 3200, 4002, 5002),
			new ServerAndPorts(3, Constants.HOSTNAME, 2003, 3300, 4003, 5003),
			new ServerAndPorts(4, Constants.HOSTNAME, 2004, 3400, 4004, 5004),
	};
	
	public static final int THREAD_POOL_SIZE = 10;
	public static final int ORIGINAL_MSG = 1;
	public static final int PROPOSED_TIMESTAMP_MSG = 2;
	public static final int FINAL_TIMESTAMP_MSG = 3;	
	
	
	public static final int MIN_PLAYER_COUNT = 30;
	public static final int MAX_PLAYER_COUNT = 60;
	public static final int DRAGON_COUNT = 20;
	public static final int TIME_BETWEEN_PLAYER_LOGIN = 5000; // In milliseconds

}
