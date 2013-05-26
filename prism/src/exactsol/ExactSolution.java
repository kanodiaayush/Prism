package exactsol;

import java.util.BitSet;
import java.util.Map;

import explicit.MDP;
import external.glpk.SWIGTYPE_p_p_uint8_t;
import external.glpk.glpk;

public class ExactSolution {
	private boolean min;
	private BitSet yes;
	private BitSet no;
	private BitSet unknown;
	private BitSet statesToObtain;
	private Map<Integer, Integer> stateToMaybe;
	private SWIGTYPE_p_p_uint8_t nums;
	private SWIGTYPE_p_p_uint8_t dens;
	
	public String getStateNumerator(int state) {
		if (yes.get(state)) {
			return "1";
		}
		if (no.get(state)) {
			return "0";
		}
		return glpk.uint8_t_array2toString(glpk.uint8t_arrayarray_get(nums, stateToMaybe.get(state)-1));
	}
	
	public String getStateDenominator(int state) {
		if (yes.get(state) || no.get(state)) {
			return "1";
		}
		return glpk.uint8_t_array2toString(glpk.uint8t_arrayarray_get(dens, stateToMaybe.get(state)-1));
	}
	
	public ExactSolution(boolean min, BitSet yes, BitSet no,
			BitSet unknown, BitSet statesToObtain, Map<Integer,Integer> stateToMaybe,
			SWIGTYPE_p_p_uint8_t nums, SWIGTYPE_p_p_uint8_t dens) {
		this.yes = yes;
		this.no = no;
		this.unknown = unknown;
		this.statesToObtain = statesToObtain;
		this.stateToMaybe = stateToMaybe;
		this.nums = nums;
		this.dens = dens;
	}
}
