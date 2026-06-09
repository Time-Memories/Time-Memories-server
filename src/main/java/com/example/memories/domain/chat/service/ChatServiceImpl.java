package com.example.memories.domain.chat.service;

import com.example.memories.domain.chat.dto.request.ChatImageSendRequestDto;
import com.example.memories.domain.chat.dto.request.ChatSendRequestDto;
import com.example.memories.domain.chat.dto.response.ChatListResponseDto;
import com.example.memories.domain.chat.dto.response.ChatResponseDto;
import com.example.memories.domain.chat.dto.response.MessageDeletedResponseDto;
import com.example.memories.domain.chat.entity.Chat;
import com.example.memories.domain.chat.entity.ChatImage;
import com.example.memories.domain.chat.exception.ChatErrorCode;
import com.example.memories.domain.chat.repository.ChatImageRepository;
import com.example.memories.domain.chat.repository.ChatRepository;
import com.example.memories.domain.room.entity.Room;
import com.example.memories.domain.room.exception.RoomErrorCode;
import com.example.memories.domain.room.repository.RoomRepository;
import com.example.memories.domain.room.repository.RoomUserRepository;
import com.example.memories.domain.user.entity.User;
import com.example.memories.global.exception.BusinessException;
import com.example.memories.infra.s3.S3ImageDeleteEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatServiceImpl implements ChatService{

    private final ChatRepository chatRepository;
    private final ChatImageRepository chatImageRepository;
    private final RoomRepository roomRepository;
    private final RoomUserRepository roomUserRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ChatResponseDto sendMessage(Long roomId, User user, ChatSendRequestDto request) {
        // 방 존재 여부 확인
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));

        // 방 멤버 여부 확인
        if (!roomUserRepository.existsByRoomAndUser(room, user)) {
            throw new BusinessException(RoomErrorCode.ROOM_FORBIDDEN);
        }

        // 채팅 메시지 저장
        Chat chat = Chat.builder()
                .room(room)
                .user(user)
                .content(request.content())
                .build();

        chatRepository.save(chat);

        ChatResponseDto response = ChatResponseDto.from(chat, List.of());

        // WebSocket으로 메시지 브로드캐스트
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId, response);

        return response;
    }

    @Override
    @Transactional
    public ChatResponseDto sendImageMessage(Long roomId, User user, ChatImageSendRequestDto request) {
        // 방 존재 여부 확인
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));

        // 방 멤버 여부 확인
        if (!roomUserRepository.existsByRoomAndUser(room, user)) {
            throw new BusinessException(RoomErrorCode.ROOM_FORBIDDEN);
        }

        // 이미지 메시지용 Chat 저장
        Chat chat = Chat.builder()
                .room(room)
                .user(user)
                .content(null)
                .build();

        chatRepository.save(chat);

        // ChatImage 저장
        List<ChatImage> chatImages = IntStream.range(0, request.imageKeys().size())
                .mapToObj(index -> ChatImage.builder()
                        .chat(chat)
                        .imageKey(request.imageKeys().get(index))
                        .sortOrder(index + 1)
                        .build())
                .toList();

        chatImageRepository.saveAll(chatImages);

        ChatResponseDto response = ChatResponseDto.from(chat, request.imageKeys());

        // WebSocket 구독자들에게 실시간 메시지 브로드캐스트
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId, response);

        return response;
    }

    @Override
    public ChatListResponseDto getChats(Long roomId, User user, Long cursor, int size) {
        // 방 존재 여부 확인
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));

        // 방 멤버 여부 확인
        if (!roomUserRepository.existsByRoomAndUser(room, user)) {
            throw new BusinessException(RoomErrorCode.ROOM_FORBIDDEN);
        }

        // size + 1개 조회해서 다음 페이지 여부 확인
        Pageable pageable = PageRequest.of(0, size + 1);

        // 첫 조회(cursor == null) 또는 커서 기반 채팅 조회
        List<Chat> chats = (cursor == null)
                ? chatRepository.findByRoomOrderByIdDescWithUser(room, pageable)
                : chatRepository.findByRoomAndIdLessThanOrderByIdDescWithUser(room, cursor, pageable);

        // 다음 페이지 있으면 size 개수만 포함
        boolean hasNext = chats.size() > size;
        if (hasNext) {
            chats = chats.subList(0, size);
        }

        // 조회된 채팅들의 이미지들을 한 번에 조회
        List<ChatImage> chatImages = chatImageRepository.findByChatInOrderBySortOrderAsc(chats);

        // 이미지 키가 비었으면 텍스트 채팅, 있으면 이미지 채팅
        Map<Long, List<String>> imageKeysByChatId = chatImages.stream()
                .collect(Collectors.groupingBy(
                        chatImage -> chatImage.getChat().getId(),
                        Collectors.mapping(ChatImage::getImageKey, Collectors.toList())
                ));

        List<ChatResponseDto> responses = chats.stream()
                .map(chat -> ChatResponseDto.from(
                        chat,
                        imageKeysByChatId.getOrDefault(chat.getId(), List.of())
                ))
                .toList();

        // 다음 페이지 커서 계산
        Long nextCursor = hasNext && !chats.isEmpty()
                ? chats.get(chats.size() - 1).getId()
                : null;

        return new ChatListResponseDto(
                responses,
                hasNext,
                nextCursor
        );
    }

    @Override
    @Transactional
    public void deleteChat(Long roomId, Long chatId, User user) {
        // 방 존재 여부 확인
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));

        // 방 멤버 여부 확인
        if (!roomUserRepository.existsByRoomAndUser(room, user)) {
            throw new BusinessException(RoomErrorCode.ROOM_FORBIDDEN);
        }

        // 채팅 조회
        Chat chat = chatRepository.findByIdAndRoom(chatId, room)
                .orElseThrow(() -> new BusinessException(ChatErrorCode.CHAT_NOT_FOUND));

        // 본인만 삭제 가능
        if (!chat.getUserId().equals(user.getId())) {
            throw new BusinessException(ChatErrorCode.CHAT_FORBIDDEN);
        }

        // 삭제할 채팅 이미지 key 먼저 조회
        List<String> keysToDelete = chatImageRepository.findAllByChat(chat).stream()
                .map(ChatImage::getImageKey)
                .toList();

        // 채팅 삭제
        chatRepository.delete(chat);

        // DB 커밋 성공 후 S3 이미지 삭제
        if (!keysToDelete.isEmpty()) {
            eventPublisher.publishEvent(new S3ImageDeleteEvent(keysToDelete));
        }

        messagingTemplate.convertAndSend(
                "/topic/rooms/" + roomId + "/updates",
                new MessageDeletedResponseDto(chatId)
        );
    }
}
