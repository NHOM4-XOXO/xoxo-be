package com.nhom4.xoxo.service;

import com.nhom4.xoxo.dto.res.SearchResultResponse;
import org.springframework.data.domain.Pageable;

public interface SearchService {
    SearchResultResponse searchAll(String keyword, Pageable pageable);
    SearchResultResponse searchUsers(String keyword, Pageable pageable);
    SearchResultResponse searchPosts(String keyword, Pageable pageable);
    SearchResultResponse searchGroups(String keyword, Pageable pageable);
}
