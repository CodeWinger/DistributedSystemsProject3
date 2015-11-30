package TM;

import java.io.Serializable;
import java.util.HashMap;

class Item implements Serializable
{
	private static final long serialVersionUID = 7361250011815963147L;
	
	int count;
    int price;
    boolean isDeleted = false;
    boolean isReserved;
    
    public Item(int c, int p, boolean r)
    {
    	count = c; 
    	price = p;
    	isReserved = r;
    }
    
}
