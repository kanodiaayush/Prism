//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

package simulator;

import org.jfree.data.xy.XYDataItem;

import parser.State;
import parser.ast.ModulesFile;
import parser.type.TypeDouble;
import prism.PrismException;
import userinterface.graph.Graph;
import userinterface.graph.Graph.SeriesKey;

/**
 * Class to display a simulation path in text form, sending to a PrismLog.
 */
public class PathToGraph extends PathDisplayer
{
	/** Graph on which to plot path */
	private Graph graphModel = null;
	private SeriesKey seriesKeys[] = null;

	// Model info
	private ModulesFile modulesFile;
	private int numVars;
	private int numRewardStructs;

	// Displayer state
	/** Step counter */
	private double lastTime;
	private State lastState;

	/**
	 * Construct a {@link PathToGraph} object
	 * @param graphModel Graph on which to plot path
	 * @param modulesFile Model associated with path
	 */
	public PathToGraph(Graph graphModel, ModulesFile modulesFile)
	{
		this.graphModel = graphModel;
		this.modulesFile = modulesFile;

		// Get model info
		numVars = modulesFile.getNumVars();
		numRewardStructs = modulesFile.getNumRewardStructs();
	}

	// Display methods

	@Override
	public void startDisplay(State initialState, double[] stateRewards)
	{
		// Configure axes
		graphModel.getXAxisSettings().setHeading("Time");
		graphModel.getYAxisSettings().setHeading("Value");

		// Create series
		seriesKeys = new SeriesKey[numVars];
		for (int j = 0; j < numVars; j++) {
			seriesKeys[j] = graphModel.addSeries(modulesFile.getVarName(j));
		}

		// Display initial state
		lastState = new State(initialState.varValues.length);
		displayState(0.0, initialState, true);
	}

	@Override
	public void displayStep(double timeSpent, double timeCumul, Object action, double[] transitionRewards, State newState, double[] newStateRewards)
	{
		displayState(timeCumul, newState, !showChangesOnly);
	}

	@Override
	public void displaySnapshot(double timeCumul, State newState, double[] newStateRewards)
	{
		displayState(timeCumul, newState, true);
	}

	private void displayState(double time, State state, boolean force)
	{
		for (int j = 0; j < numVars; j++) {
			Object val = state.varValues[j];
			double d = 0.0;
			if (force || !val.equals(lastState.varValues[j])) {
				try {
					d = TypeDouble.getInstance().castValueTo(val).doubleValue();
					graphModel.addPointToSeries(seriesKeys[j], new XYDataItem(time, d));
				} catch (PrismException e) {
					if (val instanceof Boolean) {
						d = ((Boolean) val).booleanValue() ? 1.0 : 0.0;
						graphModel.addPointToSeries(seriesKeys[j], new XYDataItem(time, d));
					}
				}
			}
		}
		lastTime = time;
		lastState.copy(state);
	}

	@Override
	public void endDisplay()
	{
		// Always display last points to ensure complete plot lines
		// (it's OK to overwrite points)
		displayState(lastTime, lastState, true);
	}
}
