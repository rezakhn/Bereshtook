package ir.blackgrape.bereshtook.game;

import ir.blackgrape.bereshtook.service.IXMPPDataService;
import android.os.RemoteException;

public class XMPPDataServiceAdapter {
	
	private IXMPPDataService serviceStub;
	
	public XMPPDataServiceAdapter(IXMPPDataService service){
		this.serviceStub = service;
	}
	
	public void saveGameData(String key, String value){
		try {
			serviceStub.saveData(key, value);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	public String loadGameData(String key){
		try {
			return serviceStub.loadData(key);
		} catch (RemoteException e) {
			e.printStackTrace();
			return null;
		}
	}

}
