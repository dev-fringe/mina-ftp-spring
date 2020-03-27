package dev.fringe.ftp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Server implements InitializingBean{

	@Value("${FTP_USER:admin}")
	private String ftp_user;

	@Value("${FTP_PWD_MD5:pass}")
	private String ftp_pwd;

	@Value("${FTP_HOME:d:\\}")
	private String ftp_home;

	@Value("${FTP_CONTROL_PORT:2333}")
	private String ftp_ctrl_port;

	@Value("${FTP_DATA_PORT:24444}")
	private String ftp_data_port;

	/**
	 * Number of seconds before an idle data connection is closed
	 */
	@Value("${FTP_IDLE_TIMEOUT:30}")
	private String ftp_idle_timeout = "30";

	/**
	 * The address the server will claim to be listening on in the PASV reply. 
	 * Useful when the server is behind a NAT firewall and the client sees a different
	 * address than the server is using
	 */
	@Value("${FTP_EXT_ADDR:127.0.0.1}")
	private String ftp_external_address;

	
	public void afterPropertiesSet() throws Exception {
		FtpServerFactory serverFactory = new FtpServerFactory();
		ListenerFactory factory = new ListenerFactory();

		// set the port of the listenher
		factory.setPort(Integer.valueOf(ftp_ctrl_port));

		DataConnectionConfigurationFactory dcFactory = new DataConnectionConfigurationFactory();
		dcFactory.setIdleTime(Integer.valueOf(ftp_idle_timeout));
		dcFactory.setPassiveExternalAddress(ftp_external_address);
		dcFactory.setPassivePorts(ftp_data_port);
		factory.setDataConnectionConfiguration(dcFactory.createDataConnectionConfiguration());

		// replace the default listener
		serverFactory.addListener("default", factory.createListener());

		PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
		try {
			// userManagerFactory.setFile(new File(this.getClass().getResource("/users.properties").toURI()));
			Properties props = new Properties();
			props.load(this.getClass().getResourceAsStream("/users.properties"));
			Properties parsedProps = new Properties();
			props.keySet().stream().forEach(k -> {
				String newValue = ((String) props.get(k)).replace("${FTP_PWD_MD5}", ftp_pwd);
				newValue = newValue.replace("${FTP_HOME}", ftp_home);
				parsedProps.put(((String) k).replace("${FTP_USER}", ftp_user), newValue);
			});
			File propFile = File.createTempFile("ftp-parsed-", ".props");
			parsedProps.store(new FileOutputStream(propFile), "");
			userManagerFactory.setFile(propFile);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		serverFactory.setUserManager(userManagerFactory.createUserManager());

		// start the server
		FtpServer server = serverFactory.createServer();
		try {
			server.start();
		} catch (FtpException e) {
			e.printStackTrace();
		}
		
	}
	public static void main(String[] args) {
	        new AnnotationConfigApplicationContext(Server.class).getBean(Server.class);   
	}

}
