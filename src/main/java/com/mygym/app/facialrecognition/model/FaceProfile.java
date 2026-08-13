package com.mygym.app.facialrecognition.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.io.Serializable;

@Entity
@Table(name = "face_profiles")
@IdClass(FaceProfile.FaceProfileId.class)
public class FaceProfile {

    @Id
    @Column(name = "member_id", nullable = false)
    private String memberId;

    @Id
    @Column(name = "frame_id", nullable = false)
    private String frameId;

    @Column(name = "embedding", columnDefinition = "vector(512)", nullable = false)
    @JdbcTypeCode(SqlTypes.VECTOR)
    public float[] embedding;

    public FaceProfile() {}

    public FaceProfile(String memberId, String frameId, float[] embedding) {
        this.memberId = memberId;
        this.frameId = frameId;
        this.embedding = embedding;
    }

    public String getUserId() { return memberId; }
    public float[] getEmbedding() { return embedding; }

    // Serializable inner class for Composite Key tracking
    public static class FaceProfileId implements Serializable {
        private String memberId;
        private String frameId;
        public FaceProfileId() {}
        public FaceProfileId(String memberId, String frameId) {
            this.memberId = memberId;
            this.frameId = frameId;
        }
    }
}
