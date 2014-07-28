package ir.blackgrape.bereshtook.service;

interface IXMPPDataService {
	void saveData(String key, String value);
	String loadData(String key);
}