package com.nhom4.xoxo.controller;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.dto.req.FriendshipRequest;
import com.nhom4.xoxo.dto.res.FriendshipResponse;
import com.nhom4.xoxo.dto.res.UserResponse;
import com.nhom4.xoxo.dto.res.SuggestedFriendResponse;
import com.nhom4.xoxo.entity.Friendship;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.service.FriendshipService;
import com.nhom4.xoxo.graph.service.SocialGraphService;
import com.nhom4.xoxo.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/friendships")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;
    private final UserService userService;
    private final SocialGraphService socialGraphService;
    private final ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<WrapRes<FriendshipResponse>> createFriendship(
            @Valid @RequestBody FriendshipRequest request,
            Principal principal) {
        String email = principal.getName();
        User user = userService.findByEmail(email);

        Friendship friendship = friendshipService.createFriendship(user.getId(), request.getFriendId());
        FriendshipResponse response = FriendshipResponse.fromFriendship(friendship, modelMapper);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @PostMapping("/accept")
    public ResponseEntity<WrapRes<FriendshipResponse>> acceptFriendship(
            @RequestParam Long friendshipId,
            Principal principal) {
        String email = principal.getName();
        User user = userService.findByEmail(email);

        Friendship friendship = friendshipService.acceptFriendship(friendshipId, user.getId());
        // Update graph when friendship accepted
        socialGraphService.connectFriends(
                friendship.getUser().getId(), friendship.getUser().getUsername(),
                friendship.getFriend().getId(), friendship.getFriend().getUsername());
        FriendshipResponse response = FriendshipResponse.fromFriendship(friendship, modelMapper);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @PostMapping("/reject")
    public ResponseEntity<WrapRes<FriendshipResponse>> rejectFriendship(
            @RequestParam Long friendshipId,
            Principal principal) {
        String email = principal.getName();
        User user = userService.findByEmail(email);

        Friendship friendship = friendshipService.rejectFriendship(friendshipId, user.getId());
        FriendshipResponse response = FriendshipResponse.fromFriendship(friendship, modelMapper);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @GetMapping("/friends")
    public ResponseEntity<WrapRes<List<UserResponse>>> getFriends(Principal principal) {
        String email = principal.getName();
        User user = userService.findByEmail(email);

        List<User> friends = friendshipService.getFriendsByUserId(user.getId());
        List<UserResponse> friendResponses = friends.stream()
                .map(friend -> modelMapper.map(friend, UserResponse.class))
                .collect(Collectors.toList());

        return ResponseEntity.ok(WrapRes.success(friendResponses));
    }
    @GetMapping("/friends/{userId}")
    public  ResponseEntity<WrapRes<List<UserResponse>>> getFriendsByUserId(@PathVariable Long userId) {
        List<User> friends = friendshipService.getFriendsByUserId(userId);
        List<UserResponse> friendResponses = friends.stream()
                .map(friend -> modelMapper.map(friend, UserResponse.class))
                .collect(Collectors.toList());
        return ResponseEntity.ok(WrapRes.success(friendResponses));
    }
  
    

    @GetMapping("/suggestions")
    public ResponseEntity<WrapRes<List<SuggestedFriendResponse>>> suggestFriends(Principal principal) {
        if (principal == null)
            return ResponseEntity.status(401).build();
        String email = principal.getName();
        User current = userService.findByEmail(email);

        var suggestions = socialGraphService.suggestFriends(current.getId());

        var mapped = suggestions.stream()
                .map(item -> SuggestedFriendResponse.builder()
                        .id(item.id())
                        .username(item.username())
                        .mutualFriendsCount(item.mutualFriendsCount())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(WrapRes.success(mapped));
    }

    @GetMapping("/received/pending")
    public ResponseEntity<WrapRes<List<FriendshipResponse>>> getPendingFriendshipsEntity(Principal principal) {
        String email = principal.getName();
        User user = userService.findByEmail(email);

        List<Friendship> pendingFriendships = friendshipService.getPendingFriendships(user.getId());
        List<FriendshipResponse> response = pendingFriendships.stream()
                .map(friendship -> FriendshipResponse.fromFriendship(friendship, modelMapper))
                .collect(Collectors.toList());

        return ResponseEntity.ok(WrapRes.success(response));
    }

    @GetMapping("/sent/pending")
    public ResponseEntity<WrapRes<List<FriendshipResponse>>> getSentFriendshipsEntity(Principal principal) {
        String email = principal.getName();
        User user = userService.findByEmail(email);

        List<Friendship> sentFriendships = friendshipService.getSentFriendships(user.getId());
        List<FriendshipResponse> response = sentFriendships.stream()
                .map(friendship -> FriendshipResponse.fromFriendship(friendship, modelMapper))
                .collect(Collectors.toList());

        return ResponseEntity.ok(WrapRes.success(response));
    }
}