package com.aelitis.azureus.vivaldi.ver2;


import edu.harvard.syrah.nc.Coordinate;

class StableLocalPosition extends LocalPosition {
	
	protected StableLocalPosition(VivaldiV2PositionProvider _v2_provider) {
		super( _v2_provider );
	}

	public Coordinate getCoords() {
		return( getStableCoords());
	}
}
