package com.nhom4.xoxo.graph.service;

import java.util.List;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import com.nhom4.xoxo.graph.projection.SuggestedFriendDto;
import com.nhom4.xoxo.graph.repository.UserGraphRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SocialGraphService {
    private final UserGraphRepository userGraphRepository;
    private final Neo4jClient neo4jClient;

    @Transactional(transactionManager = "neo4jTransactionManager")
    public void ensureUserNode(Long userId, String username) {
        neo4jClient.query(
            "MERGE (u:User {id: $id}) SET u.username = $username")
            .bind(userId).to("id")
            .bind(username).to("username")
            .run();
    }

    @Transactional(transactionManager = "neo4jTransactionManager")
    public void connectFriends(Long userId, String userUsername, Long friendId, String friendUsername) {
        // Ensure both nodes exist with latest usernames
        ensureUserNode(userId, userUsername);
        ensureUserNode(friendId, friendUsername);
        // Create undirected friendship by two relationships
        neo4jClient.query(
            "MATCH (a:User {id: $a}), (b:User {id: $b}) " +
            "MERGE (a)-[:FRIENDS_WITH]->(b) " +
            "MERGE (b)-[:FRIENDS_WITH]->(a)")
            .bind(userId).to("a")
            .bind(friendId).to("b")
            .run();
    }

    @Transactional(readOnly = true, transactionManager = "neo4jTransactionManager")
    public List<SuggestedFriendDto> suggestFriends(Long userId) {
        return userGraphRepository.suggestFriendsViaMutuals(userId);
    }

    @Transactional(transactionManager = "neo4jTransactionManager")
    public void deleteUserNode(Long userId) {
        neo4jClient.query("MATCH (u:User {id: $id}) DETACH DELETE u")
                .bind(userId).to("id")
                .run();
    }
}


