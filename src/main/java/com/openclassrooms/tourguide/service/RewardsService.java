//package com.openclassrooms.tourguide.service;
//
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.stream.Collectors;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import gpsUtil.GpsUtil;
//import gpsUtil.location.Attraction;
//import gpsUtil.location.Location;
//import gpsUtil.location.VisitedLocation;
//import rewardCentral.RewardCentral;
//import com.openclassrooms.tourguide.user.User;
//import com.openclassrooms.tourguide.user.UserReward;
//
//@Service
//public class RewardsService {
//	private final ExecutorService rewardExecutor = Executors.newFixedThreadPool(500);
//    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;
//
//	// proximity in miles
//    private int defaultProximityBuffer = 10;
//	private int proximityBuffer = defaultProximityBuffer;
//	private int attractionProximityRange = 200;
//
//	private final GpsUtil gpsUtil;
//	private final RewardCentral rewardsCentral;
//
//	@Autowired
//	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
//		this.gpsUtil = gpsUtil;
//		this.rewardsCentral = rewardCentral;
//	}
//
//	public void setProximityBuffer(int proximityBuffer) {
//		this.proximityBuffer = proximityBuffer;
//	}
//
//	public void setDefaultProximityBuffer() {
//		proximityBuffer = defaultProximityBuffer;
//	}
//
//	public void calculateRewards(User user) {
//		List<VisitedLocation> userLocations = user.getVisitedLocations();
//		List<Attraction> attractions = gpsUtil.getAttractions();
//
//		for(VisitedLocation visitedLocation : userLocations) {
////			for(Attraction attraction : attractions) {
////			attractions.parallelStream().forEach(attraction -> {
//			List<CompletableFuture<Void>> futures = attractions.stream()
//					.map(attraction -> CompletableFuture.runAsync(() -> {
//				if(user.getUserRewards().stream().noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName))) {
//					if(nearAttraction(visitedLocation, attraction)) {
//						user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
//					}
//				}
//			}, rewardExecutor))
//					.collect(Collectors.toList());
////			futures.forEach(CompletableFuture::join);
//			CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//		}
//	}
//
//	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
//		return getDistance(attraction, location) > attractionProximityRange ? false : true;
//	}
//
//	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
//		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
//	}
//
//	public int getRewardPoints(Attraction attraction, User user) {
//		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
//	}
//
//	public double getDistance(Location loc1, Location loc2) {
//        double lat1 = Math.toRadians(loc1.latitude);
//        double lon1 = Math.toRadians(loc1.longitude);
//        double lat2 = Math.toRadians(loc2.latitude);
//        double lon2 = Math.toRadians(loc2.longitude);
//
//        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
//                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));
//
//        double nauticalMiles = 60 * Math.toDegrees(angle);
//        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
//        return statuteMiles;
//	}
//
//}
package com.openclassrooms.tourguide.service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class RewardsService {

	// Grand pool adapté aux appels I/O bound (réseau / calcul)
	private final ExecutorService rewardExecutor = Executors.newFixedThreadPool(500);

	private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;

	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;

	@Autowired
	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}

	public void calculateRewards(User user) {
		List<VisitedLocation> userLocations = user.getVisitedLocations();
		List<Attraction> attractions = gpsUtil.getAttractions();

		for (VisitedLocation visitedLocation : userLocations) {
			CompletableFuture.allOf(
					attractions.stream()
							.map(attraction -> CompletableFuture.runAsync(() -> {
								if (user.getUserRewards().stream()
										.noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName))) {
									if (nearAttraction(visitedLocation, attraction)) {
										int points = getRewardPoints(attraction, user);
										user.addUserReward(new UserReward(visitedLocation, attraction, points));
									}
								}
							}, rewardExecutor))
							.toArray(CompletableFuture[]::new)
			).join();
		}
	}

	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) <= proximityBuffer;
	}

	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) <= attractionProximityRange;
	}

	public int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}

	public double getDistance(Location loc1, Location loc2) {
		double lat1 = Math.toRadians(loc1.latitude);
		double lon1 = Math.toRadians(loc1.longitude);
		double lat2 = Math.toRadians(loc2.latitude);
		double lon2 = Math.toRadians(loc2.longitude);

		double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
				+ Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

		double nauticalMiles = 60 * Math.toDegrees(angle);
		return STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
	}

	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}

	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}
}
