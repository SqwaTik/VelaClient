package dev.sqwat.vela.opt.entity;

public interface PooledState {
	Object velaopt$state();

	Object velaopt$stateOwner();

	void velaopt$setState(Object state, Object owner);
}
