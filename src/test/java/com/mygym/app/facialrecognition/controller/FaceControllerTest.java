package com.mygym.app.facialrecognition.controller;

import com.mygym.app.facialrecognition.model.Member;
import com.mygym.app.facialrecognition.model.RecognitionResult;
import com.mygym.app.facialrecognition.service.FaceRecognitionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaceControllerTest {

    @Mock
    private FaceRecognitionService faceService;

    @InjectMocks
    private FaceController faceController;

    @Test
    @DisplayName("Should register video successfully when member exists in People CRUD")
    void testRegisterVideoWithMemberId_Success() throws Exception {
        String username = "john_doe";
        String memberId = "MEM-1001";
        MockMultipartFile videoFile = new MockMultipartFile("video", "sample.mp4", "video/mp4", "video_bytes".getBytes());
        Member member = new Member();
        member.setMemberId(memberId);

        when(faceService.isExistingMember(username))
                .thenReturn(new AbstractMap.SimpleEntry<>(member, memberId));

        ResponseEntity<String> response = faceController.registerVideoWithMemberId(username, videoFile);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Video stream sliced, vectors calculated"));
        verify(faceService, times(1)).processVideoAndRegister(memberId, videoFile);
    }

    @Test
    @DisplayName("Should return 409 Conflict when member is not registered in People CRUD")
    void testRegisterVideoWithMemberId_UnregisteredMember_ReturnsConflict() throws Exception {
        String username = "unknown_user";
        MockMultipartFile videoFile = new MockMultipartFile("video", "sample.mp4", "video/mp4", "video_bytes".getBytes());

        when(faceService.isExistingMember(username))
                .thenReturn(new AbstractMap.SimpleEntry<>(null, "Unregistered Member"));

        ResponseEntity<String> response = faceController.registerVideoWithMemberId(username, videoFile);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Unregistered Member", response.getBody());
        verify(faceService, never()).processVideoAndRegister(anyString(), any());
    }

    @Test
    @DisplayName("Should identify face successfully and attach member details")
    void testIdentifyFace_Success() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile("file", "face.jpg", "image/jpeg", "image_bytes".getBytes());
        float[] mockEmbeddings = new float[512];
        RecognitionResult result = new RecognitionResult("MEM-1001", 88.5);
        Member memberDetails = new Member();
        memberDetails.setName("John Doe");

        when(faceService.extractEmbeddingsFromFace(imageFile)).thenReturn(mockEmbeddings);
        when(faceService.identifyFace(mockEmbeddings, 75.0)).thenReturn(result);
        when(faceService.getMemberByMemberId("MEM-1001")).thenReturn(memberDetails);

        ResponseEntity<Map<String, Object>> response = faceController.identifyFace(imageFile);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("success", body.get("status"));
        assertEquals("MEM-1001", body.get("identifiedUser"));
        assertEquals("John Doe", body.get("memberName"));
    }

    @Test
    @DisplayName("Should list all registered facial profiles successfully")
    void testListProfiles_Success() {
        when(faceService.getAllRegisteredUserIds()).thenReturn(List.of("MEM-1001", "MEM-1002"));

        ResponseEntity<List<String>> response = faceController.listProfiles();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        assertTrue(response.getBody().contains("MEM-1001"));
    }

    @Test
    @DisplayName("Should remove user facial profile successfully")
    void testRemoveProfile_Success() {
        String userId = "MEM-1001";

        ResponseEntity<Map<String, String>> response = faceController.removeProfile(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("success", response.getBody().get("status"));
        verify(faceService, times(1)).removeUserProfile(userId);
    }
}