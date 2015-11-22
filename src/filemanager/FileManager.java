package filemanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import server.RMHashtable;

/*class used to manager writing necessary data to disk using the shadowing technique for the 2 phase commit protocol*/
public class FileManager 
{
	//master file with the 2 files that transactions can use one at a time
	private File masterFilePointer;
	
	//sets the current Master file between f1 and f2
	private File currentMasterFile;
	private File shadowFile;
	
	//constructs the new files for stable storage by checking whether or not the master record exists and setting files accordingly
	public FileManager(String masterFile, String file1, String file2)
	{
		//create new file elements
		masterFilePointer = new File( masterFile);

		File f1 = new File(file1);
		File f2 = new File(file2);
		
		//check if master file already exists, if so do not create from scratch the master file
		if (masterFilePointer.exists())
		{
			//debug purposes TODO : remove this check
			//if (new File(directory).list().length != 3)
				//System.out.println("Directory with master node exists, but wrong number of files!");
				
			try 
			{
				//TODO: remove print statements afterwards 
				//get current file name for correct data
				BufferedReader br = new BufferedReader(new FileReader(masterFile));
				String line = br.readLine();
				line = line.replace("\\", "/"); //replace backslashes with foward slashes
				/*System.out.println("file 1 : " + file1);
				System.out.println("file2 : " + file2);
				System.out.println("line is : " + line);
				System.out.println("file1 length " + file1.length());
				System.out.println("file2 length " + file2.length());
				System.out.println("line length " + line.length());
				for ( int i = 0; i < line.length(); i++)
				{
					System.out.println("i : " + 1 + ", file[" + i + "] =  " + file1.charAt(i) + ", line[" + i + "] = "  + line.charAt(i));
				}
				System.out.println("file1 == line " + (file1.equals(line)));
				System.out.println("file2 == line " + (file2.equals(line)));*/

				//if both filenames are valid 
				if ((file1.equals(line) /*&& f2.exists()*/))
				{
					System.out.println("CORRECT : file1 == line " + (file1.equals(line)));
					currentMasterFile = f1;
					shadowFile = f2;
				}
				//if both filenames are valid
				else if (file2.equals(line) /*&& f1.exists()*/)
				{
					System.out.println("CORRECT : file2 == line " + (file2.equals(line)));
					currentMasterFile = f2;
					shadowFile = f1;
				}
				//TODO: remove this, the else ifs below should never run
				else if( file1 != line && file2 != line)
					System.out.println("none of the 2 files exists");
				//debug purposes, should not ever happen since user has no control to call this code TODO : remove this
				else if (file1 == line && !f2.exists())
				{
					System.out.println("WRONG FILENAME FOR THE SECOND FILE : " + file2);
				}
				else if (file2 == line && !f1.exists())
				{
					System.out.println("WRONG FILENAME FOR THE FIRST FILE : " + file1);
				}
			} 
			catch (Exception e) 
			{
				System.out.println("file " + masterFilePointer + " not found");
			}
		}
		else
		{
			//set currentMaster to f1 (doesn't matter, could have been f2 since both of them are the same) and f2 to shadowFile
			currentMasterFile = f1;
			shadowFile = f2;
			
			PrintWriter writer;
			try {
				//get writer object
				writer = new PrintWriter(masterFilePointer, "UTF-8");
				
				//print new current master file (this is considered an atomic expression
				writer.println(currentMasterFile.toString()); //TODO: write here places a path may, cause problems
			
				//close resource
				writer.close();

			} catch (Exception e) {	
				System.out.println("could not initialize master node");
			}
		}
	}
	
	//writes the serialized object data passed as a parameter to the function to the shadow copy
	public boolean writeMainMemoryToShadow(Object data) //TODO : change here names and checks
	{
		try
        {
		   //get file and object output stream
           FileOutputStream fos = new FileOutputStream(shadowFile);
           ObjectOutputStream oos = new ObjectOutputStream(fos);
           
           //write data to file
           oos.writeObject(data);
           
           //close resources
           oos.close();
           fos.close();
           
           System.out.printf("Serialized data object " + data + " in current shadowing file " + shadowFile); //TODO: remove this when done
           return true;
        }
		catch(Exception e)
	     {
	         System.out.println("Could not write to disk for object : " + data);
	         return false;
	     }
	}
	
	//reads from stable storage from the current master file. Returns null if object does not exist
	public Object readFromStableStorage() //TODO : change here names and checks
	{
		//return object
		Object data = null;
		try
	      {
			//checks to see if file is initially black, TODO: remove this possibly
			if (currentMasterFile.length() == 0)
				return data;
			
			//create file and object input stream
	         FileInputStream fis = new FileInputStream(currentMasterFile);
	         ObjectInputStream ois = new ObjectInputStream(fis);
	         
	         //read object
	         data = ois.readObject();
	         
	         //close resources
	         ois.close();
	         fis.close();
	      }
		catch(Exception e)
	      {
			System.out.println("Could not read from disk for object");
	      }
		return data;
	}
	
	//exchanges shadow and current master values and writes to disk on the master pointer file
	public boolean changeMasterToShadowCopy()
	{
		PrintWriter writer;
		try {
			//get writer object
			writer = new PrintWriter(masterFilePointer, "UTF-8");
			
			//print new current master file (this is considered an atomic expression
			writer.println(shadowFile.toString()); //TODO: write here places a path may, cause problems
			
			//exchange shadow and current master nodes
			File tempFile = currentMasterFile;
			currentMasterFile = shadowFile;
			shadowFile = tempFile;
					
			//close resource
			writer.close();
			
			return true;
		} catch (Exception e) {	
			System.out.println("could not change master node");
			return false;
		}
	}
	
	/*private void bootup() //TODO: create files if cannot be read from xml?
	{
		//read from master file which file to read
		if ( new File(masterFile).exists())
		{
			try (BufferedReader br = new BufferedReader(new FileReader(currentMasterFile))) {
				
				
			    String line;
			    //iterate line by line, although there should be only 1 line
			    while ((line = br.readLine()) != null) {
			    	
			      //get current file
			      currentFile = line;
			      if ( currentFile == "f1")
			    	  shadowFile = "f2";
			      else
			    	  shadowFile = "f1";
			      break;
			    }
			} catch (Exception e) {
		
			}
			
			//set up hashMap
			readFromStableStorage();
		}
		else //no master file, setup by hand
		{
			PrintWriter writer, writer2, writer3;
			try {
				//create all 3 files
				writer = new PrintWriter(masterFile, "UTF-8");
				writer2 = new PrintWriter(currentFile, "UTF-8");
				writer3 = new PrintWriter(shadowFile, "UTF-8");
				
				//make master point to current file
				writer.println(currentFile);
			
				writer.close();
				writer2.close();
				writer3.close();
			} catch (Exception e) {	
				System.out.println("could not change master node");
				
			}
		}
		
	}*/ //TODO: not sure this method is needed
}
