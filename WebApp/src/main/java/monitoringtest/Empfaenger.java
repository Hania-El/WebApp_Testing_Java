package monitoringtest;

public class Empfaenger {
	
	String emailadresse;
	String mailserver;
	String username;
	String passwort;
	String foldername;
	public String phonenumber;
	
	public Empfaenger(String mail, String server, String user, String pw, String folder, String number){
		emailadresse=mail;
		mailserver=server;
		username=user;
		passwort=pw;
		foldername=folder;
		phonenumber=number;
	}
	
}
