package ir.blackgrape.bereshtook.service;
import java.util.Map;

interface IXMPPDataService {
	void saveData(in Map map);
	Map loadData();
	
	void saveAvatar(in byte[] avatarBytes);
	byte[] loadAvatar(String username);
}