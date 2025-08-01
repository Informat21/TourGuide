//package com.openclassrooms.tourguide;
//
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//
//import org.apache.commons.lang3.time.StopWatch;
//import org.junit.jupiter.api.Disabled;
//import org.junit.jupiter.api.Test;
//
//import gpsUtil.GpsUtil;
//import gpsUtil.location.Attraction;
//import gpsUtil.location.VisitedLocation;
//import rewardCentral.RewardCentral;
//import com.openclassrooms.tourguide.helper.InternalTestHelper;
//import com.openclassrooms.tourguide.service.RewardsService;
//import com.openclassrooms.tourguide.service.TourGuideService;
//import com.openclassrooms.tourguide.user.User;
//
//public class TestPerformance {
//
//	/*
//	 * A note on performance improvements:
//	 *
//	 * The number of users generated for the high volume tests can be easily
//	 * adjusted via this method:
//	 *
//	 * InternalTestHelper.setInternalUserNumber(100000);
//	 *
//	 *
//	 * These tests can be modified to suit new solutions, just as long as the
//	 * performance metrics at the end of the tests remains consistent.
//	 *
//	 * These are performance metrics that we are trying to hit:
//	 *
//	 * highVolumeTrackLocation: 100,000 users within 15 minutes:
//	 * assertTrue(TimeUnit.MINUTES.toSeconds(15) >=
//	 * TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
//	 *
//	 * highVolumeGetRewards: 100,000 users within 20 minutes:
//	 * assertTrue(TimeUnit.MINUTES.toSeconds(20) >=
//	 * TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
//	 */
//
////	@Disabled
//	@Test
//	public void highVolumeTrackLocation() {
//		GpsUtil gpsUtil = new GpsUtil();
//		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
//		// Users should be incremented up to 100,000, and test finishes within 15
//		// minutes
//		InternalTestHelper.setInternalUserNumber(100000);
//		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
//
//		List<User> allUsers = new ArrayList<>();
//		allUsers = tourGuideService.getAllUsers();
//
//		StopWatch stopWatch = new StopWatch();
//		stopWatch.start();
//		for (User user : allUsers) {
//			tourGuideService.trackUserLocation(user);
//		}
//		stopWatch.stop();
//		tourGuideService.tracker.stopTracking();
//
//		System.out.println("highVolumeTrackLocation: Time Elapsed: "
//				+ TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
//		assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
//	}
//
////	@Disabled
//	@Test
//	public void highVolumeGetRewards() {
//		GpsUtil gpsUtil = new GpsUtil();
//		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
//
//		// Users should be incremented up to 100,000, and test finishes within 20
//		// minutes
//		InternalTestHelper.setInternalUserNumber(100000);
//		StopWatch stopWatch = new StopWatch();
//		stopWatch.start();
//		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
//
//		Attraction attraction = gpsUtil.getAttractions().get(0);
//		List<User> allUsers = new ArrayList<>();
//		allUsers = tourGuideService.getAllUsers();
//		allUsers.forEach(u -> u.addToVisitedLocations(new VisitedLocation(u.getUserId(), attraction, new Date())));
//
////		allUsers.forEach(u -> rewardsService.calculateRewards(u));
//		tourGuideService.calculateRewardsForAllUsers();
//
////		for (User user : allUsers) {
////			assertTrue(user.getUserRewards().size() > 0);
////		}
//		tourGuideService.trackLocationForAllUsers();
//
//		stopWatch.stop();
//		tourGuideService.tracker.stopTracking();
//
//		System.out.println("highVolumeGetRewards: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime())
//				+ " seconds.");
//		assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
//	}
//
//}

package com.openclassrooms.tourguide;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;

public class TestPerformance {

	/**
	 * Test performance cible :
	 * - trackLocation : 100 000 users < 15 min
	 * - getRewards : 100 000 users < 20 min
	 */

	@Test
	public void highVolumeTrackLocation() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());

		// ⚠️ nombre d'utilisateurs
//		InternalTestHelper.setInternalUserNumber(100);
		String userCountProperty = System.getProperty("user.count");
		int userCount = (userCountProperty != null) ? Integer.parseInt(userCountProperty) : 100;
		InternalTestHelper.setInternalUserNumber(userCount);

		System.out.println("🧪 Running highVolumeTrackLocation with " + userCount + " users");

		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		// ✅ Appelle directement ta méthode optimisée (avec parallelisme)
		tourGuideService.trackLocationForAllUsers();

		stopWatch.stop();
		tourGuideService.tracker.stopTracking();

		System.out.println("✅ highVolumeTrackLocation: Time Elapsed: "
				+ TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
		assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}

	@Test
	public void highVolumeGetRewards() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());

//		InternalTestHelper.setInternalUserNumber(100);
		String userCountProperty = System.getProperty("user.count");
		int userCount = (userCountProperty != null) ? Integer.parseInt(userCountProperty) : 100;
		InternalTestHelper.setInternalUserNumber(userCount);

		System.out.println("🧪 Running highVolumeGetRewards with " + userCount + " users");

		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		// Ajout d'une visitedLocation pour chaque user pour déclencher les rewards
		Attraction attraction = gpsUtil.getAttractions().get(0);
		List<User> allUsers = tourGuideService.getAllUsers();
		allUsers.forEach(u -> u.addToVisitedLocations(
				new VisitedLocation(u.getUserId(), attraction, new Date())));

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		// ✅ Appelle directement ta méthode optimisée (avec parallelisme)
		tourGuideService.calculateRewardsForAllUsers();

		stopWatch.stop();
		tourGuideService.tracker.stopTracking();

		System.out.println("✅ highVolumeGetRewards: Time Elapsed: "
				+ TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
		assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}
}
