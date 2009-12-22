/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */
package joshua.decoder.hypergraph;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.hypergraph.HyperGraph;

import java.util.HashMap;


/**
 * during the pruning process, many Item/Deductions may not be
 * explored at all due to the early-stop in pruning_deduction
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class HyperGraphPruning extends TrivialInsideOutside {
	HashMap<HGNode,Boolean> tbl_processed_items = new HashMap<HGNode,Boolean>();
	double best_log_prob;//viterbi unnormalized log prob in the hypergraph
	
	
	boolean fix_threshold_pruning = true;
	double THRESHOLD_GENERAL = 10;//if the merit is worse than the best_log_prob by this number, then prune
	double THRESHOLD_GLUE = 10;//if the merit is worse than the best_log_prob by this number, then prune
	double CUR_THRESHOLD_GENERAL = THRESHOLD_GENERAL;
	double CUR_THRESHOLD_GLUE = THRESHOLD_GLUE;
	double THRESHOLD_STEP_GENERAL = 1;
	double THRESHOLD_STEP_GLUE = 1;
	int num_survived_deduction = 0;
	int num_survived_item = 0;
	
	int glue_grammar_owner=0;//TODO
	

//####	
	
	public HyperGraphPruning(SymbolTable p_symbolTable, boolean fix_threshold, double threshold_general, double threshold_glue, double step_general, double step_glue){
		fix_threshold_pruning = fix_threshold;
		THRESHOLD_GENERAL = threshold_general;
		THRESHOLD_GLUE = threshold_glue;
		CUR_THRESHOLD_GENERAL = THRESHOLD_GENERAL;
		CUR_THRESHOLD_GLUE = THRESHOLD_GLUE;
		THRESHOLD_STEP_GENERAL = step_general;
		THRESHOLD_STEP_GLUE = step_glue;
		glue_grammar_owner = p_symbolTable.addTerminal(JoshuaConfiguration.begin_mono_owner);//TODO
	}
	
	
	
	
	public void clearState(){
		tbl_processed_items.clear();
		super.clearState();
	}
	

//	######################### pruning here ##############
	public void pruning_hg(HyperGraph hg) {
		runInsideOutside(hg, 1, 2, 1.0);//viterbi-max, log-semiring
		if (fix_threshold_pruning) {
			pruning_hg_real(hg);
			super.clearState();
		} else {
			throw new RuntimeException("wrong call");
		}
	}
	
	private void  pruning_hg_real(HyperGraph hg) {
		this.best_log_prob = getLogNormalizationConstant();//set the best_log_prob
		
		num_survived_deduction = 0;
		num_survived_item = 0;
		tbl_processed_items.clear(); 
		pruning_item(hg.goalNode);
		
		//clear up
		tbl_processed_items.clear();
		
		System.out.println("Item suvived ratio: "+ num_survived_item*1.0/hg.numNodes + " =  " + num_survived_item + "/" + hg.numNodes);
		System.out.println("Deduct suvived ratio: "+ num_survived_deduction*1.0/hg.numEdges + " =  " + num_survived_deduction + "/" + hg.numEdges);
	}
		
	
	private void pruning_item(HGNode it) {
		if (tbl_processed_items.containsKey(it)) return;
		tbl_processed_items.put(it,true);
		boolean should_survive = false;
		//### recursive call on each deduction
		for (int i = 0; i < it.hyperedges.size(); i++) {
			HyperEdge dt = it.hyperedges.get(i);
			boolean survived = pruning_deduction(dt, it);//deduction-specifc operation
			if (survived) {
				should_survive = true; // at least one deduction survive
			} else {
				it.hyperedges.remove(i);
				i--;
			}
		}
		//TODO: now we simply remove the pruned deductions, but in general, we may want to update the variables mainted in the item (e.g., best_deduction); this depends on the pruning method used
		
		/*by defintion: "should_surive==false" should be impossible, since if I got called, then my upper-deduction must survive, then i will survive
		* because there must be one way to reach me from lower part in order for my upper-deduction survive*/
		if (! should_survive) {
			throw new RuntimeException("item explored but does not survive");
			//TODO: since we always keep the best_deduction, this should never be true
		} else {
			num_survived_item++;
		}
	}
	
		
	//if survive, return true
	//best-deduction is always kept
	private boolean pruning_deduction(HyperEdge dt, HGNode parent) {
		//TODO: theoretically, if an item is get called, then its best deduction should always be kept even just by the threshold-checling. In reality, due to precision of Double, the threshold-checking may not be perfect
		if (dt != parent.bestHyperedge) { // best deduction should always survive if the Item is get called
			//### prune?
			if (should_prune_deduction(dt, parent)) {
				return false; // early stop
			}
		}
		
		//### still survive, recursive call all my ant-items
		if (null != dt.getAntNodes()) {
			for (HGNode ant_it : dt.getAntNodes()) {
				pruning_item(ant_it); // recursive call on each ant item, note: the ant_it will not be pruned as I need it
			}
		}
		
		//### if get to here, then survive; remember: if I survive, then my upper-item must survive
		num_survived_deduction++;
		return true; // survive
	}
	
	private boolean should_prune_deduction(HyperEdge dt, HGNode parent) {
		//### get merit
		double post_log_prob = get_deduction_unnormalized_posterior_log_prob(dt, parent);
		
		//sanity check
		//if (merit > worst_cost || merit < best_cost) {
		//throw new RuntimeException("merit is not between best and worst, best: " + best_cost +"; worset:" + worst_cost + "; merit: " + merit);
		//}
		
		if (dt.getRule() != null
		&& dt.getRule().getOwner() == glue_grammar_owner
		&& dt.getRule().getArity() == 2) { // specicial rule: S->S X
			//TODO
			return (post_log_prob - this.best_log_prob < CUR_THRESHOLD_GLUE);
		} else {
			return (post_log_prob - this.best_log_prob < CUR_THRESHOLD_GENERAL);
		}
	}
}
