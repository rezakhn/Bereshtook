package ir.blackgrape.bereshtook.service;

interface IXMPPDataService {
	void saveData(String key, String value);
	String loadData(String key);
	
	void saveAvatar(in byte[] avatarBytes);
	byte[] loadAvatar(String username);
}