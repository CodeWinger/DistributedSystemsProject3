package middleRM.ws;

import java.net.MalformedURLException;
import java.net.URL;

public class Connection 
{
	 ResourceManagerImplService service;
	 public ResourceManager proxy; //server.ws.ResourceManager
	 
	 public Connection(String serviceName, String serviceHost, int servicePort) 
	 throws MalformedURLException {
	 
	     URL wsdlLocation = new URL("http", serviceHost, servicePort, 
	             "/" + serviceName + "/service?wsdl");
	     
	     
	     service = new ResourceManagerImplService(wsdlLocation);
	     
	     proxy = service.getResourceManagerImplPort();
	 }
}
