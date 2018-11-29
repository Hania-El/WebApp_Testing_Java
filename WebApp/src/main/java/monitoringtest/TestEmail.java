package monitoringtest;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.search.FlagTerm;

import com.twilio.Twilio;
import com.twilio.type.PhoneNumber;

public class TestEmail implements Runnable{
	
	private int LengthmessageID;
	private String emailadresse;
	private String mailserver;
	private String username;
	private String passwort;
	private String folder;
	private String nummer;
	private String messageIDMail;
	public static final ConcurrentMap<String, MapValuestoID> mailIDlist = new ConcurrentHashMap<String, MapValuestoID>();
	private Session session = Session.getDefaultInstance(new Properties());
	private Folder inbox;
	private Store store;
	ReadProperties getproperties = new ReadProperties("ConfigureData.properties");
	
	public TestEmail(String emailadresse, String mailserver, String username, String passwort, String folder, String nummer) {
		this.emailadresse = emailadresse;
		this.mailserver = mailserver;
		this.username = username;
		this.passwort = passwort;
		this.folder = folder;
		this.nummer = nummer;
	}
	
	@Override
	public void run(){
		try {
			//Twilio authorization
			String ACCOUNT_SID = "ACd798ca089dac69a0173396e3e6b19533";
			String AUTH_TOKEN = "f64655c6c2b98cb737cdbd138c209ba2";
			Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
			
			//send mail
			sendmail();
			System.out.println("sending mail");
			
			//Wait for a predefined time and then check whether Mail is received or not. If not send an alert via Twilio SMS.
			int sendalerttime = Integer.parseInt(getproperties.getPropertiesFile().getProperty("sendalertMail"));
			TimeUnit.SECONDS.sleep(sendalerttime);
			if(mailIDlist.get(messageIDMail).getstatus().equals(MapValuestoID.Status.NOT_RECEIVED)) {
				System.out.println("Sending mail failed");
			}else {System.out.println("mail sent successfully");}
		}catch (Exception ex) {
	    Thread t = Thread.currentThread();
	    t.getUncaughtExceptionHandler().uncaughtException(t, ex);
	    }
	}
	
	private String sendmail () {
		SendMessage mail = new SendMessage("EMAIL", this.emailadresse);
		messageIDMail = mail.getMessageIDRequest();
		mailIDlist.put(messageIDMail, new MapValuestoID(new Date(), MapValuestoID.Status.NOT_RECEIVED, false));
		LengthmessageID = messageIDMail.length();
		return messageIDMail;
	}
	
	/**
	 * This method updates the status of the elements in the IDlist by checking for each element whether or not it is included in the list of
	 * received mails in the respective mail folder. It also removes IDs which are older than a predefined number of seconds.
	 */
	
	public Runnable updateList () throws Exception {
		
		Thread updateList = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					int sendalerttime = Integer.parseInt(getproperties.getPropertiesFile().getProperty("sendalertMail"));
					int deleteIDifolderthan = Integer.parseInt(getproperties.getPropertiesFile().getProperty("deleteIDMail"));
					Iterator<String> it = mailIDlist.keySet().iterator();
					Date currentdate = new Date();
					while(it.hasNext()) {
						String element = it.next();
						for (String m : getMailList ()) {
							if(m.equals(element)) {
								mailIDlist.get(element).setstatus(MapValuestoID.Status.RECEIVED);;
							} 
						}
						long difference = getDateDiff(mailIDlist.get(element).getsentdate(), currentdate, TimeUnit.SECONDS);
						if(difference>=deleteIDifolderthan) {
							it.remove();
						}
						if(mailIDlist.get(element).getstatus().equals(MapValuestoID.Status.NOT_RECEIVED)&&difference>=sendalerttime&&mailIDlist.get(element).getAlertSent()==false) {
							sendTwilioSMSAlert (element, sendalerttime + " seconds");
							mailIDlist.get(element).setAlertSent(true);
						}
					}
				} catch (Exception e) {
				e.printStackTrace();
				}
			}
		});	
		
		return updateList;
	}
	
	private ArrayList<String> getMailList () throws Exception{
		this.store = session.getStore("imaps");
		store.connect(mailserver, 993, username, passwort);
		this.inbox = store.getFolder(folder);
		inbox.open(Folder.READ_WRITE);

		ArrayList<String> maillist = new ArrayList<String>();
		// get unseen messages
		Message[] messages = inbox.search(new FlagTerm (new Flags(Flags.Flag.SEEN), false));
		// rank messages from newest to oldest
		Arrays.sort(messages, (m1, m2) -> {
	          try {
		            return m2.getSentDate().compareTo( m1.getSentDate() );
		          } catch ( MessagingException e ) {
		            throw new RuntimeException( e );
		          }
		        } );         
		for ( Message message : messages ) {
			//get sender's email
			Address[] sender = message.getFrom();
			String email = sender == null ? null : ((InternetAddress) sender[0]).getAddress(); 
			//get how old mail is
			Duration d=Duration.between(message.getReceivedDate().toInstant(), Instant.now());
			long minutes = d.toMinutes();
			//get all unseen mails with relevant sender and subject that are max d minutes old
			int timelimit = Integer.parseInt(getproperties.getPropertiesFile().getProperty("readmailtimelimit"));
			if(email.equals("paket@dhl.de")&&message.getSubject().equals("monitoringtest")&&(minutes*60)<=timelimit) {
				maillist.add(String.valueOf(message.getContent()).substring(0,LengthmessageID)); 
				message.setFlag(Flag.SEEN, false);
		    }
		}
		store.close();
		return maillist;
	}
	
	/**
	 * This method sends a SMS via Twilio to a predefined phone number in case a mail was not successfully sent, i.e. not received.
	 * @param messageID
	 * It is the ID that failed to be sent.
	 * @param duration
	 * It specifies what time passed since the mail was sent.
	 */
	
	private void sendTwilioSMSAlert (String messageID, String duration) throws Exception {
		String sendalertto = getproperties.getPropertiesFile().getProperty("alertstophonenumber");
		com.twilio.rest.api.v2010.account.Message message = com.twilio.rest.api.v2010.account.Message.creator(new PhoneNumber(sendalertto),
	        new PhoneNumber(this.nummer), "The Mail with the Message-ID " + messageID + " has not been received within " + duration + ".").create();
	}
	
	/**
	 * This method calculates the difference between any two dates in any time unit.
	 */
	
	private long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
	    long diffInMillies = date2.getTime() - date1.getTime();
	    return timeUnit.convert(diffInMillies,TimeUnit.MILLISECONDS);
	}
}
