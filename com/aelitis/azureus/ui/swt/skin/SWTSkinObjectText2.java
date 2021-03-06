/*
 * Created on Aug 4, 2006 9:03:19 AM
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
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;

/**
 * Text Skin Object.  This one paints text on parent.
 * 
 * @author TuxPaper
 * @created Aug 4, 2006
 *
 */
public class SWTSkinObjectText2 extends SWTSkinObjectBasic implements
		SWTSkinObjectText, PaintListener
{
	String sText;

	String sKey;

	boolean bIsTextDefault = false;

	private int style;

	private Canvas canvas;

	private static Font font = null;

	public SWTSkinObjectText2(SWTSkin skin, SWTSkinProperties skinProperties,
			String sID, String sConfigID, String[] typeParams, SWTSkinObject parent) {
		super(skin, skinProperties, sID, sConfigID, "text", parent);

		style = SWT.WRAP;

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

		canvas = new Canvas(createOn, SWT.NONE) {
			Point ptMax = new Point(0, 0);

			// @see org.eclipse.swt.widgets.Composite#computeSize(int, int, boolean)
			public Point computeSize(int wHint, int hHint, boolean changed) {
				int border = getBorderWidth() * 2;
				Point pt = new Point(border, border);

				if (sText == null) {
					return pt;
				}

				Font existingFont = (Font) canvas.getData("font");
				Color existingColor = (Color) canvas.getData("color");

				GC gc = new GC(this);
				if (existingFont != null) {
					gc.setFont(existingFont);
				}
				if (existingColor != null) {
					gc.setForeground(existingColor);
				}
				pt = gc.textExtent(sText);
				pt.x += border;
				pt.y += border;
				gc.dispose();

				if (pt.x > ptMax.x) {
					ptMax.x = pt.x;
				}
				if (pt.y > ptMax.y) {
					ptMax.y = pt.y;
				}

				return ptMax;
			}
		};

		if (false && font == null) {
			Display display = createOn.getDisplay();
			FontData fd = new FontData("Arial", Utils.pixelsToPoint(10,
					display.getDPI().y), SWT.NORMAL);
			font = new Font(display, fd);
		}

		canvas.setData("font", font);
		setControl(canvas);
		if (typeParams.length > 1) {
			bIsTextDefault = true;
			sText = typeParams[1];
		}

		canvas.addPaintListener(this);
		updateFont("");
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
				sText = text;
			}
		}

		updateFont(suffix);
		return suffix;
	}

	private void updateFont(String suffix) {
		String sPrefix = sConfigID + ".text";

		Color color = properties.getColor(sPrefix + ".color" + suffix);
		//System.out.println(this + "; " + sPrefix + ";" + suffix + "; " + color + "; " + text);
		if (color != null) {
			canvas.setData("color", color);
		}

		Font existingFont = (Font) canvas.getData("Font" + suffix);
		if (existingFont != null && !existingFont.isDisposed()) {
			canvas.setData("font", existingFont);
		} else {
			boolean bNewFont = false;
			int iFontSize = -1;
			int iFontWeight = -1;
			String sFontFace = null;

			String sSize = properties.getStringValue(sPrefix + ".size" + suffix);
			if (sSize != null) {
				FontData[] fd = canvas.getFont().getFontData();

				try {
					char firstChar = sSize.charAt(0);
					if (firstChar == '+' || firstChar == '-') {
						sSize = sSize.substring(1);
					}

					double dSize = NumberFormat.getInstance(Locale.US).parse(sSize).doubleValue();

					if (firstChar == '+') {
						iFontSize = (int) (fd[0].height + dSize);
					} else if (firstChar == '-') {
						iFontSize = (int) (fd[0].height - dSize);
					} else {
						if (sSize.endsWith("px")) {
							iFontSize = Utils.pixelsToPoint(dSize,
									canvas.getDisplay().getDPI().y);
						} else {
							iFontSize = (int) dSize;
						}
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
						canvas.addPaintListener(new PaintListener() {
							public void paintControl(PaintEvent e) {
								Point size = ((Control) e.widget).getSize();
								e.gc.drawLine(0, size.y - 1, size.x - 1, size.y - 1);
							}
						});
					}

					if (s.equals("strike")) {
						canvas.addPaintListener(new PaintListener() {
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
				FontData[] fd = canvas.getFont().getFontData();

				if (iFontSize > 0) {
					fd[0].setHeight(iFontSize);
				}

				if (iFontWeight >= 0) {
					fd[0].setStyle(iFontWeight);
				}

				if (sFontFace != null) {
					fd[0].setName(sFontFace);
				}

				final Font canvasFont = new Font(canvas.getDisplay(), fd);
				canvas.setData("font", canvasFont);
				canvas.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent e) {
						canvasFont.dispose();
					}
				});

				canvas.setData("Font" + suffix, canvasFont);
			}
		}

		canvas.redraw();
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
				canvas.redraw();
				Utils.relayout(canvas);
			}
		});
	}

	public void paintControl(PaintEvent e) {
		if (sText == null || sText.length() == 0) {
			return;
		}
		//		e.gc.setClipping(e.x, e.y, e.width, e.height);

		Composite composite = (Composite) e.widget;
		Rectangle clientArea = composite.getClientArea();

		Font existingFont = (Font) canvas.getData("font");
		Color existingColor = (Color) canvas.getData("color");

		if (existingFont != null) {
			e.gc.setFont(existingFont);
		}
		if (existingColor != null) {
			e.gc.setForeground(existingColor);
		}
		GCStringPrinter.printString(e.gc, sText, clientArea, true, false, style
				| SWT.TOP);
	}

	public void setTextID(String key) {
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
				canvas.redraw();
				canvas.layout(true);
				Utils.relayout(canvas);
			}
		});
	}
}
