package middleRM.ws;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.jws.WebService;

import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import LockManager.*; //for ass2
import TM.*;

import javax.naming.NamingException;

import org.apache.catalina.startup.Tomcat;

@WebService
public class Main implements server.ws.ResourceManager { //server.ws.ResourceManager
	
	//the 3 RM connections to the middle ressource manager
	public static final HashMap<Server, Connection> services = new HashMap<Server, Connection>(3);

	//customer support
	
	
	public enum Server {Flight, Car, Hotel;}
	
	//singleton object for transaction manager
	private final TransactionManager tm = TransactionManager.getInstance(this);
	
	//public static final int READ = 0;
	//public static final int WRITE = 1;
	//public static final long TTL = 20000; //time to live : 20 seconds
	
	/*
	 * Assignment 2 data structures required:
	 * 
	 * Memory to undo transactions
	 * lock manager
	 * set of transactions currently opened
	 * */
	//public static final HashMap<Integer, Transaction> trxns = new HashMap<Integer, Transaction>(1000);
	
	
	
	
	
	//TODO: customers data structure is fine, since we add teh customer after locking it 
	// 		put cmds after locking in try block
	
	public Main() throws NamingException, MalformedURLException
	{
		//Context env = (Context) new InitialContext().lookup("java:comp/env");
	
		//get flight info and place in services
		//String flightServiceHost = "lab9-28";//(String) env.lookup("flight-service-host");
		String flightServiceHost = "localhost";
		Integer flightServicePort = 1410;//(Integer) env.lookup("flight-service-port");
		String flightServiceName = "flight";//(String) env.lookup("flight-service-name");
		Connection flightServer = new Connection(flightServiceName, flightServiceHost, flightServicePort );
		services.put(Server.Flight, flightServer);
		
		//get car info and place in services
		//String carServiceHost = "lab9-28";//(String) env.lookup("flight-service-host");
		String carServiceHost = "localhost";
		Integer carServicePort = 1411;//(Integer) env.lookup("flight-service-port");
		String carServiceName = "car";//(String) env.lookup("flight-service-name");
		Connection carServer = new Connection(carServiceName, carServiceHost, carServicePort );
		services.put(Server.Car, carServer);
		
		//get hotel info and place in services
		//String hotelServiceHost = "lab9-28";//(String) env.lookup("flight-service-host");
		String hotelServiceHost = "localhost";
		Integer hotelServicePort = 1412;//(Integer) env.lookup("flight-service-port");
		String hotelServiceName = "room";//(String) env.lookup("flight-service-name");
		Connection hotelServer = new Connection(hotelServiceName, hotelServiceHost, hotelServicePort );
		services.put(Server.Hotel, hotelServer);
		
		//dispatch a thread to enforce TTL for transactions
		//new TTLEnforcer(this).run();
	}
	
	public static void main(String[] args) throws Exception 
	{  
        if (args.length != 3) {
           System.out.println(
                "Usage: java Main <service-name> <service-port> <deploy-dir> but for middleRM, so 12 inputs");
            System.exit(-1);
        }
        //services = new HashMap<String, Connection>(3);
        for(String s : args)
        {
        	System.out.println(s);
        }
       
        //setup Tomcat
        String serviceName = args[0];
        int port = Integer.parseInt(args[1]);
        String deployDir = args[2];
        
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.setBaseDir(deployDir);

        tomcat.getHost().setAppBase(deployDir);
        tomcat.getHost().setDeployOnStartup(true);
        tomcat.getHost().setAutoDeploy(true);

        //tomcat.addWebapp("", new File(deployDir).getAbsolutePath());

        
       // System.out.println("inputs: /" + serviceName + " ,file path : " + new File(deployDir + "/" + serviceName).getAbsolutePath());
        tomcat.addWebapp("/" + serviceName, 
                new File(deployDir + "/" + serviceName).getAbsolutePath());
      
        tomcat.enableNaming();
        tomcat.start();
        tomcat.getServer().await();
	}

	@Override
	public boolean addFlight(int id, int flightNumber, int numSeats, int flightPrice) 
	{
		return tm.addFlight(id, flightNumber, numSeats, flightPrice);
	}

	@Override
	public boolean deleteFlight(int id, int flightNumber)
	{
		return tm.deleteFlight(id, flightNumber);
	}

	@Override
	public int queryFlight(int id, int flightNumber) 
	{
		return tm.queryFlight(id, flightNumber);
	}

	@Override
	public int queryFlightPrice(int id, int flightNumber) 
	{
		return tm.queryFlightPrice(id, flightNumber);
	}

	@Override
	public boolean addCars(int id, String location, int numCars, int carPrice) 
	{
		return tm.addCars(id, location, numCars, carPrice);
	}

	@Override
	public boolean deleteCars(int id, String location) 
	{
		return tm.deleteCars(id, location);
	}

	@Override
	public int queryCars(int id, String location) 
	{
		return tm.queryCars(id, location);
	}

	@Override
	public int queryCarsPrice(int id, String location) 
	{
		return tm.queryCarsPrice(id, location);
	}

	@Override
	public boolean addRooms(int id, String location, int numRooms, int roomPrice) 
	{
		return tm.addRooms(id, location, numRooms, roomPrice);
	}

	@Override
	public boolean deleteRooms(int id, String location) 
	{
		return tm.deleteRooms(id, location);
	}

	@Override
	public int queryRooms(int id, String location) 
	{
		return tm.queryRooms(id, location);
	}

	@Override
	public int queryRoomsPrice(int id, String location) 
	{
		return tm.queryRoomsPrice(id, location);
	}

	@Override
	public int newCustomer(int id) 
	{
		return tm.newCustomer(id);
	}
	
	@Override
	public boolean newCustomerId(int id, int customerId)
	{
		return tm.newCustomerId(id, customerId);
	}

	@Override
	public boolean deleteCustomer(int id, int customerId) 
	{
		return tm.deleteCustomer(id, customerId);
	}

	@Override
	public String queryCustomerInfo(int id, int customerId) 
	{
		return tm.queryCustomerInfo(id, customerId);
	}

	@Override
	public boolean reserveFlight(int id, int customerId, int flightNumber) 
	{	
		return tm.reserveFlight(id, customerId, flightNumber);
	}

	@Override
	public boolean reserveCar(int id, int customerId, String location) 
	{
		return tm.reserveCar(id, customerId, location);
	}

	@Override
	public boolean reserveRoom(int id, int customerId, String location) 
	{
		return tm.reserveRoom(id, customerId, location);
	}

	
	//TODO: if we get stuck on lock, do we abort whole transaction or just send false to user and unlock everything and abort transaction?
	@Override
	public boolean reserveItinerary(int id, int customerId, Vector flightNumbers, String location, boolean car, boolean room) 
	{
		return tm.reserveItinerary(id, customerId, flightNumbers, location, car, room);
	}
	
	//TODO: check the following 2 methods
	public static boolean addCustomerToServices(int id, int customerId)
	{
		//customers.put(customerId, new Customer(customerId));
		int count = 0;
		for( Entry<Server, Connection> connection : services.entrySet())
		{
			if(connection.getValue().proxy.newCustomerId(id, customerId))
				count++;
		}
		return count==services.size();
	}

	public static boolean removeCustomerFromServices(int id, int customerId)
	{
		///customers.remove(customerId);
		int count = 0;
		for( Entry<Server, Connection> connection : services.entrySet())
		{
			if (connection.getValue().proxy.deleteCustomer(id, customerId))
				count++;
		}
		return count==services.size();
	}

	@Override
	//Start a new transaction and return its id
	public int start() {
		return tm.start();
	}
	
	@Override
	//Start a new transaction and return its id
	public boolean startid(int tid) {
		return tm.startid(tid);
	}

	@Override
	//Attempt to commit the given transaction; return true upon success
	public boolean commit(int transactionId) {
		boolean hasCommitted = tm.commit(transactionId);
		if (hasCommitted)
			System.out.println("Transaction " + transactionId + " successfully committed.");
		else
			System.out.println("Transaction " + transactionId + " did not commit.");
		return hasCommitted;
	}

	@Override
	//Abort the given transaction.
	public boolean abort(int transactionId) {
		
		boolean hasAborted = tm.abort(transactionId);
		if (hasAborted)
			System.out.println("Transaction " + transactionId + " successfully aborted.");
		else
			System.out.println("Transaction " + transactionId + " did not abort correctly.");
		return hasAborted;
	}

	
	
	
	/*table of possible strings as cmds
	 * 
	 * f : flight
		h : hotel
		c : car
		cu : customer
		i : itinerary
		
		
		cmds
		"+f," + flightNumber + "," + numSeats +"," + flightPrice; (add flight)
		"-f," + flightNumber + "," + seats + "," + price;		  (deleteFlight)
		"qf," + flightNumber;									  (queryFlight)
		"pf," + flightNumber; 									  (queryFlightPrice)
		
		 "+c," + location + "," + numCars +"," + carPrice		  (addCar)
		 "-c," + location + "," + numCars + "," + price;		  (deleteCar)
		 "qc," + location;										  (queryCar)
		 "pc," + location;										  (queryCarsPrice)
		
		 "+h," + location + "," + numRooms +"," + roomPrice;      (addRoom)
		 "-h," + location + "," + numRooms + "," + price;		  (deleteRooms)
		 "qh," + location;										  (queryRooms)
		 "ph," + location;										  (queryRoomsPrice)
		
		
		"+cu," + randomId										  (newCustomer)
		"+cu," + customerId;									  (newCustomerId)
		"-cu," + customerId;									  (deleteCustomer)
		"qcu," + customerId;									  (queryCustomerInfo)
		
		 "rf," + customerId + "," + flightNumber + "," + seats + "," + price (reserveFlight)
		 "rc," + customerId + "," +  location + "," + numCars + "," + price  (reserveCar)
		 "rh," + customerId + "," + location + "," + numRooms + "," + price; (reserveRoom)
		
		 READ/f/
	 * */	
	/*private void rollbackOperations(int id) {
		
		//get trx
		Transaction t = trxns.get(id);
	
		//get list of cmds so far
		String[] cmds =  new String[t.cmds().size()];
	    t.cmds().toArray(cmds);
		
		//iterate backwards through the commands
		for ( int i = cmds.length -1; i >= 0 ; i--)
		{
			//get parameters from csv string
			String[] args = cmds[i].split(",");
			
			switch (args[0])
			{
				case "+f":  services.get(Server.Flight).proxy.deleteFlight(id, Integer.parseInt(args[1])); //lm.Lock(id, "-f" + args[1], WRITE); //a;lready have this
							break;		
				case "-f": services.get(Server.Flight).proxy.addFlight(id, Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
						   break;
				case "qf": break; //nothing to do, only read operation
				case "pf": break; //nothing to do, only read operation
				case "+c": services.get(Server.Car).proxy.deleteCars(id, args[1]);
						   break;	
				case "-c": services.get(Server.Car).proxy.addCars(id, args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]));
				   		   break;
				case "qc": break; //nothing to do, only read operation
				case "pc": break; //nothing to do, only read operation
				case "+h": services.get(Server.Hotel).proxy.deleteRooms(id, args[1]);
						   break;
				case "-h": services.get(Server.Hotel).proxy.addRooms(id, args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]));
		   		   		   break;
				case "qh": break; //nothing to do, only read operation
				case "ph": break; //nothing to do, only read operation
				case "+cu": removeCustomerFromServices(id, Integer.parseInt(args[1]));
							customers.remove(Integer.parseInt(args[1]));
							break;//careful 2 cases here,id or  newcustomerId + id 
				case "-cu": addCustomerToServices(id, Integer.parseInt(args[1]));
							customers.put(Integer.parseInt(args[1]), new Customer(Integer.parseInt(args[1])));
							break;
				case "qcu": break; //nothing to do, only read operation
				
				case "rf": //services.get(Server.Flight).proxy.deleteFlight(id, Integer.parseInt(args[2])); //delete the whole flight
						   services.get(Server.Flight).proxy.addFlight(id, Integer.parseInt(args[2]), 1, Integer.parseInt(args[4])); //and recreate the whole flight
						   break;
				case "rc": //services.get(Server.Car).proxy.deleteCars(id, args[2]);
						   services.get(Server.Car).proxy.addCars(id, args[2], 1, Integer.parseInt(args[4]));
						   break;
				case "rh": //services.get(Server.Hotel).proxy.deleteRooms(id, args[2]);
						   services.get(Server.Hotel).proxy.addRooms(id, args[2], 1, Integer.parseInt(args[4]));
						   break;
				default: break; //reserve itinerary is a composite of the above actions, no need to actually make a cmd of it
			}
		}
	}*/

	@Override
	/* Shutdown gracefully
	 * 		at RM: it is assumed that shutdown is only called when there is no transaction active at the RM.
	 * 			   When a shutdown RM restarts, it does not need to perform any recovery;
	 * 		At middleware layer:  call shutdown of all RMs;
	 * 		at TM: if relevant.
	 */
	public boolean shutdown() {
		
		return tm.shutdown();
		/*LinkedList<Server> nonActiveServers = new LinkedList<Server>(Arrays.asList(Server.Car, Server.Flight, Server.Hotel));
		
		//iterate over each transaction, get all active servers involved in each of them, remove them from the list
		//we are left with non active servers at the end (servers not involved in transactions)
		for ( Entry<Integer, Transaction> e : trxns.entrySet())
		{
			for ( Server s : e.getValue().servers())
			{
				if (nonActiveServers.contains(s))
				{
					nonActiveServers.remove(s);
				}
			}
		}
		
		
		if ( nonActiveServers.size() != 3)
		{
			//at least one server is still being used
			return false;
		}
		
		//call shutdown for each non active server
		for ( Server s : nonActiveServers)
		{
			services.get(s).proxy.shutdown();
		}
		
		//call shutdown for middleware
		System.exit(0);
		// TODO Auto-generated method stub*/
	}

	@Override
	public boolean isFlightReserved(int id, int fid) 
	{
		return services.get(Server.Flight).proxy.isFlightReserved(id, fid);
	}

	@Override
	public boolean isCarReserved(int id, String location) 
	{
		return services.get(Server.Car).proxy.isCarReserved(id, location);
	}

	@Override
	public boolean isRoomReserved(int id, String location) 
	{
		return services.get(Server.Hotel).proxy.isRoomReserved(id, location);
	}
}