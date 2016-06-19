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

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.publicTransitMapping.config.PublicTransitMappingConfigGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * TODO doc
 */
public class PseudoScheduleImpl implements PseudoSchedule {

	private Set<PseudoRoute> pseudoSchedule = new HashSet<>();

	@Override
	public void addPseudoRoute(TransitLine transitLine, TransitRoute transitRoute, List<PseudoRouteStop> pseudoStopSequence) {
		pseudoSchedule.add(new PseudoRouteImpl(transitLine, transitRoute, pseudoStopSequence));
	}

	@Override
	public Set<PseudoRoute> getPseudoRoutes() {
		return pseudoSchedule;
	}

	@Override
	public void mergePseudoSchedule(PseudoSchedule otherPseudoSchedule) {
		pseudoSchedule.addAll(otherPseudoSchedule.getPseudoRoutes());
	}

	@Override
	public void createAndReplaceFacilities(TransitSchedule schedule) {
		TransitScheduleFactory scheduleFactory = schedule.getFactory();
		List<Tuple<Id<TransitLine>, TransitRoute>> newRoutes = new ArrayList<>();

		for(PseudoRoute pseudoRoute : pseudoSchedule) {
			List<PseudoRouteStop> pseudoStopSequence = pseudoRoute.getPseudoStops();
			List<TransitRouteStop> newStopSequence = new ArrayList<>();

			for(PseudoRouteStop pseudoStop : pseudoStopSequence) {
				String idStr = pseudoStop.getParentStopFacilityId() + PublicTransitMappingConfigGroup.SUFFIX_CHILD_STOP_FACILITIES + pseudoStop.getLinkIdStr();
				Id<TransitStopFacility> childStopFacilityId = Id.create(idStr, TransitStopFacility.class);

				// if child stop facility for this link has not yet been generated
				if(!schedule.getFacilities().containsKey(childStopFacilityId)) {
					TransitStopFacility newFacility = scheduleFactory.createTransitStopFacility(
							Id.create(childStopFacilityId, TransitStopFacility.class),
							pseudoStop.getCoord(),
							pseudoStop.isBlockingLane()
					);
					newFacility.setLinkId(Id.createLinkId(pseudoStop.getLinkIdStr()));
					newFacility.setName(pseudoStop.getFacilityName());
					newFacility.setStopPostAreaId(pseudoStop.getStopPostAreaId());
					schedule.addStopFacility(newFacility);
				}

				// create new TransitRouteStop and add it to the newStopSequence
				TransitRouteStop newTransitRouteStop = scheduleFactory.createTransitRouteStop(
						schedule.getFacilities().get(childStopFacilityId),
						pseudoStop.getArrivalOffset(),
						pseudoStop.getDepartureOffset());
				newTransitRouteStop.setAwaitDepartureTime(pseudoStop.awaitsDepartureTime());
				newStopSequence.add(newTransitRouteStop);
			}

			// create a new transitRoute
			TransitRoute newRoute = scheduleFactory.createTransitRoute(pseudoRoute.getTransitRoute().getId(), null, newStopSequence, pseudoRoute.getTransitRoute().getTransportMode());

			// add departures
			pseudoRoute.getTransitRoute().getDepartures().values().forEach(newRoute::addDeparture);

			// remove the old route
			schedule.getTransitLines().get(pseudoRoute.getTransitLineId()).removeRoute(pseudoRoute.getTransitRoute());

			// add new route to container
			newRoutes.add(new Tuple<>(pseudoRoute.getTransitLineId(), newRoute));

		}

		// add transit lines and routes again
		for(Tuple<Id<TransitLine>, TransitRoute> entry : newRoutes) {
			schedule.getTransitLines().get(entry.getFirst()).addRoute(entry.getSecond());
		}
	}
}
