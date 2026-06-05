package com.example.memories.domain.diary.repository;

import com.example.memories.domain.diary.entity.Diary;
import com.example.memories.domain.diary.entity.DiaryImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DiaryImageRepository extends JpaRepository<DiaryImage, Long> {

    // 목록 조회 시 대표 이미지(sort_order = 0) 배치 조회
    @Query("SELECT di FROM DiaryImage di WHERE di.diary IN :diaries AND di.sortOrder = 0")
    List<DiaryImage> findThumbnailsByDiaries(@Param("diaries") List<Diary> diaries);
}