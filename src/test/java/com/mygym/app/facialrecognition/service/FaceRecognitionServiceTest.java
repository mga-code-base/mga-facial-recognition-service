package com.mygym.app.facialrecognition.service;

import com.mygym.app.facialrecognition.model.FaceProfile;
import com.mygym.app.facialrecognition.model.Member;
import com.mygym.app.facialrecognition.model.RecognitionResult;
import com.mygym.app.facialrecognition.repository.FaceProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaceRecognitionServiceTest {

    @Mock
    private FaceProfileRepository profileRepository;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private FaceRecognitionService faceRecognitionService;

    @Test
    @DisplayName("Should validate active member eligible for face registration")
    void testIsExistingMember_EligibleMember_ReturnsSuccessEntry() {
        String username = "john_doe";
        Member mockMember = new Member();
        mockMember.setMemberId("MEM-1001");
        mockMember.setMobileNumber(9876543210L);
        mockMember.setIsActive(true);
        mockMember.setIsFacialRegCompleted(false);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), eq(username))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Member.class)).thenReturn(mockMember);

        AbstractMap.SimpleEntry<Member, String> result = faceRecognitionService.isExistingMember(username);

        assertNotNull(result.getKey());
        assertEquals("MEM-1001", result.getValue());
    }

    @Test
    @DisplayName("Should reject registration if member is inactive")
    void testIsExistingMember_InactiveMember_ReturnsInactiveErrorMessage() {
        String username = "inactive_user";
        Member mockMember = new Member();
        mockMember.setMemberId("MEM-1002");
        mockMember.setMobileNumber(9876543210L);
        mockMember.setIsActive(false);
        mockMember.setIsFacialRegCompleted(false);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), eq(username))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Member.class)).thenReturn(mockMember);

        AbstractMap.SimpleEntry<Member, String> result = faceRecognitionService.isExistingMember(username);

        assertNull(result.getKey());
        assertEquals("Member Inactive, Please contact Admin", result.getValue());
    }

    @Test
    @DisplayName("Should reject registration if facial registration is already completed")
    void testIsExistingMember_AlreadyRegistered_ReturnsAlreadyCompletedMessage() {
        String username = "registered_user";
        Member mockMember = new Member();
        mockMember.setMemberId("MEM-1003");
        mockMember.setMobileNumber(9876543210L);
        mockMember.setIsActive(true);
        mockMember.setIsFacialRegCompleted(true);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), eq(username))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Member.class)).thenReturn(mockMember);

        AbstractMap.SimpleEntry<Member, String> result = faceRecognitionService.isExistingMember(username);

        assertNull(result.getKey());
        assertEquals("Facial Registration Already completed", result.getValue());
    }

    @Test
    @DisplayName("Should return 'Unknown' when database has no saved face profiles")
    void testIdentifyFace_EmptyDatabase_ReturnsUnknown() {
        float[] targetEmbedding = new float[512];
        when(profileRepository.findAll()).thenReturn(Collections.emptyList());

        RecognitionResult result = faceRecognitionService.identifyFace(targetEmbedding, 75.0);

        assertEquals("Unknown", result.userId());
        assertEquals(0.0, result.confidenceScore());
    }

    @Test
    @DisplayName("Should identify matching member when embedding distance is within threshold")
    void testIdentifyFace_MatchingProfile_ReturnsIdentifiedUser() {
        float[] targetEmbedding = new float[512];
        float[] savedEmbedding = new float[512];

        FaceProfile mockProfile = new FaceProfile("MEM-1001", "frame1", savedEmbedding);
        when(profileRepository.findAll()).thenReturn(List.of(mockProfile));

        RecognitionResult result = faceRecognitionService.identifyFace(targetEmbedding, 75.0);

        assertEquals("MEM-1001", result.userId());
        assertTrue(result.confidenceScore() >= 75.0);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when face matches another registered member")
    void testRegisterBatchOfFaces_DuplicateFaceForOtherUser_ThrowsException() {
        String currentMemberId = "MEM-1002";
        float[] newEmbedding = new float[512];

        FaceProfile duplicateFace = new FaceProfile("MEM-9999", "frameX", new float[512]);

        when(profileRepository.findClosestFaceMatch(anyString()))
                .thenReturn(Optional.of(duplicateFace));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                faceRecognitionService.registerBatchOfFaces(currentMemberId, List.of(newEmbedding))
        );

        assertTrue(exception.getMessage().contains("Registration Rejected! This face already belongs to Member ID: MEM-9999"));
        verify(profileRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should save face batch successfully when face belongs to current member or is new")
    void testRegisterBatchOfFaces_ValidBatch_SavesAllProfiles() {
        String currentMemberId = "MEM-1001";
        float[] newEmbedding = new float[512];

        when(profileRepository.findClosestFaceMatch(anyString()))
                .thenReturn(Optional.empty());

        faceRecognitionService.registerBatchOfFaces(currentMemberId, List.of(newEmbedding));

        verify(profileRepository, times(1)).deleteByMemberId(currentMemberId);
        verify(profileRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Should trigger repository deleteByMemberId on removeUserProfile")
    void testRemoveUserProfile_CallsDeleteByMemberId() {
        String memberId = "MEM-1001";

        faceRecognitionService.removeUserProfile(memberId);

        verify(profileRepository, times(1)).deleteByMemberId(memberId);
    }
}