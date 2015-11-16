package main;

import java.io.File;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import server.ResourceManagerImpl;


public class Main {

	public static String file;
	
    public static void main(String[] args) 
    throws Exception {
    
        if (args.length != 3) {
            System.out.println(
                "Usage: java Main <service-name> <service-port> <deploy-dir>");
            System.exit(-1);
        }
        
        file = args[0];
        System.out.println("file is " + file);
        System.out.println("setting resourceManagerImpl file to " + file);
        ResourceManagerImpl.file = args[0];
        
        String serviceName = args[0];
        int port = Integer.parseInt(args[1]);
        String deployDir = args[2];
    
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.setBaseDir(deployDir);

        tomcat.getHost().setAppBase(deployDir);
        tomcat.getHost().setDeployOnStartup(true);
        tomcat.getHost().setAutoDeploy(true);

        //tomcat.addWebapp("", new File(deployDir).getAbsolutePath());
        System.out.println("");
        tomcat.addWebapp("/" + serviceName, 
                new File(deployDir + "/" + serviceName).getAbsolutePath());

       //System.out.println(System.getProperty("user.dir"));
       // System.out.println(tomcat.noDefaultWebXmlPath());
       // tomcat.initWebappDefaults("test.xml");
        tomcat.enableNaming();
        
        
        tomcat.start();
        tomcat.getServer().await();
    }
    
    public Main() throws Exception
    {
    	System.out.println("Im code you just added");
    	System.out.println("file is : " + file);
    }
    
}
