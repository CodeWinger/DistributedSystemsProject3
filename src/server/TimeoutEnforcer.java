package server;

public class TimeoutEnforcer extends Thread
{
	private static final int TIMEOUT = 5000;
	private static int transactionID;
	private static boolean votedYes = false;
	private static ResourceManagerImpl resourceManager;
	
	public TimeoutEnforcer(int tid, ResourceManagerImpl rm)
	{
		transactionID = tid;
		resourceManager = rm;
		votedYes = false;
	}
	
	public void setVotedYes(boolean yes)
	{
		votedYes = yes;
		//System.out.println("voted yes changed in tiemout enforcer");
	}
	
	@Override 
	public void run()
	{
		try
		{
			Thread.sleep(TIMEOUT);
			System.out.println("Thread timeout enforcer, has transaction voted yes : " + votedYes);
			if(votedYes)
				return;
			
			resourceManager.abort(transactionID);
		}
		catch( Exception e)
		{
			System.out.println("Timeout enforcer thread has been prematurely interrupted");
		}
	}
}
