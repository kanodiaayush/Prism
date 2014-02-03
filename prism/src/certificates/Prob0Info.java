package certificates;

import java.util.Iterator;
import java.util.LinkedHashSet;

public class Prob0Info {
	private final LinkedHashSet<Integer> stateToProb0Order;
	
	public Prob0Info() {
		stateToProb0Order = new LinkedHashSet<Integer>();
	}
	
	public void addState(int state) {
		stateToProb0Order.add(state);
	}
	
	// Iterates over elements, the closer to the target the first.
	public Iterator<Integer> iterator() {
		return stateToProb0Order.iterator();
	}
}
