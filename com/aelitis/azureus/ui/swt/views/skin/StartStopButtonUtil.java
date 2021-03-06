/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.ui.swt.views.skin;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.views.TorrentListView;
import com.aelitis.azureus.ui.swt.views.list.ListRow;

/**
 * @author TuxPaper
 * @created Sep 30, 2006
 *
 */
public class StartStopButtonUtil
{
	public static void updateStopButton(TorrentListView view,
			SWTSkinButtonUtility button) {
		if (button == null) {
			return;
		}

		try {
			ListRow[] selectedRows = view.getSelectedRows();

			if (selectedRows.length == 0) {
				return;
			}
			boolean bResume = true;
			for (int i = 0; i < selectedRows.length; i++) {
				ListRow row = selectedRows[i];
				DownloadManager dm = (DownloadManager) row.getDataSource(true);
				if (dm != null) {
					int state = dm.getState();
					boolean bNotRunning = state == DownloadManager.STATE_QUEUED
							|| state == DownloadManager.STATE_STOPPED
							|| state == DownloadManager.STATE_STOPPING
							|| state == DownloadManager.STATE_ERROR;
					if (!bNotRunning) {
						bResume = false;
						break;
					}
				}
			}

			if (bResume) {
				button.setTextID("MainWindow.v3.button.resume");
			} else {
				button.setTextID("MainWindow.v3.button.pause");
			}
		} catch (Exception e) {
			Debug.out(e);
		}
	}

}
