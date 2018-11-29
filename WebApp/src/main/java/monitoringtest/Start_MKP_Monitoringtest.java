package monitoringtest;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Start_MKP_Monitoringtest {
	
	public static void main(String[] args) throws Exception {
		
		ReadProperties getproperties = new ReadProperties("ConfigureData.properties");
		
		String emailaddress = getproperties.getPropertiesFile().getProperty("emailaddress");
		String mailserver = getproperties.getPropertiesFile().getProperty("mailserver");
		String username = getproperties.getPropertiesFile().getProperty("username");
		String password = getproperties.getPropertiesFile().getProperty("password");
		String foldername = getproperties.getPropertiesFile().getProperty("foldername");
		String phonenumber = getproperties.getPropertiesFile().getProperty("phonenumber");
		Empfaenger empfaenger = new Empfaenger(emailaddress, mailserver, username, password, foldername, phonenumber);
		
		int scheduleSMS = Integer.parseInt(getproperties.getPropertiesFile().getProperty("scheduleSMS"));
		int scheduleMail = Integer.parseInt(getproperties.getPropertiesFile().getProperty("scheduleMail"));
		int updateSMSList = Integer.parseInt(getproperties.getPropertiesFile().getProperty("updateSMSlist"));
		int updateMaillist = Integer.parseInt(getproperties.getPropertiesFile().getProperty("updateMaillist"));
		
		TestEmail appmail = new TestEmail(empfaenger.emailadresse, empfaenger.mailserver, empfaenger.username, empfaenger.passwort, empfaenger.foldername, empfaenger.phonenumber);
		TestSMS appsms = new TestSMS(empfaenger.phonenumber);
		
		ScheduledExecutorService scheduledrun = Executors.newScheduledThreadPool(4);
		scheduledrun.scheduleAtFixedRate(appmail, 0, scheduleMail, TimeUnit.SECONDS);
		scheduledrun.scheduleAtFixedRate(appsms, 0, scheduleSMS, TimeUnit.SECONDS);		
		scheduledrun.scheduleWithFixedDelay(appsms.updateList(), 0, updateSMSList, TimeUnit.SECONDS);		
		scheduledrun.scheduleWithFixedDelay(appmail.updateList(), 0, updateMaillist, TimeUnit.SECONDS);		
	}
}

