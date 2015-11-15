package TM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;

import middleRM.ws.Main;
import middleRM.ws.Main.Server;

/*class representing a transaction*/
class Transaction
{	
	//keeps the servers being used by the transaction
	private ArrayList<Main.Server> servers = new ArrayList<Main.Server>(3);
	
	//list of operations executed so far by transaction. needed in case we rollback
	private LinkedList<String> operationsToExecute = new LinkedList<String>();
	
	//items queried and operated on by transaction
	public final HashMap<String, Item> writeSet = new HashMap<String, Item>(5);
	
	//transaction id
	public final int tid;
	
	//checks to see if transaction is already aborting, to prevent a given transaction from aborting twice
	public boolean isTerminating = false; //either abort or commit
	
	//timestamp for TTL ( time to live) of transaction
	private long timestamp;
	
	//returns the servers
	public Server[] getServers()
	{
		Server[] s = new Server[servers.size()];
		servers.toArray(s);
		return s;
	}
	
	//returns the time-stamp of this transaction
	public long getTimestamp()
	{
		return timestamp;
	}
	
	//refreshes the time stamp to prevent an abort
	public void refreshTimeStamp()
	{
		timestamp = System.currentTimeMillis() + TransactionManager.TTL;
	}
	
	//constructor
	public Transaction(int txid)
	{
		tid = txid;
		refreshTimeStamp();
	}
	
	//returns the servers used by transaction 
	public ArrayList<Main.Server> servers() 
	{ 
		return servers;
	}
	
	//adds the server to the list of servers currently being used, if its not already there
	public void addServer(Main.Server s)
	{
		if (!servers.contains(s))
			servers.add(s);
	}
	
	//removes the servers from the list
	public void removeServer(Main.Server s)
	{
		if (servers.contains(s))
			servers.remove(s);
	}
	
	//adds a command to the list of commands to be executed by the transaction. Also, refresh the TTL
	public void addOperationToExecute(String cmd)
	{
		operationsToExecute.add(cmd);
		refreshTimeStamp();
	}
	
	//returns the list of cmds to be executed by the transaction
	public LinkedList<String> cmds()
	{
		return  operationsToExecute;
	}
	
	//to string method for printing a transaction
	@Override
	public String toString()
	{
		String result = String.format("Transaction id : %d\n'\t", tid);
		result += "is terminating : " + isTerminating + "\n\toperations\n\t\t[\n\t\t\t";
		
		for(String s : operationsToExecute)
			result += s + "\n\t\t\t";
		result += "]\n\t\tusing the server(s) : ";
		
		for(Server s : servers )
			result += s + " ";
		result += "\n\n";
		return result;
	}
}