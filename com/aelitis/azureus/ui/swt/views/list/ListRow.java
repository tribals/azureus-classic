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
package com.aelitis.azureus.ui.swt.views.list;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.tracker.host.TRHostTorrent;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.pluginsimpl.local.disk.DiskManagerFileInfoImpl;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.peers.PeerManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.tracker.TrackerTorrentImpl;
import org.gudy.azureus2.ui.swt.views.table.TableCellCore;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;
import org.gudy.azureus2.ui.swt.views.table.TableRowCore;
import org.gudy.azureus2.ui.swt.views.table.impl.TableCellImpl;

import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;

import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.ui.UIRuntimeException;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;

/**
 * @author TuxPaper
 * @created Jun 12, 2006
 *
 */
public class ListRow implements TableRowCore
{
	public static int ROW_HEIGHT = 38;

	public static int MARGIN_HEIGHT = 2;

	public static int MARGIN_WIDTH = 3;

	private SWTSkinProperties skinProperties;

	private Object coreDataSource;

	private Object pluginDataSource;

	private Map mapTableCells;

	private boolean bDisposed;

	private boolean bSelected;

	private ListView view;

	private final Composite parent;

	private Color fg;

	private Color bg;

	private boolean bRowVisuallyChangedSinceRefresh;

	/**
	 * @param position 
	 * 
	 */
	public ListRow(final ListView view, Composite parent, Object datasource) {
		this.parent = parent;
		coreDataSource = datasource;
		this.view = view;

		pluginDataSource = null;
		bDisposed = false;
		bSelected = false;
		mapTableCells = new HashMap();

		skinProperties = view.getSkinProperties();

		TableColumnCore[] columns = view.getAllColumns();
		int iStartPos = MARGIN_WIDTH;
		// this is -1 :(
		//int height = rowComposite.getSize().y;
		for (int i = 0; i < columns.length; i++) {
			TableColumnCore column = columns[i];

			boolean bVisible = column.getPosition() >= 0;
			Rectangle bounds = bVisible ? new Rectangle(iStartPos, MARGIN_HEIGHT,
					column.getWidth(), ROW_HEIGHT - (MARGIN_HEIGHT * 2)) : null;

			ListCell listCell;
			int iColumnPos = column.getPosition();
			if (column.getType() == TableColumn.TYPE_GRAPHIC) {
				listCell = new ListCellGraphic(this, iColumnPos, column.getSWTAlign(),
						bounds);
			} else {
				listCell = new ListCell(this, iColumnPos, column.getSWTAlign(), bounds);
			}

			if (bVisible) {
				iStartPos += bounds.width + (MARGIN_WIDTH * 2);
			}

			TableCellCore cell = new TableCellImpl(this, column, i, listCell);
			listCell.setTableCell(cell);
			cell.setUpToDate(false);

			mapTableCells.put(column.getName(), cell);
			//cell.refresh();
		}
	}

	/**
	 * @param bounds 
	 * 
	 */
	protected void resizeRow(Rectangle bounds) {
		if (bDisposed) {
			return;
		}

		long lTimeStart = System.currentTimeMillis();
		Iterator iter = mapTableCells.values().iterator();
		while (iter.hasNext()) {
			TableCellCore item = (TableCellCore) iter.next();
			ListCell cell = (ListCell) item.getBufferedTableItem();
			Rectangle cellBounds = cell.getBounds();
			if (cellBounds != null && cell.getPosition() >= 0) {
				cellBounds.height = bounds.height - (MARGIN_HEIGHT * 2);
				cell.setBounds(cellBounds);
			}
		}
		long diff = System.currentTimeMillis() - lTimeStart;
		if (diff >= 50) {
			System.out.println("resizeRow: " + diff + "ms");
		}
	}

	private void checkCellForSetting() {
		if (bDisposed) {
			throw new UIRuntimeException("ListRow is disposed.");
		}
	}

	private void setBackgroundColor(int iPosition) {
		checkCellForSetting();

		boolean bOdd = ((iPosition + 1) % 2) == 0;
		if (bSelected) {
			String sColorID = (bOdd) ? "color.row.odd.selected.bg"
					: "color.row.even.selected.bg";
			Color color = skinProperties.getColor(sColorID);
			if (color != null) {
				bg = color;
			} else {
				bg = parent.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
			}

			sColorID = (bOdd) ? "color.row.odd.selected.fg"
					: "color.row.even.selected.fg";
			Color cText = skinProperties.getColor(sColorID);
			if (cText == null) {
				sColorID = (bOdd) ? "color.row.odd.fg" : "color.row.even.fg";
				cText = skinProperties.getColor(sColorID);

				if (cText == null) {
					cText = parent.getDisplay().getSystemColor(
							SWT.COLOR_LIST_SELECTION_TEXT);
				}
			}

			fg = cText;
			bRowVisuallyChangedSinceRefresh = true;
			
			invalidate();
			if (isVisible()) {
				repaint();
			}
		} else {
			boolean bChanged = false;
			if (skinProperties != null) {
				String sColorID = (bOdd) ? "color.row.odd.bg" : "color.row.even.bg";
				Color color = skinProperties.getColor(sColorID);
				if (color != null && !colorsEqual(color, bg)) {
					bChanged = true;
					bg = color;
				}
			} else {
				Color oldColor = bg;
				bg = parent.getBackground();
				if (!colorsEqual(oldColor, bg)) {
					bChanged = true;
				}
			}

			String sColorID = (bOdd) ? "color.row.odd.fg" : "color.row.even.fg";
			Color cText = skinProperties.getColor(sColorID);

			if (cText == null) {
				cText = parent.getDisplay().getSystemColor(
						SWT.COLOR_LIST_SELECTION_TEXT);
			}
			if (!colorsEqual(cText, fg)) {
				bChanged = true;
				fg = cText;
			}

			if (bChanged) {
				bRowVisuallyChangedSinceRefresh = true;
				invalidate();
				if (isVisible()) {
					repaint();
				}
			}
		}
		// 1160681379555: r54c4r.v?N;Invalidate Cell;true from ListRow::setBackgroundColor::316,ListRow::setIndex::468,ListView::notifyIndexChanges::385,ListView$3::run::344,Utils::execSWTThread::590,Utils::execSWTThread::618,ListView::addDataSources::313,ListView::processDataSourceQueue::242,ListView::updateUI::660,UIUpdaterImpl::update::139,UIUpdaterImpl::access$0::126,UIUpdaterImpl$1::runSupport::72,AERunnable::run::38,RunnableLock::run::35,Synchronizer::runAsyncMessages::123,Display::runAsyncMessages::3325,Display::readAndDispatch::2971,SWTThread::<init>::130,SWTThread::createInstance::64,Initializer::<init>::169,Initializer::main::147
	}

	private boolean colorsEqual(Color color1, Color color2) {
		if (color1 == color2) {
			return true;
		}
		if (color1 == null || color2 == null) {
			return false;
		}
		return color1.equals(color2);
	}

	private Color getAlternatingColor() {
		boolean bOdd = ((getIndex() + 1) % 2) == 0;
		if (skinProperties != null) {
			String sColorID = (bOdd) ? "color.row.odd.bg" : "color.row.even.bg";
			Color color = skinProperties.getColor(sColorID);
			if (color != null) {
				return color;
			}
		}
		return parent.getBackground();
	}

	public void delete(boolean bDeleteSWTObject) {
		// XXX bDeleteSWTObject non-usage is intentional

		int iIndex = getIndex();
		bDisposed = true;

		long lTimeStart = System.currentTimeMillis();
		Iterator iter = mapTableCells.values().iterator();
		while (iter.hasNext()) {
			TableCellCore item = (TableCellCore) iter.next();
			try {
				item.dispose();
			} catch (Exception e) {
				Debug.out("Disposing ListRow Column", e);
			}
		}
		/*
		 if (rowComposite != null && !rowComposite.isDisposed()) {
		 // Link next row to what is above this row
		 ListRow rowNext = view.getRow(iIndex + 1);
		 if (rowNext != null) {
		 rowNext.fixupPosition();
		 Composite nextComposite = rowNext.getComposite();
		 if (nextComposite != null && !nextComposite.isDisposed()) {
		 FormData fdNext = (FormData) nextComposite.getLayoutData();
		 if (fdNext != null) {
		 ListRow rowPrevious = view.getRow(iIndex - 1);

		 Composite previousComposite = rowPrevious == null ? null
		 : rowPrevious.getComposite();

		 if (previousComposite != null && !previousComposite.isDisposed()) {
		 fdNext.top = new FormAttachment(previousComposite, 0);
		 } else {
		 fdNext.top = new FormAttachment(0, 0);
		 }
		 nextComposite.setLayoutData(fdNext);
		 }
		 }
		 }

		 rowComposite.dispose();
		 }
		 */
	}

	public void doPaint(GC gc, boolean bVisible) {
		// XXX Copied from TableRowImpl
		if (bDisposed || !bVisible) {
			return;
		}

		long lTimeStart = System.currentTimeMillis();
		Rectangle oldClipping = gc.getClipping();
		try {
			gc.setForeground(getForeground());
			gc.setBackground(getBackground());
			
			Rectangle clientArea = view.getClientArea();
			gc.fillRectangle(0, view.rowGetVisibleYOffset(this), clientArea.width,
					ROW_HEIGHT);
			
			Iterator iter = mapTableCells.values().iterator();
			while (iter.hasNext()) {
				TableCellCore cell = (TableCellCore) iter.next();
				try {
					if (cell.needsPainting()) {
						Rectangle bounds = cell.getBounds();
						if (bounds != null) {
							Rectangle clipping = bounds.intersection(oldClipping);
							gc.setClipping(clipping);

							cell.doPaint(gc);
						}
					}
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		} finally {
			gc.setClipping(oldClipping);
		}
		long diff = System.currentTimeMillis() - lTimeStart;
		if (diff > 30) {
			System.out.println("doPaint: " + view.getTableID() + ": " + diff + "ms");
		}
	}

	// XXX Copied from TableRowImpl!!
	public Object getDataSource(boolean bCoreObject) {
		checkCellForSetting();

		if (bCoreObject) {
			return coreDataSource;
		}

		if (pluginDataSource != null) {
			return pluginDataSource;
		}

		if (coreDataSource instanceof DownloadManager) {
			DownloadManager dm = (DownloadManager) coreDataSource;
			if (dm != null) {
				try {
					pluginDataSource = DownloadManagerImpl.getDownloadStatic(dm);
				} catch (DownloadException e) { /* Ignore */
				}
			}
		}
		if (coreDataSource instanceof PEPeer) {
			PEPeer peer = (PEPeer) coreDataSource;
			if (peer != null) {
				pluginDataSource = PeerManagerImpl.getPeerForPEPeer(peer);
			}
		}

		if (coreDataSource instanceof PEPiece) {
			// XXX There is no Piece object for plugins yet
			PEPiece piece = (PEPiece) coreDataSource;
			if (piece != null) {
				pluginDataSource = null;
			}
		}

		if (coreDataSource instanceof DiskManagerFileInfo) {
			DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) coreDataSource;
			if (fileInfo != null) {
				try {
					pluginDataSource = new DiskManagerFileInfoImpl(
							DownloadManagerImpl.getDownloadStatic(fileInfo.getDownloadManager()),
							fileInfo);
				} catch (DownloadException e) { /* Ignore */
				}
			}
		}

		if (coreDataSource instanceof TRHostTorrent) {
			TRHostTorrent item = (TRHostTorrent) coreDataSource;
			if (item != null) {
				pluginDataSource = new TrackerTorrentImpl(item);
			}
		}

		return pluginDataSource;
	}

	public Color getForeground() {
		if (fg == null) {
			setBackgroundColor(getIndex());
		}
		return fg;
	}

	public int getIndex() {
		return view.indexOf(this);
	}

	public TableCellCore getTableCellCore(String field) {
		if (bDisposed) {
			return null;
		}

		return (TableCellCore) mapTableCells.get(field);
	}

	public void invalidate() {
  	if (bDisposed)
  		return;

		long lTimeStart = System.currentTimeMillis();
    Iterator iter = mapTableCells.values().iterator();
    while (iter.hasNext()) {
      TableCellCore cell = (TableCellCore)iter.next();
      if (cell != null)
        cell.invalidate(true);
    }
		long diff = System.currentTimeMillis() - lTimeStart;
		if (diff >= 10) {
			System.out.println("invalidate: " + diff + "ms");
		}
	}

	public boolean isRowDisposed() {
		return bDisposed;
	}

	public boolean isSelected() {
		return bSelected;
	}

	public boolean isVisible() {
		return view.isRowVisible(this);
	}

	public void locationChanged(int iStartColumn) {
		// TODO Auto-generated method stub

	}

	// XXX Copied from TableRowImp!
	public boolean refresh(boolean bDoGraphics) {
    if (bDisposed)
      return false;
    
    boolean bVisible = isVisible();

    return refresh(bDoGraphics, bVisible);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableRowCore#refresh(boolean, boolean)
	public boolean refresh(boolean bDoGraphics, boolean bVisible) {
    // If this were called from a plugin, we'd have to refresh the sorted column
    // even if we weren't visible
    
    if (!bVisible) {
  		setUpToDate(false);
  		return false;
  	}
    
		boolean changed = false;
		Iterator iter = mapTableCells.values().iterator();
		while (iter.hasNext()) {
			TableCellCore item = (TableCellCore) iter.next();
			changed |= item.refresh(bDoGraphics, bVisible);
		}
		changed |= bRowVisuallyChangedSinceRefresh;
		bRowVisuallyChangedSinceRefresh = false;
		return changed;
	}

	public void setForeground(Color c) {
		Iterator iter = mapTableCells.values().iterator();
		while (iter.hasNext()) {
			TableCellCore item = (TableCellCore) iter.next();
			item.setForeground(c);
		}
	}

	public boolean setHeight(int iHeight) {
		// TODO Auto-generated method stub
		bRowVisuallyChangedSinceRefresh = true;
		return false;
	}

	public boolean setIconSize(Point pt) {
		// TODO Auto-generated method stub
		bRowVisuallyChangedSinceRefresh = true;
		return false;
	}

	public void setSelected(boolean bSelected) {
		if (this.bSelected == bSelected) {
			return;
		}

		this.bSelected = bSelected;
		setBackgroundColor(getIndex());
		view.rowSetSelected(this, bSelected);
	}

	/**
	 * @param b
	 */
	public void setFocused(boolean b) {
		if (b) {
			view.rowSetFocused(this);
		} else if (isFocused()) {
			view.rowSetFocused(null);
		}
	}

	public boolean isFocused() {
		return this.equals(view.getRowFocused());
	}

	/**
	 * Fixes up layout, and if something changed, it makes sure items near it 
	 * are fixed up
	 */
	public boolean setTableItem(int iRowPos) {
		if (!fixupPosition()) {
			return false;
		}

		ListRow row = view.getRow(iRowPos + 1);
		if (row != null) {
			row.fixupPosition();
		}

		bRowVisuallyChangedSinceRefresh = true;
		return true;
	}

	/**
	 * Fixes up row's layout.  Does not check if rows next to it are correct,
	 * even if some layout information changed
	 * 
	 * @return
	 */
	public boolean fixupPosition() {
		int iRowPos = getIndex();

		setBackgroundColor(iRowPos);

		if (parent == null || parent.isDisposed()) {
			return false;
		}

		return true;
	}

	public void setUpToDate(boolean upToDate) {
		if (bDisposed)
			return;

		long lTimeStart = System.currentTimeMillis();
		Iterator iter = mapTableCells.values().iterator();
		while (iter.hasNext()) {
			TableCellCore cell = (TableCellCore) iter.next();
			if (cell != null) {
				boolean bOldUpToDate = cell.isUpToDate();
				if (bOldUpToDate != upToDate) {
					cell.setUpToDate(upToDate);

					// hack.. a call to ListCell.isShown will trigger Visibility Listener
					ListCell listcell = (ListCell) cell.getBufferedTableItem();
					listcell.isShown();
				}
			}
		}
		long diff = System.currentTimeMillis() - lTimeStart;
		if (diff >= 50) {
			System.out.println("sutd: " + upToDate + " for " + getIndex() + "; " + diff + "ms");
		}
	}

	public Object getDataSource() {
		return getDataSource(false);
	}

	public TableCell getTableCell(String sColumnName) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getTableID() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isValid() {
  	if (bDisposed)
  		return true;

    boolean valid = true;
    Iterator iter = mapTableCells.values().iterator();
    while (iter.hasNext()) {
      TableCellCore cell = (TableCellCore)iter.next();
      if (cell != null)
        valid &= cell.isValid();
    }
    return valid;
	}

	public boolean getVisuallyChangedSinceLastRefresh() {
  	if (bDisposed)
  		return true;

  	if (bRowVisuallyChangedSinceRefresh) {
  		return true;
  	}

    Iterator iter = mapTableCells.values().iterator();
    while (iter.hasNext()) {
      TableCellCore cell = (TableCellCore)iter.next();
      if (cell != null)
        if (cell.getVisuallyChangedSinceRefresh()) {
        	return true;
        }
    }
    return false;
	}

	public Color getBackground() {
		if (bg == null) {
			setBackgroundColor(getIndex());
		}
		return bg;
	}

	/**
	 * @param x
	 * @param y
	 * @return
	 */
	public TableCellCore getTableCellCore(int x, int y) {
		Iterator iter = mapTableCells.values().iterator();
		while (iter.hasNext()) {
			TableCellCore cell = (TableCellCore) iter.next();
			if (cell.isShown()) {
				Rectangle bounds = cell.getBounds();
				if (bounds != null && bounds.contains(x, bounds.y)) {
					return cell;
				}
			}
		}
		return null;
	}

	public ListView getView() {
		return view;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableRowCore#repaint()
	public void  redraw(boolean bDoGraphics) {
		view.rowRefresh(this, bDoGraphics, true);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableRowCore#setAlternatingBGColor(boolean)
	public void setAlternatingBGColor(boolean bEvenIfNotVisible) {
		setBackgroundColor(getIndex());
	}

	public void doPaint(GC gc) {
		doPaint(gc, isVisible());
	}

	public void repaint() {
		redraw(true);
	}
	
	public String toString() {
		return "ListRow {" + getIndex() + (bDisposed ? ", Disposed" : "") + ","
				+ view.getTableID() + "}";
	}
	
	public int getVisibleYOffset() {
		return view.rowGetVisibleYOffset(this);
	}
}
