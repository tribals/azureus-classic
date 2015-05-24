package edu.harvard.syrah.nc;

/*
 * NCLib - a network coordinate library 
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
 */

/**
 * An observer to be notified when the application coordinates change.
 *
 * @author Michael Parker, Jonathan Ledlie
 */
public interface ApplicationObserver {
	/**
	 * This method is invoked when the application-level coordinates are
	 * updated.
	 *
	 * @param new_coords the new application-level coordinates
	 */
	public void coordinatesUpdated(Coordinate new_coords);
}
