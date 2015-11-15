package TM;

import java.util.HashMap;

class Item
{
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
