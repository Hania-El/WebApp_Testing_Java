package monitoringtest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Base64;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;

@SuppressWarnings("deprecation")
public class SendMessage {
	
	private String messageIDRequest;
	
	public String getMessageIDRequest() {
		return messageIDRequest;
	}

	public SendMessage(String methode, String empfaenger) {
		String url = "********************";
		HttpPost post = new HttpPost(url);
		 
		String encoding = Base64.getEncoder().encodeToString(("monitor:t$gh!6KiO").getBytes());
		
		post.setHeader("Authorization", "Basic " + encoding);
		
		String messageid = "monitoringtest" + String.valueOf(System.nanoTime());
		String subject ="monitoringtest";
		String messageidHex=String.format("%05x", new BigInteger(1, messageid.getBytes(/*YOUR_CHARSET?*/)));
		String subjectHex=String.format("%05x", new BigInteger(1, subject.getBytes(/*YOUR_CHARSET?*/)));
		String body1="";
		String body2="";
		String body3="";
		String body="";
		
		body1="<rest:MessageList xmlns:rest=\"urn:www-deutschepost-de:PointOfContact/Messaging/2.0/REST\">\n" + 
				"<Message MessageId=" + "\"" + messageid + "\"" + ">\n<Context>";
		
		if(methode=="SMS") {
			body2="<Sender>\n<Type>SMS</Type>\n<Address>+4915125026875</Address>\n</Sender>";
		}
		
		body3 = "<Recipient>\n<Addresses>\n<!--1 or more repetitions:-->\n<Address>\n<Type>" + methode +"</Type>\n<Address>" + empfaenger + "</Address>\n" + 
				"</Address>\n</Addresses>\n</Recipient>\n<MetaInformation>\n<ContactReason>OB_TEST</ContactReason>\n</MetaInformation>\n</Context>\n<Content>\n<Parts>\n<Part>\n" + 
				"<Id>body</Id><!--" + messageid +"-->\n\n<Data>" + messageidHex+ "</Data>\n<MimeType>text/plain</MimeType>\n</Part>\n<Part>\n<Id>subject</Id>\n" + 
				"<!--" + subject + "-->\n<Data>" + subjectHex + "</Data>\n<MimeType>text/plain</MimeType>\n</Part>\n</Parts>\n</Content>\n</Message>\n</rest:MessageList>";
		
		body = body1 + body2 + body3;
	     
		try {
		    StringEntity entity = new StringEntity(body);
		    entity.setContentType(new BasicHeader("Content-Type", "application/xml"));
		    post.setEntity(entity);
		    HttpClient httpclient = new DefaultHttpClient();
		    HttpResponse response = httpclient.execute(post);
		    response.getEntity().getContent().close();
		    //TODO: How to react to different response codes
		} catch (UnsupportedEncodingException e) {
		    e.printStackTrace();
		} catch (ClientProtocolException e) {
		    e.printStackTrace();
		} catch (IOException e) {
		    e.printStackTrace();
		} 
		
		messageIDRequest=messageid;
	}

}
