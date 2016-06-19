/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.polettif.publicTransitMapping.mapping.pseudoPTRouter;

import org.matsim.pt.transitSchedule.api.TransitRouteStop;

import java.util.Collection;
import java.util.List;

/**
 * TODO doc
 */
public interface PseudoGraph {

	List<PseudoRouteStop> getLeastCostPath();

	void addEdge(int orderOfFirstStop, TransitRouteStop fromTransitRouteStop, LinkCandidate fromLinkCandidate, TransitRouteStop toTransitRouteStop, LinkCandidate toLinkCandidate, double pathTravelCost);

	void addDummyEdges(List<TransitRouteStop> transitRouteStops, Collection<LinkCandidate> firstStopLinkCandidates, Collection<LinkCandidate> lastStopLinkCandidates);

}
