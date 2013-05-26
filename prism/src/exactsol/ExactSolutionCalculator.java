package exactsol;


import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import prism.PrismLog;
import explicit.MDP;
import external.glpk.*;

public class ExactSolutionCalculator {

	private PrismLog mainLog;
	
	protected PrismLog getMainLog() {
		return mainLog;
	}
	
	public ExactSolutionCalculator(PrismLog mainLog) {
		this.mainLog = mainLog;
	}

	public ExactSolution computeExactSol(MDP mdp, boolean min, BitSet yes, BitSet no, BitSet unknown,
			int[] adv, BitSet statesToObtain) {
		System.loadLibrary("glpk");
		long timer_exact_sol = System.currentTimeMillis();
		ExactSolution exactSol = solve_dual_lp_problem(mdp, min, yes, no, unknown, adv, statesToObtain);
		getMainLog().println("Exact solution time: " + ((double)System.currentTimeMillis()-timer_exact_sol)/1000 + " seconds");
		return exactSol;
	}
	
	ExactSolution solve_dual_lp_problem(MDP model, boolean min, BitSet yesStates, BitSet no, BitSet maybe, int adv[],
			BitSet statesToObtain) {
		glp_prob lp = glpk.glp_create_prob();
		int maybeTrans=0;

		/*if(filter != null) {
			initialStatesSet = new ArrayList<Integer>();
			for(int s = filter.nextSetBit(0); s >= 0; s = filter.nextSetBit(s+1)) {
				((ArrayList<Integer>)initialStatesSet).add(s);
			}
		}
		else {
			initialStatesSet = model.getInitialStates();
		}*/
		
		HashMap<Integer,Integer> maybeToState = new HashMap<Integer,Integer>();
		HashMap<Integer,Integer> stateToMaybe = new HashMap<Integer,Integer>();
		int maybeStateCount = 1;
		long maybeCreateTimer = System.currentTimeMillis();
		for(int s = maybe.nextSetBit(0); s >= 0; s = maybe.nextSetBit(s+1)) {
			maybeToState.put(maybeStateCount, s);
			stateToMaybe.put(s, maybeStateCount);
			maybeTrans+=model.getNumChoices(s);
			maybeStateCount++;
		}
		getMainLog().println("Maybe construction time: " + ((double)System.currentTimeMillis()-maybeCreateTimer)/1000 + " seconds");
		
		long lp_const_timer = System.currentTimeMillis();
		glpk.glp_add_rows(lp, maybe.cardinality());
		glpk.glp_add_cols(lp, maybeTrans);

		if(min) {
			glpk.glp_set_obj_dir(lp, glpkConstants.GLP_MAX);
		}
		else {
			glpk.glp_set_obj_dir(lp, glpkConstants.GLP_MIN);
		}

		SWIGTYPE_p_int colIndices = glpk.int_array(maybe.cardinality()+1);
		SWIGTYPE_p_double colValues = glpk.double_array(maybe.cardinality()+1);
		glpk.int_array_set(colIndices, 0, 0);
		glpk.double_array_set(colValues, 0, 0);
		for(int i = 1; i <= maybe.cardinality(); i++) {
			if(min) {
				glpk.glp_set_row_bnds(lp, i, glpkConstants.GLP_UP, 0, -1);
			}
			else {
				glpk.glp_set_row_bnds(lp, i, glpkConstants.GLP_LO, -1, 0);					
			}	
			glpk.glp_set_row_stat(lp, i, glpkConstants.GLP_NL);
			glpk.int_array_set(colIndices, i, 0);
			glpk.double_array_set(colValues, i, 0);
		}
		
		int tranCount = 1;
		
		Set<Integer> basicCols = new HashSet<Integer>();
		
		for(int s = maybe.nextSetBit(0); s >= 0; s = maybe.nextSetBit(s+1)) {
			for(int choiceCount = 0; choiceCount < model.getNumChoices(s); choiceCount++) {
				int arraysCount=1;
				double reachProb = 0;
				Iterator<Map.Entry<Integer, Double>> transitionIterator = model.getTransitionsIterator(s, choiceCount);
				boolean pointsToItself = false;
				while(transitionIterator.hasNext()) {
					Entry<Integer, Double> entry = transitionIterator.next();
					// t is in the support of d
					int t = entry.getKey();
					if (entry.getValue() == 0)
						continue;
					if(maybe.get(t)) {
						// Equations are of the form xs >= r + cs xs + ct xt + ...
						// <=> 0 >= r + (cs-1) xs + ct xt + ...
						if(t==s)
							pointsToItself = true;
						double lpVal = (t == s) ? (entry.getValue()-1) : entry.getValue();
						glpk.int_array_set(colIndices, arraysCount, stateToMaybe.get(t));
						glpk.double_array_set(colValues, arraysCount, lpVal);
						arraysCount++;
					}
					else if(yesStates.get(t)) {
						reachProb += entry.getValue();
					}
				}
				if (!pointsToItself) {
					// Put -1 corresponding to -1 in the matrix:
					// the code (entry.getValue()-1) was not executed before
					glpk.int_array_set(colIndices, arraysCount, stateToMaybe.get(s));
					glpk.double_array_set(colValues, arraysCount, -1);
					arraysCount++;
				}
			
				glpk.glp_set_mat_col(lp, tranCount, arraysCount-1, colIndices, colValues);
								
				if(choiceCount == adv[s]) {
					basicCols.add(tranCount);
				}
				glpk.glp_set_col_bnds(lp, tranCount, glpkConstants.GLP_LO, 0, 0);
				glpk.glp_set_obj_coef(lp, tranCount, -reachProb);
				tranCount++;
			}
		}
		getMainLog().println("Dual LP construction time: " + ((double)System.currentTimeMillis()-lp_const_timer)/1000 + " seconds");
		
		long basis_timer = System.currentTimeMillis();
		
		// The last one used was tranCount-1
		for(int i = 1; i <= tranCount-1; i++) {
			if(basicCols.contains(i)) {
				glpk.glp_set_col_stat(lp, i, glpkConstants.GLP_BS);
			}
			else {
				glpk.glp_set_col_stat(lp, i, glpkConstants.GLP_NL);				
			}
		}
		getMainLog().println("Basis construction time: " + ((double)System.currentTimeMillis()-basis_timer)/1000 + " seconds");
		

		/*long unex_sol_timer = System.currentTimeMillis();
		glpk.glp_simplex(lp, null);
		double unex = ((double)System.currentTimeMillis()-unex_sol_timer)/1000;*/

		glp_exact_sol_parm esol_parm = new glp_exact_sol_parm();
		SWIGTYPE_p_uint8_t num = glpk.uint8_t_array(1024), den = glpk.uint8_t_array(1024);
		
		esol_parm.setNum_variables(0);
		esol_parm.setDual_num_variables(statesToObtain.cardinality());
		esol_parm.setDual_variables(glpk.int_array(statesToObtain.cardinality()));
		esol_parm.setDual_vars_dens(glpk.uint8_t_arrayarray(statesToObtain.cardinality()));
		esol_parm.setDual_vars_nums(glpk.uint8_t_arrayarray(statesToObtain.cardinality()));
		esol_parm.setSizes(1024);
		esol_parm.setSolution_num(num);
		esol_parm.setSolution_den(den);
		int countInit = 0;
		for (int s = statesToObtain.nextSetBit(0); s >= 0; s = statesToObtain.nextSetBit(s+1)) {
			if(maybe.get(s)) {
				int k = stateToMaybe.get(s);
				glpk.int_array_set(esol_parm.getDual_variables(),countInit,k);
				glpk.uint8t_arrayarray_set(esol_parm.getDual_vars_nums(), countInit, glpk.uint8_t_array(1024));
				glpk.uint8t_arrayarray_set(esol_parm.getDual_vars_dens(), countInit, glpk.uint8_t_array(1024));
				countInit++;
			}
			else if(yesStates.get(s)) {
				getMainLog().println("" + model.getStatesList().get(s).toString() + "is a YES state, exact probability from this state is 1");
			}
			else if(no.get(s)) {
				getMainLog().println("" + model.getStatesList().get(s).toString() + "is a NO state, exact probability from this state is 0");
			}
			else {
				getMainLog().printWarning("State " + model.getStatesList().get(s).toString() + " is neither maybe nor YES nor NOT");
			}
		}
		
		for(int i = 0; i < esol_parm.getDual_num_variables(); i++) {
			getMainLog().println("Value of the variable to be retrieved: " + glpk.int_array_get(esol_parm.getDual_variables(),i));
		}
		
		
		long exact_sol_timer = System.currentTimeMillis();
		int ret = glpk.glp_exact_sol(lp, null, esol_parm);
		
		/*int basicVariables = 0;
		for(int i = 1; i <= glpk.glp_get_num_cols(lp); i++) {
			if(glpk.glp_get_col_stat(lp, i) == glpkConstants.GLP_BS) {
				basicVariables++;
			}
		}
		
		for(int i = 1; i <= glpk.glp_get_num_rows(lp); i++) {
			if(glpk.glp_get_row_stat(lp, i) == glpkConstants.GLP_BS) {
				basicVariables++;
			}
		}
		
		System.out.println("Maybe trans: " + maybeTrans + ". Maybe: " + maybe.cardinality() + ". Basic cols: " + basicCols.size());
		System.out.println("Balance: " + (maybeTrans - maybe.cardinality() - basicCols.size()) );
		System.out.println("Rows in the system: " + glpk.glp_get_num_rows(lp));
		System.out.println("Cols in the system: " + glpk.glp_get_num_cols(lp));

		System.out.println("Basic variables: " + basicVariables);*/
		
		
		if(ret == glpkConstants.GLP_EBADB) {
			System.out.println("The cardinality of the basis is invalid");
		}
		else if (ret == glpkConstants.GLP_ESING) {
			System.out.println("Ooooops, the alledged basis is singular");
		}
		else if (ret != 0) {
			System.out.println("glp_exact_sol returned with an error");
		}
		
		getMainLog().println("Transitions in maybe states: " + maybeTrans);
		
		for(int i = 0; i < countInit; i++) {
			System.out.println(i+"State numerator: " + glpk.uint8_t_array2toString(glpk.uint8t_arrayarray_get(esol_parm.getDual_vars_nums(), i)));
			System.out.println(i+"State denominator: " + glpk.uint8_t_array2toString(glpk.uint8t_arrayarray_get(esol_parm.getDual_vars_dens(), i)));			
		}
		
		getMainLog().println("Exact solver time: " + ((double)System.currentTimeMillis()-exact_sol_timer)/1000 + " seconds");
		
		//System.out.println("Inexact LP took: " + unex + " seconds");
		return new ExactSolution(min, yesStates, no, maybe, statesToObtain, stateToMaybe,
				esol_parm.getDual_vars_nums(), esol_parm.getDual_vars_dens());
	}
	
}
