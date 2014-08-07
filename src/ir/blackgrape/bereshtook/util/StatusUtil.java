package ir.blackgrape.bereshtook.util;

import android.location.Location;

public class StatusUtil {
	
	public static CharSequence makeStatus(String herStatus) {
		if(herStatus.contains("S")){
			String coins = herStatus.substring(0, herStatus.indexOf('S'));
			return coins + "سکه";
		}
		else
			return "";
	}
	
	public static CharSequence makeStatusWithLocation(String mLocation, String herStatus) {
		String[] splitedNear = mLocation.split("#");
		Location nearLoc = new Location("near");
		nearLoc.setLatitude(Double.parseDouble(splitedNear[0]));
		nearLoc.setLongitude(Double.parseDouble(splitedNear[1]));
		
		return makeStatusWithLocation(nearLoc, herStatus);
	}

	 public static CharSequence makeStatusWithLocation(Location mLocation, String herStatus) {
		 String coins = null;
		 String distance = null;
		 
		 if(herStatus.contains("S") && herStatus.contains("#")){
			 String[] splited = herStatus.split("S");
			 coins = splited[0];
			 distance = findDistance(mLocation, splited[1]);
		 }
		 else if(herStatus.contains("S") && !herStatus.contains("#")){
			 String[] splited = herStatus.split("S");
			 coins = splited[0];
		 }
		 else if(!herStatus.contains("S") && herStatus.contains("#")){
			 // never happens
			 return "";
		 }
		 else
			 return "";
		 
		 StringBuilder sb = new StringBuilder();
		 if(coins != null && distance != null){
			 sb.append(coins);
			 sb.append("سکه ");
			 sb.append(" (");
			 sb.append(distance);
			 sb.append(")");
			 return sb;
		 }
		 else if(coins != null)
			 return coins + "سکه ";
		 else if(distance != null)
			 return "(" + distance + ")";
		 else
			 return "";
	}
	 
	public static String findDistance(Location mLocation, String strStatus){
		
		String[] splited = strStatus.split("#");
		Location farLoc = new Location("far");
		farLoc.setLatitude(Double.parseDouble(splited[0]));
		farLoc.setLongitude(Double.parseDouble(splited[1]));
		Float fDis = mLocation.distanceTo(farLoc);
		
		String strDis = "";
		Integer iDis = fDis.intValue();
		
		if(fDis < 2000){
			strDis += iDis.toString() + "متر ";
		}
		else{
			Double dDis = iDis.doubleValue() / 1000;
			iDis = dDis.intValue();
			strDis += iDis.toString() + "کیلومتر ";
		}
		return strDis;
	}
}
