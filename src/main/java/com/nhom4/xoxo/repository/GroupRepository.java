package com.nhom4.xoxo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nhom4.xoxo.entity.Group;
import com.nhom4.xoxo.entity.User;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    
    Page<Group> findByCreator(User creator, Pageable pageable);
    
    @Query("SELECT g FROM Group g WHERE g.title LIKE %:keyword% OR g.description LIKE %:keyword%")
    Page<Group> findByTitleOrDescription(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT g FROM Group g WHERE g.title LIKE %:title%")
    Page<Group> findByTitle(String title, Pageable pageable);
}
