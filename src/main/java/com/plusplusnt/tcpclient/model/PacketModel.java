package com.plusplusnt.tcpclient.model;

import java.io.Serializable;

/*
Klasa sluzi se enkapsulaciju primljenih paketa
, cuvanje nekih dodatnih informacija o njima
i implementiranje pomocnih metoda
 */
public class PacketModel implements Serializable
{
	private byte[] byteData;
	private long timeOfArrival;
	private int milisecondsUntilDispatch;
	
	PacketModel(byte[] byteData, long timeOfArrival)
	{
		this.byteData = byteData.clone();
		this.timeOfArrival = timeOfArrival;
		this.milisecondsUntilDispatch = (int)byteData[12]*1000;
	}
	
	public byte[] getByteData()
	{
		return byteData;
	}
	
	public String getPackageIDString()
	{
		return byteData[8] + "|" + byteData[9] + "|" + byteData[10] + "|" + byteData[11];
	}
	
	public void decrementSecondsUntilDispatch()
	{
		milisecondsUntilDispatch = milisecondsUntilDispatch - 10;
	}
	
	public boolean checkSecondsUntilDispatch()
	{
		return milisecondsUntilDispatch <= 0;
	}
	
	public boolean packetExpirationCheck(long initTimestamp)
	{
		return initTimestamp - timeOfArrival < (long)byteData[12]*1000;
	}
	
}
