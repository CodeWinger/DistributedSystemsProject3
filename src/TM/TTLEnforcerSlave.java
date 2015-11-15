package TM;


/*slave class for TTLEnforcer, takes care of aborting a transaction*/
class TTLEnforcerSlave implements Runnable
{
	Transaction txn;
	TransactionManager tm;
	
	//constructor 
	public TTLEnforcerSlave(TransactionManager txm, Transaction t)
	{
		tm = txm;
		txn = t;
	}
	
	//simple method to execute the abort call of a transaction
	@Override
	public void run() 
	{
		try 
		{
			tm.abort(txn.tid);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
