package com.example.memories.domain.chat.service;

import com.example.memories.domain.chat.dto.ChatType;
import com.example.memories.domain.chat.dto.request.ChatImageSendRequestDto;
import com.example.memories.domain.chat.dto.request.ChatSendRequestDto;
import com.example.memories.domain.chat.dto.response.ChatListResponseDto;
import com.example.memories.domain.chat.dto.response.ChatResponseDto;
import com.example.memories.domain.chat.dto.response.MessageDeletedResponseDto;
import com.example.memories.domain.chat.entity.Chat;
import com.example.memories.domain.chat.entity.ChatImage;
import com.example.memories.domain.chat.event.ChatCreatedEvent;
import com.example.memories.domain.chat.event.ChatDeletedEvent;
import com.example.memories.domain.chat.exception.ChatErrorCode;
import com.example.memories.domain.chat.repository.ChatImageRepository;
import com.example.memories.domain.chat.repository.ChatRepository;
import com.example.memories.domain.room.entity.Room;
import com.example.memories.domain.room.entity.enums.RoomType;
import com.example.memories.domain.room.exception.RoomErrorCode;
import com.example.memories.domain.room.repository.RoomRepository;
import com.example.memories.domain.room.repository.RoomUserRepository;
import com.example.memories.domain.user.entity.AuthProvider;
import com.example.memories.domain.user.entity.User;
import com.example.memories.global.exception.BusinessException;
import com.example.memories.infra.s3.S3ImageDeleteEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock private ChatRepository chatRepository;
    @Mock private ChatImageRepository chatImageRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private RoomUserRepository roomUserRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private ChatServiceImpl chatService;

    private User user1;
    private User user2;
    private Room room;

    @BeforeEach
    void setUp() {
        user1 = User.builder()
                .name("홍길동")
                .email("hong@test.com")
                .provider(AuthProvider.GOOGLE)
                .providerId("google-1")
                .build();
        setId(user1, 1L);

        user2 = User.builder()
                .name("김철수")
                .email("kim@test.com")
                .provider(AuthProvider.GOOGLE)
                .providerId("google-2")
                .build();
        setId(user2, 2L);

        room = Room.builder()
                .title("테스트 방")
                .roomCode("ABC123")
                .type(RoomType.GROUP)
                .build();
        setId(room, 1L);
    }

    private void setId(Object target, Long id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (NoSuchFieldException e) {
            try {
                Field field = target.getClass().getSuperclass().getDeclaredField("id");
                field.setAccessible(true);
                field.set(target, id);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("텍스트 메시지 전송")
    class SendMessage {

        @Test
        @DisplayName("텍스트 메시지 전송 성공")
        void sendMessage_success() {
            // given
            Long roomId = 1L;
            ChatSendRequestDto request = new ChatSendRequestDto("안녕하세요");

            given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
            given(roomUserRepository.existsByRoomAndUser(room, user1)).willReturn(true);
            given(chatRepository.save(any(Chat.class))).willAnswer(invocation -> {
                Chat chat = invocation.getArgument(0);
                setId(chat, 1L);
                return chat;
            });

            // when
            ChatResponseDto response = chatService.sendMessage(roomId, user1, request);

            // then
            assertThat(response.chatId()).isEqualTo(1L);
            assertThat(response.senderId()).isEqualTo(1L);
            assertThat(response.senderName()).isEqualTo("홍길동");
            assertThat(response.type()).isEqualTo(ChatType.TEXT);
            assertThat(response.content()).isEqualTo("안녕하세요");
            assertThat(response.imageKeys()).isEmpty();

            verify(chatRepository).save(any(Chat.class));
            verify(eventPublisher).publishEvent(any(ChatCreatedEvent.class));
        }

        @Test
        @DisplayName("존재하지 않는 방에 메시지 전송 시 예외")
        void sendMessage_roomNotFound() {
            // given
            Long roomId = 999L;
            ChatSendRequestDto request = new ChatSendRequestDto("안녕하세요");

            given(roomRepository.findById(roomId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> chatService.sendMessage(roomId, user1, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", RoomErrorCode.ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("방 멤버가 아닌 사용자가 메시지 전송 시 예외")
        void sendMessage_forbidden() {
            // given
            Long roomId = 1L;
            ChatSendRequestDto request = new ChatSendRequestDto("안녕하세요");

            given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
            given(roomUserRepository.existsByRoomAndUser(room, user1)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> chatService.sendMessage(roomId, user1, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", RoomErrorCode.ROOM_FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("이미지 메시지 전송")
    class SendImageMessage {

        @Test
        @DisplayName("이미지 메시지 전송 성공")
        void sendImageMessage_success() {
            // given
            Long roomId = 1L;
            ChatImageSendRequestDto request =
                    new ChatImageSendRequestDto(List.of("chat/3.jpg", "chat/1.jpg", "chat/2.jpg"));

            given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
            given(roomUserRepository.existsByRoomAndUser(room, user1)).willReturn(true);
            given(chatRepository.save(any(Chat.class))).willAnswer(invocation -> {
                Chat chat = invocation.getArgument(0);
                setId(chat, 1L);
                return chat;
            });

            // when
            ChatResponseDto response = chatService.sendImageMessage(roomId, user1, request);

            // then
            assertThat(response.chatId()).isEqualTo(1L);
            assertThat(response.type()).isEqualTo(ChatType.IMAGE);
            assertThat(response.content()).isNull();
            assertThat(response.imageKeys()).containsExactly("chat/3.jpg", "chat/1.jpg", "chat/2.jpg");

            verify(chatRepository).save(any(Chat.class));
            verify(chatImageRepository).saveAll(argThat(chatImages -> {
                List<ChatImage> list = (List<ChatImage>) chatImages;
                return list.size() == 3
                        && list.get(0).getImageKey().equals("chat/3.jpg")
                        && list.get(0).getSortOrder().equals(1)
                        && list.get(1).getImageKey().equals("chat/1.jpg")
                        && list.get(1).getSortOrder().equals(2)
                        && list.get(2).getImageKey().equals("chat/2.jpg")
                        && list.get(2).getSortOrder().equals(3);
            }));

            verify(eventPublisher).publishEvent(any(ChatCreatedEvent.class));
        }
    }

    @Nested
    @DisplayName("채팅 기록 조회")
    class GetChats {

        @Test
        @DisplayName("채팅 기록 첫 조회 성공")
        void getChats_firstPage_success() {
            // given
            Long roomId = 1L;
            Long cursor = null;
            int size = 2;

            Chat chat3 = Chat.builder().room(room).user(user1).content("메시지3").build();
            setId(chat3, 3L);

            Chat chat2 = Chat.builder().room(room).user(user2).content("메시지2").build();
            setId(chat2, 2L);

            Chat chat1 = Chat.builder().room(room).user(user1).content("메시지1").build();
            setId(chat1, 1L);

            given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
            given(roomUserRepository.existsByRoomAndUser(room, user1)).willReturn(true);
            given(chatRepository.findByRoomOrderByIdDescWithUser(eq(room), any(Pageable.class)))
                    .willReturn(List.of(chat3, chat2, chat1));
            given(chatImageRepository.findByChatInOrderBySortOrderAsc(anyList()))
                    .willReturn(List.of());

            // when
            ChatListResponseDto response = chatService.getChats(roomId, user1, cursor, size);

            // then
            assertThat(response.messages()).hasSize(2);
            assertThat(response.messages().get(0).chatId()).isEqualTo(3L);
            assertThat(response.messages().get(1).chatId()).isEqualTo(2L);
            assertThat(response.hasNext()).isTrue();
            assertThat(response.nextCursor()).isEqualTo(2L);
        }

        @Test
        @DisplayName("커서 기반 채팅 기록 조회 성공")
        void getChats_withCursor_success() {
            // given
            Long roomId = 1L;
            Long cursor = 3L;
            int size = 2;

            Chat chat2 = Chat.builder().room(room).user(user2).content("메시지2").build();
            setId(chat2, 2L);

            Chat chat1 = Chat.builder().room(room).user(user1).content("메시지1").build();
            setId(chat1, 1L);

            given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
            given(roomUserRepository.existsByRoomAndUser(room, user1)).willReturn(true);
            given(chatRepository.findByRoomAndIdLessThanOrderByIdDescWithUser(eq(room), eq(cursor), any(Pageable.class)))
                    .willReturn(List.of(chat2, chat1));
            given(chatImageRepository.findByChatInOrderBySortOrderAsc(anyList()))
                    .willReturn(List.of());

            // when
            ChatListResponseDto response = chatService.getChats(roomId, user1, cursor, size);

            // then
            assertThat(response.messages()).hasSize(2);
            assertThat(response.hasNext()).isFalse();
            assertThat(response.nextCursor()).isNull();
        }
    }

    @Nested
    @DisplayName("채팅 삭제")
    class DeleteChat {

        @Test
        @DisplayName("본인 채팅 삭제 성공")
        void deleteChat_success() {
            // given
            Long roomId = 1L;
            Long chatId = 1L;

            Chat chat = Chat.builder()
                    .room(room)
                    .user(user1)
                    .content("삭제할 메시지")
                    .build();
            setId(chat, chatId);

            given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
            given(roomUserRepository.existsByRoomAndUser(room, user1)).willReturn(true);
            given(chatRepository.findByIdAndRoom(chatId, room)).willReturn(Optional.of(chat));
            given(chatImageRepository.findAllByChat(chat)).willReturn(List.of());

            // when
            chatService.deleteChat(roomId, chatId, user1);

            // then
            verify(chatRepository).delete(chat);
            verify(eventPublisher).publishEvent(any(ChatDeletedEvent.class));
            verify(eventPublisher, never()).publishEvent(any(S3ImageDeleteEvent.class));
        }

        @Test
        @DisplayName("이미지 채팅 삭제 시 S3 이미지 삭제 이벤트를 발행한다")
        void deleteChat_imageChat_publishS3DeleteEvent() {
            // given
            Long roomId = 1L;
            Long chatId = 1L;

            Chat chat = Chat.builder()
                    .room(room)
                    .user(user1)
                    .content(null)
                    .build();
            setId(chat, chatId);

            ChatImage chatImage = ChatImage.builder()
                    .chat(chat)
                    .imageKey("chat/1.jpg")
                    .sortOrder(1)
                    .build();
            setId(chatImage, 1L);

            given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
            given(roomUserRepository.existsByRoomAndUser(room, user1)).willReturn(true);
            given(chatRepository.findByIdAndRoom(chatId, room)).willReturn(Optional.of(chat));
            given(chatImageRepository.findAllByChat(chat)).willReturn(List.of(chatImage));

            // when
            chatService.deleteChat(roomId, chatId, user1);

            // then
            verify(chatRepository).delete(chat);
            verify(eventPublisher).publishEvent(any(S3ImageDeleteEvent.class));
            verify(eventPublisher).publishEvent(any(ChatDeletedEvent.class));
        }

        @Test
        @DisplayName("남의 채팅 삭제 시 예외")
        void deleteChat_forbidden() {
            // given
            Long roomId = 1L;
            Long chatId = 1L;

            Chat chat = Chat.builder()
                    .room(room)
                    .user(user2)
                    .content("남의 메시지")
                    .build();
            setId(chat, chatId);

            given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
            given(roomUserRepository.existsByRoomAndUser(room, user1)).willReturn(true);
            given(chatRepository.findByIdAndRoom(chatId, room)).willReturn(Optional.of(chat));

            // when & then
            assertThatThrownBy(() -> chatService.deleteChat(roomId, chatId, user1))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHAT_FORBIDDEN);
        }

        @Test
        @DisplayName("존재하지 않는 채팅 삭제 시 예외")
        void deleteChat_notFound() {
            // given
            Long roomId = 1L;
            Long chatId = 999L;

            given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
            given(roomUserRepository.existsByRoomAndUser(room, user1)).willReturn(true);
            given(chatRepository.findByIdAndRoom(chatId, room)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> chatService.deleteChat(roomId, chatId, user1))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHAT_NOT_FOUND);
        }
    }
}