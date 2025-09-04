
package com.nhom4.xoxo.graph.repository;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param; // <- dùng Param của Spring Data

import com.nhom4.xoxo.graph.node.UserNode;
import com.nhom4.xoxo.graph.projection.SuggestedFriendDto;

public interface UserGraphRepository extends Neo4jRepository<UserNode, Long> {

    @Query("MATCH (u:User {id: $userId})-[:FRIENDS_WITH]-(f:User) RETURN f")
    List<UserNode> findFriends(@Param("userId") Long userId);

    @Query("""
            MATCH (u:User {id: $userId})-[:FRIENDS_WITH]-(f:User)-[:FRIENDS_WITH]-(fof:User)
            WHERE fof.id <> $userId AND NOT (u)-[:FRIENDS_WITH]-(fof)
            WITH fof, count(DISTINCT f) AS mutualFriendsCount
            ORDER BY mutualFriendsCount DESC
            RETURN fof.id AS id, fof.username AS username, toInteger(mutualFriendsCount) AS mutualFriendsCount
            LIMIT 50
            """)
    List<SuggestedFriendDto> suggestFriendsViaMutuals(@Param("userId") Long userId);
}