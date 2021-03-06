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

package org.gudy.azureus2.ui.swt;

import org.gudy.azureus2.core3.config.impl.ConfigurationDefaults;

/**
 * @author TuxPaper
 * @created Nov 3, 2006
 *
 */
public class UIConfigDefaultsSWT
{

	/**
	 * 
	 */
	public static void initialize() {
		ConfigurationDefaults def = ConfigurationDefaults.getInstance();
		def.addParameter("useCustomTab", true);
		def.addParameter("GUI Refresh", 1000);
		def.addParameter("Graphics Update", 4);
		def.addParameter("ReOrder Delay", 0);
		def.addParameter("Send Version Info", true);
		def.addParameter("Show Download Basket", false);
		def.addParameter("config.style.refreshMT", 0);
		def.addParameter("Open Details", false);
		def.addParameter("IconBar.enabled", true);

		def.addParameter("DefaultDir.BestGuess", true);
		def.addParameter("DefaultDir.AutoUpdate", true);
		def.addParameter("DefaultDir.AutoSave.AutoRename", true);
		def.addParameter("GUI_SWT_bFancyTab", true);
		def.addParameter("GUI_SWT_bAlternateTablePainting", false);
		def.addParameter("Colors.progressBar.override", false);
		def.addParameter("GUI_SWT_DisableAlertSliding", false);
		def.addParameter("NameColumn.showProgramIcon", true);
		def.addParameter("Open MyTorrents", true);
		def.addParameter("DND Always In Incomplete", false);

		def.addParameter("Message Popup Autoclose in Seconds", 15);

		def.addParameter("Add URL Silently", false);
		def.addParameter("config.style.dropdiraction", "1");
		def.addParameter("MyTorrents.SplitAt", 30);

		def.addParameter("Wizard Completed", false);
		def.addParameter("Color Scheme.red", 0);
		def.addParameter("Color Scheme.green", 128);
		def.addParameter("Color Scheme.blue", 255);
		def.addParameter("Show Splash", true);
		def.addParameter("window.maximized", true);
		def.addParameter("window.rectangle", "");
		def.addParameter("Open Console", false);
		def.addParameter("Open Config", false);
		def.addParameter("Open Stats On Start", false);
		def.addParameter("Start Minimized", false);
		def.addParameter("Open Bar", false);

		def.addParameter("Close To Tray", true);
		def.addParameter("Minimize To Tray", false);
		
		def.addParameter("Status Area Show SR", true);
		def.addParameter("Status Area Show NAT", true);
		def.addParameter("Status Area Show DDB", true);
		def.addParameter("Status Area Show IPF", true);
		
		def.addParameter("ui", "az2");
	}
}
