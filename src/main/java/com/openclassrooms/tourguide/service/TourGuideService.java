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
