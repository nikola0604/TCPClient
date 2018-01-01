package com.plusplusnt.tcpclient.utilities;

import com.plusplusnt.tcpclient.model.PacketModel;
import org.springframework.stereotype.Component;

import java.io.DataOutputStream;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.LinkedList;
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
	Svi primljeni paketi (objekti u kojima su enkapsulirani) se cuvaju u listi.
	Za ovu strukturu podataka sam se odlucio jer metod Dispatcher.run() prolazi
	kroz sve objekte na svakih 10ms tako da nam mana povezanih listi gde je trazenje
	elemenata linearne slozenosti nije na smetnji jer nemamo pretragu za konkretnim objektom.
	Dodavanje i brisanje iz liste je je asimptotske slozenosti O(1). Uzevsi u obzir da na svakih
	10ms prolazimo kroz sve elemente dodatno mozemo ubrzati rad algoritma tako sto cemo cuvati pakete kao
	primitivne tipove u nizu cime dobijamo da su svi elementi sekvencijalno upisani u memoriji.
	Ovakav pristup bi dosta zakomplikovao kod i njegovu citljivost. Na osnovu testiranja
	sam dosao do zakljucka da je cena koriscenja liste koja sadrzi reference na objekte koji su
	"razbacani" u memoriji prihvatljiva jer dobijamo jednostavniji i citljiviji kod kao i modularniji
	dizajn.
	 */
	private LinkedList<PacketModel> packetModelLinkedList = new LinkedList<>();
	private Iterator<PacketModel> packetModelIterator;
	private PacketModel packetModel;
	private DataOutputStream socketOutputStream;
	
	public void setSocketOutputStream(DataOutputStream socketOutputStream)
	{
		this.socketOutputStream = socketOutputStream;
	}
	
	public void setPacketModelLinkedList(LinkedList<PacketModel> packetModelArrayList)
	{
		this.packetModelLinkedList = packetModelArrayList;
	}
	
	LinkedList<PacketModel> getPacketModelLinkedList()
	{
		return packetModelLinkedList;
	}
	
	public synchronized void addToPacketModelLinkedList(PacketModel packetModel)
	{
		this.packetModelLinkedList.add(packetModel);
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
			System.out.println((char)27 + "[32mCLIENT: dummy packet, ID: " +  packetModel.getPacketIDString() +
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
		packetModelIterator = packetModelLinkedList.iterator();
		
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
				System.out.println((char)27 + "[34mPacket: " + packetModel.getPacketIDString() + " expired");
				packetModelIterator.remove();
			}
		}
	}
	
	/*
	Metod koji daemon thread poziva svakih 10ms. Sekvencijalno prolazi
	kroz LinkedList koji sadrzi zive pakete. Umanjuje preostalo vreme zivota
	paketa za 10ms i ukoliko je millisecondsUntilDispatch == 0 poziva metod
	dispatch() kojim se paket vraca serveru.
	 */
	public synchronized void run()
	{
		packetModelIterator = packetModelLinkedList.iterator();
		
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
