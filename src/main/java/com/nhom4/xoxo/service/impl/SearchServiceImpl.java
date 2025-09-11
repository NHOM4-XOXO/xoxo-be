package com.nhom4.xoxo.service.impl;

import com.nhom4.xoxo.dto.res.SearchResultResponse;
import com.nhom4.xoxo.entity.Group;
import com.nhom4.xoxo.entity.Post;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.repository.GroupRepository;
import com.nhom4.xoxo.repository.PostRepository;
import com.nhom4.xoxo.repository.UserRepository;
import com.nhom4.xoxo.service.CloudinaryService;
import com.nhom4.xoxo.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final GroupRepository groupRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    public SearchResultResponse searchAll(String keyword, Pageable pageable) {
        log.info("Searching all entities with keyword: {}", keyword);
        
        // Search with smaller page sizes for each entity type
        Pageable userPageable = Pageable.ofSize(Math.min(5, pageable.getPageSize()));
        Pageable postPageable = Pageable.ofSize(Math.min(10, pageable.getPageSize()));
        Pageable groupPageable = Pageable.ofSize(Math.min(5, pageable.getPageSize()));
        
        // Search users
        Page<User> users = userRepository.searchUsersPageable(keyword, userPageable);
        List<SearchResultResponse.UserSearchItem> userItems = users.getContent().stream()
                .map(this::mapToUserSearchItem)
                .collect(Collectors.toList());
        
        // Search posts (only public and active)
        Page<Post> posts = postRepository.searchPublicPostsPageable(keyword, postPageable);
        List<SearchResultResponse.PostSearchItem> postItems = posts.getContent().stream()
                .map(this::mapToPostSearchItem)
                .collect(Collectors.toList());
        
        // Search groups (only active)
        Page<Group> groups = groupRepository.searchActiveGroupsPageable(keyword, groupPageable);
        List<SearchResultResponse.GroupSearchItem> groupItems = groups.getContent().stream()
                .map(this::mapToGroupSearchItem)
                .collect(Collectors.toList());
        
        return SearchResultResponse.builder()
                .users(userItems)
                .posts(postItems)
                .groups(groupItems)
                .totalUsers((int) users.getTotalElements())
                .totalPosts((int) posts.getTotalElements())
                .totalGroups((int) groups.getTotalElements())
                .totalResults((int) (users.getTotalElements() + posts.getTotalElements() + groups.getTotalElements()))
                .build();
    }

    @Override
    public SearchResultResponse searchUsers(String keyword, Pageable pageable) {
        log.info("Searching users with keyword: {}", keyword);
        
        Page<User> users = userRepository.searchUsersPageable(keyword, pageable);
        List<SearchResultResponse.UserSearchItem> userItems = users.getContent().stream()
                .map(this::mapToUserSearchItem)
                .collect(Collectors.toList());
        
        return SearchResultResponse.builder()
                .users(userItems)
                .posts(List.of())
                .groups(List.of())
                .totalUsers((int) users.getTotalElements())
                .totalPosts(0)
                .totalGroups(0)
                .totalResults((int) users.getTotalElements())
                .build();
    }

    @Override
    public SearchResultResponse searchPosts(String keyword, Pageable pageable) {
        log.info("Searching posts with keyword: {}", keyword);
        
        Page<Post> posts = postRepository.searchPublicPostsPageable(keyword, pageable);
        List<SearchResultResponse.PostSearchItem> postItems = posts.getContent().stream()
                .map(this::mapToPostSearchItem)
                .collect(Collectors.toList());
        
        return SearchResultResponse.builder()
                .users(List.of())
                .posts(postItems)
                .groups(List.of())
                .totalUsers(0)
                .totalPosts((int) posts.getTotalElements())
                .totalGroups(0)
                .totalResults((int) posts.getTotalElements())
                .build();
    }

    @Override
    public SearchResultResponse searchGroups(String keyword, Pageable pageable) {
        log.info("Searching groups with keyword: {}", keyword);
        
        Page<Group> groups = groupRepository.searchActiveGroupsPageable(keyword, pageable);
        List<SearchResultResponse.GroupSearchItem> groupItems = groups.getContent().stream()
                .map(this::mapToGroupSearchItem)
                .collect(Collectors.toList());
        
        return SearchResultResponse.builder()
                .users(List.of())
                .posts(List.of())
                .groups(groupItems)
                .totalUsers(0)
                .totalPosts(0)
                .totalGroups((int) groups.getTotalElements())
                .totalResults((int) groups.getTotalElements())
                .build();
    }

    private SearchResultResponse.UserSearchItem mapToUserSearchItem(User user) {
        return SearchResultResponse.UserSearchItem.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .avatarUrl(cloudinaryService.buildCloudinaryUrl(user.getAvatarUrl(), com.nhom4.xoxo.enums.MediaType.IMAGE))
                .bio(user.getBio())
                .build();
    }

    private SearchResultResponse.PostSearchItem mapToPostSearchItem(Post post) {
        return SearchResultResponse.PostSearchItem.builder()
                .id(post.getId())
                .content(post.getContent())
                .authorName(post.getAuthor().getFirstName() + " " + post.getAuthor().getLastName())
                .authorAvatarUrl(cloudinaryService.buildCloudinaryUrl(post.getAuthor().getAvatarUrl(), com.nhom4.xoxo.enums.MediaType.IMAGE))
                .authorId(post.getAuthor().getId())
                .location(post.getLocation())
                .hashtags(post.getHashtags())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .shareCount(post.getShareCount())
                .createdAt(post.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    private SearchResultResponse.GroupSearchItem mapToGroupSearchItem(Group group) {
        return SearchResultResponse.GroupSearchItem.builder()
                .id(group.getId())
                .title(group.getTitle())
                .description(group.getDescription())
                .coverUrl(cloudinaryService.buildCloudinaryUrl(group.getCoverUrl(), com.nhom4.xoxo.enums.MediaType.IMAGE))
                .creatorName(group.getCreator().getFirstName() + " " + group.getCreator().getLastName())
                .privacy(group.getPrivacy().toString())
                .memberCount(group.getMemberCount())
                .postCount(group.getPostCount())
                .location(group.getLocation())
                .tags(group.getTags())
                .build();
    }
}
