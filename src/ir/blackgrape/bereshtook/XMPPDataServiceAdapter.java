package ir.blackgrape.bereshtook;

import ir.blackgrape.bereshtook.service.IXMPPDataService;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.RemoteException;

public class XMPPDataServiceAdapter {
	
	private IXMPPDataService serviceStub;
	
	public XMPPDataServiceAdapter(IXMPPDataService service){
		this.serviceStub = service;
	}
	
	public void saveGameData(Map<String, String> map){
		try {
			serviceStub.saveData(map);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	public Map<String, String> loadGameData(){
		try {
			return serviceStub.loadData();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void saveAvatar(Bitmap avatar){
		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			avatar.compress(Bitmap.CompressFormat.PNG, 100, stream);
			serviceStub.saveAvatar(stream.toByteArray());
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	public Bitmap loadAvatar(String username){
		byte[] avatarBytes;
		try {
			avatarBytes = serviceStub.loadAvatar(username);
			if(avatarBytes == null)
				return null;
			Bitmap avatar = BitmapFactory.decodeByteArray(avatarBytes, 0, avatarBytes.length);
			return avatar;
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return null;
	}

}
