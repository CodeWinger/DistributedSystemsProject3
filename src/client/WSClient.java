package client;

import java.net.URL;
import java.net.MalformedURLException;

public class WSClient {

    MainService service; //ResourceManagerImplService
    
    Main proxy;
    
    public WSClient(String serviceName, String serviceHost, int servicePort) 
    throws MalformedURLException {
    
        URL wsdlLocation = new URL("http", serviceHost, servicePort, 
                "/" + serviceName + "/service?wsdl");
                
        service = new MainService(wsdlLocation);
        
        proxy = service.getMainPort(); //getResourceManagerImplport()
    }

}
