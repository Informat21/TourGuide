//package com.openclassrooms.tourguide.service;
//
//import com.openclassrooms.tourguide.helper.InternalTestHelper;
//import com.openclassrooms.tourguide.tracker.Tracker;
//import com.openclassrooms.tourguide.user.User;
//import com.openclassrooms.tourguide.user.UserReward;
//
//import java.time.LocalDateTime;
//import java.time.ZoneOffset;
//import java.util.Date;
//import java.util.List;
//import java.util.Locale;
//import java.util.Map;
//import java.util.Random;
//import java.util.UUID;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.stream.Collectors;
//import java.util.stream.IntStream;
//
//import gpsUtil.location.Attraction;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import gpsUtil.GpsUtil;
//import gpsUtil.location.Location;
//import gpsUtil.location.VisitedLocation;
//
//import tripPricer.Provider;
//import tripPricer.TripPricer;
//
//import static com.google.common.io.ByteStreams.limit;
//
//@Service
//public class TourGuideService {
//	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
//	private final GpsUtil gpsUtil;
//	private final RewardsService rewardsService;
//	private final TripPricer tripPricer = new TripPricer();
//	public final Tracker tracker;
//	boolean testMode = true;
//	private final ExecutorService rewardExecutor = Executors.newFixedThreadPool(100);
//
//	@Autowired
//	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
//		this.gpsUtil = gpsUtil;
//		this.rewardsService = rewardsService;
//
//		Locale.setDefault(Locale.US);
//
//		if (testMode) {
//			logger.info("TestMode enabled");
//			logger.debug("Initializing users");
//			InternalTestHelper.setInternalUserNumber(100000);
//			initializeInternalUsers();
//			logger.debug("Finished initializing users");
//		}
//		tracker = new Tracker(this);
//		addShutDownHook();
//		logger.info("✅ TourGuideService constructor called");
//
//	}
//
//	public List<UserReward> getUserRewards(User user) {
//
//		return user.getUserRewards();
//	}
//
//	public VisitedLocation getUserLocation(User user) {
//		VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ? user.getLastVisitedLocation()
//				: trackUserLocation(user);
//		return visitedLocation;
//	}
//
//	public User getUser(String userName) {
//		User user = internalUserMap.get(userName);
//		if (user == null) {
//			logger.error("User not found: " + userName);
//		}
//		return user;
////		return internalUserMap.get(userName);
//	}
//
//	public List<User> getAllUsers() {
//
//		return internalUserMap.values().stream().collect(Collectors.toList());
//	}
//
//	public void addUser(User user) {
//		if (!internalUserMap.containsKey(user.getUserName())) {
//			internalUserMap.put(user.getUserName(), user);
//		}
//	}
//
//	public List<Provider> getTripDeals(User user) {
//		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
//		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(),
//				user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
//				user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
//		user.setTripDeals(providers);
//		return providers;
//	}
//
//	public VisitedLocation trackUserLocation(User user) {
//		VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
//		user.addToVisitedLocations(visitedLocation);
//		rewardsService.calculateRewards(user);
//		return visitedLocation;
//	}
//
//	public List<Attraction> getNearByAttractions(VisitedLocation visitedLocation) {
//		return gpsUtil.getAttractions().stream()
//				.sorted((loc1, loc2) -> {
//					double distanceA1 = rewardsService.getDistance(visitedLocation.location, loc1);
//					double distanceA2 = rewardsService.getDistance(visitedLocation.location, loc2);
//					return Double.compare(distanceA1, distanceA2);
//				})
//				.limit(5)
//				.collect(Collectors.toList());
//	}
////		List<Attraction> nearbyAttractions = new ArrayList<>();
////		for (Attraction attraction : gpsUtil.getAttractions()) {
////			if (rewardsService.isWithinAttractionProximity(attraction, visitedLocation.location)) {
////				nearbyAttractions.add(attraction);
////			}
////		}
////
////		return nearbyAttractions;
//
//
//	private void addShutDownHook() {
//		Runtime.getRuntime().addShutdownHook(new Thread() {
//			public void run() {
//				tracker.stopTracking();
//			}
//		});
//	}
//
//	/**********************************************************************************
//	 *
//	 * Methods Below: For Internal Testing
//	 *
//	 **********************************************************************************/
//	private static final String tripPricerApiKey = "test-server-api-key";
//	// Database connection will be used for external users, but for testing purposes
//	// internal users are provided and stored in memory
//	private final Map<String, User> internalUserMap = new ConcurrentHashMap<>();
//
//	private void initializeInternalUsers() {
//		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
//			String userName = "internalUser" + i;
//			String phone = "000";
//			String email = userName + "@tourGuide.com";
//			User user = new User(UUID.randomUUID(), userName, phone, email);
//			generateUserLocationHistory(user);
//
//			internalUserMap.put(userName, user);
//		});
//		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
//		logger.info("Internal users: " + internalUserMap.keySet());
//
//	}
//
//	private void generateUserLocationHistory(User user) {
//		IntStream.range(0, 3).forEach(i -> {
//			user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
//					new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
//		});
//	}
//
//	private double generateRandomLongitude() {
//		double leftLimit = -180;
//		double rightLimit = 180;
//		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
//	}
//
//	private double generateRandomLatitude() {
//		double leftLimit = -85.05112878;
//		double rightLimit = 85.05112878;
//		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
//	}
//
//	private Date getRandomTime() {
//		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
//		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
//	}
//
//
//	public void calculateRewardsForAllUsers() {
//		List<User> allUsers = getAllUsers();
//
//		List<CompletableFuture<Void>> futures = allUsers.stream()
//				.map(user -> CompletableFuture.runAsync(() -> rewardsService.calculateRewards(user), rewardExecutor))
//				.collect(Collectors.toList());
//
//		// attendre que toutes les tâches soient terminées
//		futures.forEach(CompletableFuture::join);
//	}
//	public void trackLocationForAllUsers() {
//		List<User> allUsers = getAllUsers();
//		List<CompletableFuture<Void>> futures = allUsers.stream()
//				.map(user -> CompletableFuture.runAsync(() -> trackUserLocation(user), rewardExecutor))
//				.toList();
//		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//	}
//
//}
package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;
import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import tripPricer.Provider;
import tripPricer.TripPricer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class TourGuideService {

	private final Logger logger = LoggerFactory.getLogger(TourGuideService.class);

	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;

	// Pool pour paralléliser les users (CPU et I/O bound)
	private final ExecutorService userExecutor = Executors.newFixedThreadPool(500);

	private final Map<String, User> internalUserMap = new ConcurrentHashMap<>();
	private static final String tripPricerApiKey = "test-server-api-key";
	private boolean testMode = true;

	@Autowired
	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;

		Locale.setDefault(Locale.US);

		if (testMode) {
			logger.info("TestMode enabled, internalUserNumber=" + InternalTestHelper.getInternalUserNumber());
			initializeInternalUsers();
		}

		tracker = new Tracker(this);
		addShutDownHook();
	}

	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	public VisitedLocation getUserLocation(User user) {
		return (user.getVisitedLocations().size() > 0)
				? user.getLastVisitedLocation()
				: trackUserLocation(user);
	}

	public VisitedLocation trackUserLocation(User user) {
		VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
		user.addToVisitedLocations(visitedLocation);
		rewardsService.calculateRewards(user);
		return visitedLocation;
	}

	public List<Provider> getTripDeals(User user) {
		int cumulativeRewardPoints = user.getUserRewards().stream()
				.mapToInt(UserReward::getRewardPoints).sum();
		List<Provider> providers = tripPricer.getPrice(
				tripPricerApiKey, user.getUserId(),
				user.getUserPreferences().getNumberOfAdults(),
				user.getUserPreferences().getNumberOfChildren(),
				user.getUserPreferences().getTripDuration(),
				cumulativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}

	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}

	public List<User> getAllUsers() {
		return new ArrayList<>(internalUserMap.values());
	}

	public void addUser(User user) {
		internalUserMap.putIfAbsent(user.getUserName(), user);
	}

	public List<Attraction> getNearByAttractions(VisitedLocation visitedLocation) {
		return gpsUtil.getAttractions().stream()
				.sorted(Comparator.comparingDouble(a ->
						rewardsService.getDistance(visitedLocation.location, a)))
				.limit(5)
				.collect(Collectors.toList());
	}

	public void trackLocationForAllUsers() {
		List<User> allUsers = getAllUsers();
		CompletableFuture.allOf(
				allUsers.stream()
						.map(user -> CompletableFuture.runAsync(() -> trackUserLocation(user), userExecutor))
						.toArray(CompletableFuture[]::new)
		).join();
	}

	public void calculateRewardsForAllUsers() {
		List<User> allUsers = getAllUsers();
		CompletableFuture.allOf(
				allUsers.stream()
						.map(user -> CompletableFuture.runAsync(() -> rewardsService.calculateRewards(user), userExecutor))
						.toArray(CompletableFuture[]::new)
		).join();
	}

	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			User user = new User(UUID.randomUUID(), userName, "000", userName + "@tourGuide.com");
			generateUserLocationHistory(user);
			internalUserMap.put(userName, user);
		});
		logger.info("Created {} internal users.", internalUserMap.size());
	}

	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i -> user.addToVisitedLocations(
				new VisitedLocation(user.getUserId(),
						new Location(generateRandomLatitude(), generateRandomLongitude()),
						getRandomTime())));
	}

	private double generateRandomLongitude() {
		return -180 + new Random().nextDouble() * 360;
	}

	private double generateRandomLatitude() {
		return -85.0511 + new Random().nextDouble() * (2 * 85.0511);
	}

	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}

	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> tracker.stopTracking()));
	}
}
