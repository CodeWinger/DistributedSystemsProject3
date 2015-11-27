package server;

public class TimeoutEnforcer extends Thread
{
	private static final int TIMEOUT = 5000;
	private static int transactionID;
	public static boolean receivedPrepareCall = false;
	private static ResourceManagerImpl resourceManager;
	
	public TimeoutEnforcer(int tid, ResourceManagerImpl rm)
	{
		transactionID = tid;
		resourceManager = rm;
	}
	
	
	@Override 
	public void run()
	{
		try
		{
			Thread.sleep(TIMEOUT);
			
			if(receivedPrepareCall)
				Thread.sleep(TIMEOUT);
			
			resourceManager.abort(transactionID);
		}
		catch( Exception e)
		{
			System.out.println("Timeout enforcer thread has been prematurely interrupted");
		}
	}
}
