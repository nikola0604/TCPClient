package com.plusplusnt.tcpclient.model;

import com.plusplusnt.tcpclient.utilities.Dispatcher;
import com.plusplusnt.tcpclient.utilities.MatildaSocket;
import com.plusplusnt.tcpclient.utilities.ShutdownHook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

@Component
public class TCPClient implements CommandLineRunner
{
	private TimerTask dispatcher;
	private MatildaSocket matildaSocket;
	private ShutdownHook shutdownHook;
	private Timer dispatcherTimer = null;
	/*
	Automatsko ubrizgavanje bean-ova klasa Dispatcher, MatildaSocket i ShutdownHook
	 */
	@Autowired
	public TCPClient(TimerTask dispatcher, MatildaSocket matildaSocket, ShutdownHook shutdownHook)
	{
		this.dispatcher = dispatcher;
		this.matildaSocket = matildaSocket;
		this.shutdownHook = shutdownHook;
	}
	
	private void tcpClientLoop()
	{
		/*
		ShutdownHook thread se pokrece pri gasenju aplikacije i cuva zive pakete koji ce biti ucitani po
		narednom pokretanju aplikacije
		*/
		shutdownHook.registerShutdownHook((Dispatcher)dispatcher);
		
		try
		{
			/*
			Timer thread (daemon u ovom slucaju) se izvrasava u pozadini i na svakih 10ms pokrece metod run()
			metod definisan u klasi Dispatcher koji je nasledjen od apstraktne klase TimerTask. Potencijalni
			problem je sto Timer zakazuje naredno pokretanje metode relativno u odnosu na okoncanje prethodnog
			izvrsavanja sto moze dovesti do pomeraja kako vreme prolazi. Na osnovu testiranja nisam dosao do
			znacajnijeg pomeraja u vremenima pokretanja metode i zbog toga ovo ostavljam ovako.
			--------------------------------
			Poziva se metod processSerialized() zaduzen za procesiranje i eventualno slanje paketa
			sacuvanih u prethodnoj sesiji
			*/
			dispatcherTimer = new Timer(true);
			((Dispatcher)dispatcher).setSocketOutputStream(matildaSocket.getOutputStream());
			processSerialized();
			dispatcherTimer.schedule(dispatcher, 10,10);
			
			/*
			klijentska petlja koja osluskuje server i zaduzena je za pozivanje metoda receive() koji je zaduzen
			za prijem i procesiranje pristiglih paketa
			 */
			while (true)
			{
				byte[] byteData = receive(matildaSocket.getInputStream());
				
				if(byteData != null)
				{
					if (byteData[0] == 1)
						System.out.println((char) 27 + "[36mSERVER: dummy packet, ID: " + byteData[8] + "|" + byteData[9] + "|" +
								byteData[10] + "|" + byteData[11] + ", time of arrival: " + LocalDateTime.now() +
								", Delay: " + byteData[12]);
					else if (byteData[0] == 2)
					{
						System.out.println((char) 27 + "[33mServer response = cancel");
						System.exit(0);
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			dispatcherTimer.cancel();
			dispatcherTimer.purge();
		}
	}
	
	private byte[] receive(DataInputStream socketInputStream)
	{
		try
		{
			byte[] inputData = new byte[16];
			int bytesRead;
			/*
			Citamo paket i unosimo ga u kolekciju (LinkedList) paketa koja je atribut klase Dispatcher
			Svi pristigli 16-bajtni paketi se enkapsuliraju u objekat klase PacketModel
			 */
			bytesRead = socketInputStream.read(inputData);
			if(bytesRead != 16 && bytesRead != 12)
			{
				System.err.println((char)27 + "[31m" + bytesRead +
						" bytes read, 12 or 16 expected! (Connection severed?)");
				System.exit(1);
			}
			
			((Dispatcher)dispatcher).addToPacketModelLinkedList(new PacketModel(inputData, System.currentTimeMillis()));
			
			return inputData;
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
			System.exit(1);
		}
		
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private void processSerialized()
	{
//		File serFile = new File("./src/main/resources/packets.ser");
//		if (!serFile.exists())
//			System.err.println("Serialization file doesn't exist!");
//		else
//			System.out.println((char)27 + "[33mSerialization file located, deserialization initiated");
		
		try
		{
			File serFile = new File("./src/main/resources/packets.ser");
			if (!serFile.exists())
					System.err.println((char)27 + "[31mSerialization file doesn't exist! (First run?)");
			else
				System.out.println((char)27 + "[33mSerialization file located, deserialization initiated");

			if(serFile.canRead())
			{
				FileInputStream serFileInputStream = new FileInputStream(serFile);
				ObjectInputStream objectInputStream = new ObjectInputStream(serFileInputStream);
				
				/*
				Paketi se deserijalizuju i unose u kolekciju (LinkedList) paketa koja je atribut klase Dispatcher
				------------------------------
				Poziva se metod dispatchDeserialized() klase Dispatcher zaduzen za proveru zivosti paketa i njihovo
				eventualno vracanje serveru
			 	*/
				((Dispatcher)dispatcher).setPacketModelLinkedList((LinkedList<PacketModel>)(objectInputStream.readObject()));
				((Dispatcher)dispatcher).dispatchDeserialized();
				
				
				/*
				Brise se fajl u kom su cuvani paketi
			 	*/
				if(serFile.delete())
					System.out.println((char)27 + "[33mPackets deserialized and serFile deleted");
				else
					System.err.println((char)27 + "[31mUnable to delete serFile!");
				System.out.println("------------------------------\n");
				
				serFileInputStream.close();
				objectInputStream.close();
			}
		}
		catch(IOException | ClassNotFoundException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void run(String... args) throws Exception
	{
		tcpClientLoop();
	}
	
}