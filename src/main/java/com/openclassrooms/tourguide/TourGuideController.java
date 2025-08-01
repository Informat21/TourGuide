package com.openclassrooms.tourguide;

import java.util.List;
import java.util.stream.Collectors;

import com.openclassrooms.tourguide.DTO.NearbyAttractionDTO;
import com.openclassrooms.tourguide.service.RewardsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;

import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import tripPricer.Provider;

@RestController
public class TourGuideController {

	@Autowired
	TourGuideService tourGuideService;
	
    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }
    
    @RequestMapping("/getLocation") 
    public VisitedLocation getLocation(@RequestParam String userName) {
    	return tourGuideService.getUserLocation(getUser(userName));
    }
    
    //  TODO: Change this method to no longer return a List of Attractions.
 	//  Instead: Get the closest five tourist attractions to the user - no matter how far away they are.
 	//  Return a new JSON object that contains:
    	// Name of Tourist attraction, 
        // Tourist attractions lat/long, 
        // The user's location lat/long, 
        // The distance in miles between the user's location and each of the attractions.
        // The reward points for visiting each Attraction.
        //    Note: Attraction reward points can be gathered from RewardsCentral

    @Autowired
    RewardsService rewardsService;  // pour avoir getDistance et getRewardPoints

    @RequestMapping("/getNearbyAttractions")
    public List<NearbyAttractionDTO> getNearbyAttractions(@RequestParam String userName) {
        User user = getUser(userName);
        VisitedLocation visitedLocation = tourGuideService.getUserLocation(user);

        return tourGuideService.getNearByAttractions(visitedLocation).stream()
                .map(attraction -> {
                    double distance = rewardsService.getDistance(visitedLocation.location, attraction);
                    int rewardPoints = rewardsService.getRewardPoints(attraction, user);
                    return new NearbyAttractionDTO(
                            attraction.attractionName,
                            attraction.latitude,
                            attraction.longitude,
                            visitedLocation.location.latitude,
                            visitedLocation.location.longitude,
                            distance,
                            rewardPoints
                    );
                })
                .collect(Collectors.toList());
    }
//    @RequestMapping("/getNearbyAttractions")
//    public List<Attraction> getNearbyAttractions(@RequestParam String userName) {
//    	VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));
//    	return tourGuideService.getNearByAttractions(visitedLocation);
//    }
    
    @RequestMapping("/getRewards") 
    public List<UserReward> getRewards(@RequestParam String userName) {
    	return tourGuideService.getUserRewards(getUser(userName));
    }
       
    @RequestMapping("/getTripDeals")
    public List<Provider> getTripDeals(@RequestParam String userName) {
    	return tourGuideService.getTripDeals(getUser(userName));
    }
    
    private User getUser(String userName) {
        User user = tourGuideService.getUser(userName);
        if (user == null) {
            System.err.println("⚠️ User not found in controller: " + userName);
        } else {
            System.out.println("✅ User found in controller: " + userName);
        }
        return user;
//        return tourGuideService.getUser(userName);
    }

    @RequestMapping("/listUsers")
    public List<String> listUsers() {
        return tourGuideService.getAllUsers().stream()
                .map(User::getUserName)
                .collect(Collectors.toList());
    }

}