package com.example.memories.domain.room.dto.response;

import com.example.memories.domain.room.entity.RoomUser;
import com.example.memories.domain.room.entity.enums.RoomRole;
import com.example.memories.domain.user.entity.User;

import java.util.List;

public record RoomMemberListResponse(
        List<MemberDto> members,
        Long nextCursor,
        Boolean hasNext
) {
    public static RoomMemberListResponse of(
            List<MemberDto> members,
            Long nextCursor,
            Boolean hasNext
    ) {
        return new RoomMemberListResponse(
                members,
                nextCursor,
                hasNext
        );
    }

    public record MemberDto(
            Long userId,
            String name,
            RoomRole role
    ) {
        public static MemberDto from(RoomUser roomUser) {
            User user = roomUser.getUser();

            return new MemberDto(
                    user.getId(),
                    user.getName(),
                    roomUser.getRole()
            );
        }
    }
}
