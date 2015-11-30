package TM;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;

class Customer implements Serializable
{
	private static final long serialVersionUID = -2562289447576073230L;

	//customer id
	public final int id;
	
	//field to check if customer is new or existing in database
	boolean isNew = true;
	boolean isDeleted = false;
	
	//list of reservations made by customer
	 final HashMap<String, Item> reservations = new HashMap<String, Item>();
	
	public Customer(int pid)
	{
		id = pid;
	}
	
	public void addReservation(String key, Item i)
	{
		if (!reservations.containsKey(key))
			reservations.put(key, i);
	}
}