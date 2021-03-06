/*
 * Created on Jun 26, 2006 12:46:42 PM
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
package com.aelitis.azureus.ui.swt.skin;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;

/**
 * Text Skin Object.  This one uses a label widget.
 * 
 * @author TuxPaper
 * @created Jun 26, 2006
 *
 */
public class SWTSkinObjectText1 extends SWTSkinObjectBasic implements
		SWTSkinObjectText
{
	String sText;

	String sKey;

	boolean bIsTextDefault = false;

	Label label;

	public SWTSkinObjectText1(SWTSkin skin, SWTSkinProperties skinProperties,
			String sID, String sConfigID, String[] typeParams, SWTSkinObject parent) {
		super(skin, skinProperties, sID, sConfigID, "text", parent);

		int style = SWT.WRAP;

		String sAlign = skinProperties.getStringValue(sConfigID + ".align");
		if (sAlign != null) {
			int align = SWTSkinUtils.getAlignment(sAlign, SWT.NONE);
			if (align != SWT.NONE) {
				style |= align;
			}
		}

		if (skinProperties.getIntValue(sConfigID + ".border", 0) == 1) {
			style |= SWT.BORDER;
		}

		Composite createOn;
		if (parent == null) {
			createOn = skin.getShell();
		} else {
			createOn = (Composite) parent.getControl();
		}

		boolean bKeepMaxSize = properties.getStringValue(
				sConfigID + ".keepMaxSize", "").equals("1");
		label = bKeepMaxSize ? new LabelNoShrink(createOn, style) : new Label(
				createOn, style);
		setControl(label);
		if (typeParams.length > 1) {
			bIsTextDefault = true;
			sText = typeParams[1];
			label.setText(sText);
		}
	}

	public String switchSuffix(String suffix, int level, boolean walkUp) {
		suffix = super.switchSuffix(suffix, level, walkUp);
		if (suffix == null) {
			return null;
		}

		String sPrefix = sConfigID + ".text";

		if (sText == null || bIsTextDefault) {
			String text = properties.getStringValue(sPrefix + suffix);
			if (text != null) {
				label.setText(text);
			}
		}

		Color color = properties.getColor(sPrefix + ".color" + suffix);
		//System.out.println(this + "; " + sPrefix + ";" + suffix + "; " + color + "; " + text);
		if (color != null) {
			label.setForeground(color);
		}

		Font existingFont = (Font) label.getData("Font" + suffix);
		if (existingFont != null && !existingFont.isDisposed()) {
			label.setFont(existingFont);
		} else {
			boolean bNewFont = false;
			int iFontSize = -1;
			int iFontWeight = -1;
			String sFontFace = null;

			String sSize = properties.getStringValue(sPrefix + ".size" + suffix);
			if (sSize != null) {
				FontData[] fd = label.getFont().getFontData();

				try {
					char firstChar = sSize.charAt(0);
					if (firstChar == '+' || firstChar == '-') {
						sSize = sSize.substring(1);
					}

					int iSize = NumberFormat.getInstance(Locale.US).parse(sSize).intValue();

					if (firstChar == '+') {
						iFontSize = (int)(fd[0].height + iSize);
					} else if (firstChar == '-') {
						iFontSize = (int)(fd[0].height - iSize);
					} else {
						iFontSize = iSize;
					}

					if (sSize.endsWith("px")) {
						iFontSize = Utils.pixelsToPoint(iSize,
								label.getDisplay().getDPI().y);
					}

					bNewFont = true;
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			String sStyle = properties.getStringValue(sPrefix + ".style" + suffix);
			if (sStyle != null) {
				String[] sStyles = sStyle.toLowerCase().split(",");
				for (int i = 0; i < sStyles.length; i++) {
					String s = sStyles[i];
					if (s.equals("bold")) {
						if (iFontWeight == -1) {
							iFontWeight = SWT.BOLD;
						} else {
							iFontWeight |= SWT.BOLD;
						}
						bNewFont = true;
					}

					if (s.equals("italic")) {
						if (iFontWeight == -1) {
							iFontWeight = SWT.ITALIC;
						} else {
							iFontWeight |= SWT.ITALIC;
						}
						bNewFont = true;
					}

					if (s.equals("underline")) {
						label.addPaintListener(new PaintListener() {
							public void paintControl(PaintEvent e) {
								Point size = ((Control) e.widget).getSize();
								e.gc.drawLine(0, size.y - 1, size.x - 1, size.y - 1);
							}
						});
					}

					if (s.equals("strike")) {
						label.addPaintListener(new PaintListener() {
							public void paintControl(PaintEvent e) {
								Point size = ((Control) e.widget).getSize();
								int y = size.y / 2;
								e.gc.drawLine(0, y, size.x - 1, y);
							}
						});
					}
				}
			}

			sFontFace = properties.getStringValue(sPrefix + ".font" + suffix);
			if (sFontFace != null) {
				bNewFont = true;
			}

			if (bNewFont) {
				FontData[] fd = label.getFont().getFontData();

				if (iFontSize > 0) {
					fd[0].setHeight(iFontSize);
				}

				if (iFontWeight >= 0) {
					fd[0].setStyle(iFontWeight);
				}

				if (sFontFace != null) {
					fd[0].setName(sFontFace);
				}

				final Font labelFont = new Font(label.getDisplay(), fd);
				label.setFont(labelFont);
				label.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent e) {
						labelFont.dispose();
					}
				});

				label.setData("Font" + suffix, labelFont);
			}
		}

		label.update();

		return suffix;
	}

	/**
	 * @param searchText
	 */
	public void setText(String text) {
		if (text == null) {
			text = "";
		}

		if (text.equals(sText)) {
			return;
		}

		this.sText = text;
		this.sKey = null;
		bIsTextDefault = false;

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (label != null && !label.isDisposed()) {
					label.setText(sText);
					Utils.relayout(label);
				}
			}
		});
	}

	public void setTextID(final String key) {
		if (key == null) {
			setText("");
		}

		if (key.equals(sKey)) {
			return;
		}

		this.sText = MessageText.getString(key);
		this.sKey = key;
		bIsTextDefault = false;

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (label != null && !label.isDisposed()) {
					Messages.setLanguageText(label, key);
					Utils.relayout(label);
				}
			}
		});
	}

	private class LabelNoShrink extends Label
	{
		Point ptMax;

		/**
		 * Default Constructor
		 * 
		 * @param parent
		 * @param style
		 */
		public LabelNoShrink(Composite parent, int style) {
			super(parent, style | SWT.CENTER);
			ptMax = new Point(0, 0);
		}

		// I know what I'm doing. Maybe ;)
		public void checkSubclass() {
		}

		public Point computeSize(int wHint, int hHint, boolean changed) {
			Point pt = super.computeSize(wHint, hHint, changed);
			if (pt.x > ptMax.x) {
				ptMax.x = pt.x;
			}
			if (pt.y > ptMax.y) {
				ptMax.y = pt.y;
			}

			return ptMax;
		}
	}
}
