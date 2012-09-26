/*******************************************************************************
 * Copyright (c) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
 ******************************************************************************/
package org.matsim.contrib.freight.vrp.basics;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class VehicleRoutingProblemImpl implements VehicleRoutingProblem {

	private static Logger logger = Logger
			.getLogger(VehicleRoutingProblemImpl.class);

	private VehicleRoutingCosts costs;

	private Map<String, Job> jobs;

	private Collection<Vehicle> vehicles;

	public VehicleRoutingProblemImpl(Collection<? extends Job> jobs,
			Collection<Vehicle> vehicles, VehicleRoutingCosts costs) {
		this.jobs = new HashMap<String, Job>();
		mapJobs(jobs);
		this.vehicles = vehicles;
		this.costs = costs;
	}

	private void mapJobs(Collection<? extends Job> jobs) {
		for (Job j : jobs) {
			this.jobs.put(j.getId(), j);
		}
	}

	@Override
	public Map<String, Job> getJobs() {
		return jobs;
	}

	@Override
	public Collection<Vehicle> getVehicles() {
		return vehicles;
	}

	@Override
	public VehicleRoutingCosts getCosts() {
		return costs;
	}
}
