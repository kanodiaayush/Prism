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

package explicit;

import java.util.*;
import java.io.*;

import prism.ModelType;
import prism.PrismException;
import prism.PrismUtils;

/**
 * Explicit representation of stochastic two-player game (STPG)
 */
public class STPG extends ModelSimple
{
	// Transition function (Steps)
	protected List<ArrayList<DistributionSet>> trans;

	// Rewards
	protected List<List<Double>> transRewards;
	protected Double transRewardsConstant;

	// Flag: allow dupes in distribution sets?
	public boolean allowDupes = false;

	// Other statistics
	protected int numDistrSets;
	protected int numDistrs;
	protected int numTransitions;
	protected int maxNumDistrSets;
	protected int maxNumDistrs;

	/**
	 * Constructor: empty STPG.
	 */
	public STPG()
	{
		initialise(0);
	}

	/**
	 * Constructor: new STPG with fixed number of states.
	 */
	public STPG(int numStates)
	{
		initialise(numStates);
	}

	/**
	 * Constructor: build an STPG from an MDP.
	 * Data is copied directly from the MDP so take a copy first if you plan to keep/modify the MDP.
	 */
	public STPG(MDPSimple m)
	{
		DistributionSet set;
		int i;
		// TODO: actions? rewards?
		initialise(m.getNumStates());
		for (i = 0; i < numStates; i++) {
			set = newDistributionSet(null);
			set.addAll(m.getChoices(i));
			addDistributionSet(i, set);
		}
	}

	/**
	 * Initialise: new model with fixed number of states.
	 */
	public void initialise(int numStates)
	{
		super.initialise(numStates);
		numDistrSets = numDistrs = numTransitions = 0;
		maxNumDistrSets = maxNumDistrs = 0;
		trans = new ArrayList<ArrayList<DistributionSet>>(numStates);
		for (int i = 0; i < numStates; i++) {
			trans.add(new ArrayList<DistributionSet>());
		}
		clearAllRewards();
	}

	/**
	 * Clear all information for a state (i.e. remove all transitions).
	 */
	public void clearState(int i)
	{
		// Do nothing if state does not exist
		if (i >= numStates || i < 0)
			return;
		// Clear data structures and update stats
		List<DistributionSet> list = trans.get(i);
		numDistrSets -= list.size();
		for (DistributionSet set : list) {
			numDistrs -= set.size();
			for (Distribution distr : set)
				numTransitions -= distr.size();
		}
		//TODO: recompute maxNumDistrSets
		//TODO: recompute maxNumDistrs
		// Remove all distribution sets
		trans.set(i, new ArrayList<DistributionSet>(0));
	}

	/**
	 * Add a new state and return its index.
	 */
	public int addState()
	{
		addStates(1);
		return numStates - 1;
	}

	/**
	 * Add multiple new states.
	 */
	public void addStates(int numToAdd)
	{
		for (int i = 0; i < numToAdd; i++) {
			trans.add(new ArrayList<DistributionSet>());
		}
		numStates += numToAdd;
	}

	/**
	 * Creates a new distribution set suitable for passing to addDistributionSet(...)
	 * i.e. a data structure consistent with the internals of the this class.
	 * Action label (any Object type) must be specified.
	 */
	public DistributionSet newDistributionSet(Object action)
	{
		return new DistributionSet(action);
	}

	/**
	 * Add distribution set 'newSet' to state s (which must exist).
	 * Distribution set is only actually added if it does not already exists for state s.
	 * (Assuming 'allowDupes' flag is not enabled.)
	 * Returns the index of the (existing or newly added) set.
	 * Returns -1 in case of error.
	 */
	public int addDistributionSet(int s, DistributionSet newSet)
	{
		ArrayList<DistributionSet> set;
		// Check state exists
		if (s >= numStates || s < 0)
			return -1;
		// Add distribution set (if new)
		set = trans.get(s);
		if (!allowDupes) {
			int i = set.indexOf(newSet);
			if (i != -1)
				return i;
		}
		set.add(newSet);
		// Update stats
		numDistrSets++;
		maxNumDistrSets = Math.max(maxNumDistrSets, set.size());
		numDistrs += newSet.size();
		maxNumDistrs = Math.max(maxNumDistrs, newSet.size());
		for (Distribution distr : newSet)
			numTransitions += distr.size();
		return set.size() - 1;
	}

	/**
	 * Remove all rewards from the model
	 */
	public void clearAllRewards()
	{
		transRewards = null;
		transRewardsConstant = null;
	}

	/**
	 * Set a constant reward for all transitions
	 */
	public void setConstantTransitionReward(double r)
	{
		// This replaces any other reward definitions
		transRewards = null;
		// Store as a Double (because we use null to check for its existence)
		transRewardsConstant = new Double(r);
	}

	/**
	 * Set the reward for choice i in some state s to r.
	 */
	public void setTransitionReward(int s, int i, double r)
	{
		// This would replace any constant reward definition, if it existed
		transRewardsConstant = null;
		// If no rewards array created yet, create it
		if (transRewards == null) {
			transRewards = new ArrayList<List<Double>>(numStates);
			for (int j = 0; j < numStates; j++)
				transRewards.add(null);
		}
		// If no rewards for state i yet, create list
		if (transRewards.get(s) == null) {
			int n = trans.get(s).size();
			List<Double> list = new ArrayList<Double>(n);
			for (int j = 0; j < n; j++) {
				list.add(0.0);
			}
			transRewards.set(s, list);
		}
		// Set reward
		transRewards.get(s).set(i, r);
	}

	/**
	 * Get the number of nondeterministic (player 1) choices in state s.
	 */
	public int getNumChoices(int s)
	{
		return trans.get(s).size();
	}

	/**
	 * Get the list of choices (distribution sets) for state s.
	 */
	public List<DistributionSet> getChoices(int s)
	{
		return trans.get(s);
	}

	/**
	 * Get the ith choice (distribution set) for state s.
	 */
	public DistributionSet getChoice(int s, int i)
	{
		return trans.get(s).get(i);
	}

	@Override
	public ModelType getModelType()
	{
		return ModelType.MDP;
	}

	/**
	 * Get the transition reward (if any) for choice i of state s.
	 */
	public double getTransitionReward(int s, int i)
	{
		List<Double> list;
		if (transRewardsConstant != null)
			return transRewardsConstant;
		if (transRewards == null || (list = transRewards.get(s)) == null)
			return 0.0;
		return list.get(i);
	}

	/**
	 * Returns true if state s2 is a successor of state s1.
	 */
	public boolean isSuccessor(int s1, int s2)
	{
		for (DistributionSet distrs : trans.get(s1)) {
			for (Distribution distr : distrs) {
				if (distr.contains(s2))
					return true;
			}
		}
		return false;
	}

	public boolean allSuccessorsInSet(int s, BitSet set)
	{
		for (DistributionSet distrs : trans.get(s)) {
			for (Distribution distr : distrs) {
				if (!distr.isSubsetOf(set))
					return false;
			}
		}
		return true;
	}

	public boolean someSuccessorsInSet(int s, BitSet set)
	{
		for (DistributionSet distrs : trans.get(s)) {
			for (Distribution distr : distrs) {
				if (distr.isSubsetOf(set))
					return true;
			}
		}
		return false;
	}

	/**
	 * Get the total number of player 1 choices (distribution sets) over all states.
	 */
	public int getNumP1Choices()
	{
		return numDistrSets;
	}

	/**
	 * Get the total number of player 2 choices (distributions) over all states.
	 */
	public int getNumP2Choices()
	{
		return numDistrs;
	}

	/**
	 * Get the total number of transitions in the model.
	 */
	public int getNumTransitions()
	{
		return numTransitions;
	}

	/**
	 * Get the maximum number of player 1 choices (distribution sets) in any state.
	 */
	public int getMaxNumP1Choices()
	{
		// TODO: Recompute if necessary
		return maxNumDistrSets;
	}

	/**
	 * Get the maximum number of player 2 choices (distributions) in any state.
	 */
	public int getMaxNumP2Choices()
	{
		// TODO: Recompute if necessary
		return maxNumDistrs;
	}

	/**
	 * Checks for deadlocks (states with no choices) and throws an exception if any exist.
	 * States in 'except' (If non-null) are excluded from the check.
	 */
	public void checkForDeadlocks(BitSet except) throws PrismException
	{
		for (int i = 0; i < numStates; i++) {
			if (trans.get(i).isEmpty() && (except == null || !except.get(i)))
				throw new PrismException("STPG has a deadlock in state " + i);
		}
		// TODO: Check for empty distributions sets too?
	}

	/**
	 * Build (anew) from a list of transitions exported explicitly by PRISM (i.e. a .tra file).
	 */
	public void buildFromPrismExplicit(String filename) throws PrismException
	{
		BufferedReader in;
		Distribution distr;
		DistributionSet distrs;
		String s, ss[];
		int i, j, k1, k2, iLast, k1Last, k2Last, n, lineNum = 0;
		double prob;

		try {
			// Open file
			in = new BufferedReader(new FileReader(new File(filename)));
			// Parse first line to get num states
			s = in.readLine();
			lineNum = 1;
			if (s == null)
				throw new PrismException("Missing first line of .tra file");
			ss = s.split(" ");
			n = Integer.parseInt(ss[0]);
			// Initialise
			initialise(n);
			// Go though list of transitions in file
			iLast = -1;
			k1Last = -1;
			k2Last = -1;
			distrs = null;
			distr = null;
			s = in.readLine();
			lineNum++;
			while (s != null) {
				s = s.trim();
				if (s.length() > 0) {
					ss = s.split(" ");
					i = Integer.parseInt(ss[0]);
					k1 = Integer.parseInt(ss[1]);
					k2 = Integer.parseInt(ss[2]);
					j = Integer.parseInt(ss[3]);
					prob = Double.parseDouble(ss[4]);
					// For a new state or distribution set or distribution
					if (i != iLast || k1 != k1Last || k2 != k2Last) {
						// Add any previous distribution to the last set, create new one
						if (distrs != null) {
							distrs.add(distr);
						}
						distr = new Distribution();
						// Only for a new state or distribution set...
						if (i != iLast || k1 != k1Last) {
							// Add any previous distribution set to the last state, create new one
							if (distrs != null) {
								addDistributionSet(iLast, distrs);
							}
							distrs = newDistributionSet(null);
						}
					}
					// Add transition to the current distribution
					distr.add(j, prob);
					// Prepare for next iter
					iLast = i;
					k1Last = k1;
					k2Last = k2;
				}
				s = in.readLine();
				lineNum++;
			}
			// Add previous distribution to the last set
			distrs.add(distr);
			// Add previous distribution set to the last state
			addDistributionSet(iLast, distrs);
			// Close file
			in.close();
		} catch (IOException e) {
			System.out.println(e);
			System.exit(1);
		} catch (NumberFormatException e) {
			throw new PrismException("Problem in .tra file (line " + lineNum + ") for " + getModelType());
		}
		// Set initial state (assume 0)
		initialStates.add(0);
	}

	/**
	 * Do a matrix-vector multiplication followed by two min/max ops, i.e. one step of value iteration.
	 * @param vect: Vector to multiply by
	 * @param min1: Min or max for player 1 (true=min, false=max)
	 * @param min2: Min or max for player 2 (true=min, false=max)
	 * @param result: Vector to store result in
	 * @param subset: Only do multiplication for these rows
	 * @param complement: If true, 'subset' is taken to be its complement
	 */
	public void mvMultMinMax(double vect[], boolean min1, boolean min2, double result[], BitSet subset,
			boolean complement)
	{
		int s;
		// Loop depends on subset/complement arguments
		if (subset == null) {
			for (s = 0; s < numStates; s++)
				result[s] = mvMultMinMaxSingle(s, vect, min1, min2);
		} else if (complement) {
			for (s = subset.nextClearBit(0); s < numStates; s = subset.nextClearBit(s + 1))
				result[s] = mvMultMinMaxSingle(s, vect, min1, min2);
		} else {
			for (s = subset.nextSetBit(0); s >= 0; s = subset.nextSetBit(s + 1))
				result[s] = mvMultMinMaxSingle(s, vect, min1, min2);
		}
	}

	/**
	 * Do a single row of matrix-vector multiplication followed by two min/max ops.
	 * @param s: Row index
	 * @param vect: Vector to multiply by
	 * @param min1: Min or max for player 1 (true=min, false=max)
	 * @param min2: Min or max for player 2 (true=min, false=max)
	 */
	public double mvMultMinMaxSingle(int s, double vect[], boolean min1, boolean min2)
	{
		int k;
		double d, prob, minmax1, minmax2;
		boolean first1, first2;
		ArrayList<DistributionSet> step;

		minmax1 = 0;
		first1 = true;
		step = trans.get(s);
		for (DistributionSet distrs : step) {
			minmax2 = 0;
			first2 = true;
			for (Distribution distr : distrs) {
				// Compute sum for this distribution
				d = 0.0;
				for (Map.Entry<Integer, Double> e : distr) {
					k = (Integer) e.getKey();
					prob = (Double) e.getValue();
					d += prob * vect[k];
				}
				// Check whether we have exceeded min/max so far
				if (first2 || (min2 && d < minmax2) || (!min2 && d > minmax2))
					minmax2 = d;
				first2 = false;
			}
			// Check whether we have exceeded min/max so far
			if (first1 || (min1 && minmax2 < minmax1) || (!min1 && minmax2 > minmax1))
				minmax1 = minmax2;
			first1 = false;
		}

		return minmax1;
	}

	/**
	 * Determine which choices result in min/max after a single row of matrix-vector multiplication.
	 * @param s: Row index
	 * @param vect: Vector to multiply by
	 * @param min: Min or max for player 1 (true=min, false=max)
	 * @param min: Min or max for player 2 (true=min, false=max)
	 * @param val: Min or max value to match
	 */
	public List<Integer> mvMultMinMaxSingleChoices(int s, double vect[], boolean min1, boolean min2, double val)
	{
		int j, k;
		double d, prob, minmax2;
		boolean first2;
		List<Integer> res;
		ArrayList<DistributionSet> step;

		// Create data structures to store strategy
		res = new ArrayList<Integer>();
		// One row of matrix-vector operation 
		j = -1;
		step = trans.get(s);
		for (DistributionSet distrs : step) {
			j++;
			minmax2 = 0;
			first2 = true;
			for (Distribution distr : distrs) {
				// Compute sum for this distribution
				d = 0.0;
				for (Map.Entry<Integer, Double> e : distr) {
					k = (Integer) e.getKey();
					prob = (Double) e.getValue();
					d += prob * vect[k];
				}
				// Check whether we have exceeded min/max so far
				if (first2 || (min2 && d < minmax2) || (!min2 && d > minmax2))
					minmax2 = d;
				first2 = false;
			}
			// Store strategy info if value matches
			//if (PrismUtils.doublesAreClose(val, d, termCritParam, termCrit == TermCrit.ABSOLUTE)) {
			if (PrismUtils.doublesAreClose(val, minmax2, 1e-12, false)) {
				res.add(j);
				//res.add(distrs.getAction());
			}
		}

		return res;
	}

	/**
	 * Export to a dot file.
	 */
	public void exportToDotFile(String filename) throws PrismException
	{
		exportToDotFile(filename, null);
	}

	@Override
	public void exportToPrismExplicitTra(String filename) throws PrismException
	{
		int i, j, k;
		FileWriter out;
		TreeMap<Integer, Double> sorted;
		try {
			// Output transitions to .tra file
			out = new FileWriter(filename);
			out.write(numStates + " " + numDistrSets + " " + numDistrs + " " + numTransitions + "\n");
			sorted = new TreeMap<Integer, Double>();
			for (i = 0; i < numStates; i++) {
				j = -1;
				for (DistributionSet distrs : trans.get(i)) {
					j++;
					k = -1;
					for (Distribution distr : distrs) {
						k++;
						// Extract transitions and sort by destination state index (to match PRISM-exported files)
						for (Map.Entry<Integer, Double> e : distr) {
							sorted.put(e.getKey(), e.getValue());
						}
						// Print out (sorted) transitions
						for (Map.Entry<Integer, Double> e : distr) {
							// Note use of PrismUtils.formatDouble to match PRISM-exported files
							out.write(i + " " + j + " " + k + " " + e.getKey() + " "
									+ PrismUtils.formatDouble(e.getValue()) + "\n");
						}
						sorted.clear();
					}
				}
			}
			out.close();
		} catch (IOException e) {
			throw new PrismException("Could not export " + getModelType() + " to file \"" + filename + "\"" + e);
		}
	}

	/**
	 * Export to a dot file, highlighting states in 'mark'.
	 */
	public void exportToDotFile(String filename, BitSet mark) throws PrismException
	{
		int i, j, k;
		String nij, nijk;
		try {
			FileWriter out = new FileWriter(filename);
			out.write("digraph " + getModelType() + " {\nsize=\"8,5\"\nnode [shape=box];\n");
			for (i = 0; i < numStates; i++) {
				if (mark != null && mark.get(i))
					out.write(i + " [style=filled  fillcolor=\"#cccccc\"]\n");
				j = -1;
				for (DistributionSet distrs : trans.get(i)) {
					j++;
					nij = "n" + i + "_" + j;
					out.write(i + " -> " + nij + " [ arrowhead=none,label=\"" + j + "\" ];\n");
					out.write(nij + " [ shape=circle,width=0.1,height=0.1,label=\"\" ];\n");
					k = -1;
					for (Distribution distr : distrs) {
						k++;
						nijk = "n" + i + "_" + j + "_" + k;
						out.write(nij + " -> " + nijk + " [ arrowhead=none,label=\"" + k + "\" ];\n");
						out.write(nijk + " [ shape=point,label=\"\" ];\n");
						for (Map.Entry<Integer, Double> e : distr) {
							out.write(nijk + " -> " + e.getKey() + " [ label=\"" + e.getValue() + "\" ];\n");
						}
					}
				}
			}
			out.write("}\n");
			out.close();
		} catch (IOException e) {
			throw new PrismException("Could not write " + getModelType() + " to file \"" + filename + "\"" + e);
		}
	}

	/**
	 * Get string with model info/stats.
	 */
	public String infoString()
	{
		String s = "";
		s += numStates + " states";
		s += " (" + getNumInitialStates() + " initial)";
		s += ", " + numDistrSets + " distribution sets";
		s += ", " + numDistrs + " distributions";
		s += ", " + numTransitions + " transitions";
		s += ", p1max/avg = " + maxNumDistrSets + "/" + PrismUtils.formatDouble2dp(((double) numDistrSets) / numStates);
		s += ", p2max/avg = " + maxNumDistrs + "/" + PrismUtils.formatDouble2dp(((double) numDistrs) / numDistrSets);
		return s;
	}

	/**
	 * Get transition function as string.
	 */
	public String toString()
	{
		int i;
		boolean first;
		String s = "";
		first = true;
		s = "[ ";
		for (i = 0; i < numStates; i++) {
			if (first)
				first = false;
			else
				s += ", ";
			s += i + ": " + trans.get(i);
		}
		s += " ]";
		return s;
	}

	/**
	 * Equality check.
	 */
	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof STPG))
			return false;
		STPG stpg = (STPG) o;
		if (numStates != stpg.numStates)
			return false;
		if (!initialStates.equals(stpg.initialStates))
			return false;
		if (!trans.equals(stpg.trans))
			return false;
		return true;
	}
}
