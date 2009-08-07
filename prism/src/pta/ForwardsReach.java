//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package pta;

import java.util.*;

import prism.*;
import explicit.*;

public class ForwardsReach
{
	// Log for output
	protected PrismLog mainLog;

	// PTA target info
	protected BitSet targetLocs;
	protected Constraint targetConstraint;

	// Extra information associated with reachability graph
	protected BitSet target; // Bit set specifying target states
	protected List<Integer> initialStates; // Initial states

	// Constructors

	public ForwardsReach()
	{
		this(new PrismPrintStreamLog(System.out));
	}

	public ForwardsReach(PrismLog log)
	{
		this.mainLog = log;
	}

	// Accessors for other info generated during construction of reachability graph

	public BitSet getTarget()
	{
		return target;
	}

	public List<Integer> getInitialStates()
	{
		return initialStates;
	}

	/**
	 * Build a reachability graph based on a forwards PTA traversal.
	 * All zones in the symbolic states in the graph are guaranteed to be convex (i.e. DBMs).
	 * The set of target states found and the (singleton) set of initial states are stored separately
	 * and can be obtained subsequently using getTarget() and getInitialStates().
	 */
	public ReachabilityGraph buildForwardsGraph(PTA pta, BitSet targetLocs, Constraint targetConstraint)
			throws PrismException
	{
		Zone z;
		LocZone init, lz, lz2;
		LinkedList<LocZone> Y;
		IndexedSet<LocZone> Zset;
		//LocZoneSetOld Zset;
		ReachabilityGraph graph;
		int cMax;
		int src, dest, count;
		long timer, timerProgress;
		boolean progressDisplayed;

		// Store target info
		this.targetLocs = targetLocs;
		this.targetConstraint = targetConstraint;

		// Starting reachability...
		mainLog.println("\nBuilding forwards reachability graph...");
		timer = timerProgress = System.currentTimeMillis();
		progressDisplayed = false;

		// Compute max clock constraint value
		if (targetConstraint != null)
			pta.recomputeMaxClockConstraint(targetConstraint);
		cMax = pta.getMaxClockConstraint();

		// Initialise data structures
		graph = new ReachabilityGraph(pta);
		//Zset = new LocZoneSetOld();
		Zset = new IndexedSet<LocZone>();
		Y = new LinkedList<LocZone>();
		target = new BitSet();

		// Build initial symbolic state (NB: assume initial location = 0)
		z = DBM.createZero(pta);
		z.up(pta.getInvariantConstraints(0));
		z.cClosure(cMax);
		init = new LocZone(0, z);

		// Reachability loop
		Zset.add(init);
		Y.add(init);
		src = -1;
		// While there are unexplored symbolic states (in Y)...
		while (!Y.isEmpty()) {
			// Pick next state to explore
			// Y is a list containing states in order found
			// (so we know index of lz is src)
			lz = Y.removeFirst();
			src++;
			// Is this a target state?
			if (targetLocs.get(lz.loc) && (targetConstraint == null || lz.zone.isSatisfied(targetConstraint))) {
				target.set(src);
				// Add null for this state (no need to store info)
				graph.addState();
				continue;
			}
			// Otherwise, explore this symbolic state
			// First, check there is at least one transition
			// (don't want deadlocks in non-target states)
			if (pta.getTransitions(lz.loc).size() == 0) {
				throw new PrismException("PTA deadlocks in location \"" + pta.getLocationNameString(lz.loc) + "\"");
			}
			// For each outgoing transition...
			graph.addState();
			for (Transition transition : pta.getTransitions(lz.loc)) {
				int[] dests = new int[transition.getNumEdges()];
				boolean enabled = false;
				boolean unenabled = false;
				count = 0;
				for (Edge edge : transition.getEdges()) {
					// Build "post" zone for this edge
					lz2 = lz.deepCopy();
					lz2.post(edge);
					// If non-empty, create edge, also adding state to Y if new 
					if (!lz2.zone.isEmpty()) {
						if (Zset.add(lz2)) {
							Y.add(lz2);
						}
						dest = Zset.getIndexOfLastAdd();
						enabled = true;
						dests[count] = dest;
					} else {
						unenabled = true;
						dests[count] = -1;
					}
					count++;
				}
				if (enabled) {
					if (unenabled)
						throw new PrismException("Badly formed PTA: state " + src);
					graph.addTransition(src, transition, dests, null);
				}
			}
			// Print some progress info occasionally
			if (System.currentTimeMillis() - timerProgress > 3000) {
				if (!progressDisplayed) {
					mainLog.print("Number of states so far:");
					progressDisplayed = true;
				}
				mainLog.print(" " + Zset.size());
				mainLog.flush();
				timerProgress = System.currentTimeMillis();
			}
		}

		// Tidy up progress display
		if (progressDisplayed)
			mainLog.println(" " + Zset.size());

		// Convert state set to ArrayList and store
		graph.states = Zset.toArrayList();

		// Always have a single initial state 0 after this construction
		initialStates = new ArrayList<Integer>();
		initialStates.add(0);

		// Reachability complete
		timer = System.currentTimeMillis() - timer;
		mainLog.println("Graph constructed in " + (timer / 1000.0) + " secs.");
		mainLog.print("Graph: " + graph.states.size() + " symbolic states");
		mainLog.println("), " + target.cardinality() + " target states");

		// Print a warning if there are no target states
		if (target.cardinality() == 0)
			mainLog.println("Warning: There are no target states.");

		return graph;
	}
}