package com.mygym.app.facialrecognition.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mygym.app.facialrecognition.model.FaceProfile;

@Repository
public interface FaceProfileRepository extends JpaRepository<FaceProfile, Long> {
    
    Optional<FaceProfile> findByMemberId(String memberId);
    
    void deleteByMemberId(String memberId);

    @Query(value = "SELECT * FROM face_profiles ORDER BY embedding <-> cast(:targetEmbedding as vector) LIMIT 1", nativeQuery = true)
    Optional<FaceProfile> findClosestFaceMatch(@Param("targetEmbedding") String targetEmbeddingAsString);
}
