/*
 * Created on 24-Apr-2006 Created by Paul Gardner Copyright (C) 2006 Aelitis,
 * All Rights Reserved.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros 8 Allee Lenotre, La Grille Royale,
 * 78600 Le Mesnil le Roi, France.
 * 
 */

package com.aelitis.azureus.vivaldi.ver2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TreeSet;

import org.gudy.azureus2.core3.util.Timer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;

import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPosition;
import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPositionManager;
import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPositionProvider;
import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPositionProviderInstance;
import com.aelitis.azureus.core.dht.router.DHTRouter;
import com.aelitis.azureus.core.dht.router.DHTRouterContact;
import com.aelitis.azureus.core.dht.router.DHTRouterFactory;
import com.aelitis.azureus.core.dht.router.DHTRouterFactoryObserver;
import com.aelitis.azureus.core.dht.router.DHTRouterObserver;
import com.aelitis.azureus.vivaldi.ver2.stats.SerializationController;
import com.aelitis.azureus.vivaldi.ver2.stats.V1Serializer;

import edu.harvard.syrah.nc.Coordinate;
import edu.harvard.syrah.nc.VivaldiClient;

public class VivaldiV2PositionProvider implements DHTNetworkPositionProvider,
		DHTRouterFactoryObserver, DHTRouterObserver {
	public static final int NUM_DIMS = 5;
	public static final int TRANSIENT_TIME = 45 * 1000;		// in milliseconds
  public static final long MIN_NC_UPDATE_INTERVAL = 10*1000;
  
	private static final boolean LOGGING_ENABLED = false;

	private static boolean initialised = false;
	private static volatile DHTNetworkPositionProviderInstance provider = null;

	private final VivaldiClient<IDWrapper> vc;
	private final InitialPosition ip;
	
	protected final SortedSet<IDWrapper> transient_ids;
	protected final LinkedList<TransientTuple> transient_list;
	
	protected final SerializationController serializer;
	protected final SortedSet<IDWrapper> router_entries;
  protected DHTRouter router = null;
  protected long last_nc_update = 0;
  
  private boolean	started_up = false;
  
	public static synchronized void initialise() {
		if (!initialised) {

			initialised = true;

			provider = DHTNetworkPositionManager
					.registerProvider(new VivaldiV2PositionProvider());

			doLog("Vivaldi V2 position provider created");
		}
	}

	protected VivaldiV2PositionProvider() {
		DHTRouterFactory.addObserver(this);

		vc = new VivaldiClient<IDWrapper>(NUM_DIMS);
		ip = new InitialPosition();
		
		transient_ids = new TreeSet<IDWrapper>();
		transient_list = new LinkedList<TransientTuple>();
		
		serializer = new SerializationController();
		router_entries = new TreeSet<IDWrapper>();
    

    Timer timer = new Timer("VivaldiV2PositionProvider:ping");
    
    timer.addPeriodicEvent(
      MIN_NC_UPDATE_INTERVAL,
      new TimerEventPerformer()
      {
        public void
        perform(
          TimerEvent  event )
        {
          ping();
        
        }
      });

	}

  protected void resetPingClock (long curr_time) {
    last_nc_update = curr_time;
  }
  
  protected void ping () {
    long curr_time = System.currentTimeMillis();
    if (curr_time > last_nc_update + MIN_NC_UPDATE_INTERVAL) {
	//System.out.println ("starting v2position ping");
      if (router != null) {
        IDWrapper id = vc.getNeighborToPing(curr_time);
        if (id != null) {
          byte[] raw_id = id.getRawId();
          router.requestPing(raw_id);
          //System.out.println ("pinging "+id);
        } else {
	    //System.out.println ("vc neighbor to ping is null");
        }
      } else {
	  //System.out.println ("router is null");  
      }
    } else {
	//System.out.println ("supressing v2position ping");
    }
  }
    
	public byte getPositionType() {
		return (DHTNetworkPosition.POSITION_TYPE_VIVALDI_V2 );
	}

	public DHTNetworkPosition create(byte[] ID, boolean is_local) {
		if (is_local) {
			doLog("Returning position for local peer");
			
			return new LocalPosition(this);
		}

		purgeTransient(System.currentTimeMillis());

		// return the shared initial position of a remote peer
		return ip;
	}

	public DHTNetworkPosition deserialisePosition(DataInputStream is)
			throws IOException {
		return new RemotePosition(is);
	}

	public void
	serialiseStats(
		DataOutputStream	os )
	
		throws IOException
	{
		if (!serializer.contains(V1Serializer.VER_01)) {
			serializer.addSerializer(V1Serializer.getInstance());
		}
		serializer.toSerialized(V1Serializer.VER_01, os, vc);
	}
	
	public void routerCreated(DHTRouter _router) {
		doLog("Vivaldi notified of created router");
		
		_router.addObserver(this);
    router = _router;
	}

	public synchronized void added(DHTRouterContact contact) {
		purgeTransient(System.currentTimeMillis());
    
		// add to set of router entries
		IDWrapper id = new IDWrapper(contact.getID());
		if (!router_entries.contains(id)) {
			router_entries.add(id);
			if ( LOGGING_ENABLED ){
				doLog("added router entry " + id + " " + getStats());
			}
		}
		
		if (transient_ids.remove(id)) {
			// was a transient entry, get its latest position and sample
			TransientTuple tt = null;
			for (Iterator<TransientTuple> i = transient_list.iterator(); i.hasNext(); ) {
				tt = i.next();
				if (tt.id.equals(id)) {
					i.remove();
					break;
				}
			}
			
			// add state to the vivaldi client
			long curr_time = System.currentTimeMillis();
			// add the time this guy has been sitting as a TransientTuple to its age
			long tt_age = curr_time - tt.create_time + tt.last_pos.getAge();
			if (vc.processSample(id, tt.last_pos.getCoords(), tt.last_pos.getError(), tt.last_rtt, tt_age,
					curr_time, true)) {
			  resetPingClock(curr_time);
      }

			if ( LOGGING_ENABLED ){
				doLog(id + "added to router, promoting transient to host " + getStats());
			}
		}
		else if (vc.getHosts().contains(id)) {
			if ( LOGGING_ENABLED ){
				doLog(id + " added to router, but was already found as a host");
			}
		}
		else {
			if ( LOGGING_ENABLED ){
				doLog(id + " added to router, but was not found as a host or transient");
			}
		}
	}

	public synchronized void removed(DHTRouterContact contact) {
		purgeTransient(System.currentTimeMillis());

		// remove from router entries
		IDWrapper id = new IDWrapper(contact.getID());
		if (router_entries.remove(id)) {
			if ( LOGGING_ENABLED ){
				doLog("removed router entry " + id + " " + getStats());
			}
		}
		
		if (vc.removeHost(id)) {
			if ( LOGGING_ENABLED ){
				doLog(id + " removed from router, removed as host " + getStats());
			}
		}
		else if (transient_ids.remove(id)) {
			// contact identifier is present in list, so find and remove it
			for (Iterator<TransientTuple> i = transient_list.iterator(); i.hasNext(); ) {
				TransientTuple tt = i.next();
				if (tt.id.equals(id)) {
					i.remove();
					break;
				}
			}
	
			if ( LOGGING_ENABLED ){
				doLog(id + " removed from router, removed as transient " + getStats());
			}
		}
		else {
			if ( LOGGING_ENABLED ){
				doLog(id + " removed from router, but was not found as a host or transient");
			}
		}
	}

	public void locationChanged(DHTRouterContact contact) {
		purgeTransient(System.currentTimeMillis());
		
		// only remove from VivaldiClient when removed from routing table
	}

	public void nowAlive(DHTRouterContact contact) {
		purgeTransient(System.currentTimeMillis());
		
		// only add to VivaldiClient when coordinates are updated
	}

	public void nowFailing(DHTRouterContact contact) {
		purgeTransient(System.currentTimeMillis());
		
		// only remove from VivaldiClient when removed from routing table
	}

	public void destroyed(DHTRouter router) {
		doLog("Vivaldi notified of destroyed router");
		
		router.removeObserver(this);
		
		vc.reset();
		router_entries.clear();

		transient_ids.clear();
		transient_list.clear();
	}
	
	public DHTNetworkPosition
	getLocalPosition()
	{
		if ( started_up ){
			
			return( new StableLocalPosition( this ));
		}
		
			// we only have a stable local position if we've been started up - i.e things are running
		
		return( null );
	}
	
	/*
	 * Pass-thru methods.
	 */
	
	protected Coordinate getCoords() {
		return vc.getSystemCoords();
	}

	protected Coordinate getStableCoords() {
		return vc.getApplicationCoords();
	}
	
  protected float getError() {
    return (float) vc.getSystemError();
  }

  protected long getAge() {
    long age = vc.getAge(System.currentTimeMillis());
    //System.out.println("age "+age);
    return age;
  }
  
	protected InitialPosition getInitialPosition() {
		return ip;
	}
	
	protected VivaldiClient<IDWrapper> getVivaldiClient() {
		return vc;
	}
	
	/*
	 * Methods for maintaining the potential adds to the VivaldiClient.
	 */
	
	protected synchronized void update(LocalPosition local_pos, IDWrapper id, SyrahPosition sp, float sample_rtt) {
		if (sp == local_pos) {
			doLog("update invoked on LocalPosition with itself, ID = " + id);
			return;
		}
		else if (sp == ip) {
			doLog("update invoked with the InitialPosition singleton, ID = " + id);
			return;
		}
		
		long curr_time = System.currentTimeMillis();
		if (vc.getHosts().contains(id)) {
			// already maintain state for this peer, so process sample now
			if (vc.processSample(id, sp.getCoords(), sp.getError(), sample_rtt, sp.getAge(), curr_time, false)) {
        resetPingClock(curr_time);
      }
			
			doLog("update called on host, ID = " + id);
			return;
		}
		else if (router_entries.contains(id)) {
			if (transient_ids.remove(id)) {
				// contact identifier is present in list, so find and remove it
				for (Iterator<TransientTuple> i = transient_list.iterator(); i.hasNext(); ) {
					TransientTuple tt = i.next();
					if (tt.id.equals(id)) {
						i.remove();
						break;
					}
				}
			}
			
			// peer is already in the router, so add and process sample now
			if (vc.processSample(id, sp.getCoords(), sp.getError(), sample_rtt, sp.getAge(), curr_time, true)) {
			  resetPingClock (curr_time);
      }
			
			doLog("update called on host already in router, ID = " + id);
			return;
		}
		
		final long new_remove_time = curr_time + TRANSIENT_TIME;
		if (!transient_ids.contains(id)) {
			// identifier is not in list yet, so add it and return
			transient_ids.add(id);
			transient_list.addLast(new TransientTuple(new_remove_time, id, sp, sample_rtt, curr_time));
			doLog("added transient " + id + " " + getStats());
			return;
		}
		
		doLog("updating transient " + id + " " + getStats());
		
		// find the entry belonging to the identifier in the list
		TransientTuple tt = null;
		for (Iterator<TransientTuple> i = transient_list.iterator(); i.hasNext(); ) {
			tt = i.next();
			if (tt.id.equals(id)) {
				// once found, remove it
				i.remove();
				break;
			}
		}
		
		// update the entry and add it to the end
		tt.remove_time = new_remove_time;
		tt.last_pos = sp;
		tt.last_rtt = sample_rtt;
		transient_list.addLast(tt);
	}

	protected synchronized void purgeTransient(long curr_time) {
		for (Iterator<TransientTuple> i = transient_list.iterator(); i.hasNext(); ) {
			TransientTuple tt = i.next();
			if (tt.remove_time > curr_time) {
				// entry expires in the future, so end now
				return;
			}
			
			i.remove();
			transient_ids.remove(tt.id);
			doLog("removed transient " + tt.id + " " + getStats());
		}
	}
	
	protected String getStats() {
		return "[h:" + vc.getHosts().size() + ", t:" + transient_list.size() + " re:" + router_entries.size() + "]";
	}
	
	protected static void doLog(String str) {
    //System.out.println (str);
		if (LOGGING_ENABLED && (provider != null)) {
			provider.log(str);
		}
	}
	
	public void
	startUp(
		DataInputStream		is ) //throws IOException
	{
		started_up	= true;
		
		try{
			vc.startUp (is);
			
		}catch( IOException e ){
			
			doLog("startUp failed:" + e.toString());
		}
	}
	
	public void
	shutDown(
		DataOutputStream	os )// throws IOException
	{	
		try{
			vc.shutDown (os);
			
		}catch( IOException e ){
			
			doLog("shutDown failed:" + e.toString());
		}
	}
}
