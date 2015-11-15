package client;

import java.util.*;
import java.io.*;


public class Client extends WSClient {

    private static long LOOPDELAY = 1000; //delay before loop to beginning of file
    //performance analysis variables
    private HashMap<Integer, Long> startTimes = new HashMap<Integer, Long>();
    private HashMap<Integer, Long> endTimes = new HashMap<Integer, Long>();
    
	public Client(String serviceName, String serviceHost, int servicePort) 
    throws Exception {
        super(serviceName, serviceHost, servicePort);
    }

    static String file = null;
	private int clientID;
	
    public static void main(String[] args) {
        try {
        	if (args[3] != "")
        		file = args[3];
        	else if (args.length != 4) {
                System.out.println("Usage: MyClient <service-name> " 
                        + "<service-host> <service-port> <input-filename> <client-id>");
                System.exit(-1);
            }
            
            String serviceName = args[0];
            String serviceHost = args[1];
            int servicePort = Integer.parseInt(args[2]);
            
            Client client = new Client(serviceName, serviceHost, servicePort);
            
            client.run();
            
        } catch(Exception e) {
            e.printStackTrace();
        }
    }


    public void run() {
    
        int id;
        int flightNumber;
        int flightPrice;
        int numSeats;
        boolean room;
        boolean car;
        int price;
        int numRooms;
        int numCars;
        String location;

        String command = "";
        Vector arguments = new Vector();

        BufferedReader stdin = null;
        try{
        	if (file != "")
        		stdin = new BufferedReader(new FileReader(new File(file)));
        	else
        		stdin= new BufferedReader(new InputStreamReader(System.in));
        }catch (Exception e)
        {System.out.println("file not found");}
        System.out.println("Client Interface");
        System.out.println("Type \"help\" for list of supported commands");
        
        while (true) {
        
            try {
                //read the next command
                command = stdin.readLine();
                
                //ignore comments
                while(command.charAt(0) == '/')
                	command = stdin.readLine();
                
                if(command.charAt(0) == '%') {
                	LOOPDELAY = Long.parseLong(command.substring(1));
                	command = stdin.readLine();
                }
                if(command.charAt(0) == '@') {
                	clientID = Integer.parseInt(command.substring(1));
                	command = stdin.readLine();
                }
                
                if (command == null || command == "")
                {
                	stdin= new BufferedReader(new InputStreamReader(System.in));
                	command = stdin.readLine();
                }
            }
            catch (Exception io) {
            	
                System.out.println("Unable to read from standard in");
                try{
                	stdin= new BufferedReader(new InputStreamReader(System.in));
                	command = stdin.readLine();
                } catch (Exception e) {}
                //System.exit(1);
            }
            //remove heading and trailing white space
            command = command.trim();
            arguments = parse(command);
            
            //decide which of the commands this was
            switch(findChoice((String) arguments.elementAt(0))) {

            case 1: //help section
                if (arguments.size() == 1)   //command was "help"
                    listCommands();
                else if (arguments.size() == 2)  //command was "help <commandname>"
                    listSpecific((String) arguments.elementAt(1));
                else  //wrong use of help command
                    System.out.println("Improper use of help command. Type help or help, <commandname>");
                break;
                
            case 2:  //new flight
                if (arguments.size() != 5) {
                    wrongNumber();
                    break;
                }
                System.out.println("Adding a new Flight using id: " + arguments.elementAt(1));
                System.out.println("Flight number: " + arguments.elementAt(2));
                System.out.println("Add Flight Seats: " + arguments.elementAt(3));
                System.out.println("Set Flight Price: " + arguments.elementAt(4));
                
                try {
                    id = getInt(arguments.elementAt(1));
                    flightNumber = getInt(arguments.elementAt(2));
                    numSeats = getInt(arguments.elementAt(3));
                    flightPrice = getInt(arguments.elementAt(4));
                    
                    if (proxy.addFlight(id, flightNumber, numSeats, flightPrice))
                        System.out.println("Flight added");
                    else
                        System.out.println("Flight could not be added");
                }
                catch(Exception e) {
                    System.out.println("EXCEPTION: ");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                break;
                
            case 3:  //new car
                if (arguments.size() != 5) {
                    wrongNumber();
                    break;
                }
                System.out.println("Adding a new car using id: " + arguments.elementAt(1));
                System.out.println("car Location: " + arguments.elementAt(2));
                System.out.println("Add Number of cars: " + arguments.elementAt(3));
                System.out.println("Set Price: " + arguments.elementAt(4));
                try {
                    id = getInt(arguments.elementAt(1));
                    location = getString(arguments.elementAt(2));
                    numCars = getInt(arguments.elementAt(3));
                    price = getInt(arguments.elementAt(4));

                    if (proxy.addCars(id, location, numCars, price))
                        System.out.println("cars added");
                    else
                        System.out.println("cars could not be added");
                }
                catch(Exception e) {
                    System.out.println("EXCEPTION: ");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                break;
                
            case 4:  //new room
                if (arguments.size() != 5) {
                    wrongNumber();
                    break;
                }
                System.out.println("Adding a new room using id: " + arguments.elementAt(1));
                System.out.println("room Location: " + arguments.elementAt(2));
                System.out.println("Add Number of rooms: " + arguments.elementAt(3));
                System.out.println("Set Price: " + arguments.elementAt(4));
                try {
                    id = getInt(arguments.elementAt(1));
                    location = getString(arguments.elementAt(2));
                    numRooms = getInt(arguments.elementAt(3));
                    price = getInt(arguments.elementAt(4));

                    if (proxy.addRooms(id, location, numRooms, price))
                        System.out.println("rooms added");
                    else
                        System.out.println("rooms could not be added");
                }
                catch(Exception e) {
                    System.out.println("EXCEPTION: ");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                break;
                
            case 5:  //new Customer
                if (arguments.size() != 2) {
                    wrongNumber();
                    break;
                }
                System.out.println("Adding a new Customer using id: " + arguments.elementAt(1));
                try {
                    id = getInt(arguments.elementAt(1));
                    int customer = proxy.newCustomer(id);
                    System.out.println("new customer id: " + customer);
                }
                catch(Exception e) {
                    System.out.println("EXCEPTION: ");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                break;
                
            case 6: //delete Flight
                if (arguments.size() != 3) {
                    wrongNumber();
                    break;
                }
                System.out.println("Deleting a flight using id: " + arguments.elementAt(1));
                System.out.println("Flight Number: " + arguments.elementAt(2));
                try {
                    id = getInt(arguments.elementAt(1));
                    flightNumber = getInt(arguments.elementAt(2));

                    if (proxy.deleteFlight(id, flightNumber))
                        System.out.println("Flight Deleted");
                    else
                        System.out.println("Flight could not be deleted");
                }
                catch(Exception e) {
                    System.out.println("EXCEPTION: ");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                break;
                
            case 7: //delete car
                if (arguments.size() != 3) {
                    wrongNumber();
                    break;
                }
                System.out.println("Deleting the cars from a particular location  using id: " + arguments.elementAt(1));
                System.out.println("car Location: " + arguments.elementAt(2));
                try {
                    id = getInt(arguments.elementAt(1));
                    location = getString(arguments.elementAt(2));

                    if (proxy.deleteCars(id, location))
                        System.out.println("cars Deleted");
                    else
                        System.out.println("cars could not be deleted");
                }
                catch(Exception e) {
                    System.out.println("EXCEPTION: ");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                break;
                
            case 8: //delete room
                if (arguments.size() != 3) {
                    wrongNumber();
                    break;
                }
                System.out.println("Deleting all rooms from a particular location  using id: " + arguments.elementAt(1));
                System.out.println("room Location: " + arguments.elementAt(2));
                try {
                    id = getInt(arguments.elementAt(1));
                    location = getString(arguments.elementAt(2));

                    if (proxy.deleteRooms(id, location))
                        System.out.println("rooms Deleted");
                    else
                        System.out.println("rooms could not be deleted");
                }
                catch(Exception e) {
                    System.out.println("EXCEPTION: ");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                break;
                
            case 9: //delete Customer
                if (arguments.size() != 3) {
                    wrongNumber();
                    break;
                }
                System.out.println("Deleting a customer from the database using id: " + arguments.elementAt(1));
                System.out.println("Customer id: " + arguments.elementAt(2));
                try {
                    id = getInt(arguments.elementAt(1));
                    int customer = getInt(arguments.elementAt(2));

                    if (proxy.deleteCustomer(id, customer))
                        System.out.println("Customer Deleted");
                    else
                        System.out.println("Customer could not be deleted");
                }
                catch(Exception e) {
                    System.out.println("EXCEPTION: ");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                break;
                
            case 10: //querying a flight
                if (arguments.size() != 3) {
                    wrongNumber();
                    break;
                }
                System.out.println("Querying a flight using id: " + arguments.elementAt(1));
                System.out.println("Flight number: " + arguments.elementAt(2));
                try {
                    id = getInt(arguments.elementAt(1));
                    flightNumber = getInt(arguments.elementAt(2));
                    int seats = proxy.queryFlight(id, flightNumber);
                    System.out.println("Number of seats available: " + seats);
                }
                catch(Exception e) {
                    System.out.println("EXCEPTION: ");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                break;
                
            case 11: //querying a car Location
                if (arguments.size() != 3) {
                    wrongNumber();
                    break;
                }
                System.out.println("Querying a car location using id: " + arguments.elementAt(1));
                System.out.println("car location: " + arguments.elementAt(2));
                try {
                    id = getInt(arguments.elementAt(1));
                    location = getString(arguments.elementAt(2));

                    numCars = proxy.queryCars(id, location);
                    System.out.println("number of cars at this location: " + numCars);
                }
                catch(Exception e) {
                    System.out.println("EXCEPTION: ");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                break;
                
            case 12: //querying a room location
                if (arguments.size() != 3) {
                    wrongNumber();
                    break;
                }
                System.out.println("Querying a room location using id: " + arguments.elementAt(1));
                System.out.println("room location: " + arguments.elementAt(2));
                try {
                    id = getInt(arguments.elementAt(1));
                    location = getString(arguments.elementAt(2));

                    numRooms = proxy.queryRooms(id, location);
                    System.out.println("number of rooms at this location: " + numRooms);
                }
                catch(Exception e) {
                    System.out.println("EXCEPTION: ");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                break;
                
            case 13: //querying Customer Information
                if (arguments.size() != 3) {
                    wrongNumber();
                    break;
                }
                System.out.println("Querying Customer information using id: " + arguments.elementAt(1));
                System.out.println("Customer id: " + arguments.elementAt(2));
                try {
                    id = getInt(arguments.elementAt(1));
                    int customer = getInt(arguments.elementAt(2));

                    String bill = proxy.queryCustomerInfo(id, customer);
                    System.out.println("Customer info: " + bill);
                }
                catch(Exception e) {
                    System.out.println("EXCEPTION: ");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                break;               
                
            case 14: //querying a flight Price
                if (arguments.size() != 3) {
                    wrongNumber();
                    break;
                }
                System.out.println("Querying a flight Price using id: " + arguments.elementAt(1));
                System.out.println("Flight number: " + arguments.elementAt(2));
                try {
                    id = getInt(arguments.elementAt(1));
                    flightNumber = getInt(arguments.elementAt(2));

                    price = proxy.queryFlightPrice(id, flightNumber);
                    System.out.println("Price of a seat: " + price);
                }
                catch(Exception e) {
                    System.out.println("EXCEPTION: ");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                break;
                
            case 15: //querying a car Price
                if (arguments.size() != 3) {
                    wrongNumber();
                    break;
                }
                System.out.println("Querying a car price using id: " + arguments.elementAt(1));
                System.out.println("car location: " + arguments.elementAt(2));
                try {
                    id = getInt(arguments.elementAt(1));
                    location = getString(arguments.elementAt(2));

                    price = proxy.queryCarsPrice(id, location);
                    System.out.println("Price of a car at this location: " + price);
                }
                catch(Exception e) {
                    System.out.println("EXCEPTION: ");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }                
                break;

            case 16: //querying a room price
                if (arguments.size() != 3) {
                    wrongNumber();
                    break;
                }
                System.out.println("Querying a room price using id: " + arguments.elementAt(1));
                System.out.println("room Location: " + arguments.elementAt(2));
                try {
                    id = getInt(arguments.elementAt(1));
                    location = getString(arguments.elementAt(2));

                    price = proxy.queryRoomsPrice(id, location);
                    System.out.println("Price of rooms at this location: " + price);
                }
                catch(Exception e) {
                    System.out.println("EXCEPTION: ");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                break;
                
            case 17:  //reserve a flight
                if (arguments.size() != 4) {
                    wrongNumber();
                    break;
                }
                System.out.println("Reserving a seat on a flight using id: " + arguments.elementAt(1));
                System.out.println("Customer id: " + arguments.elementAt(2));
                System.out.println("Flight number: " + arguments.elementAt(3));
                try {
                    id = getInt(arguments.elementAt(1));
                    int customer = getInt(arguments.elementAt(2));
                    flightNumber = getInt(arguments.elementAt(3));

                    if (proxy.reserveFlight(id, customer, flightNumber))
                        System.out.println("Flight Reserved");
                    else
                        System.out.println("Flight could not be reserved.");
                }
                catch(Exception e) {
                    System.out.println("EXCEPTION: ");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                break;
                
            case 18:  //reserve a car
                if (arguments.size() != 4) {
                    wrongNumber();
                    break;
                }
                System.out.println("Reserving a car at a location using id: " + arguments.elementAt(1));
                System.out.println("Customer id: " + arguments.elementAt(2));
                System.out.println("Location: " + arguments.elementAt(3));
                try {
                    id = getInt(arguments.elementAt(1));
                    int customer = getInt(arguments.elementAt(2));
                    location = getString(arguments.elementAt(3));
                    
                    if (proxy.reserveCar(id, customer, location))
                        System.out.println("car Reserved");
                    else
                        System.out.println("car could not be reserved.");
                }
                catch(Exception e) {
                    System.out.println("EXCEPTION: ");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                break;
                
            case 19:  //reserve a room
                if (arguments.size() != 4) {
                    wrongNumber();
                    break;
                }
                System.out.println("Reserving a room at a location using id: " + arguments.elementAt(1));
                System.out.println("Customer id: " + arguments.elementAt(2));
                System.out.println("Location: " + arguments.elementAt(3));
                try {
                    id = getInt(arguments.elementAt(1));
                    int customer = getInt(arguments.elementAt(2));
                    location = getString(arguments.elementAt(3));
                    
                    if (proxy.reserveRoom(id, customer, location))
                        System.out.println("room Reserved");
                    else
                        System.out.println("room could not be reserved.");
                }
                catch(Exception e) {
                    System.out.println("EXCEPTION: ");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                break;
                
            case 20:  //reserve an Itinerary
                if (arguments.size()<7) {
                    wrongNumber();
                    break;
                }
                System.out.println("Reserving an Itinerary using id: " + arguments.elementAt(1));
                System.out.println("Customer id: " + arguments.elementAt(2));
                for (int i = 0; i<arguments.size()-6; i++)
                    System.out.println("Flight number" + arguments.elementAt(3 + i));
                System.out.println("Location for car/room booking: " + arguments.elementAt(arguments.size()-3));
                System.out.println("car to book?: " + arguments.elementAt(arguments.size()-2));
                System.out.println("room to book?: " + arguments.elementAt(arguments.size()-1));
                try {
                    id = getInt(arguments.elementAt(1));
                    int customer = getInt(arguments.elementAt(2));
                    Vector flightNumbers = new Vector();
                    for (int i = 0; i < arguments.size()-6; i++)
                        flightNumbers.addElement(arguments.elementAt(3 + i));
                    location = getString(arguments.elementAt(arguments.size()-3));
                    car = getBoolean(arguments.elementAt(arguments.size()-2));
                    room = getBoolean(arguments.elementAt(arguments.size()-1));
                    
                    if (proxy.reserveItinerary(id, customer, flightNumbers, 
                            location, car, room))
                        System.out.println("Itinerary Reserved");
                    else
                        System.out.println("Itinerary could not be reserved.");
                }
                catch(Exception e) {
                    System.out.println("EXCEPTION: ");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                break;
                            
            case 21:  //quit the client
                if (arguments.size() != 1) {
                    wrongNumber();
                    break;
                }
                System.out.println("Quitting client.");
                return;
                
            case 22:  //new Customer given id
                if (arguments.size() != 3) {
                    wrongNumber();
                    break;
                }
                System.out.println("Adding a new Customer using id: "
                        + arguments.elementAt(1)  +  " and cid "  + arguments.elementAt(2));
                try {
                    id = getInt(arguments.elementAt(1));
                    int customer = getInt(arguments.elementAt(2));

                    boolean c = proxy.newCustomerId(id, customer);
                    System.out.println("new customer id: " + customer);
                }
                catch(Exception e) {
                    System.out.println("EXCEPTION: ");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                break;
            case 23: //start a new transaction
            	if (arguments.size() != 1 && arguments.size() != 2) {
                    wrongNumber();
                    break;
                }
            	if (arguments.size() == 1 )
            	{
            		 System.out.println("Creating a new transaction");
                     try {
                         int trxId = proxy.start();
                         System.out.println("new transaction id: " + trxId);
                         //performance analysis
                 		 startTimes.put(trxId, System.currentTimeMillis());
                     }
                     catch(Exception e) {
                         System.out.println("EXCEPTION: ");
                         System.out.println(e.getMessage());
                         e.printStackTrace();
                     }
                     
                     
                     break;
            	}
            	else
            	{
            		System.out.println("Creating a new transaction");
                    try {
                        boolean c = proxy.startid(getInt(arguments.elementAt(1)));
                        System.out.println("new transaction " + arguments.elementAt(1) + " : " + c);
                        //performance analysis
                		startTimes.put(getInt(arguments.elementAt(1)), System.currentTimeMillis());
                    }
                    catch(Exception e) {
                        System.out.println("EXCEPTION: ");
                        System.out.println(e.getMessage());
                        e.printStackTrace();
                    }
                    break;
            	}
               
            case 24:  //commit an existing transaction
                if (arguments.size() != 2) {
                    wrongNumber();
                    break;
                }
                System.out.println("Committing transaction using id: "
                        + arguments.elementAt(1));
                try {
                    id = getInt(arguments.elementAt(1));

                    boolean c = proxy.commit(id);
                    System.out.println("Transaction " + id + " committed : " + c);
                    if (startTimes.containsKey(id)) {
						//performance analysis
						endTimes.put(id, System.currentTimeMillis());
						long duration = endTimes.get(id) - startTimes.get(id);
						try (PrintWriter out = new PrintWriter(new FileWriter(("stats"+clientID+".txt"), true))) {
							//append transaction duration to stats file: txid,duration,tps
							double tps =  1000.0 / (double)LOOPDELAY;
							out.println( id + "," + duration + "," + tps);
						} catch (IOException e) {
							System.out.println("Stats file couldn't be written");
						} 
					}
                }
                catch(Exception e) {
                    System.out.println("EXCEPTION: ");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                break;	
            case 25:  //abort an existing transaction
                if (arguments.size() != 2) {
                    wrongNumber();
                    break;
                }
                System.out.println("Aborting transaction using id: "
                        + arguments.elementAt(1));
                try {
                    id = getInt(arguments.elementAt(1));

                    boolean c = proxy.abort(id);
                    System.out.println("Transaction " + id + " aborted : " + c);
                }
                catch(Exception e) {
                    System.out.println("EXCEPTION: ");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                break;	
            case 26:  //shut down computers
                if (arguments.size() != 1) {
                    wrongNumber();
                    break;
                }
                System.out.println("shutting down servers... ");
                       
                try {
                	boolean c = proxy.shutdown();
                }
                catch(Exception e) {
                   //an exception thrown means midleware shut down
                   System.out.println("Servers successfully shut down");
                   System.exit(0);
                }
                break;
            case 27:  //loops to read file
                if (arguments.size() != 1) {
                    wrongNumber();
                    break;
                }
                       
                if (file != "") {
            		try {
            			//variation in loop delay
            			Random r = new Random();
            			long delay = r.nextInt((int) (LOOPDELAY/10.0));
            			if(r.nextBoolean()) {
            				delay = LOOPDELAY + delay;
            			}
            			else  {
            				delay = LOOPDELAY - delay;
            			}
            			//wait and restart reading file
            			Thread.sleep(delay);
            			stdin.close();
						stdin = new BufferedReader(new FileReader(new File(file)));
						System.out.println("looping to start of file after "+delay+"ms delay ... ");
					} catch (FileNotFoundException e) {
						System.out.println("File not found!");
					} catch (Exception e) {}
                }
            	else {
                	System.out.println("Not in file reading mode!");
            	}
                break;
                
            case 28:  //sleeps loopdelay
                if (arguments.size() != 1) {
                    wrongNumber();
                    break;
                }
                      
                try {
                	System.out.println("Sleeping for "+LOOPDELAY+"ms delay ... ");
                	Thread.sleep(LOOPDELAY);
                }catch(Exception e){}
                break;
                
            	
            default:
                System.out.println("The interface does not support this command.");
                break;
            }
        }
    }
        
    public Vector parse(String command) {
        Vector arguments = new Vector();
        StringTokenizer tokenizer = new StringTokenizer(command, ",");
        String argument = "";
        while (tokenizer.hasMoreTokens()) {
            argument = tokenizer.nextToken();
            argument = argument.trim();
            arguments.add(argument);
        }
        return arguments;
    }
    
    public int findChoice(String argument) {
        if (argument.compareToIgnoreCase("help") == 0)
            return 1;
        else if (argument.compareToIgnoreCase("newflight") == 0)
            return 2;
        else if (argument.compareToIgnoreCase("newcar") == 0)
            return 3;
        else if (argument.compareToIgnoreCase("newroom") == 0)
            return 4;
        else if (argument.compareToIgnoreCase("newcustomer") == 0)
            return 5;
        else if (argument.compareToIgnoreCase("deleteflight") == 0)
            return 6;
        else if (argument.compareToIgnoreCase("deletecar") == 0)
            return 7;
        else if (argument.compareToIgnoreCase("deleteroom") == 0)
            return 8;
        else if (argument.compareToIgnoreCase("deletecustomer") == 0)
            return 9;
        else if (argument.compareToIgnoreCase("queryflight") == 0)
            return 10;
        else if (argument.compareToIgnoreCase("querycar") == 0)
            return 11;
        else if (argument.compareToIgnoreCase("queryroom") == 0)
            return 12;
        else if (argument.compareToIgnoreCase("querycustomer") == 0)
            return 13;
        else if (argument.compareToIgnoreCase("queryflightprice") == 0)
            return 14;
        else if (argument.compareToIgnoreCase("querycarprice") == 0)
            return 15;
        else if (argument.compareToIgnoreCase("queryroomprice") == 0)
            return 16;
        else if (argument.compareToIgnoreCase("reserveflight") == 0)
            return 17;
        else if (argument.compareToIgnoreCase("reservecar") == 0)
            return 18;
        else if (argument.compareToIgnoreCase("reserveroom") == 0)
            return 19;
        else if (argument.compareToIgnoreCase("itinerary") == 0)
            return 20;
        else if (argument.compareToIgnoreCase("quit") == 0)
            return 21;
        else if (argument.compareToIgnoreCase("newcustomerid") == 0)
            return 22;
        else if (argument.compareToIgnoreCase("start") == 0)
        	return 23;
        else if (argument.compareToIgnoreCase("commit") == 0)
        	return 24;
        else if (argument.compareToIgnoreCase("abort") == 0)
        	return 25;
        else if (argument.compareToIgnoreCase("shutdown") == 0)
        	return 26;
        else if (argument.compareToIgnoreCase("loop") == 0)
        	return 27;
        else if (argument.compareToIgnoreCase("sleep") == 0)
        	return 28;
        else
            return 666;
    }

    public void listCommands() {
        System.out.println("\nWelcome to the client interface provided to test your project.");
        System.out.println("Commands accepted by the interface are: ");
        System.out.println("help");
        System.out.println("newflight\nnewcar\nnewroom\nnewcustomer\nnewcustomerid\ndeleteflight\ndeletecar\ndeleteroom");
        System.out.println("deletecustomer\nqueryflight\nquerycar\nqueryroom\nquerycustomer");
        System.out.println("queryflightprice\nquerycarprice\nqueryroomprice");
        System.out.println("reserveflight\nreservecar\nreserveroom\nitinerary");
        System.out.println("start\ncommit\nabort\nloop");
        System.out.println("quit");
        System.out.println("\ntype help, <commandname> for detailed info (note the use of comma).");
    }


    public void listSpecific(String command) {
        System.out.print("Help on: ");
        switch(findChoice(command)) {
            case 1:
            System.out.println("Help");
            System.out.println("\nTyping help on the prompt gives a list of all the commands available.");
            System.out.println("Typing help, <commandname> gives details on how to use the particular command.");
            break;

            case 2:  //new flight
            System.out.println("Adding a new Flight.");
            System.out.println("Purpose: ");
            System.out.println("\tAdd information about a new flight.");
            System.out.println("\nUsage: ");
            System.out.println("\tnewflight, <id>, <flightnumber>, <numSeats>, <flightprice>");
            break;
            
            case 3:  //new car
            System.out.println("Adding a new car.");
            System.out.println("Purpose: ");
            System.out.println("\tAdd information about a new car location.");
            System.out.println("\nUsage: ");
            System.out.println("\tnewcar, <id>, <location>, <numberofcars>, <pricepercar>");
            break;
            
            case 4:  //new room
            System.out.println("Adding a new room.");
            System.out.println("Purpose: ");
            System.out.println("\tAdd information about a new room location.");
            System.out.println("\nUsage: ");
            System.out.println("\tnewroom, <id>, <location>, <numberofrooms>, <priceperroom>");
            break;
            
            case 5:  //new Customer
            System.out.println("Adding a new Customer.");
            System.out.println("Purpose: ");
            System.out.println("\tGet the system to provide a new customer id. (same as adding a new customer)");
            System.out.println("\nUsage: ");
            System.out.println("\tnewcustomer, <id>");
            break;
            
            
            case 6: //delete Flight
            System.out.println("Deleting a flight");
            System.out.println("Purpose: ");
            System.out.println("\tDelete a flight's information.");
            System.out.println("\nUsage: ");
            System.out.println("\tdeleteflight, <id>, <flightnumber>");
            break;
            
            case 7: //delete car
            System.out.println("Deleting a car");
            System.out.println("Purpose: ");
            System.out.println("\tDelete all cars from a location.");
            System.out.println("\nUsage: ");
            System.out.println("\tdeletecar, <id>, <location>, <numCars>");
            break;
            
            case 8: //delete room
            System.out.println("Deleting a room");
            System.out.println("\nPurpose: ");
            System.out.println("\tDelete all rooms from a location.");
            System.out.println("Usage: ");
            System.out.println("\tdeleteroom, <id>, <location>, <numRooms>");
            break;
            
            case 9: //delete Customer
            System.out.println("Deleting a Customer");
            System.out.println("Purpose: ");
            System.out.println("\tRemove a customer from the database.");
            System.out.println("\nUsage: ");
            System.out.println("\tdeletecustomer, <id>, <customerid>");
            break;
            
            case 10: //querying a flight
            System.out.println("Querying flight.");
            System.out.println("Purpose: ");
            System.out.println("\tObtain Seat information about a certain flight.");
            System.out.println("\nUsage: ");
            System.out.println("\tqueryflight, <id>, <flightnumber>");
            break;
            
            case 11: //querying a car Location
            System.out.println("Querying a car location.");
            System.out.println("Purpose: ");
            System.out.println("\tObtain number of cars at a certain car location.");
            System.out.println("\nUsage: ");
            System.out.println("\tquerycar, <id>, <location>");        
            break;
            
            case 12: //querying a room location
            System.out.println("Querying a room Location.");
            System.out.println("Purpose: ");
            System.out.println("\tObtain number of rooms at a certain room location.");
            System.out.println("\nUsage: ");
            System.out.println("\tqueryroom, <id>, <location>");        
            break;
            
            case 13: //querying Customer Information
            System.out.println("Querying Customer Information.");
            System.out.println("Purpose: ");
            System.out.println("\tObtain information about a customer.");
            System.out.println("\nUsage: ");
            System.out.println("\tquerycustomer, <id>, <customerid>");
            break;               
            
            case 14: //querying a flight for price 
            System.out.println("Querying flight.");
            System.out.println("Purpose: ");
            System.out.println("\tObtain price information about a certain flight.");
            System.out.println("\nUsage: ");
            System.out.println("\tqueryflightprice, <id>, <flightnumber>");
            break;
            
            case 15: //querying a car Location for price
            System.out.println("Querying a car location.");
            System.out.println("Purpose: ");
            System.out.println("\tObtain price information about a certain car location.");
            System.out.println("\nUsage: ");
            System.out.println("\tquerycarprice, <id>, <location>");        
            break;
            
            case 16: //querying a room location for price
            System.out.println("Querying a room Location.");
            System.out.println("Purpose: ");
            System.out.println("\tObtain price information about a certain room location.");
            System.out.println("\nUsage: ");
            System.out.println("\tqueryroomprice, <id>, <location>");        
            break;

            case 17:  //reserve a flight
            System.out.println("Reserving a flight.");
            System.out.println("Purpose: ");
            System.out.println("\tReserve a flight for a customer.");
            System.out.println("\nUsage: ");
            System.out.println("\treserveflight, <id>, <customerid>, <flightnumber>");
            break;
            
            case 18:  //reserve a car
            System.out.println("Reserving a car.");
            System.out.println("Purpose: ");
            System.out.println("\tReserve a given number of cars for a customer at a particular location.");
            System.out.println("\nUsage: ");
            System.out.println("\treservecar, <id>, <customerid>, <location>, <nummberofcars>");
            break;
            
            case 19:  //reserve a room
            System.out.println("Reserving a room.");
            System.out.println("Purpose: ");
            System.out.println("\tReserve a given number of rooms for a customer at a particular location.");
            System.out.println("\nUsage: ");
            System.out.println("\treserveroom, <id>, <customerid>, <location>, <nummberofrooms>");
            break;
            
            case 20:  //reserve an Itinerary
            System.out.println("Reserving an Itinerary.");
            System.out.println("Purpose: ");
            System.out.println("\tBook one or more flights.Also book zero or more cars/rooms at a location.");
            System.out.println("\nUsage: ");
            System.out.println("\titinerary, <id>, <customerid>, "
                    + "<flightnumber1>....<flightnumberN>, "
                    + "<LocationToBookcarsOrrooms>, <NumberOfcars>, <NumberOfroom>");
            break;
            

            case 21:  //quit the client
            System.out.println("Quitting client.");
            System.out.println("Purpose: ");
            System.out.println("\tExit the client application.");
            System.out.println("\nUsage: ");
            System.out.println("\tquit");
            break;
            
            case 22:  //new customer with id
            System.out.println("Create new customer providing an id");
            System.out.println("Purpose: ");
            System.out.println("\tCreates a new customer with the id provided");
            System.out.println("\nUsage: ");
            System.out.println("\tnewcustomerid, <id>, <customerid>");
            break;
            
            case 23:
            	  System.out.println("Create new transaction yielding back an id");
                  System.out.println("Purpose: ");
                  System.out.println("\tCreates a new transaction");
                  System.out.println("\nUsage: ");
                  System.out.println("\tstart");
                  break;
            case 24:
            	System.out.println("Commits a transaction providing an id");
                System.out.println("Purpose: ");
                System.out.println("\tCommit a  transaction");
                System.out.println("\nUsage: ");
                System.out.println("\tcommit, <id>");
                break;
            	
            case 25:
            	System.out.println("Aborts a transaction providing an id");
                System.out.println("Purpose: ");
                System.out.println("\tAborts a  transaction");
                System.out.println("\nUsage: ");
                System.out.println("\tabort, <id>");
                break;
               
            case 26:
            	System.out.println("Shuts down all servers");
                System.out.println("Purpose: ");
                System.out.println("\tShuts down all servers. \n\t\tAll transactions must have committed for the call to succeed");
                System.out.println("\nUsage: ");
                System.out.println("\tshutdown, <id>");
                break;
                
            case 27:
            	System.out.println("Loops to start of file");
                System.out.println("Purpose: ");
                System.out.println("\tRead the commands in the file again. \n\t\tThe client must be in file reading mode");
                System.out.println("\nUsage: ");
                System.out.println("\tloop, <id>");
                break;

            default:
            System.out.println(command);
            System.out.println("The interface does not support this command.");
            break;
        }
    }
    
    public void wrongNumber() {
        System.out.println("The number of arguments provided in this command are wrong.");
        System.out.println("Type help, <commandname> to check usage of this command.");
    }

    public int getInt(Object temp) throws Exception {
        try {
            return (new Integer((String)temp)).intValue();
        }
        catch(Exception e) {
            throw e;
        }
    }
    
    public boolean getBoolean(Object temp) throws Exception {
        try {
            return (new Boolean((String)temp)).booleanValue();
        }
        catch(Exception e) {
            throw e;
        }
    }

    public String getString(Object temp) throws Exception {
        try {    
            return (String)temp;
        }
        catch (Exception e) {
            throw e;
        }
    }
    
}
