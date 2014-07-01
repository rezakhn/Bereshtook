package ir.bereshtook.androidclient.location;

import android.location.Location;

public class LocationUtil {

	public static String findDistance(Location mLocation, String strStatus){
		
		String[] splited = strStatus.split("#");
		Location farLoc = new Location("far");
		farLoc.setLatitude(Double.parseDouble(splited[0]));
		farLoc.setLongitude(Double.parseDouble(splited[1]));
		Float fDis = mLocation.distanceTo(farLoc);
		
		String strDis = "";
		Integer iDis = fDis.intValue();
		
		if(fDis < 2000){
			strDis += iDis.toString() + " " + "متر";
		}
		else{
			Double dDis = iDis.doubleValue() / 1000;
			iDis = dDis.intValue();
			strDis += iDis.toString() + " " + "کیلومتر";
		}
		return strDis;
	}
	
	public static String findDistance(String strNear, String strFar){
		
		String[] splitedNear = strNear.split("#");
		Location nearLoc = new Location("far");
		nearLoc.setLatitude(Double.parseDouble(splitedNear[0]));
		nearLoc.setLongitude(Double.parseDouble(splitedNear[1]));
		
		String[] splitedFar = strFar.split("#");
		Location farLoc = new Location("far");
		farLoc.setLatitude(Double.parseDouble(splitedFar[0]));
		farLoc.setLongitude(Double.parseDouble(splitedFar[1]));
		Float fDis = nearLoc.distanceTo(farLoc);
		
		String strDis = "";
		Integer iDis = fDis.intValue();
		
		if(fDis < 2000){
			strDis += iDis.toString() + " " + "متر";
		}
		else{
			Double dDis = iDis.doubleValue() / 1000;
			iDis = dDis.intValue();
			strDis += iDis.toString() + " " + "کیلومتر";
		}
		return strDis;
	}	
}
