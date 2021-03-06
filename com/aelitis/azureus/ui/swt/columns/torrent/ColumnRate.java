/*
 * Created on Jun 16, 2006 2:41:08 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package com.aelitis.azureus.ui.swt.columns.torrent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.HSLColor;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.TableRowCore;
import org.gudy.azureus2.ui.swt.views.table.impl.TableCellImpl;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.torrent.GlobalRatingUtils;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Jun 16, 2006
 */
public class ColumnRate extends CoreTableColumn implements
		TableCellAddedListener
{
	static Font font = null;
	static Font smallFont = null;

	/**
	 * 
	 */
	public ColumnRate(String sTableID) {
		super("Rating", sTableID);
		initializeAsGraphic(POSITION_LAST, 40);
		setAlignment(ALIGN_CENTER);
	}

	public void cellAdded(TableCell cell) {
		new Cell(cell);
	}

	private class Cell implements TableCellRefreshListener,
			TableCellDisposeListener, TableCellMouseListener
	{
		String rating = "--";

		public Cell(final TableCell cell) {
			cell.addListeners(this);
			cell.setMarginWidth(0);
			cell.setMarginHeight(0);

			DownloadManager dm = (DownloadManager) cell.getDataSource();
			if (dm != null) {
				boolean isContent = PlatformTorrentUtils.isContent(dm.getTorrent());
				if (!isContent) {
					rating = "";
					return;
				}
			}
		}

		public void dispose(TableCell cell) {
			disposeOldImage(cell);
		}

		public void refresh(TableCell cell) {
			DownloadManager dm = (DownloadManager) cell.getDataSource();
			if (dm == null) {
				return;
			}

			TOTorrent torrent = dm.getTorrent();
			String rating = GlobalRatingUtils.getRatingString(torrent);
			long count = GlobalRatingUtils.getCount(torrent);

			boolean b;
			try {
				b = !cell.setSortValue(Float.parseFloat(rating) * 100000 + count);
			} catch (Exception e) {
				b = !cell.setSortValue(new Float(count));
			}
			
			if (b && cell.isValid()) {
				return;
			}
			if (!cell.isShown()) {
				return;
			}

			int width = cell.getWidth();
			int height = cell.getHeight();
			if (width <= 0 || height <= 0) {
				return;
			}
			Image img = new Image(Display.getDefault(), width, height);

			// draw border
			GC gcImage = new GC(img);

			Color background = ((TableRowCore) cell.getTableRow()).getBackground();
			if (background != null) {
				gcImage.setBackground(background);
				gcImage.fillRectangle(0, 0, width, height);
			}

			if (font == null) {
				// no sync required, SWT is on single thread
				FontData[] fontData = gcImage.getFont().getFontData();
				fontData[0].setHeight(Utils.pixelsToPoint(20,
						Display.getDefault().getDPI().y));
				fontData[0].setStyle(SWT.BOLD);
				font = new Font(Display.getDefault(), fontData);
			}

			gcImage.setFont(font);

			SWTSkinProperties skinProperties = SWTSkinFactory.getInstance().getSkinProperties();

			Color bg = ((TableCellImpl) cell).getTableRowCore().getBackground();
			HSLColor hsl = new HSLColor();
			hsl.initHSLbyRGB(bg.getRed(), bg.getGreen(), bg.getBlue());
			hsl.setLuminence(hsl.getLuminence() - 10);
			Color color2 = ColorCache.getColor(Display.getDefault(), hsl.getRed(),
					hsl.getGreen(), hsl.getBlue());

			Rectangle r = img.getBounds();
			r.x += 2;
			r.y += 2;
			if (color2 != null) {
				gcImage.setForeground(color2);
			}
			r.height -= 12;

			GCStringPrinter.printString(gcImage, rating, r, true, false, SWT.CENTER);

			Color color1 = ColorCache.getColor(Display.getDefault(),
					GlobalRatingUtils.getColor(torrent));
			if (color1 == null) {
				color1 = skinProperties.getColor("color.row.fg");
			}

			r = img.getBounds();
			r.height -= 12;
			gcImage.setForeground(color1);
			GCStringPrinter.printString(gcImage, rating, r, true,
					false, SWT.CENTER);

			if (count > 0) {
				if (smallFont == null) {
					gcImage.setFont(null);
					// no sync required, SWT is on single thread
					FontData[] fontData = gcImage.getFont().getFontData();
					fontData[0].setHeight(Utils.pixelsToPoint(9,
							Display.getDefault().getDPI().y));
					smallFont = new Font(Display.getDefault(), fontData);
				}

				gcImage.setFont(smallFont);

				GCStringPrinter.printString(gcImage, "" + count + " ratings",
						img.getBounds(), true, false, SWT.BOTTOM);
			}

			gcImage.dispose();

			Graphic graphic = new UISWTGraphicImpl(img);

			disposeOldImage(cell);

			cell.setGraphic(graphic);
		}

		/**
		 * 
		 */
		private void disposeOldImage(TableCell cell) {
			Graphic oldGraphic = cell.getGraphic();
			if (oldGraphic instanceof UISWTGraphic) {
				Image image = ((UISWTGraphic) oldGraphic).getImage();
				if (image != null && !image.isDisposed()) {
					image.dispose();
				}
			}
		}

		public void cellMouseTrigger(TableCellMouseEvent event) {
			if (event.eventType == TableCellMouseEvent.EVENT_MOUSEUP
					&& event.button == 2) {
				DownloadManager dm = (DownloadManager) event.cell.getDataSource();
				if (dm == null) {
					return;
				}

				TOTorrent torrent = dm.getTorrent();
				GlobalRatingUtils.updateFromPlatform(torrent, 0);
				Utils.beep();
			}
		}
	}
}
