package com.plusplusnt.tcpclient.utilities;

import com.plusplusnt.tcpclient.model.PacketModel;
import org.springframework.stereotype.Component;

import java.io.DataOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TimerTask;

/*
Dispatcher klasa (tretirana kao bean) zaduzena za cuvanje zivih paketa u kolekciji
implementira i definise metod run() nasledjen od apstraktne klase TimerTask
koji se u daemon thread-u izvrsava svakih 10ms
 */
@Component
public class Dispatcher extends TimerTask
{
	/*
	Svi primljeni paketi (objekti u kojima su enkapsulirani) se cuvaju u nizu.
	Za ovu strukturu podataka sam se odlucio jer metod Dispatcher.run() prolazi
	kroz sve objekte na svakih 10ms gde je od pomoci sekvencijalno pozicioniranje
	elemenata niza u memoriji. Dodavanje na kraj je asimptotske slozenosti O(1),
	svaka promena nad atributima objekata se izvrsava u toku prolaska tako da nemamo
	search nad strukturom. Brisanje elemenata nije jeftino ali se ne desava da lista
	ima vise od 10 elemenata tako da je ova cena prihvatljiva.
	 */
	private ArrayList<PacketModel> packetModelArrayList = new ArrayList<>(20);
	private Iterator<PacketModel> packetModelIterator;
	private PacketModel packetModel;
	private DataOutputStream socketOutputStream;
	
	public void setOs(DataOutputStream socketOutputStream)
	{
		this.socketOutputStream = socketOutputStream;
	}
	
	public void setPacketModelArrayList(ArrayList<PacketModel> packetModelArrayList)
	{
		this.packetModelArrayList = packetModelArrayList;
	}
	
	ArrayList<PacketModel> getPacketModelArrayList()
	{
		return packetModelArrayList;
	}
	
	public synchronized void addToPackageModelArrayList(PacketModel packetModel)
	{
		this.packetModelArrayList.add(packetModel);
	}
	
	/*
	Metod zaduzen za slanje paketa serveru
	 */
	private void dispatch(PacketModel packetModel)
	{
		try
		{
			socketOutputStream.write(packetModel.getByteData());
			socketOutputStream.flush();
			System.out.println((char)27 + "[32mCLIENT: dummy packet, ID: " +  packetModel.getPackageIDString() +
					", time of dispatch: " + LocalDateTime.now());
		}
		catch (Exception exception)
		{
			exception.printStackTrace();
		}
	}
	
	/*
	Metod zaduzen za proveru zivost deserijalizovaniih paketa
	i njihovo eventualno ignorisanje, odnosno vracanje serveru
	Pri kreiranju objekta za svaki paket cuva se i timestamp
	kreiranja objekta koji se poredi sa trenutnim timestamp-om
	i na osnovu toga se odredjuje da li je paket istekao ili ne.
	Uzevsi u obzir da su vremena u pitanju na klijentskoj strani
	i da su velicine reda sekunda moze se ignorisati diskrepanca
	kod nekih OS-ova kad je u pitanju Unix epoha
	 */
	public void dispatchDeserialized()
	{
		long initTimestamp = System.currentTimeMillis();
		packetModelIterator = packetModelArrayList.iterator();
		
		while(packetModelIterator.hasNext())
		{
			packetModel = packetModelIterator.next();
			
			if(packetModel.packetExpirationCheck(initTimestamp))
			{
				dispatch(packetModel);
				packetModelIterator.remove();
			}
			else
			{
				System.out.println((char)27 + "[34mPacket: " + packetModel.getPackageIDString() + " expired");
				packetModelIterator.remove();
			}
		}
	}
	
	/*
	Metod koji daemon thread poziva svakih 10ms. Sekvencijalno prolazi
	kroz ArrayList koji sadrzi zive pakete. Umanjuje preostalo vreme zivota
	paketa za 10ms i ukoliko je milisecondsUntilDispatch == 0 poziva metod
	dispatch() kojim se paket vraca serveru.
	 */
	public synchronized void run()
	{
		packetModelIterator = packetModelArrayList.iterator();
		
		while(packetModelIterator.hasNext())
		{
			packetModel = packetModelIterator.next();
			packetModel.decrementSecondsUntilDispatch();

			if(packetModel.checkSecondsUntilDispatch())
			{
				dispatch(packetModel);
				packetModelIterator.remove();
			}
		}
	}
}
