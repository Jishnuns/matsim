package org.matsim.contrib.freight.mobsim;

import java.util.HashSet;
import java.util.Set;

import org.junit.Ignore;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionAccumulator.BasicScoring;
import org.matsim.core.scoring.ScoringFunctionAccumulator.LegScoring;

@Ignore
public class ScoringFunctionFactoryForTests implements CarrierScoringFunctionFactory{

	 static class DriverLegScoring implements BasicScoring, LegScoring{
			
			private double score = 0.0;

			private final Network network;
			
			private final Carrier carrier;
			
			private Set<CarrierVehicle> employedVehicles;
			
			private Leg currentLeg = null;
			
			private double currentLegStartTime;
			
			public DriverLegScoring(Carrier carrier, Network network) {
				super();
				this.network = network;
				this.carrier = carrier;
				employedVehicles = new HashSet<CarrierVehicle>();
			}

			
			@Override
			public void finish() {
				
			}


			@Override
			public double getScore() {
				return score;
			}


			@Override
			public void reset() {
				score = 0.0;
				employedVehicles.clear();
			}


			@Override
			public void startLeg(double time, Leg leg) {
				currentLeg = leg;
				currentLegStartTime = time; 
			}


			@Override
			public void endLeg(double time) {
				if(currentLeg.getRoute() instanceof NetworkRoute){
					NetworkRoute nRoute = (NetworkRoute) currentLeg.getRoute();
					Id vehicleId = nRoute.getVehicleId();
					CarrierVehicle vehicle = getVehicle(vehicleId);
					assert vehicle != null : "cannot find vehicle with id=" + vehicleId;
					if(!employedVehicles.contains(vehicle)){
						employedVehicles.add(vehicle);
						score += (-1)*getFixEmploymentCost(vehicle);
					}
					double distance = 0.0;
					double toll = 0.0;
					if(currentLeg.getRoute() instanceof NetworkRoute){
						distance += network.getLinks().get(currentLeg.getRoute().getStartLinkId()).getLength();
						for(Id linkId : ((NetworkRoute) currentLeg.getRoute()).getLinkIds()){
							distance += network.getLinks().get(linkId).getLength();
							toll += getToll(linkId, vehicle, null);
						}
						distance += network.getLinks().get(currentLeg.getRoute().getEndLinkId()).getLength();
						toll += getToll(currentLeg.getRoute().getEndLinkId(), vehicle, null);
					}
					score += (-1)*(time-currentLegStartTime)*getTimeParameter(vehicle,null);
					score += (-1)*distance*getDistanceParameter(vehicle,null);
					score += (-1)*toll;
				}
				
			}
			
			private double getFixEmploymentCost(CarrierVehicle vehicle) {
				return vehicle.getVehicleType().getVehicleCostInformation().fix;
			}

			private double getToll(Id linkId, CarrierVehicle vehicle, Person driver) {
				return 0;
			}

			private double getDistanceParameter(CarrierVehicle vehicle, Person driver) {
				return vehicle.getVehicleType().getVehicleCostInformation().perDistanceUnit;
			}

			private double getTimeParameter(CarrierVehicle vehicle, Person driver) {
				return vehicle.getVehicleType().getVehicleCostInformation().perTimeUnit;
			}

			private CarrierVehicle getVehicle(Id vehicleId) {
				for(CarrierVehicle cv : carrier.getCarrierCapabilities().getCarrierVehicles()){
					if(cv.getVehicleId().equals(vehicleId)){
						return cv;
					}
				}
				return null;
			}
			
		}
	
	static class NumberOfToursAward implements BasicScoring{

		private Carrier carrier;
		
		public NumberOfToursAward(Carrier carrier) {
			super();
			this.carrier = carrier;
		}

		@Override
		public void finish() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public double getScore() {
			if(carrier.getSelectedPlan().getScheduledTours().size() > 1){
				return 10000.0;
			}
			return 0;
		}

		@Override
		public void reset() {
			// TODO Auto-generated method stub
			
		}
		
	}
	 
	 private Network network;
	
	public ScoringFunctionFactoryForTests(Network network) {
		super();
		this.network = network;
	}

	@Override
	public ScoringFunction createScoringFunction(Carrier carrier) {
		ScoringFunctionAccumulator sf = new ScoringFunctionAccumulator();
		DriverLegScoring driverLegScoring = new DriverLegScoring(carrier, network);
		sf.addScoringFunction(driverLegScoring);
		sf.addScoringFunction(new NumberOfToursAward(carrier));
		return sf;
	}

}
