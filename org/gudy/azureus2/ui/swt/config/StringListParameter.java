/*
 * File    : StringListParameter.java
 * Created : 18-Nov-2003
 * By      : parg
 * 
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
 */

package org.gudy.azureus2.ui.swt.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;

/**
 * @author Olivier
 * 
 */
public class StringListParameter extends Parameter {

  Control list;
  final String name;
  final String default_value;
	private final String[] values;
	private final boolean useCombo;

  /**
   * 
   * @param composite
   * @param _name
   * @param labels
   * @param values
   * @param bUseCombo
   */
  public StringListParameter(Composite composite, String _name,
			String labels[], String values[], boolean bUseCombo) {
		this(composite, _name, COConfigurationManager.getStringParameter(_name),
				labels, values, bUseCombo);
	}

  /**
   * 
   * @param composite
   * @param _name
   * @param labels
   * @param values
   */
	public StringListParameter(Composite composite, String _name,
			String labels[], String values[]) {
		this(composite, _name, COConfigurationManager.getStringParameter(_name),
				labels, values, true);
	}

	/**
	 * 
	 * @param composite
	 * @param _name
	 * @param defaultValue
	 * @param labels
	 * @param values
	 */
	public StringListParameter(Composite composite, String _name,
			String defaultValue, final String labels[], final String values[]) {
		this(composite, _name, defaultValue, labels, values, true);
	}

	/**
	 * 
	 * @param composite
	 * @param _name
	 * @param defaultValue
	 * @param labels
	 * @param values
	 * @param bUseCombo
	 */
	public StringListParameter(Composite composite, String _name,
			String defaultValue, final String labels[], final String values[],
			final boolean bUseCombo) {
  	super(_name);
    this.name = _name;
    this.default_value = defaultValue;
		this.values = values;
		useCombo = bUseCombo;
    
    if(labels.length != values.length) {
      return;
    }
    
    String value = COConfigurationManager.getStringParameter(name,defaultValue);
    int index = findIndex(value,values);
    if (bUseCombo)
    	list = new Combo(composite,SWT.SINGLE | SWT.READ_ONLY);
    else
    	list = new List(composite, SWT.SINGLE | SWT.BORDER);
    
    for(int i = 0 ; i < labels.length  ;i++) {
    	if (bUseCombo)
    		((Combo)list).add(labels[i]);
    	else
    		((List)list).add(labels[i]);
    }
      
    setIndex(index);
      
    list.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
      	int index;
      	if (bUseCombo)
      		index = ((Combo)list).getSelectionIndex();
      	else
      		index = ((List)list).getSelectionIndex();
      	setIndex(index);
        
        if( change_listeners != null ) {
          for (int i=0;i<change_listeners.size();i++){
            ((ParameterChangeListener)change_listeners.get(i)).parameterChanged(StringListParameter.this,false);
          }
        }
      }
    }); 
  }
    
  private int findIndex(String value,String values[]) {
    for(int i = 0 ; i < values.length ;i++) {
      if(values[i].equals( value))
        return i;
    }
    return 0;
  }
  
	protected void setIndex(final int index) {
  	String selected_value = values[index];

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (list == null || list.isDisposed()) {
					return;
				}

		  	if (useCombo) {
		  		if (((Combo)list).getSelectionIndex() != index) {
		  			((Combo)list).select(index);
		  		}
		  	} else {
		  		if (((List)list).getSelectionIndex() != index) {
		  			((List)list).select(index);
		  		}
		  	}
			}
		});
  	
  	if (!COConfigurationManager.getStringParameter(name).equals(selected_value)) {
  		COConfigurationManager.setParameter(name, selected_value);
  	}
	}
  
  public void setLayoutData(Object layoutData) {
    list.setLayoutData(layoutData);
   }
   
  public Control getControl() {
    return list;
  }
  
  public String getValue() {
    return COConfigurationManager.getStringParameter( name, default_value );
  }

  public void setValue(Object value) {
  	if (value instanceof String) {
  		String s = (String)value;
      setIndex(findIndex(s, values));
  	}
  }
  
  public Object getValueObject() {
  	return getValue();
  }
}