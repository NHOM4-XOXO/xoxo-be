package com.nhom4.xoxo.dto.res;

import java.time.LocalDateTime;

import org.modelmapper.ModelMapper;

import com.nhom4.xoxo.entity.Friendship;
import com.nhom4.xoxo.enums.FriendshipStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendshipResponse {
    private Long id;
    private UserResponse user;          
    private UserResponse friend;        
    private UserResponse initiator;      
    private FriendshipStatus status;     
    private LocalDateTime createdAt;     
    private LocalDateTime updatedAt;     
    

    public static FriendshipResponse fromFriendship(Friendship friendship, ModelMapper modelMapper) {
        return modelMapper.map(friendship, FriendshipResponse.class);
    }
} 