package com.plusplusnt.tcpclient.utilities;

import org.springframework.context.annotation.Configuration;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/*
Pomocni bean pri cijem se instanciranju
kreira socket koji se povezuje sa serverom
 */
@Configuration
public class MatildaSocket
{
	private Socket socket;
	private DataInputStream socketInputStream;
	private DataOutputStream socketOutputStream;
	
	public MatildaSocket()
	{
		try
		{
			this.socket = new Socket("matilda.plusplus.rs", 4000);
			this.socket.setSoTimeout(10000);
			this.socket.setTcpNoDelay(true);
			this.socketInputStream = new DataInputStream(socket.getInputStream());
			this.socketOutputStream = new DataOutputStream(socket.getOutputStream());
			
			System.out.println((char)27 + "[33mClient socket live on local port: " + socket.getLocalPort());
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public DataInputStream getInputStream()
	{
		return this.socketInputStream;
	}
	
	public DataOutputStream getOutputStream()
	{
		return this.socketOutputStream;
	}
	
}
