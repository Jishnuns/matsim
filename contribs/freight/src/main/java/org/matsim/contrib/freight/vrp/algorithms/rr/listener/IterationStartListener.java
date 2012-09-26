package org.matsim.contrib.freight.vrp.algorithms.rr.listener;

import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateSolution;

public interface IterationStartListener extends RuinAndRecreateListener {

	public void informIterationStarts(int iteration,
			RuinAndRecreateSolution currentSolution);

}
