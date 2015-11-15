package LockManager;

import java.util.BitSet;
import java.util.Vector;

public class LockManager
{
    public static final int READ = 0;
    public static final int WRITE = 1;
    
    private static int TABLE_SIZE = 2039;
    private static int DEADLOCK_TIMEOUT = 10000;
    
    private static TPHashTable lockTable = new TPHashTable(LockManager.TABLE_SIZE);
    private static TPHashTable stampTable = new TPHashTable(LockManager.TABLE_SIZE);
    private static TPHashTable waitTable = new TPHashTable(LockManager.TABLE_SIZE);
    
    public LockManager() {
        super();
    }
    
    public boolean Lock(int xid, String strData, int lockType) throws DeadlockException {
    
        // if any parameter is invalid, then return false
        if (xid < 0) { 
        	System.out.println("lm : xid < 0");
            return false;
        }
        
        if (strData == null) {
        	System.out.println("lm : strdata null");
            return false;
        }
        
        if ((lockType != TrxnObj.READ) && (lockType != TrxnObj.WRITE)) {
        	System.out.println("lm : not read or write operation");
            return false;
        }
        
        //System.out.println("Lock Manager: in lock()");
        /*System.out.print("size of lockTable : " + lockTable.getSize() + "\n\t");
        Vector v1 = lockTable.allElements();
        for ( Object o : v1)
        	System.out.print(o + ",");
        System.out.print("\nsize of stampTable : " + stampTable.getSize() + "\n\t");
        Vector v2 = stampTable.allElements();
        for ( Object o : v2)
        	System.out.print(o + ",");
        System.out.print("\nsize of waitTable : " + waitTable.getSize() + "\n\t");
        Vector v3 = waitTable.allElements();
        for ( Object o : v3)
        	System.out.print(o + ",");
       System.out.println("\n");*/
        
        // two objects in lock table for easy lookup.
        TrxnObj trxnObj = new TrxnObj(xid, strData, lockType);
        DataObj dataObj = new DataObj(xid, strData, lockType);
        
        // return true when there is no lock conflict or throw a deadlock exception.
        try {
            boolean bConflict = true;
            BitSet bConvert = new BitSet(1);
            while (bConflict) {
                synchronized (this.lockTable) {
                    // check if this lock request conflicts with existing locks
                    bConflict = LockConflict(dataObj, bConvert);
                    if (!bConflict) {
                        // no lock conflict
                        synchronized (this.stampTable) {
                            // remove the timestamp (if any) for this lock request
                            TimeObj timeObj = new TimeObj(xid);
                            this.stampTable.remove(timeObj);
                        }
                        synchronized (this.waitTable) {
                            // remove the entry for this transaction from waitTable (if it
                            // is there) as it has been granted its lock request
                            WaitObj waitObj = new WaitObj(xid, strData, lockType);
                            this.waitTable.remove(waitObj);
                        }
                         
                        if (bConvert.get(0) == true) {
                            // lock conversion 
                            // *** ADD CODE HERE *** to carry out the lock conversion in the
                            // lock table
                        	
                        	System.out.println("LM:Conversion of locks...");
                        	
                        	TrxnObj readTrxnObj = new TrxnObj(xid, strData, READ);
                            DataObj readDataObj = new DataObj(xid, strData, READ);
                            lockTable.remove(readTrxnObj);
                            lockTable.remove(readDataObj);
                            lockTable.add(trxnObj);
                            lockTable.add(dataObj);
                        	/*
                        	//get current data obj, clone it, change the lock in clone, remove original, place clone back in
                        	try {
								//XObj x2 = this.lockTable.get(dataObj);
								//DataObj dataObj1 = (DataObj) x2.clone();
								//dataObj1.lockType = dataObj1.WRITE;
								//this.lockTable.remove(x2);
								//this.lockTable.add(dataObj1);
								
								//get current trx obj, clone it, change the lock in clone, remove original, place clone back in
								//XObj x3 = lockTable.get(trxnObj);
								//TrxnObj trx1 = (TrxnObj) x3.clone();
								//trx1.lockType = trx1.WRITE;
								//this.lockTable.remove(x3);
								//this.lockTable.add(trx1);
                        		System.out.println("before loop");
								TrxnObj trxnQueryObj = new TrxnObj(xid, "", -1);  // Only used in elements() call below.
						        Vector vect = this.lockTable.elements(trxnQueryObj);
					            TrxnObj trxnObj2;
					    
					            int size = vect.size();                            
					            for (int i = (size - 1); i >= 0; i--) 
					            {
					            	System.out.println("in loop i = " + i);
					                trxnObj2 = (TrxnObj) vect.elementAt(i);
					                System.out.println("data = " + trxnObj2.getDataName() + "; current lock " + trxnObj2.lockType + "; current trxn : " + trxnObj2.getXId() + "; strdata = " + strData);
					                if ( trxnObj2.getDataName().equalsIgnoreCase(strData)) //same data
					                {
					                	System.out.println("\t in if i = " + i + " data = " + trxnObj2.getDataName() + "; current lock " + trxnObj2.lockType + "; current trxn : " + trxnObj2.getXId());
					                	System.out.println("\tbefore removing trxn object and creating a new one");
					                	TrxnObj o = (TrxnObj) this.lockTable.get(trxnObj2);
					                	o.lockType = WRITE;
					                	this.lockTable.remove(trxnObj2);
					                	this.lockTable.add(o);
					                	
					                	System.out.println("\tbefore removing data object and creating a new one");
				                	    DataObj data2remove = new DataObj(trxnObj2.getXId(), trxnObj2.getDataName(), trxnObj2.getLockType());
						                this.lockTable.remove(data2remove);
						                DataObj data2add = new DataObj(o.getXId(), o.getDataName(), o.getLockType());
						                this.lockTable.add(data2add);
					                
						                System.out.println("\tbefore break");
						                break; //only 1 data item request should match
					                }
					            } 
                        	}
					        catch (Exception e) {
							e.printStackTrace();
							}
                        	*/
                        } else {
                            // a lock request that is not lock conversion
                        	//System.out.println("addflight should go here : trxid = " + xid);
                            this.lockTable.add(trxnObj);
                            this.lockTable.add(dataObj);
                        }
                    }
                }
                if (bConflict) {
                    // lock conflict exists, wait
                    WaitLock(dataObj);
                }
            }
        } 
        catch (DeadlockException deadlock) {
            throw deadlock;
        }
        catch (RedundantLockRequestException redundantlockrequest) {
              // just ignore the redundant lock request
            return true;
        } 

        return true;
    }

    
    // remove all locks for this transaction in the lock table.
    public boolean  UnlockAll(int xid) {

        // if any parameter is invalid, then return false
        if (xid < 0) {
            return false;
        }

        TrxnObj trxnQueryObj = new TrxnObj(xid, "", -1);  // Only used in elements() call below.
        synchronized (this.lockTable) {
            Vector vect = this.lockTable.elements(trxnQueryObj);

            TrxnObj trxnObj;
            Vector waitVector;
            WaitObj waitObj;
            int size = vect.size();
                                                
            for (int i = (size - 1); i >= 0; i--) {
                
                trxnObj = (TrxnObj) vect.elementAt(i);
                this.lockTable.remove(trxnObj);

                DataObj dataObj = new DataObj(trxnObj.getXId(), trxnObj.getDataName(), trxnObj.getLockType());
                this.lockTable.remove(dataObj);
                                        
                // check if there are any waiting transactions. 
                synchronized (this.waitTable) {
                    // get all the transactions waiting on this dataObj
                    waitVector = this.waitTable.elements(dataObj);
                    int waitSize = waitVector.size();
                    for (int j = 0; j < waitSize; j++) {
                        waitObj = (WaitObj) waitVector.elementAt(j);
                        if (waitObj.getLockType() == LockManager.WRITE) {
                            if (j == 0) {
                                // get all other transactions which have locks on the
                                // data item just unlocked. 
                                Vector vect1 = this.lockTable.elements(dataObj);
                                
                                // remove interrupted thread from waitTable only if no
                                // other transaction has locked this data item
                                if (vect1.size () == 0) {
                                    this.waitTable.remove(waitObj);     
                                    
                                    try {
                                        synchronized (waitObj.getThread())    {
                                            waitObj.getThread().notify();
                                        }    
                                    }
                                    catch (Exception e)    {
                                        System.out.println("Exception on unlock\n" + e.getMessage());
                                    }        
                                }
                                else {
                                    // some other transaction still has a lock on
                                    // the data item just unlocked. So, WRITE lock
                                    // cannot be granted.
                                    break;
                                }
                            }

                            // stop granting READ locks as soon as you find a WRITE lock
                            // request in the queue of requests
                            System.out.println("stopping granting read locks since write lock is on it");
                            break;
                        } else if (waitObj.getLockType() == LockManager.READ) {
                            // remove interrupted thread from waitTable.
                            this.waitTable.remove(waitObj);    
                            
                            try {
                                synchronized (waitObj.getThread()) {
                                    waitObj.getThread().notify();
                                }    
                            }
                            catch (Exception e) {
                                System.out.println("Exception e\n" + e.getMessage());
                            }
                        }
                    }
                } 
            }
        } 

        return true;
    }

    
    // returns true if the lock request on dataObj conflicts with already existing locks. If the lock request is a
    // redundant one (for eg: if a transaction holds a read lock on certain data item and again requests for a read
    // lock), then this is ignored. This is done by throwing RedundantLockRequestException which is handled 
    // appropriately by the caller. If the lock request is a conversion from READ lock to WRITE lock, then bitset 
    // is set. 
    
    private boolean LockConflict(DataObj dataObj, BitSet bitset) throws DeadlockException, RedundantLockRequestException {
    	//System.out.println("\t Lock Manager: in Lock Conflict");
        Vector vect = this.lockTable.elements(dataObj);
       // System.out.println("\t Lock Manager: vector empty = " + vect.isEmpty());
        DataObj dataObj2;
        int size = vect.size();
        
        //System.out.println("trx id in lock conflict " + dataObj.xid);
        // as soon as a lock that conflicts with the current lock request is found, return true
        for (int i = 0; i < size; i++) {
            dataObj2 = (DataObj) vect.elementAt(i);
           // System.out.println("dataObj1 =  " + dataObj + " ; dataObj2= = " + dataObj2);
            if (dataObj.getXId() == dataObj2.getXId()) {    
            	//System.out.println("dataObj.getXId() == dataObj2.getXId()");
                // the transaction already has a lock on this data item which means that it is either
                // relocking it or is converting the lock
                if (dataObj.getLockType() == DataObj.READ) {    
                    // since transaction already has a lock (may be READ, may be WRITE. we don't
                    // care) on this data item and it is requesting a READ lock, this lock request
                    // is redundant.
                    throw new RedundantLockRequestException(dataObj.getXId(), "Redundant READ lock request");
                } else if (dataObj.getLockType() == DataObj.WRITE) {
                    // transaction already has a lock and is requesting a WRITE lock
                    // now there are two cases to analyze here
                    // (1) transaction already had a READ lock
                    // (2) transaction already had a WRITE lock
                    // Seeing the comments at the top of this function might be helpful
                    // *** ADD CODE HERE *** to take care of both these cases
                	//System.out.println("queryflight should not go here : dataObj.getLockType() == DataObj.WRITE");
                	 // (1) transaction already had a READ lock
                	if(dataObj2.getLockType() == DataObj.READ)
                	{
                		System.out.println("lock conversion from READ to WRITE");
                		//boolean onlyTrx = true;
                		/*
                		for ( int j = 0; j < size; j++)
                		{
                			DataObj dataObj3 = (DataObj) vect.elementAt(j);
                			if ( dataObj3.getXId() != dataObj2.getXId())
                			{
                				System.out.println("trx wants WRITE and already has READ, someone else has READ");
                				return true;
                			}
                				
                		}
                		
                		System.out.println("Before returning false, lock conversion should succeed...");
                		*/
                		//set conversion
                		bitset.set(0); //sets at index 0 to true
                		return false;
                	}
                	// (2) transaction already had a WRITE lock
                	else if (dataObj2.getLockType() == DataObj.WRITE)
                	{
                		 throw new RedundantLockRequestException(dataObj.getXId(), "Redundant WRITE lock request");
                	}
                	else
                	{
                		System.out.println("should not be here, either it requests a READ or WRITE!!");
                	}
                	
                }
            } 
            else {
                if (dataObj.getLockType() == DataObj.READ) {
                    if (dataObj2.getLockType() == DataObj.WRITE) {
                        // transaction is requesting a READ lock and some other transaction
                        // already has a WRITE lock on it ==> conflict
                    	//System.out.println("queryflight should go here after calling newflight");
                        System.out.println("Want READ, someone has WRITE");
                        return true;
                    }
                    else {
                        // do nothing 
                    }
                } else if (dataObj.getLockType() == DataObj.WRITE) {
                    // transaction is requesting a WRITE lock and some other transaction has either
                    // a READ or a WRITE lock on it ==> conflict
                    System.out.println("Want WRITE, someone has READ or WRITE");
                    return true;
                }
            }
        }
        //System.out.println("returning false, no case above handled");
        // no conflicting lock found, return false
        return false;
    }
    
    private void WaitLock(DataObj dataObj) throws DeadlockException {
        // Check timestamp or add a new one.
        // Will always add new timestamp for each new lock request since
        // the timeObj is deleted each time the transaction succeeds in
        // getting a lock (see Lock() )
        
        TimeObj timeObj = new TimeObj(dataObj.getXId());
        TimeObj timestamp = null;
        long timeBlocked = 0;
        Thread thisThread = Thread.currentThread();
        WaitObj waitObj = new WaitObj(dataObj.getXId(), dataObj.getDataName(), dataObj.getLockType(), thisThread);

        synchronized (this.stampTable) {
            Vector vect = this.stampTable.elements(timeObj);
            if (vect.size() == 0) {
                // add the time stamp for this lock request to stampTable
                this.stampTable.add(timeObj);
                timestamp = timeObj;
            } else if (vect.size() == 1) {
                // lock operation could have timed out; check for deadlock
                TimeObj prevStamp = (TimeObj)vect.firstElement();
                timestamp = prevStamp;
                timeBlocked = timeObj.getTime() - prevStamp.getTime();
                if (timeBlocked >= LockManager.DEADLOCK_TIMEOUT) {
                    // the transaction has been waiting for a period greater than the timeout period
                    cleanupDeadlock(prevStamp, waitObj);
                }
            } else {
                // should never get here. shouldn't be more than one time stamp per transaction
                // because a transaction at a given time the transaction can be blocked on just one lock
                // request. 
            }
        } 
        
        // suspend thread and wait until notified...

        synchronized (this.waitTable) {
            if (! this.waitTable.contains(waitObj)) {
                // register this transaction in the waitTable if it is not already there 
                this.waitTable.add(waitObj);
            }
            else {
                // else lock manager already knows the transaction is waiting.
            }
        }
        
        synchronized (thisThread) {
            try {
                thisThread.wait(LockManager.DEADLOCK_TIMEOUT - timeBlocked);
                TimeObj currTime = new TimeObj(dataObj.getXId());
                timeBlocked = currTime.getTime() - timestamp.getTime();
                if (timeBlocked >= LockManager.DEADLOCK_TIMEOUT) {
                    // the transaction has been waiting for a period greater than the timeout period
                    cleanupDeadlock(timestamp, waitObj);
                }
                else {
                    return;
                }
            }
            catch (InterruptedException e) {
                System.out.println("Thread interrupted?");
            }
        }
    }
    

    // cleanupDeadlock cleans up stampTable and waitTable, and throws DeadlockException
    private void cleanupDeadlock(TimeObj tmObj, WaitObj waitObj)
        throws DeadlockException
    {
        synchronized (this.stampTable) {
            synchronized (this.waitTable) {
                this.stampTable.remove(tmObj);
                this.waitTable.remove(waitObj);
            }
        }
        throw new DeadlockException(waitObj.getXId(), "Sleep timeout...deadlock.");
    }
}
