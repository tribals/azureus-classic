package com.aelitis.azureus.vivaldi.ver2;

class TransientTuple {
	protected long remove_time;
	protected final IDWrapper id;
	protected SyrahPosition last_pos;
	protected float last_rtt;
	protected long create_time;
  
	public TransientTuple(long _remove_time, IDWrapper _id, SyrahPosition _last_pos, 
    float _last_rtt, long _create_time) {
		remove_time = _remove_time;
		id = _id;
		last_pos = _last_pos;
		last_rtt = _last_rtt;
    create_time = _create_time;
	}
}
