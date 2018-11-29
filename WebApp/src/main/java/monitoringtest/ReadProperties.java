package monitoringtest;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

public class ReadProperties{

	public Properties properties;
	FileInputStream fs;
	String propertiesFileName;
	
	public ReadProperties(String propertiesFileName) {
		this.propertiesFileName = propertiesFileName;
	}
	
	public Properties getPropertiesFile() throws Exception{
		
		properties = new Properties();
		fs = new FileInputStream(this.propertiesFileName);
		properties.load(fs);
		
		return properties;
	}
}
