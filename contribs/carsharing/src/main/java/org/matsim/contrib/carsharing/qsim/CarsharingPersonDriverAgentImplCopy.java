package org.matsim.contrib.carsharing.qsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.carsharing.events.NoParkingSpaceEvent;
import org.matsim.contrib.carsharing.events.NoVehicleCarSharingEvent;
import org.matsim.contrib.carsharing.facility.DummyFacility;
import org.matsim.contrib.carsharing.stations.FreeFloatingStation;
import org.matsim.contrib.carsharing.stations.OneWayCarsharingStation;
import org.matsim.contrib.carsharing.stations.TwoWayCarsharingStation;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.agents.BasicPlanAgentImpl;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.PlanBasedDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.TransitAgentImpl;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.PTPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;



/**
 * Current version includes:
 * -- two-way carsharing with reservation of the vehicle at the end of the activity preceding the rental.
 * -- one-way carsharing with each station having a parking capacity with the reservation system as the one with two-way
 * -- free-floating carsharing with parking at the link of the next activity following free-floating trip, reservation system as the one with two-way cs.
 * -- end of the free-floating rental is always on the link of the next activity, therefore no egress walk leg
 * @author balac
 */


public class CarsharingPersonDriverAgentImplCopy implements MobsimDriverAgent, MobsimPassengerAgent, HasPerson, PlanAgent, PTPassengerAgent {

	private static final Logger log = Logger.getLogger(PersonDriverAgentImpl.class);

	private Link startLinkFF;
	private Link startLinkTW;

	private OneWayCarsharingStation startStationOW;
	private OneWayCarsharingStation endStationOW;

	private CarSharingVehicles carSharingVehicles;

	HashMap<Link, Link> mapTW = new HashMap<Link, Link>();
	HashMap<Link, Link> mapOW = new HashMap<Link, Link>();

	private String ffVehId;
	private String owVehId;
	private String twVehId;

	double beelineFactor = 0.0;

	double walkSpeed = 0.0;

	private TripRouter tripRouter;

	private final BasicPlanAgentImpl basicAgentDelegate ;
	private final TransitAgentImpl transitAgentDelegate ;
	private final PlanBasedDriverAgentImpl driverAgentDelegate ;

	private PlanElement previousPlanElement;


	public CarsharingPersonDriverAgentImplCopy(final Person person, final Plan plan, 
			final Netsim simulation, final Scenario scenario,
			CarSharingVehicles carSharingVehicles, TripRouter tripRouter) {
		// yy person and plan can be inconsistent.  kai, may'15
		// yy simulation and scenario can be inconsistent.  kai, may'15

		this.basicAgentDelegate = new BasicPlanAgentImpl( plan, scenario, simulation.getEventsManager(), simulation.getSimTimer() ) ;
		this.transitAgentDelegate = new TransitAgentImpl( this.basicAgentDelegate ) ;
		this.driverAgentDelegate = new PlanBasedDriverAgentImpl( this.basicAgentDelegate ) ;

		this.previousPlanElement = this.basicAgentDelegate.getCurrentPlanElement() ;

		this.carSharingVehicles = carSharingVehicles;

		this.tripRouter = tripRouter;

		beelineFactor = ((PlansCalcRouteConfigGroup)scenario.getConfig().getModule("planscalcroute")).getBeelineDistanceFactors().get("walk");
		walkSpeed = (((PlansCalcRouteConfigGroup)scenario.getConfig().getModule("planscalcroute")).getTeleportedModeSpeeds().get("walk"));
		//carsharingVehicleLocations = new ArrayList<ActivityFacility>();
		mapTW = new HashMap<Link, Link>();
		mapOW = new HashMap<Link, Link>();
	}

	// -----------------------------------------------------------------------------------------------------------------------------


	@Override
	public final void endActivityAndComputeNextState(final double now) {
		this.basicAgentDelegate.endActivityAndComputeNextState(now);

		log.warn( "following method may duplicate code:") ;
		advanceCSPlan(now);
	}

	@Override
	public final void endLegAndComputeNextState(final double now) {
		this.basicAgentDelegate.endLegAndComputeNextState(now);
		if ( this.getState()!=State.ABORT ) {

			if (this.getVehicle()!=null && (this.getVehicle().getId().toString().startsWith("TW") ||
					this.getVehicle().getId().toString().startsWith("OW") || 
					this.getVehicle().getId().toString().startsWith("FF")))

				parkCSVehicle( );			

			log.warn( "following method may duplicate code:") ;
			advanceCSPlan(now) ;
		}
	}	

	private void parkCSVehicle() {
		Leg currentLeg = (Leg) this.basicAgentDelegate.getCurrentPlanElement() ;
		Scenario scenario = this.basicAgentDelegate.getScenario() ;

		if (currentLeg.getMode().equals("onewaycarsharing")) {

			this.carSharingVehicles.getOneWayVehicles().addVehicle(endStationOW, owVehId);
			owVehId = null;
		}
		else if (currentLeg.getMode().equals("twowaycarsharing") 
				//				&& plan.getPlanElements().get(currentPlanElementIndex + 1) instanceof Leg
				&& this.basicAgentDelegate.getNextPlanElement() instanceof Leg
				) {

			this.carSharingVehicles.getTwoWayVehicles().addVehicle(scenario.getNetwork().getLinks().get(this.getDestinationLinkId()), twVehId);
			twVehId = null;
		}
		else if (currentLeg.getMode().equals("freefloating")) {

			this.carSharingVehicles.getFreeFLoatingVehicles().addVehicle(scenario.getNetwork().getLinks().get(this.getDestinationLinkId()), ffVehId);
			ffVehId = null;
		}


	}

	private void advanceCSPlan(double now) {
		// we can either call this before or after the basicPlanAgentImpl.advancePlan.
		// If we call it after, the leg has already been initialized.  This may or may not be a problem.
		
		Leg leg = (Leg) this.basicAgentDelegate.getCurrentPlanElement() ;
		//start of added stuff

		String mode = leg.getMode();

		//			PlanElement nextPlanElement = this.plan.getPlanElements().get(this.plan.getPlanElements().indexOf(pe) + 1);
		PlanElement nextPlanElement = this.basicAgentDelegate.getNextPlanElement() ;

		switch (mode) {

		case "walk_rb":
			if (nextPlanElement instanceof Leg) {

				initializeTwoWayCarsharingStartWalkLeg(leg, now);
			}
			else if (previousPlanElement instanceof Leg) {

				initializeTwoWayCarsharingEndWalkLeg(leg, now);

			}
			break;

		case "twowaycarsharing": 
			if (previousPlanElement instanceof Activity &&
					nextPlanElement instanceof Activity)

				initializeTwoWayCSMidleCarLeg(startLinkTW, now);

			else if (previousPlanElement instanceof Leg &&
					!(nextPlanElement instanceof Leg))

				initializeTwoWayCarsharingCarLeg(startLinkTW, now);

			else if (previousPlanElement instanceof Leg && 
					(nextPlanElement instanceof Leg))

				initializeTwoWayCarsharingEmptyCarLeg(startLinkTW, now);

			else if (nextPlanElement instanceof Leg)

				initializeTwoWayCarsharingEndCarLeg(startLinkTW, now);
			else 
				log.error("This should never happen");
			break;

		case "walk_ff":
			initializeFreeFLoatingWalkLeg(leg, now); 
			break;

		case "freefloating":
			initializeFreeFLoatingCarLeg(startLinkFF, now);
			break;

		case "walk_ow_sb": 
			if (nextPlanElement instanceof Leg) {

				initializeOneWayCarsharingStartWalkLeg(leg, now);
			}
			else if (previousPlanElement instanceof Leg) {

				initializeOneWayCarsharingEndWalkLeg(leg, now);

			}				
			break;

		case "onewaycarsharing": 
			initializeOneWayCarsharingCarLeg(startStationOW.getLink(), now);
			break;
		default:
			//				initializeLeg(leg);
			break;
		}

		previousPlanElement = this.basicAgentDelegate.getCurrentPlanElement() ;
	}

	//added methods

	private void initializeCSWalkLeg(String mode, double now, Link startLink, Link destinationLink) {
		LegImpl walkLeg = new LegImpl(mode);

		GenericRouteImpl walkRoute = new GenericRouteImpl(startLink.getId(), destinationLink.getId());
		final double dist = CoordUtils.calcDistance(startLink.getCoord(), destinationLink.getCoord());
		final double estimatedNetworkDistance = dist * beelineFactor;

		final int travTime = (int) (estimatedNetworkDistance / walkSpeed);
		walkRoute.setTravelTime(travTime);
		walkRoute.setDistance(estimatedNetworkDistance);	

		walkLeg.setRoute(walkRoute);

		walkLeg.setDepartureTime(now);
		walkLeg.setTravelTime(travTime);
		walkLeg.setArrivalTime(now + travTime);
	}

	private void initializeCSVehicleLeg (String mode, double now, Link startLink, Link destinationLink) {
		double travelTime = 0.0;
		List<Id<Link>> ids = new ArrayList<Id<Link>>();

		CoordImpl coordStart = new CoordImpl(startLink.getCoord());

		DummyFacility dummyStartFacility = new DummyFacility(coordStart, startLink.getId());

		CoordImpl coordEnd = new CoordImpl(destinationLink.getCoord());

		DummyFacility dummyEndFacility = new DummyFacility(coordEnd, destinationLink.getId());

		for(PlanElement pe1: this.tripRouter.calcRoute("car", dummyStartFacility, dummyEndFacility, now, this.basicAgentDelegate.getPerson() )) {

			if (pe1 instanceof Leg) {
				ids = ((NetworkRoute)((Leg) pe1).getRoute()).getLinkIds();
				travelTime += ((Leg) pe1).getTravelTime();
			}
		}

		LegImpl carLeg = new LegImpl(mode);

		carLeg.setTravelTime( travelTime );

		Scenario scenario = this.basicAgentDelegate.getScenario() ;
		LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) ((PopulationFactoryImpl)scenario.getPopulation().getFactory()).getModeRouteFactory().createRoute("car", startLink.getId(), destinationLink.getId());

		route.setLinkIds( startLink.getId(), ids, destinationLink.getId());
		route.setTravelTime( travelTime);

		if (mode.equals("twowaycarsharing"))
			route.setVehicleId(Id.create("TW_" + (twVehId), Vehicle.class));
		else if (mode.equals("onewaycarsharing"))
			route.setVehicleId(Id.create("OW_" + (owVehId), Vehicle.class));
		else if (mode.equals("freefloating"))
			route.setVehicleId(Id.create("FF_" + (ffVehId), Vehicle.class));

		carLeg.setRoute(route);

	}

	private void initializeTwoWayCarsharingStartWalkLeg(Leg leg, double now) {

		//		this.setState(MobsimAgent.State.LEG);
		Route route = leg.getRoute();

		TwoWayCarsharingStation station = findClosestAvailableTWCar(route.getStartLinkId());

		if (station == null) {
			this.setStateToAbort(now);
			EventsManager events = this.basicAgentDelegate.getEvents() ;
			events.processEvent(new NoVehicleCarSharingEvent(now, route.getStartLinkId(), "rt"));
			return;

		}		

		startLinkTW = station.getLink();
		twVehId = station.getIDs().get(0);
		this.carSharingVehicles.getTwoWayVehicles().removeVehicle(station, station.getIDs().get(0));


		mapTW.put(this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(leg.getRoute().getStartLinkId()), startLinkTW);
		initializeCSWalkLeg("walk_rb", now, this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()), startLinkTW);

	}

	private void initializeTwoWayCarsharingCarLeg(Link l, double now) {
		//		this.setState(MobsimAgent.State.LEG);

		Leg leg =  (Leg) this.getCurrentPlanElement();

		//create route for the car part of the twowaycarsharing trip
		initializeCSVehicleLeg("twowaycarsharing", now, l, this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(leg.getRoute().getEndLinkId()));


	}	
	private void initializeTwoWayCarsharingEmptyCarLeg(Link l, double now) {
		initializeCSVehicleLeg("twowaycarsharing", now, l, l);		
	}
	private void initializeTwoWayCSMidleCarLeg(Link l, double now) {
		Leg leg =  (Leg) this.getCurrentPlanElement();
		Scenario scenario = this.basicAgentDelegate.getScenario() ;
		Network network = scenario.getNetwork();

		//create route for the car part of the twowaycarsharing trip
		initializeCSVehicleLeg("twowaycarsharing", now, network.getLinks().get(leg.getRoute().getStartLinkId()), network.getLinks().get(leg.getRoute().getEndLinkId()));
	}

	private void initializeTwoWayCarsharingEndCarLeg(Link l, double now) {
		Leg leg =  (Leg) this.getCurrentPlanElement();
		Scenario scenario = this.basicAgentDelegate.getScenario() ;
		Link link = mapTW.get(scenario.getNetwork().getLinks().get(leg.getRoute().getEndLinkId()));

		//create route for the car part of the twowaycarsharing trip
		initializeCSVehicleLeg("twowaycarsharing", now, scenario.getNetwork().getLinks().get(leg.getRoute().getStartLinkId()), link);

	}

	private void initializeTwoWayCarsharingEndWalkLeg(Leg leg, double now) {
		Route route = leg.getRoute();		

		final Network network = this.basicAgentDelegate.getScenario().getNetwork();
		Link link = mapTW.get(network.getLinks().get(leg.getRoute().getEndLinkId()));
		mapTW.remove(network.getLinks().get(leg.getRoute().getEndLinkId()));
		initializeCSWalkLeg("walk_rb", now, link, network.getLinks().get(route.getEndLinkId()));				

	}

	private TwoWayCarsharingStation findClosestAvailableTWCar(Id<Link> linkId) {
		Scenario scenario = this.basicAgentDelegate.getScenario() ;

		//find the closest available car in the quad tree(?) reserve it (make it unavailable)
		//if no cars within certain radius return null
		Link link = scenario.getNetwork().getLinks().get(linkId);

		Collection<TwoWayCarsharingStation> location = this.carSharingVehicles.getTwoWayVehicles().getQuadTree().get(link.getCoord().getX(), link.getCoord().getY(), Double.parseDouble(scenario.getConfig().getModule("TwoWayCarsharing").getParams().get("searchDistanceTwoWayCarsharing")));
		if (location.isEmpty()) return null;
		double distanceSearch = Double.parseDouble(scenario.getConfig().getModule("TwoWayCarsharing").getParams().get("searchDistanceTwoWayCarsharing"));
		TwoWayCarsharingStation closest = null;
		for(TwoWayCarsharingStation station: location) {
			if (CoordUtils.calcDistance(link.getCoord(), station.getLink().getCoord()) < distanceSearch && station.getNumberOfVehicles() > 0) {
				closest = station;
				distanceSearch = CoordUtils.calcDistance(link.getCoord(), station.getLink().getCoord());
			}			

		}

		return closest;

	}	

	private void initializeFreeFLoatingWalkLeg(Leg leg, double now) {
		//		this.setState(MobsimAgent.State.LEG);
		Route route = leg.getRoute();
		FreeFloatingStation location = findClosestAvailableCar(route.getStartLinkId());

		if (location == null) {
			this.setStateToAbort(now);
			this.basicAgentDelegate.getEvents().processEvent(new NoVehicleCarSharingEvent(now, route.getStartLinkId(), "ff"));
			return;
		}
		ffVehId = location.getIDs().get(0);
		this.carSharingVehicles.getFreeFLoatingVehicles().removeVehicle(location.getLink(), ffVehId);
		startLinkFF = location.getLink();
		initializeCSWalkLeg("walk_ff", now, this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()), startLinkFF);
	}

	private void initializeFreeFLoatingCarLeg(Link l, double now) {
		Leg leg =  (Leg) this.getCurrentPlanElement();

		//create route for the car part of the freefloating trip
		initializeCSVehicleLeg("freefloating", now, l, this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(leg.getRoute().getEndLinkId()));
	}


	private FreeFloatingStation findClosestAvailableCar(Id<Link> linkId) {		
		//find the closest available car in the quad tree(?) reserve it (make it unavailable)
		Link link = this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(linkId);

		FreeFloatingStation location = this.carSharingVehicles.getFreeFLoatingVehicles().getQuadTree().get(link.getCoord().getX(), link.getCoord().getY());

		return location;
	}


	private void initializeOneWayCarsharingStartWalkLeg(Leg leg, double now) {
		//		this.setState(MobsimAgent.State.LEG);
		Route route = leg.getRoute();
		OneWayCarsharingStation station = findClosestAvailableOWCar(route.getStartLinkId());

		if (station == null) {
			this.setStateToAbort(now);
			this.basicAgentDelegate.getEvents().processEvent(new NoVehicleCarSharingEvent(now, route.getStartLinkId(), "ow"));

			return;

		}
		startStationOW = station;
		owVehId = station.getIDs().get(0);
		this.carSharingVehicles.getOneWayVehicles().removeVehicle(station, owVehId);

		initializeCSWalkLeg("walk_ow_sb", now, this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getStartLinkId()), startStationOW.getLink());


	}

	private void initializeOneWayCarsharingCarLeg(Link l, double now) {
		//		this.setState(MobsimAgent.State.LEG);
		Scenario scenario = this.basicAgentDelegate.getScenario() ;

		Leg leg =  (Leg) this.getCurrentPlanElement();
		Network network = scenario.getNetwork();
		endStationOW = findClosestAvailableParkingSpace(network.getLinks().get(leg.getRoute().getEndLinkId()));

		if (endStationOW == null) {

			this.setStateToAbort(now);
			this.basicAgentDelegate.getEvents().processEvent(new NoParkingSpaceEvent(now, leg.getRoute().getEndLinkId(), "ow"));

			return;
		}
		else {
			startStationOW.freeParkingSpot();
			endStationOW.reserveParkingSpot();

			Link destinationLink = endStationOW.getLink();
			//create route for the car part of the onewaycarsharing trip
			initializeCSVehicleLeg("onewaycarsharing", now, l, destinationLink);
		}

	}
	private void initializeOneWayCarsharingEndWalkLeg(Leg leg, double now) {
		Route route = leg.getRoute();		
		initializeCSWalkLeg("walk_ow_sb", now, endStationOW.getLink(), this.basicAgentDelegate.getScenario().getNetwork().getLinks().get(route.getEndLinkId()));

	}

	private OneWayCarsharingStation findClosestAvailableOWCar(Id<Link> linkId) {
		Scenario scenario = this.basicAgentDelegate.getScenario() ;

		//find the closest available car and reserve it (make it unavailable)
		//if no cars within certain radius return null
		Link link = scenario.getNetwork().getLinks().get(linkId);
		double distanceSearch = Double.parseDouble(scenario.getConfig().getModule("OneWayCarsharing").getParams().get("searchDistanceOneWayCarsharing"));

		Collection<OneWayCarsharingStation> location = this.carSharingVehicles.getOneWayVehicles().getQuadTree().get(link.getCoord().getX(), link.getCoord().getY(), distanceSearch);
		if (location.isEmpty()) return null;

		OneWayCarsharingStation closest = null;
		for(OneWayCarsharingStation station: location) {
			if (CoordUtils.calcDistance(link.getCoord(), station.getLink().getCoord()) < distanceSearch && station.getNumberOfVehicles() > 0) {
				closest = station;
				distanceSearch = CoordUtils.calcDistance(link.getCoord(), station.getLink().getCoord());
			}			

		}			

		return closest;


	}

	private OneWayCarsharingStation findClosestAvailableParkingSpace(Link link) {
		Scenario scenario = this.basicAgentDelegate.getScenario() ;

		//find the closest available parking space and reserve it (make it unavailable)
		//if there are no parking spots within search radius, return null

		double distanceSearch = Double.parseDouble(scenario.getConfig().getModule("OneWayCarsharing").getParams().get("searchDistanceOneWayCarsharing"));

		Collection<OneWayCarsharingStation> location = this.carSharingVehicles.getOneWayVehicles().getQuadTree().get(link.getCoord().getX(), link.getCoord().getY(), distanceSearch);
		if (location.isEmpty()) return null;

		OneWayCarsharingStation closest = null;
		for(OneWayCarsharingStation station: location) {
			if (CoordUtils.calcDistance(link.getCoord(), station.getLink().getCoord()) < distanceSearch && station.getNumberOfAvailableParkingSpaces() > 0) {
				closest = station;
				distanceSearch = CoordUtils.calcDistance(link.getCoord(), station.getLink().getCoord());
			}			

		}		

		return closest;

	}
	//the end of added methods	

	void resetCaches() {
		WithinDayAgentUtils.resetCaches(this.basicAgentDelegate);
	}

	@Override
	public final Id<Vehicle> getPlannedVehicleId() {
		PlanElement currentPlanElement = this.getCurrentPlanElement();
		NetworkRoute route = (NetworkRoute) ((Leg) currentPlanElement).getRoute(); // if casts fail: illegal state.

		if (((Leg)currentPlanElement).getMode().equals("freefloating")){

			return Id.create("FF_"+ (ffVehId), Vehicle.class);	

		}
		else if (((Leg)currentPlanElement).getMode().equals("onewaycarsharing")){

			return Id.create("OW_"+ (owVehId), Vehicle.class);	

		}
		else if (((Leg)currentPlanElement).getMode().equals("twowaycarsharing")){
			if (twVehId == null) {

				log.info("Twowaycarsahring vehicle ID is null for person with id " + this.getId() +" , returning person id as vehicle id and continuing! ");
				return Id.create(this.getId(), Vehicle.class);
			}

			return Id.create("TW_"+ (twVehId), Vehicle.class);	

		}
		else if (route.getVehicleId() != null) 
			return route.getVehicleId();

		else
			return Id.create(this.getId(), Vehicle.class); // we still assume the vehicleId is the agentId if no vehicleId is given.

	}

	// ####################################################################
	// only pure delegate methods below this line

	@Override
	public final PlanElement getCurrentPlanElement() {
		return this.basicAgentDelegate.getCurrentPlanElement() ;
	}

	@Override
	public final PlanElement getNextPlanElement() {
		return this.basicAgentDelegate.getNextPlanElement() ;
	}

	@Override
	public final void setVehicle(final MobsimVehicle veh) {
		this.basicAgentDelegate.setVehicle(veh) ;
	}

	@Override
	public final MobsimVehicle getVehicle() {
		return this.basicAgentDelegate.getVehicle() ;
	}

	@Override
	public final double getActivityEndTime() {
		return this.basicAgentDelegate.getActivityEndTime() ;
	}

	@Override
	public final Id<Link> getCurrentLinkId() {
		return this.driverAgentDelegate.getCurrentLinkId() ;
	}

	@Override
	public final Double getExpectedTravelTime() {
		return this.basicAgentDelegate.getExpectedTravelTime() ;

	}

	@Override
	public Double getExpectedTravelDistance() {
		return this.basicAgentDelegate.getExpectedTravelDistance() ;
	}

	@Override
	public final String getMode() {
		return this.basicAgentDelegate.getMode() ;
	}

	@Override
	public final Id<Link> getDestinationLinkId() {
		return this.basicAgentDelegate.getDestinationLinkId() ;
	}

	@Override
	public final Person getPerson() {
		return this.basicAgentDelegate.getPerson() ;
	}

	@Override
	public final Id<Person> getId() {
		return this.basicAgentDelegate.getId() ;
	}

	@Override
	public final Plan getCurrentPlan() {
		return this.basicAgentDelegate.getCurrentPlan() ;
	}

	@Override
	public boolean getEnterTransitRoute(final TransitLine line, final TransitRoute transitRoute, final List<TransitRouteStop> stopsToCome, TransitVehicle transitVehicle) {
		return this.transitAgentDelegate.getEnterTransitRoute(line, transitRoute, stopsToCome, transitVehicle) ;
	}

	@Override
	public boolean getExitAtStop(final TransitStopFacility stop) {
		return this.transitAgentDelegate.getExitAtStop(stop) ;
	}

	@Override
	public double getWeight() {
		return this.transitAgentDelegate.getWeight() ;
	}

	@Override
	public Id<TransitStopFacility> getDesiredAccessStopId() {
		return this.transitAgentDelegate.getDesiredAccessStopId() ;
	}

	@Override
	public Id<TransitStopFacility> getDesiredDestinationStopId() {
		return this.transitAgentDelegate.getDesiredAccessStopId() ;
	}

	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		return this.driverAgentDelegate.isWantingToArriveOnCurrentLink() ;
	}

	@Override
	public MobsimAgent.State getState() {
		return this.basicAgentDelegate.getState() ;
	}
	@Override
	public final void setStateToAbort(final double now) {
		this.basicAgentDelegate.setStateToAbort(now);
	}

	@Override
	public final void notifyArrivalOnLinkByNonNetworkMode(final Id<Link> linkId) {
		this.basicAgentDelegate.notifyArrivalOnLinkByNonNetworkMode(linkId);
	}

	@Override
	public final void notifyMoveOverNode(Id<Link> newLinkId) {
		this.driverAgentDelegate.notifyMoveOverNode(newLinkId);
	}

	@Override
	public Id<Link> chooseNextLinkId() {
		return this.driverAgentDelegate.chooseNextLinkId() ;
	}



}
