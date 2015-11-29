// -------------------------------
// Adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.Map.Entry;

import javax.jws.WebService;

import main.Main;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import filemanager.FileManager;


@WebService(endpointInterface = "server.ws.ResourceManager")
public class ResourceManagerImpl implements server.ws.ResourceManager {
    
    private boolean isOnline = false;
    protected RMHashtable m_itemHT = new RMHashtable();
    protected RMHashtable prepared_itemHT = new RMHashtable();
    protected RMHashtable tempHT = null;
    
    //resources needed to reboot server
    private FileManager fm;
    private String directory = "server/";
    private static String masterFile = "m.txt";
    private static String shadowFile = "f2.txt";
    private static String currentFile = "f1.txt";
    private static String copyFile = "copy.txt";
    
    private static TimeoutEnforcer timeoutEnforcer;
    
    //constructor to obtain the type of server (Flight, Car, Room) upon booting up to check for master node
	 public ResourceManagerImpl()
	 {
		 try 
		 {
			//get server's responsibility
            Context env = (Context) new InitialContext().lookup("java:comp/env");
            String serviceName = (String) env.lookup("service-name");
            
            //get directory for server to read files from
            directory += serviceName + "/";
            
            //set all files 
            masterFile = directory + masterFile;
            shadowFile = directory + shadowFile; 
            currentFile = directory + currentFile;
            copyFile = directory + copyFile;
            
            //TODO: remove theses checks once you are done
           /* System.out.println("path is  " + System.getProperty("user.dir"));
            System.out.println("does client0 exist? " + new File("client0.txt").exists());
            System.out.println("does master file exist? " + new File(masterFile).exists());
            System.out.println("does current file exist? " + new File(shadowFile).exists());
            System.out.println("does shadow file exist? " + new File(currentFile).exists());*/
            
            //create new file manager
            fm = new FileManager(masterFile, shadowFile, currentFile, copyFile);
            
            //set up hashtable
            Object data = fm.readFromStableStorage();
            if ( data != null)
            {
            	System.out.println("read from disk, recovering... ");
            	m_itemHT = (RMHashtable) data;
            }
            else
            {
            	System.out.println("could not read from disk ... ");
            }
        } 
		catch (Exception ex) 
		{
            ex.printStackTrace();
            System.exit(1);
	    }
	 }
    
    // Basic operations on RMItem //
    
    // Read a data item.
    private RMItem readData(int id, String key) {
        synchronized(m_itemHT) {
            return (RMItem) m_itemHT.get(key);
        }
    }

    // Write a data item.
    private void writeData(int id, String key, RMItem value) {
        synchronized(m_itemHT) {
            m_itemHT.put(key, value);
        }
    }
    
    // Remove the item out of storage.
    protected RMItem removeData(int id, String key) {
        synchronized(m_itemHT) {
            return (RMItem) m_itemHT.remove(key);
        }
    }
    
    /**
     * This method should be called after the RM restarts and before it handles
     * any other call
     * @param lastCommitedTxn the last committed transaction, sent by the middleware
     * @return true on successful recovery and synch, false otherwise
     */
    @Override
    public boolean recover(int lastCommitedTxn) {
        
        if(lastCommitedTxn == 0) 
        {
            //initial activation, sent from middleware constructor
            isOnline = true;
            return true;
        }
        
    	System.out.println("last committed transaction (" + fm.getLastCommittedTxn() + "), passed value: " + lastCommitedTxn);

        //check if last committed transaction is not the same as the recorded one in the file manager
        if(lastCommitedTxn != fm.getLastCommittedTxn())
        {
        	System.out.println("last committed transaction (" + fm.getLastCommittedTxn() + ") not the same as passed value: " + lastCommitedTxn);
        	//switch master to shadow copy
        	fm.changeMasterToShadowCopy(lastCommitedTxn);
        	
        	m_itemHT = (RMHashtable) fm.readFromStableStorage(); //switch hashtables
        }
        
        //server has fully recovered and is available to recover
        isOnline = true;
        return true;
    }
    
    public boolean isServerOnline() {
        //checks if server is online and in synch with other servers
        //      isOnline is set to false initially and MW constructor calls recover to set it to true
        //      if RM crashes, it is false until MW calls recover, 
        //      while set to false server refuses all method calls (returns false or zero) 
        return isOnline;
        //return true;
    }
    
    // Basic operations on ReservableItem //
    
    // Delete the entire item.
    protected boolean deleteItem(int id, String key) {
        Trace.info("RM::deleteItem(" + id + ", " + key + ") called.");
        ReservableItem curObj = (ReservableItem) readData(id, key);
        // Check if there is such an item in the storage.
        if (curObj == null) {
            Trace.warn("RM::deleteItem(" + id + ", " + key + ") failed: " 
                    + " item doesn't exist.");
            return false;
        } else {
            if (curObj.getReserved() == 0) {
                removeData(id, curObj.getKey());
                Trace.info("RM::deleteItem(" + id + ", " + key + ") OK.");
                return true;
            }
            else {
                Trace.info("RM::deleteItem(" + id + ", " + key + ") failed: "
                        + "some customers have reserved it.");
                return false;
            }
        }
    }
    
    // Query the number of available seats/rooms/cars.
    protected int queryNum(int id, String key) {
        Trace.info("RM::queryNum(" + id + ", " + key + ") called.");
        ReservableItem curObj = (ReservableItem) readData(id, key);
        int value = 0;  
        if (curObj != null) {
            value = curObj.getCount();
        }
        Trace.info("RM::queryNum(" + id + ", " + key + ") OK: " + value);
        return value;
    }    
    
    // Query the price of an item.
    protected int queryPrice(int id, String key) {
        Trace.info("RM::queryCarsPrice(" + id + ", " + key + ") called.");
        ReservableItem curObj = (ReservableItem) readData(id, key);
        int value = 0; 
        if (curObj != null) {
            value = curObj.getPrice();
        }
        Trace.info("RM::queryCarsPrice(" + id + ", " + key + ") OK: $" + value);
        return value;
    }

    // Reserve an item.
    protected boolean reserveItem(int id, int customerId, 
                                  String key, String location) {
        Trace.info("RM::reserveItem(" + id + ", " + customerId + ", " 
                + key + ", " + location + ") called.");
        // Read customer object if it exists (and read lock it).
        Customer cust = (Customer) readData(id, Customer.getKey(customerId));
        if (cust == null) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", " 
                   + key + ", " + location + ") failed: customer doesn't exist.");
            return false;
        } 
        
        // Check if the item is available.
        ReservableItem item = (ReservableItem) readData(id, key);
        if (item == null) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", " 
                    + key + ", " + location + ") failed: item doesn't exist.");
            return false;
        } else if (item.getCount() == 0) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", " 
                    + key + ", " + location + ") failed: no more items.");
            return false;
        } else {
            // Do reservation.
            cust.reserve(key, location, item.getPrice());
            writeData(id, cust.getKey(), cust);
            
            // Decrease the number of available items in the storage.
            item.setCount(item.getCount() - 1);
            item.setReserved(item.getReserved() + 1);
            
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", " 
                    + key + ", " + location + ") OK.");
            return true;
        }
    }
    
    
    // Flight operations //
    
    // Create a new flight, or add seats to existing flight.
    // Note: if flightPrice <= 0 and the flight already exists, it maintains 
    // its current price.
    @Override
    public boolean addFlight(int id, int flightNumber, 
                             int numSeats, int flightPrice) {
        if(!isServerOnline()) return false;
        Trace.info("RM::addFlight(" + id + ", " + flightNumber 
                + ", $" + flightPrice + ", " + numSeats + ") called.");
        Flight curObj = (Flight) readData(id, Flight.getKey(flightNumber));
        if (curObj == null) {
            // Doesn't exist; add it.
            Flight newObj = new Flight(flightNumber, numSeats, flightPrice);
            writeData(id, newObj.getKey(), newObj);
            Trace.info("RM::addFlight(" + id + ", " + flightNumber 
                    + ", $" + flightPrice + ", " + numSeats + ") OK.");
        } else {
            // Add seats to existing flight and update the price.
            curObj.setCount(curObj.getCount() + numSeats);
            if (flightPrice > 0) {
                curObj.setPrice(flightPrice);
            }
            writeData(id, curObj.getKey(), curObj);
            Trace.info("RM::addFlight(" + id + ", " + flightNumber 
                    + ", $" + flightPrice + ", " + numSeats + ") OK: "
                    + "seats = " + curObj.getCount() + ", price = $" + flightPrice);
        }
        return(true);
    }

    @Override
    public boolean deleteFlight(int id, int flightNumber) {
        if(!isServerOnline()) return false;
        return deleteItem(id, Flight.getKey(flightNumber));
    }

    // Returns the number of empty seats on this flight.
    @Override
    public int queryFlight(int id, int flightNumber) {
        if(!isServerOnline()) return 0;
        return queryNum(id, Flight.getKey(flightNumber));
    }

    // Returns price of this flight.
    public int queryFlightPrice(int id, int flightNumber) {
        if(!isServerOnline()) return 0;
        return queryPrice(id, Flight.getKey(flightNumber));
    }

    /*
    // Returns the number of reservations for this flight. 
    public int queryFlightReservations(int id, int flightNumber) {
        Trace.info("RM::queryFlightReservations(" + id 
                + ", #" + flightNumber + ") called.");
        RMInteger numReservations = (RMInteger) readData(id, 
                Flight.getNumReservationsKey(flightNumber));
        if (numReservations == null) {
            numReservations = new RMInteger(0);
       }
        Trace.info("RM::queryFlightReservations(" + id + 
                ", #" + flightNumber + ") = " + numReservations);
        return numReservations.getValue();
    }
    */
    
    /*
    // Frees flight reservation record. Flight reservation records help us 
    // make sure we don't delete a flight if one or more customers are 
    // holding reservations.
    public boolean freeFlightReservation(int id, int flightNumber) {
        Trace.info("RM::freeFlightReservations(" + id + ", " 
                + flightNumber + ") called.");
        RMInteger numReservations = (RMInteger) readData(id, 
                Flight.getNumReservationsKey(flightNumber));
        if (numReservations != null) {
            numReservations = new RMInteger(
                    Math.max(0, numReservations.getValue() - 1));
        }
        writeData(id, Flight.getNumReservationsKey(flightNumber), numReservations);
        Trace.info("RM::freeFlightReservations(" + id + ", " 
                + flightNumber + ") OK: reservations = " + numReservations);
        return true;
    }
    */


    // Car operations //

    // Create a new car location or add cars to an existing location.
    // Note: if price <= 0 and the car location already exists, it maintains 
    // its current price.
    @Override
    public boolean addCars(int id, String location, int numCars, int carPrice) {
        if(!isServerOnline()) return false;
        Trace.info("RM::addCars(" + id + ", " + location + ", " 
                + numCars + ", $" + carPrice + ") called.");
        Car curObj = (Car) readData(id, Car.getKey(location));
        if (curObj == null) {
            // Doesn't exist; add it.
            Car newObj = new Car(location, numCars, carPrice);
            writeData(id, newObj.getKey(), newObj);
            Trace.info("RM::addCars(" + id + ", " + location + ", " 
                    + numCars + ", $" + carPrice + ") OK.");
        } else {
            // Add count to existing object and update price.
            curObj.setCount(curObj.getCount() + numCars);
            if (carPrice > 0) {
                curObj.setPrice(carPrice);
            }
            writeData(id, curObj.getKey(), curObj);
            Trace.info("RM::addCars(" + id + ", " + location + ", " 
                    + numCars + ", $" + carPrice + ") OK: " 
                    + "cars = " + curObj.getCount() + ", price = $" + carPrice);
        }
        return(true);
    }

    // Delete cars from a location.
    @Override
    public boolean deleteCars(int id, String location) {
        if(!isServerOnline()) return false;
        return deleteItem(id, Car.getKey(location));
    }

    // Returns the number of cars available at a location.
    @Override
    public int queryCars(int id, String location) {
        if(!isServerOnline()) return 0;
        return queryNum(id, Car.getKey(location));
    }

    // Returns price of cars at this location.
    @Override
    public int queryCarsPrice(int id, String location) {
        if(!isServerOnline()) return 0;
        return queryPrice(id, Car.getKey(location));
    }
    

    // Room operations //

    // Create a new room location or add rooms to an existing location.
    // Note: if price <= 0 and the room location already exists, it maintains 
    // its current price.
    @Override
    public boolean addRooms(int id, String location, int numRooms, int roomPrice) {
        if(!isServerOnline()) return false;
        Trace.info("RM::addRooms(" + id + ", " + location + ", " 
                + numRooms + ", $" + roomPrice + ") called.");
        Room curObj = (Room) readData(id, Room.getKey(location));
        if (curObj == null) {
            // Doesn't exist; add it.
            Room newObj = new Room(location, numRooms, roomPrice);
            writeData(id, newObj.getKey(), newObj);
            Trace.info("RM::addRooms(" + id + ", " + location + ", " 
                    + numRooms + ", $" + roomPrice + ") OK.");
        } else {
            // Add count to existing object and update price.
            curObj.setCount(curObj.getCount() + numRooms);
            if (roomPrice > 0) {
                curObj.setPrice(roomPrice);
            }
            writeData(id, curObj.getKey(), curObj);
            Trace.info("RM::addRooms(" + id + ", " + location + ", " 
                    + numRooms + ", $" + roomPrice + ") OK: " 
                    + "rooms = " + curObj.getCount() + ", price = $" + roomPrice);
        }
        return(true);
    }

    // Delete rooms from a location.
    @Override
    public boolean deleteRooms(int id, String location) {
        if(!isServerOnline()) return false;
        return deleteItem(id, Room.getKey(location));
    }

    // Returns the number of rooms available at a location.
    @Override
    public int queryRooms(int id, String location) {
        if(!isServerOnline()) return 0;
        return queryNum(id, Room.getKey(location));
    }
    
    // Returns room price at this location.
    @Override
    public int queryRoomsPrice(int id, String location) {
        if(!isServerOnline()) return 0;
        return queryPrice(id, Room.getKey(location));
    }


    // Customer operations //

    @Override
    public int newCustomer(int id) {
        if(!isServerOnline()) return 0;
        Trace.info("INFO: RM::newCustomer(" + id + ") called.");
        // Generate a globally unique Id for the new customer.
        int customerId = Integer.parseInt(String.valueOf(id) +
                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                String.valueOf(Math.round(Math.random() * 100 + 1)));
        Customer cust = new Customer(customerId);
        writeData(id, cust.getKey(), cust);
        Trace.info("RM::newCustomer(" + id + ") OK: " + customerId);
        return customerId;
    }

    // This method makes testing easier.
    @Override
    public boolean newCustomerId(int id, int customerId) {
        if(!isServerOnline()) return false;
        Trace.info("INFO: RM::newCustomer(" + id + ", " + customerId + ") called.");
        Customer cust = (Customer) readData(id, Customer.getKey(customerId));
        if (cust == null) {
            cust = new Customer(customerId);
            writeData(id, cust.getKey(), cust);
            Trace.info("INFO: RM::newCustomer(" + id + ", " + customerId + ") OK.");
            return true;
        } else {
            Trace.info("INFO: RM::newCustomer(" + id + ", " + 
                    customerId + ") failed: customer already exists.");
            return false;
        }
    }

    // Delete customer from the database. 
    @Override
    public boolean deleteCustomer(int id, int customerId) {
        if(!isServerOnline()) return false;
        Trace.info("RM::deleteCustomer(" + id + ", " + customerId + ") called.");
        Customer cust = (Customer) readData(id, Customer.getKey(customerId));
        if (cust == null) {
            Trace.warn("RM::deleteCustomer(" + id + ", " 
                    + customerId + ") failed: customer doesn't exist.");
            return false;
        } else {            
            // Increase the reserved numbers of all reservable items that 
            // the customer reserved. 
            RMHashtable reservationHT = cust.getReservations();
            for (Enumeration e = reservationHT.keys(); e.hasMoreElements();) {        
                String reservedKey = (String) (e.nextElement());
                ReservedItem reservedItem = cust.getReservedItem(reservedKey);
                Trace.info("RM::deleteCustomer(" + id + ", " + customerId + "): " 
                        + "deleting " + reservedItem.getCount() + " reservations "
                        + "for item " + reservedItem.getKey());
                ReservableItem item = 
                        (ReservableItem) readData(id, reservedItem.getKey());
                item.setReserved(item.getReserved() - reservedItem.getCount());
                item.setCount(item.getCount() + reservedItem.getCount());
                Trace.info("RM::deleteCustomer(" + id + ", " + customerId + "): "
                        + reservedItem.getKey() + " reserved/available = " 
                        + item.getReserved() + "/" + item.getCount());
            }
            // Remove the customer from the storage.
            removeData(id, cust.getKey());
            Trace.info("RM::deleteCustomer(" + id + ", " + customerId + ") OK.");
            return true;
        }
    }

    // Return data structure containing customer reservation info. 
    // Returns null if the customer doesn't exist. 
    // Returns empty RMHashtable if customer exists but has no reservations.
    public RMHashtable getCustomerReservations(int id, int customerId) {
        if(!isServerOnline()) return null;
        Trace.info("RM::getCustomerReservations(" + id + ", " 
                + customerId + ") called.");
        Customer cust = (Customer) readData(id, Customer.getKey(customerId));
        if (cust == null) {
            Trace.info("RM::getCustomerReservations(" + id + ", " 
                    + customerId + ") failed: customer doesn't exist.");
            return null;
        } else {
            return cust.getReservations();
        }
    }

    // Return a bill.
    @Override
    public String queryCustomerInfo(int id, int customerId) {
        if(!isServerOnline()) return "";
        Trace.info("RM::queryCustomerInfo(" + id + ", " + customerId + ") called.");
        Customer cust = (Customer) readData(id, Customer.getKey(customerId));
        if (cust == null) {
            Trace.warn("RM::queryCustomerInfo(" + id + ", " 
                    + customerId + ") failed: customer doesn't exist.");
            // Returning an empty bill means that the customer doesn't exist.
            return null;
        } else {
            String s = cust.printBill();
            Trace.info("RM::queryCustomerInfo(" + id + ", " + customerId + "): \n");
            System.out.println(s);
            return s;
        }
    }

    // Add flight reservation to this customer.  
    @Override
    public boolean reserveFlight(int id, int customerId, int flightNumber) {
        if(!isServerOnline()) return false;
        return reserveItem(id, customerId, 
                Flight.getKey(flightNumber), String.valueOf(flightNumber));
    }

    // Add car reservation to this customer. 
    @Override
    public boolean reserveCar(int id, int customerId, String location) {
        if(!isServerOnline()) return false;
        return reserveItem(id, customerId, Car.getKey(location), location);
    }

    // Add room reservation to this customer. 
    @Override
    public boolean reserveRoom(int id, int customerId, String location) {
        if(!isServerOnline()) return false;
        return reserveItem(id, customerId, Room.getKey(location), location);
    }
    

    // Reserve an itinerary.
    @Override
    public boolean reserveItinerary(int id, int customerId, Vector flightNumbers,
                                    String location, boolean car, boolean room) {
    	return false;
    }

	@Override
	public int start() {
	    if(!isServerOnline()) return 0;
		System.out.println("Transaction initiated");
		return 0;
	}
	
	private static final Object LOCK = new Object();
	
	@Override
	public boolean startid(int tid) {
	    if(!isServerOnline()) return false;
	    
	    //prevent 2 prepare statements from racing against each other
  		synchronized(trxPrepared)
  		{
  			//transaction prepared == -1, it is open to grab
  			if (trxPrepared == -1)
  				trxPrepared = tid;
  			//not the right transaction id, we return false
  			else if (tid != trxPrepared)
  				return false;
  		}
  		
  		//create new thread to enforce timeout mechanism
  		timeoutEnforcer = new TimeoutEnforcer(tid, this);
  		timeoutEnforcer.start();
	    
	    //copy current hashtable
  		prepared_itemHT = (RMHashtable) fm.deepCopy(m_itemHT);
  		               
        //swap main and prepared hastables so that cmds are made in the prepared hashtable
        synchronized(LOCK)
        {
        	tempHT = m_itemHT;
            m_itemHT = prepared_itemHT;
        }
      
        //reset lastCommitedTxn of file manager
        //fm.resetLastCommittedTxn(tid); //TODO: not sure here, if server crashes the last committed txn will be wrong?

		System.out.println("Transaction initiated : " + tid);
		return true;
	}

	@Override
	public boolean shutdown() {
	    if(!isServerOnline()) return false;
		System.out.println("Shutting down...");
		System.exit(0);
		return false;
	}

	@Override
	public boolean isFlightReserved(int id, int fid) {
	    if(!isServerOnline()) return false;
		 ReservableItem i = (ReservableItem) readData(id, "" + fid);
		 if ( i == null)
			 return false;
		 else
			 return i.getReserved() > 0;
	}

	@Override
	public boolean isCarReserved(int id, String location) {
	    if(!isServerOnline()) return false;
		 ReservableItem i = (ReservableItem) readData(id, location);
		 if ( i == null)
			 return false;
		 else
			 return i.getReserved() > 0;
	}

	@Override
	public boolean isRoomReserved(int id, String location) {
	    if(!isServerOnline()) return false;
		 ReservableItem i = (ReservableItem) readData(id, location);
		 if ( i == null)
			 return false;
		 else
			 return i.getReserved() > 0;
	}

	//int to represent transaction preparing to commit, only 1 at a time
	 private Integer trxPrepared = -1;
	 
	@Override
	//writes the current values in its hashtable in stable storage,awaiting the commit request
	public boolean prepare(int transactionId) 
	{
	    if(!isServerOnline()) return false;
		System.out.println("preparing transaction id  : " + transactionId ); //TODO: remove this when done
		
		//prevent 2 prepare statements from racing against each other
  		synchronized(trxPrepared)
  		{
  			//transaction prepared == -1, it is open to grab
  			if (trxPrepared == -1)
  				trxPrepared = transactionId;
  			//not the right transaction id, we return false
  			else if (transactionId != trxPrepared)
  				return false;
  		}
  		
  		//tell the timeout enforcer that we have received the prepare call, so he will sleep another timeout interval
  		TimeoutEnforcer.receivedPrepareCall = true;
		
		//write to disk the whole hash table
		boolean result = fm.writeMainMemoryToShadow(m_itemHT);
		
		System.out.println("is transaction id " + transactionId + " ready to commit: " + result ); //TODO: remove this when done
		
		//server is ready to commit
		return result;
	}
	
	@Override
	//changes the shadow and master copy in the file manager, this action should be atomic
	public boolean commit(int transactionId) 
	{
	    if(!isServerOnline()) return false;
		//get lock on transaction object
		synchronized(trxPrepared)
		{
			//should not happen, TODO remove this check afterwards
			if (transactionId != trxPrepared)
				System.out.println("CANNOT COMMIT TRANSACTIONID IS WRONG, expecting " + trxPrepared + " , but received " + transactionId);
			
			//reset trxPrepared
			trxPrepared = -1;
		}
		
		//kill the timeout enforcer thread if its alive
		if(timeoutEnforcer.isAlive())
			timeoutEnforcer.interrupt();
		
		boolean result = fm.changeMasterToShadowCopy(transactionId);
		System.out.println("Transaction committed : " + transactionId);
		return result;
	}

	@Override
	//aborts the transaction, either forced (deadlock) or by user
	public boolean abort(int transactionId) 
	{
	    if(!isServerOnline()) return false;
		//check here if this is the transaction that was prepared, if so reset the trxPrepared value
		if ( trxPrepared == transactionId ) 
		{
			//reset trxPrepared
			trxPrepared = -1;
			//this allows another transaction to overwrite the current shadow file so no harm is done
			
			//reset the hashmap to what it was before
			synchronized(LOCK)
			{
				m_itemHT = tempHT;
				prepared_itemHT = null;
			}
		}
		else if (trxPrepared != transactionId) //TODO: not sure, probably not needed
		{
			
		}
		
		//if we abort not because of the enforcer, we kill the latter
		if(!Thread.currentThread().equals(timeoutEnforcer))
		{
			//kill the timeout enforcer thread if its alive
			if(timeoutEnforcer != null && timeoutEnforcer.isAlive())
				timeoutEnforcer.interrupt();
		}
		
		System.out.println("Transaction aborted : " + transactionId);
		return true;
	}

	@Override
	public void crash() 
	{
		//kill server
		shutdown();
	}

	@Override
	public void selfdestruct(String which) 
	{
		//is never called
	}

	
	//forces a specific crash on a given RM, the list being
		//  0 No crashes
		/*At the TM (coordinator):
			1 Crash before sending vote request
			2 Crash after sending vote request and before receiving any replies
			3 Crash after receiving some replies but not all
			4 Crash after receiving all replies but before deciding
			5 Crash after deciding but before sending decision
			6 Crash after sending some but not all decisions
			7 Crash after having sent all decisions
		* At the RMs (participants)
			8 Crash after receive vote request but before sending answer
			9  Which answer to send (commit/abort)
			10 Crash after sending answer
			11 Crash after receiving decision but before committing/aborting
			12 Recovery of RM*/
	@Override
	public boolean commitWithCrash(int transactionId, int crashNumber, int RM) {
	    if(!isServerOnline()) return false;
		//Crash after receiving decision but before committing/aborting
		if (crashNumber == 11)
		{
			System.out.println("Crash after receiving commit decision but before doing so for transaction " + transactionId + ", crash number " + crashNumber + ", RM number " + RM);
			shutdown();
		}
		
		//get lock on transaction object
		synchronized(trxPrepared)
		{
			//should not happen, TODO remove this check afterwards
			if (transactionId != trxPrepared)
				System.out.println("CANNOT COMMIT WITH CRASH TRANSACTIONID IS WRONG, expecting " + trxPrepared + " , but received " + transactionId);
			
			//reset trxPrepared
			trxPrepared = -1;
			
			//kill enforecer thread
			if ( timeoutEnforcer !=null && timeoutEnforcer.isAlive())
				timeoutEnforcer.interrupt();
			
			boolean result = fm.changeMasterToShadowCopy(transactionId);
			System.out.println("TransactionWitCrash committed : " + transactionId + ", crash number " + crashNumber + ", RM number " + RM);
			return result;
		}
	}

	@Override
	public boolean abortWithCrash(int transactionId, int crashNumber, int RM) {
	    if(!isServerOnline()) return false;
		//Crash after receiving decision but before committing/aborting
		if (crashNumber == 11)
		{
			System.out.println("Crash after receiving abort decision but before doing so for transaction " + transactionId + ", crash number " + crashNumber + ", RM number " + RM);
			shutdown();
		}
		
		//check here if this is the transaction that was prepared, if so reset the trxPrepared value
		if ( trxPrepared == transactionId ) 
		{
			//reset trxPrepared
			trxPrepared = -1;
			//this allows another transaction to overwrite the current shadow file so no harm is done
			
			//reset the hashmap to what it was before
			synchronized(LOCK)
			{
				m_itemHT = tempHT;
				prepared_itemHT = null;
			}
		}
		else if (trxPrepared != transactionId) //TODO: not sure, probably not needed
		{
			
		}
		
		//if we abort not because of the enforcer, we kill the latter
		if(!Thread.currentThread().equals(timeoutEnforcer))
		{
			//kill the timeout enforcer thread if its alive
			if(timeoutEnforcer != null && timeoutEnforcer.isAlive())
				timeoutEnforcer.interrupt();
		}
		
		System.out.println("Transaction (with crash) aborted : " + transactionId + ", crash number " + crashNumber + ", RM number " + RM);
		return true;
	}

	@Override
	public boolean prepareWithCrash(int transactionId, int crashNumber, int RM) {
	    if(!isServerOnline()) return false;
	    System.out.println("preparing transaction id with crash  : " + transactionId  + ", crash number " + crashNumber + ", RM number " + RM); //TODO: remove this when done
		
		//Crash after receive vote request but before sending answer
		if ( crashNumber == 8)
		{
			System.out.println("Prepare : Crashing before sending answer for transaction " + transactionId);
			shutdown();
		}
		
		//force an abort return by returning false
		if ( crashNumber == 9)
		{
			System.out.println("Forcing abort for transaction " + transactionId);
			abortWithCrash(transactionId, crashNumber, RM);
			return false;
		}

		//prevent 2 prepare statements from racing against each other
		synchronized(trxPrepared)
		{
			//transaction prepared == -1, it is open to grab
			if (trxPrepared == -1)
				trxPrepared = transactionId;
			//not the right transaction id, we return false
			else if (transactionId != trxPrepared)
				return false;
		}
		
		//write to disk the whole hash table
		boolean result = fm.writeMainMemoryToShadow(m_itemHT);
		
  		//tell the timeout enforcer that we have received the prepare call, so he will sleep another timeout interval
  		TimeoutEnforcer.receivedPrepareCall = true;
		
		System.out.println("is transaction id (with crash) " + transactionId + " ready to commit: " + result + ", crash number " + crashNumber + ", RM number " + RM  ); //TODO: remove this when done
		
		//crash after sending response
		if (crashNumber == 10)
		{
			System.out.println("Crashing after sending answer (" + result + ") for transction " + transactionId);
			
			//dispatch new thread to force this RM to crash after sending its answer back
			new Thread(new Runnable()
			{
				@Override
				public void run() 
				{
					try 
					{
						//sleep 0.5 seconds
						//Thread.sleep(10);
						
						//force shutdown of RM
						shutdown();
					} 
					catch (Exception e) 
					{
					}
				}
			}).start();
		}
		
		//server is ready to commit
		return result;
	}
}
