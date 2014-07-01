package ir.bereshtook.androidclient.service;

import ir.bereshtook.androidclient.exceptions.BereshtookXMPPException;
import ir.bereshtook.androidclient.util.ConnectionState;


public interface Smackable {
	boolean doConnect(boolean create_account) throws BereshtookXMPPException;
	boolean isAuthenticated();
	void requestConnectionState(ConnectionState new_state);
	void requestConnectionState(ConnectionState new_state, boolean create_account);
	ConnectionState getConnectionState();
	String getLastError();

	void addRosterItem(String user, String alias, String group) throws BereshtookXMPPException;
	void removeRosterItem(String user) throws BereshtookXMPPException;
	void renameRosterItem(String user, String newName) throws BereshtookXMPPException;
	void moveRosterItemToGroup(String user, String group) throws BereshtookXMPPException;
	void renameRosterGroup(String group, String newGroup);
	void sendPresenceRequest(String user, String type);
	void addRosterGroup(String group);
	
	void setStatusFromConfig();
	void sendMessage(String user, String message);
	void sendServerPing();
	
	void registerCallback(XMPPServiceCallback callBack);
	void unRegisterCallback();
	
	String getNameForJID(String jid);
}
