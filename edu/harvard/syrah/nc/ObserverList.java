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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A list of observers for the application-level coordinate.
 *
 * @author Michael Parker, Jonathan Ledlie
 */
public class ObserverList {
	final protected List<ApplicationObserver> obs_list;

	protected ObserverList() {
		obs_list = new LinkedList<ApplicationObserver>();
	}

	/**
	 * Returns the number of observers in the list.
	 *
	 * @return the size of the observer list
	 */
	public int size() {
		return obs_list.size();
	}

	/**
	 * Returns whether the observer list is empty, meaning its size equals
	 * <code>0</code>.
	 *
	 * @return <code>true</code> if the observer list is empty,
	 * <code>false</code> otherwise
	 */
	public boolean isEmpty() {
		return obs_list.isEmpty();
	}

	/**
	 * Returns whether the list contains the given observer. If the parameter
	 * <code>obj</code> is <code>null</code>, this method returns
	 * <code>false</code>.
	 *
	 * @param obj
	 * the observer to query for membership in the list
	 * @return <code>true</code> if the list contains the observer,
	 * <code>false</code> otherwise
	 */
	public boolean contains(ApplicationObserver obj) {
		return (obj != null) ? obs_list.contains(obj) : false;
	}

	/**
	 * Returns an iterator over the list of observers.
	 *
	 * @return an iterator over the observer list
	 */
	public Iterator<ApplicationObserver> iterator() {
		return obs_list.iterator();
	}

	/**
	 * Adds the given observer to the list of observers. If the parameter
	 * <code>obj</code> is <code>null</code>, this method returns
	 * <code>false</code> and the underlying list remains unchanged.
	 *
	 * @param obj
	 * the observer to add to the list
	 * @return <code>true</code> if the observer is added to the list,
	 * <code>false</code> otherwise
	 */
	public boolean add(ApplicationObserver obj) {
		return (obj != null) ? obs_list.add(obj) : false;
	}

	/**
	 * Removes the given observer from the list of observers. If the parameter
	 * <code>obj</code> is <code>null</code> or the list does not contain
	 * the observer, this method returns <code>false</code> and the underlying
	 * list remains unchanged.
	 *
	 * @param obj
	 * the observer to remove from the list
	 * @return <code>true</code> if the observer is removed from the list,
	 * <code>false</code> otherwise
	 */
	public boolean remove(ApplicationObserver obj) {
		return (obj != null) ? obs_list.remove(obj) : true;
	}

	/**
	 * Removes all the observers from the list.
	 */
	public void clear() {
		obs_list.clear();
	}
}
