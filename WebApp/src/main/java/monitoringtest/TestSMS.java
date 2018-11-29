package monitoringtest;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;

import com.twilio.Twilio;
import com.twilio.base.ResourceSet;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class TestSMS implements Runnable{
	
	private String nummer;
	private String messageIDSMS;
	ReadProperties getproperties = new ReadProperties("ConfigureData.properties");
	public static final ConcurrentMap<String, MapValuestoID> SMSIDlist = new ConcurrentHashMap<String, MapValuestoID>();
	
	public TestSMS(String nummer) {
		this.nummer=nummer;
	}
	
	@Override
	public void run(){
		try {
			//Twilio authorization
			String ACCOUNT_SID = "ACd798ca089dac69a0173396e3e6b19533";
			String AUTH_TOKEN = "f64655c6c2b98cb737cdbd138c209ba2";
			Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
			//send sms
			sendsms();
			System.out.println("sending SMS");
			
			//Wait for a predefined time and then check whether SMS is received or not. If not send an alert via Twilio SMS.
			int sendalerttime = Integer.parseInt(getproperties.getPropertiesFile().getProperty("sendalertSMS"));
			TimeUnit.SECONDS.sleep(sendalerttime);
			if(SMSIDlist.get(messageIDSMS).getstatus().equals(MapValuestoID.Status.NOT_RECEIVED)) {
				System.out.println("Sending SMS failed");
			} else {System.out.println("SMS sent successfully");}
			
		}catch (Exception ex) {
	    Thread t = Thread.currentThread();
	    t.getUncaughtExceptionHandler().uncaughtException(t, ex);}
	}

	private String sendsms() throws Exception{
		SendMessage sms = new SendMessage("SMS", this.nummer);
		messageIDSMS = sms.getMessageIDRequest();
		SMSIDlist.put(messageIDSMS, new MapValuestoID(new Date(), MapValuestoID.Status.NOT_RECEIVED, false));
		return messageIDSMS;
	}

	/**
	 * This method updates the status of the elements in the IDlist by checking for each element whether or not it is included in the list of
	 * received Twilio SMSs. It also removes IDs which are older than a predefined number of seconds.
	 */
	public Runnable updateList () throws Exception {
		
		Thread updateList = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					int deleteIDifolderthan = Integer.parseInt(getproperties.getPropertiesFile().getProperty("deleteIDSMS"));
					int sendalerttime = Integer.parseInt(getproperties.getPropertiesFile().getProperty("sendalertSMS"));
					Iterator<String> it = SMSIDlist.keySet().iterator();
					Date currentdate = new Date();
					while(it.hasNext()) {
						String element = it.next();
						for (String m : getListfromTwilio()) {
							if(m.equals(element)) {
								SMSIDlist.get(element).setstatus(MapValuestoID.Status.RECEIVED);
							} 
						}
						long difference = getDateDiff(SMSIDlist.get(element).getsentdate(), currentdate, TimeUnit.SECONDS);
						if(difference>=deleteIDifolderthan) {
							it.remove();
						}
						if(SMSIDlist.get(element).getstatus().equals(MapValuestoID.Status.NOT_RECEIVED)&&difference>=sendalerttime&&SMSIDlist.get(element).getAlertSent()==false) {
							sendTwilioSMSAlert (element, sendalerttime + " seconds");
							SMSIDlist.get(element).setAlertSent(true);
						}
					}
				} catch (Exception e) {
				e.printStackTrace();
				}
			}
		});	
		
		return updateList;
	}
	
	private ArrayList<String> getListfromTwilio() throws Exception{
		
		ArrayList<String> twilioMessagesList = new ArrayList<String>();
		int daysfromnow = Integer.parseInt(getproperties.getPropertiesFile().getProperty("twilioGetListTimeHorizontInDays"));
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	    Calendar starttime = Calendar.getInstance();
	    starttime.add(Calendar.DATE, -daysfromnow);
	    DateTime start = DateTime.parse(dateFormat.format(starttime.getTime()));
	    for (DateTime date = start; date.isBeforeNow(); date = date.plusDays(1)){
	  
	    	//get List of Messages from Twilio...
	    	ResourceSet<Message> messages = Message.reader().setTo(new PhoneNumber(this.nummer)).setDateSent(date).read();
	    	//... and add the contents to twilioMessagesList
	    	for (Message message : messages) {
	    		twilioMessagesList.add(message.getBody());
		    }
	    }
	    return twilioMessagesList;
	}
	
	/**
	 * This method sends a SMS via Twilio to a predefined phone number in case a SMS was not successfully sent, i.e. not received.
	 * @param messageID
	 * It is the ID that failed to be sent.
	 * @param duration
	 * It specifies what time passed since the SMS was sent.
	 */
	private void sendTwilioSMSAlert (String messageID, String duration) throws Exception {
		String sendalertto = getproperties.getPropertiesFile().getProperty("alertstophonenumber");
	    Message message = Message.creator(new PhoneNumber(sendalertto),
	        new PhoneNumber(this.nummer), "The SMS with the Message-ID " + messageID + " has not been received within " + duration + ".").create();
	}
	
	/**
	 * This method calculates the difference between any two dates in any time unit.
	 */
	private long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
	    long diffInMillies = date2.getTime() - date1.getTime();
	    return timeUnit.convert(diffInMillies,TimeUnit.MILLISECONDS);
	}
	
	public static ConcurrentMap<String, MapValuestoID> getSMSIDlist(){
		return SMSIDlist;
	}
}

