/*
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Created on 3 juil. 2003
 *
 */
package org.gudy.azureus2.core3.disk;

import java.io.File;
import java.io.IOException;

import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

/**
 * @author Olivier
 * 
 */
public interface 
DiskManagerFileInfo 
{
	public static final int READ = 1;
	public static final int WRITE = 2;

	public static final int	ST_LINEAR	= 1;
	public static final int	ST_COMPACT	= 2;
	
		// set methods
		
	public void setPriority(boolean b);
	
	public void setSkipped(boolean b);
	 
	
	public boolean
	setLink(
		File	link_destination );
	
		// gets the current link, null if none
	
	public File
	getLink();
	
		/**
		 * Download must be stopped before calling this!
		 * @param type	one of ST_LINEAR or ST_COMPACT
		 */
	
	public boolean
	setStorageType(
		int		type );
	
	public int
	getStorageType();
	
	 	// get methods
	 	
	public int getAccessMode();
	
	public long getDownloaded();
	
	public String getExtension();
		
	public int getFirstPieceNumber();
  
	public int getLastPieceNumber();
	
	public long getLength();
		
	public int getNbPieces();
			
	public boolean isPriority();
	
	public boolean isSkipped();
	
	public int	getIndex();
	
	public DownloadManager	getDownloadManager();
	
	public DiskManager getDiskManager();
	
	public File getFile( boolean follow_link );
	
	public TOTorrentFile
	getTorrentFile();
	
	public DirectByteBuffer
	read(
		long	offset,
		int		length )
	
		throws IOException;
	
	public void
	flushCache()
	
		throws	Exception;
	
	public void
	close();
	
	public void
	addListener(
		DiskManagerFileInfoListener	listener );
	
	public void
	removeListener(
		DiskManagerFileInfoListener	listener );
	
}