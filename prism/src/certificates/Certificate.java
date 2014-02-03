package certificates;

import java.util.Iterator;

import prism.PrismLog;
import exactsol.ExactSolution;
import explicit.MDP;

public class Certificate {
	private final MDP mdp;
	private final Prob0Info prob0Info;
	private final ExactSolution exactSolution;

	public Certificate(MDP mdp, Prob0Info prob0Info,
			ExactSolution exactSolution) {
		this.mdp = mdp;
		this.prob0Info = prob0Info;
		this.exactSolution = exactSolution;
	}

	public void print(PrismLog log) {
		printBoilerPlate(log);
		printProbArray(log);
		printIndexMap(log);
	}
	
	private void printIndexMap(PrismLog log) {
		Iterator<Integer> i = prob0Info.iterator();
		log.print("let indexMap = assoc_list2hashtbl [ ");
		int indexCount=0;
		while(i.hasNext()) {
			int s = i.next();
			printState(log, s);
			log.print(", ");
			log.print(indexCount++);
			log.print(";\n");
		}
		log.print("];;\n");
	}

	private void printProbArray(PrismLog log) {
		Iterator<Integer> i = prob0Info.iterator();
		log.print("let probArray = [| ");
		
		while(i.hasNext()) {
			int s = i.next();
			printState(log, s);
			log.print(", ");
			log.print("\"" +
					exactSolution.getStateNumerator(s) +
					"\", \""+ exactSolution.getStateDenominator(s) +
					"\";\n");
		}
		
		log.print("|];;\n");
	}

	private void printState(PrismLog log, int s) {
		log.print("{ ");
		for(int i = 0; i < mdp.getVarNames().size(); i++) {
			log.print(mdp.getVarNames().get(i) + " = " +
					mdp.getStatesList().get(s).varValues[i] +
					"; ");
		}
		log.print("}");
	}

	private void printBoilerPlate(PrismLog log) {
		printDefinitionState(log);
		printDefinitionHashFunction(log);
	}

	private void printDefinitionHashFunction(PrismLog log) {
		log.print("let assoc_list2hashtbl assoc_list = \n" +              
                  "let h = Hashtbl.create 0 in \n" +
                  "List.iter (fun (k,v) -> Hashtbl.replace h k v) assoc_list ; \n" +
                  "h;;\n");
	}

	private void printDefinitionState(PrismLog log) {
		log.print("type state = { ");
		for(String var : mdp.getVarNames()) {
			log.print(var + ": int; ");
		}
		log.print("}\n");
	}
}
