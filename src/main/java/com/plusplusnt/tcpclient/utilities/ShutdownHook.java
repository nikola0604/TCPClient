package com.plusplusnt.tcpclient.utilities;

import com.plusplusnt.tcpclient.model.PacketModel;
import com.plusplusnt.tcpclient.model.TCPClient;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/*
Bean ciji je metod registerShutdownHook zaduzen za registrovanje
ShutdownHook thread-a u okruzenju u kom se aplikacija izvrsava
 */
@Component
public class ShutdownHook
{
	public void registerShutdownHook(Dispatcher dispatcher)
	{
		Runtime.getRuntime().addShutdownHook(new Thread(() ->
		{
			System.out.println((char)27 + "[33m\n\nShutdown Hook is running!");
			System.out.println("------------------------------");

			try
			{
				/*
				Kreiranje fajla i inicijalizacija ispisnih
				tokova  kojim ce se serijalizovati paketi
				 */
				File serFile = new File("./src/main/resources/packets.ser");
				if(!serFile.exists())
					if(!serFile.createNewFile())
						System.err.println((char)27 + "[31mSerialization file not created!");
					else
						System.out.println((char)27 + "[33mSerialization file created!");
				else
					System.err.println((char)27 + "[31mSerialization file already exists, data will be truncated");

				FileOutputStream serFileOutputStream = new FileOutputStream(serFile);
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(serFileOutputStream);

				/*
				Serijalizacija kolekcije (LinkedList) koja sadrzi
				trenutno zive pakete uz ispis ID-eva sacuvanih paketa
				 */
				objectOutputStream.writeObject(dispatcher.getPacketModelLinkedList());
				
				System.out.println("------------------------------");
				for(PacketModel packetModel : dispatcher.getPacketModelLinkedList())
					System.out.println((char)27 + "[34mPacket saved: " + packetModel.getPacketIDString());
				
				serFileOutputStream.close();
				objectOutputStream.close();
				System.out.flush();
			}
			
			catch (IOException ioe)
			{
				ioe.printStackTrace();
			}
		}));
	}
}
