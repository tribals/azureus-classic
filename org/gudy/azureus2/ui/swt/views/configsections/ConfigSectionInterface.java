/*
 * File    : ConfigPanel*.java
 * Created : 11 mar. 2004
 * By      : TuxPaper
 * 
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
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
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package org.gudy.azureus2.ui.swt.views.configsections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.plugins.platform.PlatformManagerException;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

import java.util.HashMap;

public class ConfigSectionInterface implements UISWTConfigSection {
	private final static String KEY_PREFIX = "ConfigView.section.interface.";

	private final static String LBLKEY_PREFIX = "ConfigView.label.";

	Label passwordMatch;

	private ParameterListener decisions_parameter_listener;

	public String configSectionGetParentSection() {
		return ConfigSection.SECTION_ROOT;
	}

	public String configSectionGetName() {
		return ConfigSection.SECTION_INTERFACE;
	}

	public void configSectionSave() {
	}

	public void configSectionDelete() {

		if (decisions_parameter_listener != null) {

			COConfigurationManager.removeParameterListener(
					"MessageBoxWindow.decisions", decisions_parameter_listener);
		}
	}

	public Composite configSectionCreate(final Composite parent) {
		GridData gridData;
		GridLayout layout;
		Label label;

		Composite cDisplay = new Composite(parent, SWT.NULL);

		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL
				| GridData.HORIZONTAL_ALIGN_FILL);
		cDisplay.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 1;
		cDisplay.setLayout(layout);

		new BooleanParameter(cDisplay, "Open Details", LBLKEY_PREFIX
				+ "opendetails");
		new BooleanParameter(cDisplay, "Open Bar", false, LBLKEY_PREFIX + "openbar");

		if (!Constants.isOSX || SWT.getVersion() >= 3300) {

			BooleanParameter est = new BooleanParameter(cDisplay,
					"Enable System Tray", KEY_PREFIX + "enabletray");

			BooleanParameter ctt = new BooleanParameter(cDisplay, "Close To Tray",
					LBLKEY_PREFIX + "closetotray");
			BooleanParameter mtt = new BooleanParameter(cDisplay, "Minimize To Tray",
					LBLKEY_PREFIX + "minimizetotray");

			est.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(ctt
					.getControls()));
			est.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(mtt
					.getControls()));

		}
		
        /**
         * Default download / upload limits available in the UI.
         */
        Group limit_group = new Group(cDisplay, SWT.NULL);
        Messages.setLanguageText(limit_group, LBLKEY_PREFIX + "set_ui_transfer_speeds");
        layout = new GridLayout();
        limit_group.setLayout(layout);
        
        Label limit_group_label = new Label(limit_group, SWT.WRAP);
        Messages.setLanguageText(limit_group_label, LBLKEY_PREFIX + "set_ui_transfer_speeds.description");
        
        String[] limit_types = new String[] {"download", "upload"};
        final String limit_type_prefix = "config.ui.speed.partitions.manual.";
        for (int i=0; i<limit_types.length; i++) {
        	final BooleanParameter bp = new BooleanParameter(limit_group, limit_type_prefix + limit_types[i] + ".enabled", false, LBLKEY_PREFIX + "set_ui_transfer_speeds.description." + limit_types[i]);
        	final StringParameter sp = new StringParameter(limit_group, limit_type_prefix + limit_types[i] + ".values", "");
        	IAdditionalActionPerformer iaap = new GenericActionPerformer(new Control[] {}) {
        		public void performAction() {
        			sp.getControl().setEnabled(bp.isSelected());	
        		}
        	};
        	
            gridData = new GridData();
            gridData.widthHint = 150;
            sp.setLayoutData(gridData);
        	iaap.performAction();
        	bp.setAdditionalActionPerformer(iaap);
        }

		new BooleanParameter(cDisplay, "Send Version Info", true, LBLKEY_PREFIX
				+ "allowSendVersion");

		Composite cArea = new Composite(cDisplay, SWT.NULL);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		cArea.setLayout(layout);
		cArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		if (!Constants.isOSX) {

			BooleanParameter confirm = new BooleanParameter(cArea,
					"confirmationOnExit",
					"ConfigView.section.style.confirmationOnExit");
			gridData = new GridData();
			gridData.horizontalSpan = 2;
			confirm.setLayoutData(gridData);
		}
		
		cArea = new Composite(cDisplay, SWT.NULL);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		cArea.setLayout(layout);
		cArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// config torrent removal
		
		BooleanParameter confirm_removal = new BooleanParameter(cArea,
				"confirm_torrent_removal", KEY_PREFIX + "confirm_torrent_removal");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		confirm_removal.setLayoutData(gridData);

		// clear remembered decisions

		final Label clear_label = new Label(cArea, SWT.NULL);
		Messages.setLanguageText(clear_label, KEY_PREFIX + "cleardecisions");

		final Button clear_decisions = new Button(cArea, SWT.PUSH);
		Messages.setLanguageText(clear_decisions, KEY_PREFIX
				+ "cleardecisionsbutton");

		clear_decisions.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {

				COConfigurationManager.setParameter("MessageBoxWindow.decisions",
						new HashMap());
			}
		});

		decisions_parameter_listener = new ParameterListener() {
			public void parameterChanged(String parameterName) {
				if (clear_decisions.isDisposed()) {

					// tidy up from previous incarnations

					COConfigurationManager.removeParameterListener(
							"MessageBoxWindow.decisions", this);

				} else {

					boolean enabled = COConfigurationManager.getMapParameter(
							"MessageBoxWindow.decisions", new HashMap()).size() > 0;

					clear_label.setEnabled(enabled);
					clear_decisions.setEnabled(enabled);
				}
			}
		};

		decisions_parameter_listener.parameterChanged(null);

		COConfigurationManager.addParameterListener("MessageBoxWindow.decisions",
				decisions_parameter_listener);

		// password

		label = new Label(cArea, SWT.NULL);
		Messages.setLanguageText(label, LBLKEY_PREFIX + "password");

		gridData = new GridData();
		gridData.widthHint = 150;
		PasswordParameter pw1 = new PasswordParameter(cArea, "Password");
		pw1.setLayoutData(gridData);
		Text t1 = (Text) pw1.getControl();

		//password confirm

		label = new Label(cArea, SWT.NULL);
		Messages.setLanguageText(label, LBLKEY_PREFIX + "passwordconfirm");
		gridData = new GridData();
		gridData.widthHint = 150;
		PasswordParameter pw2 = new PasswordParameter(cArea, "Password Confirm");
		pw2.setLayoutData(gridData);
		Text t2 = (Text) pw2.getControl();

		// password activated

		label = new Label(cArea, SWT.NULL);
		Messages.setLanguageText(label, LBLKEY_PREFIX + "passwordmatch");
		passwordMatch = new Label(cArea, SWT.NULL);
		gridData = new GridData();
		gridData.widthHint = 150;
		passwordMatch.setLayoutData(gridData);
		refreshPWLabel();

		t1.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				refreshPWLabel();
			}
		});
		t2.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				refreshPWLabel();
			}
		});

		// drag-drop

		label = new Label(cArea, SWT.NULL);
		Messages.setLanguageText(label, "ConfigView.section.style.dropdiraction");

		String[] drop_options = {
				"ConfigView.section.style.dropdiraction.opentorrents",
				"ConfigView.section.style.dropdiraction.sharefolder",
				"ConfigView.section.style.dropdiraction.sharefoldercontents",
				"ConfigView.section.style.dropdiraction.sharefoldercontentsrecursive", };

		String dropLabels[] = new String[drop_options.length];
		String dropValues[] = new String[drop_options.length];
		for (int i = 0; i < drop_options.length; i++) {

			dropLabels[i] = MessageText.getString(drop_options[i]);
			dropValues[i] = "" + i;
		}
		new StringListParameter(cArea, "config.style.dropdiraction", "1",
				dropLabels, dropValues);

		// reset associations

		final PlatformManager platform = PlatformManagerFactory
				.getPlatformManager();

		if (platform
				.hasCapability(PlatformManagerCapabilities.RegisterFileAssociations)) {

			Composite cResetAssoc = new Composite(cArea, SWT.NULL);
			layout = new GridLayout();
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			layout.numColumns = 2;
			cResetAssoc.setLayout(layout);
			cResetAssoc.setLayoutData(new GridData());

			label = new Label(cResetAssoc, SWT.NULL);
			Messages.setLanguageText(label, KEY_PREFIX + "resetassoc");

			Button reset = new Button(cResetAssoc, SWT.PUSH);
			Messages.setLanguageText(reset, KEY_PREFIX + "resetassocbutton"); //$NON-NLS-1$

			reset.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {

					try {
						platform.registerApplication();

					} catch (PlatformManagerException e) {

						Logger.log(new LogAlert(LogAlert.UNREPEATABLE,
								"Failed to register application", e));
					}
				}
			});

			new BooleanParameter(cArea, "config.interface.checkassoc",
					KEY_PREFIX + "checkassoc");

		}

		return cDisplay;
	}

	private void refreshPWLabel() {

		if (passwordMatch == null || passwordMatch.isDisposed())
			return;
		byte[] password = COConfigurationManager.getByteParameter("Password", ""
				.getBytes());
		COConfigurationManager.setParameter("Password enabled", false);
		if (password.length == 0) {
			passwordMatch.setText(MessageText.getString(LBLKEY_PREFIX
					+ "passwordmatchnone"));
		} else {
			byte[] confirm = COConfigurationManager.getByteParameter(
					"Password Confirm", "".getBytes());
			if (confirm.length == 0) {
				passwordMatch.setText(MessageText.getString(LBLKEY_PREFIX
						+ "passwordmatchno"));
			} else {
				boolean same = true;
				for (int i = 0; i < password.length; i++) {
					if (password[i] != confirm[i])
						same = false;
				}
				if (same) {
					passwordMatch.setText(MessageText.getString(LBLKEY_PREFIX
							+ "passwordmatchyes"));
					COConfigurationManager.setParameter("Password enabled", true);
				} else {
					passwordMatch.setText(MessageText.getString(LBLKEY_PREFIX
							+ "passwordmatchno"));
				}
			}
		}
	}

}
