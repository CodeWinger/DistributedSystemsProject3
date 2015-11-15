package TM;

import java.util.HashMap;
import java.util.LinkedList;

class Customer
{
	//customer id
	public final int id;
	
	//field to check if customer is new or existing in database
	boolean isNew = true;
	boolean isDeleted = false;
	
	//list of reservations made by customer
	 final HashMap<String, Item> reservations = new HashMap<String, Item>();
	
	//public final LinkedList<Transaction> exclusiveAccess = new LinkedList<Transaction>();
	
	public Customer(int pid /*, Transaction t*/)
	{
		id = pid;
		//exclusiveAccess.add(t);
	}
	
	/*public Item[] getReservations()
	{
		Item[] items = new Item[reservations.size()];
		reservations.
		return items;
	}*/
	
	public void addReservation(String key, Item i)
	{
		if (!reservations.containsKey(key))
			reservations.put(key, i);
	}
	
	
}