package monitoringtest;

import java.util.Date;

public class MapValuestoID {
	
	public static enum Status{
		RECEIVED, NOT_RECEIVED;
	}
	
	private Status status;
	private Date sentdate;
	private boolean isAlertSent;

	public MapValuestoID(Date sentdate, Status status, boolean isAlertSent) {
		this.sentdate = sentdate;
		this.status = status;
		this.isAlertSent = isAlertSent;
	}
	
	public synchronized void setsentdate(Date sentdate) {
		this.sentdate=sentdate;
	}
	
	public synchronized Date getsentdate() {
		return sentdate;
	}
	
	public synchronized void setstatus(Status status) {
		this.status=status;
	}
	
	public synchronized Status getstatus() {
		return status;
	}
	
	public synchronized void setAlertSent(boolean isAlertSent) {
		this.isAlertSent = isAlertSent;
	}
	
	public synchronized boolean getAlertSent() {
		return isAlertSent;
	}
}
