package TM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Vector;

import middleRM.ws.*;
import middleRM.ws.Main.Server;
import LockManager.*;

/*
 * questions: customers, should we add it to middleware before actually sending it to the servers? If so, that is what is implemented
 * follow-up question, how to make sure 2 transactions can't delete/create both customers locally before committing? put field in customer allowed transaction? 
 * */

//TODO: new/delete/reserve client methods with deadlocks return false, order of operations may corrupt state

/*singleton object that handles transactions of the middle-ware*/
public class TransactionManager implements server.ws.ResourceManager
{
	//unique string identifiers for strdata in lock table addressing
	private static final String CUSTOMER = "cu";
	private static final String HOTEL = "h";
	private static final String FLIGHT = "f";
	private static final String CAR = "c";
	
	//reference to singleton TransactionManager, to lock manager and to Main middle-ware server
	private static TransactionManager tm;
	private static LockManager lm = new LockManager();
	private static Main middleware;
	static Thread enforcer;
	
	//hashmap of current transactions
	static final HashMap<Integer, Transaction> trxns = new HashMap<Integer,Transaction>(1000);
	
	//hashmap of current customers
	static final HashMap<Integer, Customer> customers = new HashMap<Integer, Customer>(1000);
	
	//various immutable global variables
	public static final int READ = 0;
	public static final int WRITE = 1;
	public static final long TTL = 20000; //time to live : 20 seconds
	
	//returns instance of the transaction manager
	public static TransactionManager getInstance(Main main)
	{
		if(tm == null)
			tm = new TransactionManager(main);
		return tm;
	}
	
	//initiate new transaction manager. Constructor is only called once and we only get 1 enforcer
	private TransactionManager(Main m)
	{
		//setup Main reference for this class
		middleware = m;
		
		//fire new thread to enforce TTL mechanism for transactions
		enforcer = new Thread(new TTLEnforcer(this));
		enforcer.start();
	}
	
	//Start a new transaction and return its id
	@Override
	public int start()
	{
		//get new trxn id
		int randomTrnxId = Math.abs(new Random().nextInt());
		while ( trxns.containsKey(randomTrnxId))
			randomTrnxId = Math.abs(new Random().nextInt());
		
		//put in hashpmap of currently executed transactions
		trxns.put(randomTrnxId, new Transaction(randomTrnxId));
		return randomTrnxId;
	}
	
	//Start a new transaction with a specified id
	@Override
	public boolean startid(int tid) 
	{
		if (trxns.containsKey(tid))
			  return false;
		trxns.put(tid, new Transaction(tid));
		return true;
	}
	
	//Attempt to commit the given transaction; return true upon success; upon deadlock, we abort, upon a false response from server we abort as well
	@Override
	public boolean commit(int transactionId) 
	{
		 //check if transaction exists
		 if (!trxns.containsKey(transactionId))
			  return false;
		 
		 //get transaction
		Transaction t = trxns.get(transactionId);
		t.refreshTimeStamp(); //refresh time stamp so that TTLenforcer won't remove transaction
		
		//synchronize t to set isTerminating to true
		synchronized(t)
		{
			if ( t.isTerminating) //to prevent double aborts of a transaction
				return true;
			t.isTerminating= true;
		}
		
		//in case of a deadlock call
		try 
		{
			//iterate through all commands
			for ( String cmd : t.cmds())
				requestLock(t.tid, cmd); //get locks for each command
			
			//alert servers that transaction is beginning
			alertServersStart(t);
			
			//if we get all locks, we may start to execute the commands
			for ( String cmd : t.cmds())
			{
				//execute command
				executeCommand(t.tid, cmd);
			}
			
			//every operation committed to every server, we unlock all locks
			lm.UnlockAll(t.tid);
			
			//alert servers transaction has committed
			alertServersCommit(t);
			
			//remove transaction from currently executing transactions set
			trxns.remove(t.tid);
				
		} 
		catch (DeadlockException e) 
		{
			//we abort the transaction
			//e.printStackTrace();
			System.out.println("Deadlock: Transaction " + t.tid + " will abort.");
			abort(transactionId);
			return false;
		} 
		
		//return true to user, everything committed fine
		return true;
	}
	
	//alerts all the servers needed by the transaction that the transaction is starting
	private void alertServersStart(Transaction t) 
	{
		for( Server s : t.getServers())
			Main.services.get(s).proxy.startid(t.tid);
	}

	//alerts all the servers needed by the transaction that the transaction is committing
	private void alertServersCommit(Transaction t) 
	{
		for( Server s : t.getServers())
			Main.services.get(s).proxy.commit(t.tid);
	}
	
	//alerts all the servers needed by the transaction that the transaction is aborting
	private void alertServersAbort(Transaction t) 
	{
		for( Server s : t.getServers())
			Main.services.get(s).proxy.abort(t.tid);
	}
	
	//executes the command and returns the values obtained to the user
	private void executeCommand(int id, String cmd)
	{
		//split the csv string
		String[] args = cmd.split(",");
		
		switch (args[0])
		{
			case "+" + FLIGHT:  Main.services.get(Server.Flight).proxy.addFlight(id, Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
								break;		
			case "-" + FLIGHT: Main.services.get(Server.Flight).proxy.deleteFlight(id, Integer.parseInt(args[1]));
					   			break;
			case "q" + FLIGHT: //nothing to do, only read operation
					   			break; //nothing to do, only read operation
			case "p" + FLIGHT: //nothing to do, only read operation
					   			break; //nothing to do, only read operation
			case "+" + CAR: Main.services.get(Server.Car).proxy.addCars(id, args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]));  
					   		break;	
			case "-" + CAR:  Main.services.get(Server.Car).proxy.deleteCars(id, args[1]);
							break;
			case "q" + CAR: //nothing to do, only read operation
							break; //nothing to do, only read operation
			case "p" + CAR: //nothing to do, only read operation
							break; 
			case "+" + HOTEL:  Main.services.get(Server.Hotel).proxy.addRooms(id, args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3])); 
					   		break;
			case "-" + HOTEL:  Main.services.get(Server.Hotel).proxy.deleteRooms(id, args[1]);
	   		   		   		break;
			case "q" + HOTEL: //nothing to do, only read operation
								break; 
			case "p" + HOTEL: //nothing to do, only read operation
							 break; //nothing to do, only read operation
			case "+" + CUSTOMER:  Main.addCustomerToServices(id, Integer.parseInt(args[1])); 
								  customers.get(Integer.parseInt(args[1])).isNew = false;
								break;
			case "-" + CUSTOMER: Main.removeCustomerFromServices(id, Integer.parseInt(args[1]));
								customers.remove(Integer.parseInt(args[1]));
								break;
			case "q" + CUSTOMER:  //nothing to do, only read operation
								break; 	
			case "r" + FLIGHT:  Main.services.get(Server.Flight).proxy.reserveFlight(id, Integer.parseInt(args[1]), Integer.parseInt(args[2]));
					   			break;
			case "r" + CAR: 	Main.services.get(Server.Car).proxy.reserveCar(id, Integer.parseInt(args[1]), args[2]);
								break;
			case "r" + HOTEL:   Main.services.get(Server.Hotel).proxy.reserveRoom(id, Integer.parseInt(args[1]), args[2]);
					   			break;
			default: break; //reserve itinerary is a composite of the above actions, no need to actually make a cmd of it
		}
		
	}

	//requests the lock of a cmd throws a deadlock exception if we can't acquire the lock
	private void requestLock(int xid, String cmd) throws DeadlockException
	{
		//get parameters from csv string
		String[] args = cmd.split(",");
	
		//get different type of locks depending on operation and object
		switch (args[0])
		{
			case "+" + FLIGHT: lm.Lock(xid, FLIGHT + args[1], WRITE);
					   break;		
			case "-" + FLIGHT: lm.Lock(xid, FLIGHT + args[1], WRITE);
					   break;
			case "q" + FLIGHT: lm.Lock(xid, FLIGHT + args[1], READ);
					   break;
			case "p" + FLIGHT: lm.Lock(xid, FLIGHT + args[1], READ);
					   break;
			case "+" + CAR: lm.Lock(xid, CAR + args[1], WRITE);
					   break;	
			case "-" + CAR: lm.Lock(xid, CAR + args[1], WRITE);
			   		   break;
			case "q" + CAR: lm.Lock(xid, CAR + args[1], READ);
					   break;
			case "p" + CAR: lm.Lock(xid, CAR + args[1], READ);
					   break;
			case "+" + HOTEL: lm.Lock(xid, HOTEL + args[1], WRITE);
					   break;
			case "-" + HOTEL: lm.Lock(xid, HOTEL + args[1], WRITE);
	   		   		   break;
			case "q" + HOTEL: lm.Lock(xid, HOTEL + args[1], READ);
					   break;
			case "p" + HOTEL: lm.Lock(xid, HOTEL + args[1], READ);
					   break;
			case "+" + CUSTOMER: lm.Lock(xid, CUSTOMER + args[1], WRITE);
						break;
			case "-" + CUSTOMER: lm.Lock(xid, CUSTOMER + args[1], WRITE);
						break;
			case "q" + CUSTOMER: lm.Lock(xid, CUSTOMER + args[1], READ);
						break;
			case "r" + FLIGHT: lm.Lock(xid, CUSTOMER + args[1], READ);
							   lm.Lock(xid, FLIGHT + args[2], WRITE);
							   break;
			case "r" + CAR: lm.Lock(xid, CUSTOMER + args[1], READ);
					   		   lm.Lock(xid, CAR + args[2], WRITE);
							   break;
			case "r" + HOTEL: lm.Lock(xid, CUSTOMER + args[1], READ);
							   lm.Lock(xid, HOTEL + args[2], WRITE);
							   break;
			default: break; //reserve itinerary is a composite of the above actions, no need to actually make a cmd of it
		}
	}
	
	//cleans up the middle ware resources. In this case we have only customers 
	private void cleanup(String cmd) 
	{
		//get parameters from csv string
		String[] args = cmd.split(",");
	
		//get different type of locks depending on operation and object
		switch (args[0])
		{
			case "+" + CUSTOMER: //delete customer from the hash map if they were created from this transaction
						Customer c = customers.get(Integer.parseInt(args[1]));
						if (c.isNew)//if customer was created by this transaction, delete it
							customers.remove(Integer.parseInt(args[1]));
						break;
			case "-" + CUSTOMER: //reset the is deleted flag to false, the customer is not deleted by this transaction
						customers.get(args[1]).isDeleted = false;
						break;
			default: break; //no cleanup needed for other resources
		}
	}

	//Aborts the given transaction and rollbacks all cmds that have been executed
	@Override
	public boolean abort(int transactionId) 
	{
		 //check if transaction exists
		 if (!trxns.containsKey(transactionId))
			  return false;
		 
		 //get transaction
		 Transaction t = trxns.get(transactionId);
		 t.refreshTimeStamp();
		 
		//synchronize t to set isAborting to true
		synchronized(t)
		{
			if ( t.isTerminating) //to prevent double aborts/commits of a transaction
				return true;
			t.isTerminating= true;
		}
		 
		//clean up resources
		for ( String cmd : t.cmds())
			cleanup(cmd);
		
		 //unlock all resources held by transaction, if any
		 lm.UnlockAll(transactionId);
		 
		 //call all servers to tell them to abort
		 alertServersAbort(t);
		 
		 //delete transaction from pool of currently executing transactions
		 trxns.remove(transactionId);
	
		return true;
	}

	//shuts down the servers if no transactions are currently executing on the servers
	@Override
	public boolean shutdown() 
	{
		//get lock on trxns to prevent additions of transactions
		synchronized (trxns)
		{
			//check to see if transactions are still opened
			if ( !trxns.isEmpty())
				return false;
			enforcer.interrupt(); //kill the TLL thread
			
			//call shutdown for each non active server
			for (  Entry<Server, Connection> e: Main.services.entrySet())
			{
				try {
					e.getValue().proxy.shutdown();
				} catch (Exception e1) {
				}
			}
		}
		
		//terminate the middle-ware as well
		System.out.println("Shutting down...");
		System.exit(0);
		return true;
	}

	@Override
	public boolean addFlight(int id, int flightNumber, int numSeats, int flightPrice) 
	{
		  //check if transaction exists
		  if (!trxns.containsKey(id))
			  return false;
		  
		  //get transaction and update its structures
		  Transaction t = trxns.get(id);
		  
		  //update write set of transaction with modified values
		  Item flight = getFlight(flightNumber, t);
		  flight.count += numSeats;
		  flight.price = flightPrice > 0 ? flightPrice : flight.price;
		  flight.isDeleted = false;
		  
		  t.addServer(Server.Flight);
		  t.addOperationToExecute("+" + FLIGHT +"," + flightNumber + "," + numSeats +"," + flightPrice);
		  return true;
	}

	@Override
	public boolean deleteFlight(int id, int flightNumber) 
	{
		 //check if transaction exists
		 if (!trxns.containsKey(id))
			  return false;
		 
		 //get transaction and update its structures
		  Transaction t = trxns.get(id);
		  
		 //update write set of transaction with modified values
		 Item flight = getFlight(flightNumber, t);
		
		 //check if there are reservations on the flight
		 if ( !flight.isReserved)
			 flight.isDeleted = true;
			 
		 
		 //add operation to execute
		  t.addServer(Server.Flight);
		  t.addOperationToExecute("-" + FLIGHT +"," + flightNumber);
		  
		  //return whether or not the flight was deleted
		  return flight.isDeleted;
	}

	@Override
	public int queryFlight(int id, int flightNumber) 
	{
		 //check if transaction exists
		  if (!trxns.containsKey(id))
			  return 0;
		  
		  //get transaction and update its structures
		  Transaction t = trxns.get(id);
		  
		  //update write set of transaction with modified values
		 Item flight = getFlight(flightNumber, t);
		 
		 //update operations on transactions
		  t.addServer(Server.Flight);
		  t.addOperationToExecute("q" + FLIGHT +"," + flightNumber);
		  
		 if ( flight.isDeleted)
			return 0;
		 else
			 return flight.count;
	}

	@Override
	public int queryFlightPrice(int id, int flightNumber) 
	{ 
		  //check if transaction exists
		  if (!trxns.containsKey(id))
			  return 0;
		  
		  //get transaction and update its structures
		  Transaction t = trxns.get(id);
		  
		  //update write set of transaction with modified values
		 Item flight = getFlight(flightNumber, t);
		 
		 //update operations on transactions
		  t.addServer(Server.Flight);
		  t.addOperationToExecute("p" + FLIGHT +"," + flightNumber);
		  
		 if ( flight.isDeleted)
			return 0;
		 else
			 return flight.price;
	}

	@Override
	public boolean addCars(int id, String location, int numCars, int carPrice) 
	{
		 //check if transaction exists
		  if (!trxns.containsKey(id))
			  return false;
		  
		  //get transaction and update its structures
		  Transaction t = trxns.get(id);
		  
		  //update write set of transaction with modified values
		  Item car = getCar(location, t);
		  car.count += numCars;
		  car.price = carPrice > 0 ? carPrice : car.price;
		  car.isDeleted = false;
		  
		  t.addServer(Server.Car);
		  t.addOperationToExecute("+" + CAR +"," + location + "," + numCars +"," + carPrice);
		  return true;
	}

	@Override
	public boolean deleteCars(int id, String location) 
	{
		//check if transaction exists
		  if (!trxns.containsKey(id))
			  return false;
		  
		  //get transaction and update its structures
		  Transaction t = trxns.get(id);
		  
		  //update write set of transaction with modified values
		  Item car = getCar(location, t);
		  
		  //check if there are reservations on the cars
			 if ( !car.isReserved)
				 car.isDeleted = true;
		  
		  //add operation to execute
		  t.addServer(Server.Car);
		  t.addOperationToExecute("-" + CAR +"," + location);
		  
		//return whether or not the car was deleted
		  return car.isDeleted;
	}

	@Override
	public int queryCars(int id, String location) 
	{
		 //check if transaction exists
		 if (!trxns.containsKey(id))
			  return 0;

		 //check if transaction exists
		  if (!trxns.containsKey(id))
			  return 0;
		  
		  //get transaction and update its structures
		  Transaction t = trxns.get(id);
		  
		  //update write set of transaction with modified values
		 Item car = getCar(location, t);
		 
		 //update operations on transactions
		  t.addServer(Server.Car);
		  t.addOperationToExecute("q" + CAR +"," + car);
		  
		 if ( car.isDeleted)
			return 0;
		 else
			 return car.count;
	}

	@Override
	public int queryCarsPrice(int id, String location) 
	{
		 if (!trxns.containsKey(id))
			  return 0;

		 //check if transaction exists
		  if (!trxns.containsKey(id))
			  return 0;
		  
		  //get transaction and update its structures
		  Transaction t = trxns.get(id);
		  
		  //update write set of transaction with modified values
		  Item car = getCar(location, t);
		 
		 //update operations on transactions
		  t.addServer(Server.Car);
		  t.addOperationToExecute("p" + CAR +"," + car);
		  
		 if ( car.isDeleted)
			return 0;
		 else
			 return car.price;
	}

	@Override
	public boolean addRooms(int id, String location, int numRooms, int roomPrice) 
	{
		  //check if transaction exists
		  if (!trxns.containsKey(id))
			  return false;
		  
		  //get transaction and update its structures
		  Transaction t = trxns.get(id);
		  
		  Item room = getRoom(location, t);
		  room.count += numRooms;
		  room.price = roomPrice > 0 ? roomPrice : room.price;
		  room.isDeleted = false;
		  
		  //update write set of transaction with modified values
		  t.addServer(Server.Hotel);
		  t.addOperationToExecute("+" + HOTEL +"," + location + "," + numRooms +"," + roomPrice);
		  return true;
	}

	@Override
	public boolean deleteRooms(int id, String location) 
	{
		  //check if transaction exists
		  if (!trxns.containsKey(id))
			  return false;
		  
		  //get transaction and update its structures
		  Transaction t = trxns.get(id);
		  
		  //update write set of transaction with modified values
		  Item room = getRoom(location, t);
		  
		  //check if there are reservations on the rooms
			 if ( !room.isReserved)
				 room.isDeleted = true;
		  
		 //add operation to execute
		  t.addServer(Server.Hotel);
		  t.addOperationToExecute("-" + HOTEL +"," + location);
		  
		//return whether or not the room was deleted
		  return room.isDeleted;
	}

	@Override
	public int queryRooms(int id, String location) 
	{
		//check if transaction exists
		 if (!trxns.containsKey(id))
			  return 0;
		  
		  //get transaction and update its structures
		  Transaction t = trxns.get(id);
		  
		  //update write set of transaction with modified values
		  Item room = getRoom(location, t);
		 
		 //update operations on transactions
		  t.addServer(Server.Hotel);
		  t.addOperationToExecute("q" + HOTEL +"," + location);
		  
		 if ( room.isDeleted)
			return 0;
		 else
			return room.count;
	}

	@Override
	public int queryRoomsPrice(int id, String location) 
	{
		 //check if transaction exists
		 if (!trxns.containsKey(id))
			  return 0;
		  
		  //get transaction and update its structures
		  Transaction t = trxns.get(id);
		  
		  //update write set of transaction with modified values
		  Item room = getRoom(location, t);
		 
		 //update operations on transactions
		  t.addServer(Server.Hotel);
		  t.addOperationToExecute("p" + HOTEL +"," + location);
		  
		 if ( room.isDeleted)
			return 0;
		 else
			return room.price;		
	}

	@Override
	public int newCustomer(int id) 
	{
		 //check if transaction exists
		 if (!trxns.containsKey(id))
			  return -1;

		 //get transaction and update its structures
		 Transaction t = trxns.get(id);
		 t.addServer(Server.Car); //add server to transaction
		 t.addServer(Server.Flight); //add server to transaction
		 t.addServer(Server.Hotel); //add server to transaction
		 
		 //get randomId for customer
		 int  randomId = Math.abs(new Random().nextInt());
		 while(customers.containsKey(randomId))
			 randomId = Math.abs(new Random().nextInt());
		
		 //add to list of operations to be executed
		 t.addOperationToExecute("+cu," + randomId);
		 
		 try 
		 {
		 	lm.Lock(id, CUSTOMER + randomId, WRITE);
		 } 
		 catch (DeadlockException e) 
		 {
			return -1;
		 }
		 
		 //place customer in data structure
		 customers.put(randomId, new Customer(randomId));
		 
		 //return the id
		 return randomId;
	}
	
	@Override
	public boolean newCustomerId(int id, int customerId) 
	{
		//check if transaction exists
		 if (!trxns.containsKey(id))
			  return false;
		 if( customers.containsKey(customerId) && !customers.get(customerId).isDeleted)
			  return false;
		
		 //get transaction and update its structures
		 Transaction t = trxns.get(id);
		 t.addServer(Server.Car); //add server to transaction
		 t.addServer(Server.Flight); //add server to transaction
		 t.addServer(Server.Hotel); //add server to transaction
		 
		 //add to list of operations to be executed
		 t.addOperationToExecute("+" + CUSTOMER +"," + customerId);
		 
		 try 
		 {
		 	lm.Lock(id, CUSTOMER+customerId, WRITE);
		 } 
		 catch (DeadlockException e) 
		 {
			return false;
		 }
		 
		 //place customer in data structure
		 customers.put(customerId, new Customer(customerId));
		 
		 //return the id
		 return true;
	}

	@Override
	public boolean deleteCustomer(int id, int customerId) 
	{
		//check if transaction exists
		 if (!trxns.containsKey(id))
			  return false;
		 if( !customers.containsKey(customerId) || customers.get(customerId).isDeleted)
			  return false;
		
		try 
		{
			lm.Lock(id, CUSTOMER+customerId, WRITE);
		} 
		catch (DeadlockException e) 
		{
			return false;
		}
		
		 //get transaction
		 Transaction t = trxns.get(id);
		 
		 //update its structures
		 t.addServer(Server.Car); //add server to transaction
		 t.addServer(Server.Flight); //add server to transaction
		 t.addServer(Server.Hotel); //add server to transaction
		 
		 //add to list of operations to be executed
		 t.addOperationToExecute("-" + CUSTOMER +"," + customerId);
		 
		 //place customer in data structure
		 customers.get(customerId).isDeleted = true;
		 //customers.remove(customerId);
		 
		 //return the id
		 return true;	
	}

	@Override
	public String queryCustomerInfo(int id, int customerId) 
	{
		//check if transaction exists
		 if (!trxns.containsKey(id))
			  return "wrong transaction id";
		 //check if customer actually exists
		 if (!customers.containsKey(customerId) || customers.get(customerId).isDeleted)
			 return "customer doesn't exist";
	
		 try 
		 { 
			lm.Lock(id, CUSTOMER + customerId, READ);
		 } 
		  catch (DeadlockException e) 
		  {

		  }
		 
		 //get transaction
		 Transaction t = trxns.get(id);
		 
		 //update transaction structure
		 t.addServer(Server.Car); //add server to transaction
		 t.addServer(Server.Flight); //add server to transaction
		 t.addServer(Server.Hotel); //add server to transaction
		 
		 //add to list of operations to be executed
		 t.addOperationToExecute("qcu," + customerId);
		 
		 if(customers.get(customerId).isNew)
		 {
			 String Bill = "Items reserved so far : \n{\n\t";
			 for(Entry<String, Item> e : customers.get(customerId).reservations.entrySet())
			 {
				 Bill = e.getKey().substring(1) + " : " + e.getValue().count + " seats reserved : $" + (e.getValue().price * e.getValue().count) + "\n\t";
			 }
			 return Bill;
		 }
		
		 //get customer
		 Customer c = customers.get(customerId);
		 
		 //get all csv (of the form  key,num,price) bills from different servers
		 String flight = Main.services.get(Server.Flight).proxy.queryCustomerInfo(id, customerId);
		 String car = Main.services.get(Server.Car).proxy.queryCustomerInfo(id, customerId);
		 String room = Main.services.get(Server.Hotel).proxy.queryCustomerInfo(id, customerId);
		 
		 String Bill = "";
		 if ( flight != null)
		 {
			 String[] flights = flight.split("\n");
			 Bill = "Flights :\n{\n\t";
			 //iterate over all records in flight bill
			 for (String reservedItem : flights)
			 {
				 //break record into components
				 String[] args = reservedItem.split(",");
				 
				 String key = FLIGHT + args[0];
				 if ( c.reservations.containsValue(t.writeSet.get(key)) &&  !t.writeSet.get(key).isDeleted)
				 {
					 Item i = t.writeSet.get(key);
					 Bill = args[0] + " : " + i.count + " seats reserved : $" + (i.price * i.count) + "\n\t";
				 }
				 else
				 {
					 Bill += args[0] + " : " + args[1] + " seats reserved : $" + (Integer.parseInt(args[2]) * Integer.parseInt(args[1])) + "\n\t";
				 }
				 
			 }
			 Bill += "}";
		 }
		 if ( car != null)
		 {
			 String[] cars = car.split("\n");
			 Bill += "\n Cars :\n{\n\t";
				//iterate over all records in car bill
				 for (String reservedItem : cars)
				 {
					 //break record into components
					 String[] args = reservedItem.split(",");
					 
					 String key = CAR + args[0];
					 if ( c.reservations.containsValue(t.writeSet.get(key)) &&  !t.writeSet.get(key).isDeleted)
					 {
						 Item i = t.writeSet.get(key);
						 Bill = args[0] + " : " + i.count + " cars reserved : $" + (i.price * i.count) + "\n\t";
					 }
					 else
					 {
						 Bill += args[0] + " : " + args[1] + " cars reserved : $" + (Integer.parseInt(args[2]) * Integer.parseInt(args[1])) + "\n\t";
					 }
					 
				 }
				 Bill += "}";
		 }
		 if ( room != null)
		 {
			 //System.out.println("rooms : " + room + " is null " + room == null);
			 String[] rooms = room.split("\n");
			 Bill += "}\n Rooms :\n{\n\t";
				//iterate over all records in hotel bill
				 for (String reservedItem : rooms)
				 {
					 //break record into components
					 String[] args = reservedItem.split(",");
					 
					 String key = HOTEL + args[0];
					 if ( c.reservations.containsValue(t.writeSet.get(key)) &&  !t.writeSet.get(key).isDeleted)
					 {
						 Item i = t.writeSet.get(key);
						 Bill = args[0] + " : " + i.count + " rooms reserved : $" + (i.price * i.count) + "\n\t";
					 }
					 else
					 {
						 Bill += args[0] + " : " + args[1] + " rooms reserved : $" + (Integer.parseInt(args[2]) * Integer.parseInt(args[1])) + "\n\t";
					 }	 
				 }
				 Bill += "}\n";
		 }
		 return Bill == "" ? "no bill for customer " + customerId : Bill; 
	}

	@Override
	public boolean reserveFlight(int id, int customerId, int flightNumber) 
	{
		  //check if transaction exists
		  if (!trxns.containsKey(id))
			  return false;
		  if( !customers.containsKey(customerId)  || customers.get(customerId).isDeleted)
			  return false;
		  
		  try 
		  {
			  //acquire lock on customer
		 	 lm.Lock(id, CUSTOMER + customerId, READ);
		  } 
		  catch (DeadlockException e) 
		  {

		  }
		  
		  //get transaction and update its structures
		  Transaction t = trxns.get(id);
		  
		  //update write set of transaction with modified values
		  Item flight = getFlight(flightNumber, t);
		  
		  //get customer
		  Customer c = customers.get(customerId);
		  
		  //can't add reservation if no seats or flight is deleted
		  if ( flight.isDeleted ||  flight.count == 0)
			  return false;
		
		  //update values of flight and customer data structures
		  flight.isReserved = true;
		  flight.count -= 1;
		  c.addReservation(FLIGHT+ flightNumber, flight);

		 //update the transaction fields
		  t.addServer(Server.Flight);
		  t.addOperationToExecute("r" + FLIGHT +"," + customerId + "," + flightNumber);
		  return true;
	}

	@Override
	public boolean reserveCar(int id, int customerId, String location) 
	{
		//check if transaction exists
		if (!trxns.containsKey(id))
			  return false;
		if( !customers.containsKey(customerId) || customers.get(customerId).isDeleted)
			  return false;
		  
		  try 
		  {
			  //acquire lock on customer
		 	 lm.Lock(id, CUSTOMER + customerId, READ);
		  } 
		  catch (DeadlockException e) 
		  {

		  }
		  
		  //get transaction and update its structures
		  Transaction t = trxns.get(id);
		  
		  //update write set of transaction with modified values
		  Item car = getCar(location, t);
		  
		  //get customer
		  Customer c = customers.get(customerId);
		  
		  //can't add reservation if no cars or car is deleted
		  if ( car.isDeleted ||  car.count == 0)
			  return false;
		
		  //update values of car and customer data structures
		  car.isReserved = true;
		  car.count -= 1;
		  c.addReservation(CAR + location, car);

		 //update the transaction fields
		  t.addServer(Server.Car);
		  t.addOperationToExecute("r" + CAR +"," + customerId + "," + location);
		  return true;
	}

	@Override
	public boolean reserveRoom(int id, int customerId, String location) 
	{
		//check if transaction exists
		if (!trxns.containsKey(id))
			  return false;
		if( !customers.containsKey(customerId) || customers.get(customerId).isDeleted)
			  return false;
		  
		  try 
		  {
			  //acquire lock on customer
		 	 lm.Lock(id, CUSTOMER + customerId, READ);
		  } 
		  catch (DeadlockException e) 
		  {

		  }
		  
		  //get transaction and update its structures
		  Transaction t = trxns.get(id);
		  
		  //update write set of transaction with modified values
		  Item room = getCar(location, t);
		  
		  //get customer
		  Customer c = customers.get(customerId);
		  
		  //can't add reservation if no rooms or room is deleted
		  if ( room.isDeleted || room.count == 0)
			  return false;
		
		  //update values of room and customer data structures
		  room.isReserved = true;
		  room.count -= 1;
		  c.addReservation(HOTEL + location, room);

		 //update the transaction fields
		  t.addServer(Server.Hotel);
		  t.addOperationToExecute("r" + HOTEL +"," + customerId + "," + location);
		  return true;
	}

	@Override
	public synchronized boolean reserveItinerary(int id, int customerId, Vector flightNumbers, String location, boolean car, boolean room) 
	{
		 if (!trxns.containsKey(id))
			  return false;
		 if( !customers.containsKey(customerId)|| customers.get(customerId).isDeleted)
			  return false;
		 
		//get transaction and update its structures
		Transaction t = trxns.get(id);
		  
		try 
		{
			//acquire lock on customer
			lm.Lock(id, CUSTOMER + customerId, READ);
		} 
		catch (DeadlockException e) 
		{

		}
		
		//update transaction data structures
		 if (!flightNumbers.isEmpty()) t.addServer(Server.Flight); //add server to transaction
		 if (car) t.addServer(Server.Car); //add server to transaction
		 if (room)t.addServer(Server.Hotel); //add server to transaction
		 
		//check for available slots on flights
    	for(Object flightNum : flightNumbers)
    	{
			//get lock on flight number
			int flightNumber =  Integer.parseInt(flightNum.toString());
		
			//query flight
			if (queryFlight(id, flightNumber) == 0)
				return false;
    	}
		 
    	//check for available car, if needed
    	if (car)
    	{
    		if (queryCars(id, location) == 0)
    			return false;
    	}
    	
    	//check for available room, if needed
    	if (room)
    	{
    		if (queryRooms(id, location) == 0)
    			return false;
    	}
		 
    	//reserve seats
    	for(Object flightNum : flightNumbers)
    	{
			int flightNumber =  Integer.parseInt(flightNum.toString());
			if (!reserveFlight(id, customerId, flightNumber))
				return false;
    	}
    	
    	if (car)
    		if (!reserveCar(id, customerId, location))
    			return false;

    	if (room)
    		if (reserveRoom(id, customerId,  location))
    			return false;
    	return true;
	}

	//gets the image of a database flight item for a transaction and updates the write set of the latter
	private Item getFlight(int fid, Transaction t)
	{
		//create unique key built on fid
		String key = FLIGHT + fid;
		
		//lock object in the lock manager so that the database can never be updated by another transaction
		try 
		{
			lm.Lock(t.tid, key, READ);
		} 
		catch (DeadlockException e) 
		{
			
		}
		
		//check if transaction t contains this object with the given key
		if ( !t.writeSet.containsKey(key))
		{
			//query database for most up to date image of item on the server
			int seatsAvailable = Main.services.get(Server.Flight).proxy.queryFlight(t.tid, fid);
			int price = Main.services.get(Server.Flight).proxy.queryFlightPrice(t.tid, fid);
			boolean reserved =  Main.services.get(Server.Flight).proxy.isFlightReserved(t.tid, fid);
			
			//create new item
			Item flight = new Item(seatsAvailable, price, reserved);
			
			//add item to transactions write set
			t.writeSet.put(key, flight);
		}
		
		return t.writeSet.get(key);
	}
	
	//gets the image of a database car item for a transaction and updates the write set of the latter
	private Item getCar(String location, Transaction t)
	{
		//create unique key built on fid
		String key = CAR + location;
		
		//lock object in the lock manager so that the database can never be updated by another transaction
		try 
		{
			lm.Lock(t.tid, key, READ);
		} 
		catch (DeadlockException e) 
		{
			
		}
		
		//check if transaction t contains this object with the given key
		if ( !t.writeSet.containsKey(key))
		{
			//query database for most up to date image of item on the server
			int carsAvailable = Main.services.get(Server.Car).proxy.queryCars(t.tid, location);
			int price = Main.services.get(Server.Car).proxy.queryCarsPrice(t.tid, location);
			boolean reserved = Main.services.get(Server.Car).proxy.isCarReserved(t.tid, location);
			
			//create new item
			Item car = new Item(carsAvailable, price, reserved);
			
			//add item to transactions write set
			t.writeSet.put(key, car);
		}
		
		return t.writeSet.get(key);
	}
	
	//gets the image of a database room item for a transaction and updates the write set of the latter
	private Item getRoom(String location, Transaction t)
	{
		//create unique key built on fid
		String key = HOTEL + location;
		
		//lock object in the lock manager so that the database can never be updated by another transaction
		try 
		{
			lm.Lock(t.tid, key, READ);
		} 
		catch (DeadlockException e) 
		{
			
		}
		
		//check if transaction t contains this object with the given key
		if ( !t.writeSet.containsKey(key))
		{
			//query database for most up to date image of item on the server
			int roomsAvailable = Main.services.get(Server.Hotel).proxy.queryRooms(t.tid, location);
			int price = Main.services.get(Server.Hotel).proxy.queryRoomsPrice(t.tid, location);
			boolean reserved = Main.services.get(Server.Hotel).proxy.isRoomReserved(t.tid, location);
			
			//create new item
			Item room = new Item(roomsAvailable, price, reserved);
			
			//add item to transactions write set
			t.writeSet.put(key, room);
		}
		return t.writeSet.get(key);
	}

	@Override
	public boolean isFlightReserved(int id, int fid) 
	{
		return Main.services.get(Server.Flight).proxy.isFlightReserved(id, fid);
	}

	@Override
	public boolean isCarReserved(int id, String location) 
	{
		return Main.services.get(Server.Car).proxy.isCarReserved(id, location);
	}

	@Override
	public boolean isRoomReserved(int id, String location) 
	{
		return Main.services.get(Server.Hotel).proxy.isRoomReserved(id, location);
	}
}