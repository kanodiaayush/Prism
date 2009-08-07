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
 * Explicit representation of Markov decision process (MDP).
 */
public class MDP extends Model
{
	// Model type
	public static ModelType modelType = ModelType.MDP;

	// Transition function (Steps)
	public List<ArrayList<Distribution>> steps;

	// Rewards
	private List<List<Double>> transRewards;
	private Double transRewardsConstant;

	// Flag: allow dupes in distribution sets?
	public boolean allowDupes = false;

	// Other statistics
	public int numDistrs;
	public int numTransitions;
	public int maxNumDistrs;
	public boolean maxNumDistrsOk;

	// TODO: add accessor for maxNumDistrs that recomputes if necessary 

	/**
	 * Constructor: empty MDP.
	 */
	public MDP()
	{
		initialise(0);
	}

	/**
	 * Constructor: new MDP with fixed number of states.
	 */
	public MDP(int numStates)
	{
		initialise(numStates);
	}

	/**
	 * Initialise: new model with fixed number of states.
	 */
	public void initialise(int numStates)
	{
		super.initialise(numStates);
		numDistrs = numTransitions = maxNumDistrs = 0;
		maxNumDistrsOk = true;
		steps = new ArrayList<ArrayList<Distribution>>(numStates);
		for (int i = 0; i < numStates; i++) {
			steps.add(new ArrayList<Distribution>());
		}
		clearAllRewards();
	}

	/**
	 * Clear all information for a state (i.e. remove all transitions).
	 */
	public void clearState(int s)
	{
		// Do nothing if state does not exist
		if (s >= numStates || s < 0)
			return;
		// Clear data structures and update stats
		List<Distribution> list = steps.get(s);
		numDistrs -= list.size();
		for (Distribution distr : list) {
			numTransitions -= distr.size();
		}
		//TODO: recompute maxNumDistrs (reset maxNumDistrsOk flag)
		steps.get(s).clear();
		if (transRewards != null && transRewards.get(s) != null)
			transRewards.get(s).clear();
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
			steps.add(new ArrayList<Distribution>());
			if (transRewards != null)
				transRewards.add(null);
			numStates++;
		}
	}

	/**
	 * Add distribution 'distr' to state s (which must exist).
	 * Distribution is only actually added if it does not already exists for state s.
	 * (Assuming 'allowDupes' flag is not enabled.)
	 * Returns the index of the (existing or newly added) distribution.
	 * Returns -1 in case of error.
	 */
	public int addDistribution(int s, Distribution distr) throws PrismException
	{
		ArrayList<Distribution> set;
		// Check state exists
		if (s >= numStates || s < 0)
			return -1;
		// Add distribution (if new)
		set = steps.get(s);
		if (!allowDupes) {
			int i = set.indexOf(distr);
			if (i != -1)
				return i;
		}
		set.add(distr);
		// Add zero reward if necessary
		if (transRewards != null && transRewards.get(s) != null)
			transRewards.get(s).add(0.0);
		// Update stats
		numDistrs++;
		maxNumDistrs = Math.max(maxNumDistrs, set.size());
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
			int n = steps.get(s).size();
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
	 * Get the number of nondeterministic choices in state s.
	 */
	public int getNumChoices(int s)
	{
		return steps.get(s).size();
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
		for (Distribution distr : steps.get(s1)) {
			if (distr.contains(s2))
				return true;
		}
		return false;
	}
	
	/**
	 * Checks for deadlocks (states with no choices) and throws an exception if any exist.
	 * States in 'except' (If non-null) are excluded from the check.
	 */
	public void checkForDeadlocks(BitSet except) throws PrismException
	{
		for (int i = 0; i < numStates; i++) {
			if (steps.get(i).isEmpty() && (except == null || !except.get(i)))
				throw new PrismException("MDP has a deadlock in state " + i);
		}
		// TODO: Check for empty distributions too?
	}
	
	/**
	 * Build (anew) from a list of transitions exported explicitly by PRISM (i.e. a .tra file).
	 */
	public void buildFromPrismExplicit(String filename) throws PrismException
	{
		BufferedReader in;
		Distribution distr;
		String s, ss[];
		int i, j, k, iLast, kLast, n;
		double prob;

		try {
			// Open file
			in = new BufferedReader(new FileReader(new File(filename)));
			// Parse first line to get num states
			s = in.readLine();
			if (s == null)
				throw new PrismException("Missing first line of .tra file");
			ss = s.split(" ");
			n = Integer.parseInt(ss[0]);
			// Initialise
			initialise(n);
			// Go though list of transitions in file
			iLast = -1;
			kLast = -1;
			distr = null;
			s = in.readLine();
			while (s != null) {
				ss = s.split(" ");
				i = Integer.parseInt(ss[0]);
				k = Integer.parseInt(ss[1]);
				j = Integer.parseInt(ss[2]);
				prob = Double.parseDouble(ss[3]);
				// For a new state or distribution
				if (i != iLast || k != kLast) {
					// Add any previous distribution to the last state, create new one
					if (distr != null) {
						addDistribution(iLast, distr);
					}
					distr = new Distribution();
				}
				// Add transition to the current distribution
				distr.add(j, prob);
				// Prepare for next iter
				iLast = i;
				kLast = k;
				s = in.readLine();
			}
			// Add previous distribution to the last state
			addDistribution(iLast, distr);
			// Close file
			in.close();
		} catch (IOException e) {
			System.out.println(e);
			System.exit(1);
		} catch (NumberFormatException e) {
			throw new PrismException("Problem in .tra file for " + modelType);
		}
		// Set initial state (assume 0)
		initialStates.add(0);
	}

	/**
	 * Do a matrix-vector multiplication followed by min/max, i.e. one step of value iteration.
	 * @param vect: Vector to multiply by
	 * @param min: Min or max for (true=min, false=max)
	 * @param result: Vector to store result in
	 * @param subset: Only do multiplication for these rows
	 * @param complement: If true, 'subset' is taken to be its complement
	 */
	public void mvMultMinMax(double vect[], boolean min, double result[], BitSet subset, boolean complement)
	{
		int s = -1;
		while (s < numStates) {
			// Pick next state
			s = (subset == null) ? s + 1 : complement ? subset.nextClearBit(s + 1) : subset.nextSetBit(s + 1);
			if (s < 0)
				break;
			// Do operation
			result[s] = mvMultMinMaxSingle(s, vect, min);
		}
	}

	/**
	 * Do a single row of matrix-vector multiplication followed by min/max.
	 * @param s: Row index
	 * @param vect: Vector to multiply by
	 * @param min: Min or max for (true=min, false=max)
	 */
	public double mvMultMinMaxSingle(int s, double vect[], boolean min)
	{
		int k;
		double d, prob, minmax;
		boolean first;
		ArrayList<Distribution> step;

		minmax = 0;
		first = true;
		step = steps.get(s);
		for (Distribution distr : step) {
			// Compute sum for this distribution
			d = 0.0;
			for (Map.Entry<Integer, Double> e : distr) {
				k = (Integer) e.getKey();
				prob = (Double) e.getValue();
				d += prob * vect[k];
			}
			// Check whether we have exceeded min/max so far
			if (first || (min && d < minmax) || (!min && d > minmax))
				minmax = d;
			first = false;
		}

		return minmax;
	}

	/**
	 * Determine which choices result in min/max after a single row of matrix-vector multiplication.
	 * @param s: Row index
	 * @param vect: Vector to multiply by
	 * @param min: Min or max (true=min, false=max)
	 * @param val: Min or max value to match
	 */
	public List<Integer> mvMultMinMaxSingleChoices(int s, double vect[], boolean min, double val)
	{
		int j, k;
		double d, prob;
		List<Integer> res;
		ArrayList<Distribution> step;

		// Create data structures to store strategy
		res = new ArrayList<Integer>();
		// One row of matrix-vector operation 
		j = -1;
		step = steps.get(s);
		for (Distribution distr : step) {
			j++;
			// Compute sum for this distribution
			d = 0.0;
			for (Map.Entry<Integer, Double> e : distr) {
				k = (Integer) e.getKey();
				prob = (Double) e.getValue();
				d += prob * vect[k];
			}
			// Store strategy info if value matches
			//if (PrismUtils.doublesAreClose(val, d, termCritParam, termCrit == TermCrit.ABSOLUTE)) {
			if (PrismUtils.doublesAreClose(val, d, 1e-12, false)) {
				res.add(j);
				//res.add(distrs.getAction());
			}
		}

		return res;
	}

	/**
	 * Do a matrix-vector multiplication and sum of action reward followed by min/max, i.e. one step of value iteration.
	 * @param vect: Vector to multiply by
	 * @param min: Min or max for (true=min, false=max)
	 * @param result: Vector to store result in
	 * @param subset: Only do multiplication for these rows
	 * @param complement: If true, 'subset' is taken to be its complement
	 */
	public void mvMultRewMinMax(double vect[], boolean min, double result[], BitSet subset, boolean complement)
	{
		int s = -1;
		while (s < numStates) {
			// Pick next state
			s = (subset == null) ? s + 1 : complement ? subset.nextClearBit(s + 1) : subset.nextSetBit(s + 1);
			if (s < 0)
				break;
			// Do operation
			result[s] = mvMultRewMinMaxSingle(s, vect, min);
		}
	}

	/**
	 * Do a single row of matrix-vector multiplication and sum of action reward followed by min/max.
	 * @param s: Row index
	 * @param vect: Vector to multiply by
	 * @param min: Min or max for (true=min, false=max)
	 */
	public double mvMultRewMinMaxSingle(int s, double vect[], boolean min)
	{
		int j, k;
		double d, prob, minmax;
		boolean first;
		ArrayList<Distribution> step;

		minmax = 0;
		first = true;
		j = -1;
		step = steps.get(s);
		for (Distribution distr : step) {
			j++;
			// Compute sum for this distribution
			d = getTransitionReward(s, j);
			for (Map.Entry<Integer, Double> e : distr) {
				k = (Integer) e.getKey();
				prob = (Double) e.getValue();
				d += prob * vect[k];
			}
			// Check whether we have exceeded min/max so far
			if (first || (min && d < minmax) || (!min && d > minmax))
				minmax = d;
			first = false;
		}

		return minmax;
	}

	/**
	 * Determine which choices result in min/max after a single row of matrix-vector multiplication and sum of action reward.
	 * @param s: Row index
	 * @param vect: Vector to multiply by
	 * @param min: Min or max (true=min, false=max)
	 * @param val: Min or max value to match
	 */
	public List<Integer> mvMultRewMinMaxSingleChoices(int s, double vect[], boolean min, double val)
	{
		int j, k;
		double d, prob;
		List<Integer> res;
		ArrayList<Distribution> step;

		// Create data structures to store strategy
		res = new ArrayList<Integer>();
		// One row of matrix-vector operation 
		j = -1;
		step = steps.get(s);
		for (Distribution distr : step) {
			j++;
			// Compute sum for this distribution
			d = getTransitionReward(s, j);
			for (Map.Entry<Integer, Double> e : distr) {
				k = (Integer) e.getKey();
				prob = (Double) e.getValue();
				d += prob * vect[k];
			}
			// Store strategy info if value matches
			//if (PrismUtils.doublesAreClose(val, d, termCritParam, termCrit == TermCrit.ABSOLUTE)) {
			if (PrismUtils.doublesAreClose(val, d, 1e-12, false)) {
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

	/**
	 * Export to explicit format readable by PRISM (i.e. a .tra file, etc.).
	 */
	public void exportToPrismExplicit(String baseFilename) throws PrismException
	{
		int i, j;
		String filename = null;
		FileWriter out;
		try {
			// Output transitions to .tra file
			filename = baseFilename + ".tra";
			out = new FileWriter(filename);
			out.write(numStates + " " + numDistrs + " " + numTransitions + "\n");
			for (i = 0; i < numStates; i++) {
				j = -1;
				for (Distribution distr : steps.get(i)) {
					j++;
					for (Map.Entry<Integer, Double> e : distr) {
						out.write(i + " " + j + " " + e.getKey() + " " + e.getValue() + "\n");
					}
				}
			}
			out.close();
			// Output transition rewards to .trew file
			// TODO
			filename = baseFilename + ".trew";
			out = new FileWriter(filename);
			out.write(numStates + " " + "?" + " " + "?" + "\n");
			for (i = 0; i < numStates; i++) {
				j = -1;
				for (Distribution distr : steps.get(i)) {
					j++;
					for (Map.Entry<Integer, Double> e : distr) {
						out.write(i + " " + j + " " + e.getKey() + " " + "1.0" + "\n");
					}
				}
			}
			out.close();
		} catch (IOException e) {
			throw new PrismException("Could not export " + modelType + " to file \"" + filename + "\"" + e);
		}
	}
	
	/**
	 * Export to a dot file, highlighting states in 'mark'.
	 */
	public void exportToDotFile(String filename, BitSet mark) throws PrismException
	{
		int i, j;
		try {
			FileWriter out = new FileWriter(filename);
			out.write("digraph " + modelType + " {\nsize=\"8,5\"\nnode [shape=box];\n");
			for (i = 0; i < numStates; i++) {
				if (mark != null && mark.get(i))
					out.write(i + " [style=filled  fillcolor=\"#cccccc\"]\n");
				j = -1;
				for (Distribution distr : steps.get(i)) {
					j++;
					for (Map.Entry<Integer, Double> e : distr) {
						out.write(i + " -> " + e.getKey() + " [ label=\"");
						out.write(j + ":" + e.getValue() + "\" ];\n");
					}
				}
			}
			out.write("}\n");
			out.close();
		} catch (IOException e) {
			throw new PrismException("Could not write " + modelType + " to file \"" + filename + "\"" + e);
		}
	}

	/**
	 * Get string with model info/stats.
	 */
	public String infoString()
	{
		String s = "";
		s += numStates + " states";
		s += ", " + numDistrs + " distributions";
		s += ", " + numTransitions + " transitions";
		s += ", dist max/avg = " + maxNumDistrs + "/" + PrismUtils.formatDouble2dp(((double) numDistrs) / numStates);
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
			s += i + ": " + steps.get(i) + transRewards.get(i);
		}
		s += " ]";
		return s;
	}

	/**
	 * Equality check.
	 */
	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof MDP))
			return false;
		MDP mdp = (MDP) o;
		if (numStates != mdp.numStates)
			return false;
		if (!initialStates.equals(mdp.initialStates))
			return false;
		if (!steps.equals(mdp.steps))
			return false;
		// TODO: compare rewards (complicated: null = 0,0,0,0)
		return true;
	}
}