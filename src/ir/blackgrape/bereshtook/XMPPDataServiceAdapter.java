package ir.blackgrape.bereshtook;

import ir.blackgrape.bereshtook.service.IXMPPDataService;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
