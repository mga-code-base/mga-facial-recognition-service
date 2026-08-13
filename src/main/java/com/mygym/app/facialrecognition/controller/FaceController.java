package com.mygym.app.facialrecognition.controller;

import static com.mygym.app.facialrecognition.util.FacialConstants.FACIAL_REQUEST_MAPPING;
import static com.mygym.app.facialrecognition.util.FacialConstants.FACIAL_REQUEST_MAPPING_SAFETY;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mygym.app.facialrecognition.model.Member;
import com.mygym.app.facialrecognition.model.RecognitionResult;
import com.mygym.app.facialrecognition.service.FaceRecognitionService;

/**
 * REST controller providing endpoints for biometric face registration,
 * identity verification, profile listing, and deletion.
 */
@RestController
@RequestMapping({FACIAL_REQUEST_MAPPING, FACIAL_REQUEST_MAPPING_SAFETY})
public class FaceController {

    private final FaceRecognitionService faceService;

    public FaceController(FaceRecognitionService faceService) {
        this.faceService = faceService;
    }

    /**
     * Registers a member's facial embeddings extracted from an uploaded video stream.
     */
    @PostMapping("/register-video")
    public ResponseEntity<String> registerVideoWithMemberId(
            @RequestParam("username") String username, 
            @RequestParam("video") MultipartFile videoFile) {
        
        AbstractMap.SimpleEntry<Member, String> entryStatus = faceService.isExistingMember(username);
        if (Objects.isNull(entryStatus.getKey())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(entryStatus.getValue());
        }
        
        String memberId = entryStatus.getValue();
        
        try {
            faceService.processVideoAndRegister(memberId, videoFile);
            return ResponseEntity.ok("Video stream sliced, vectors calculated, and profiles registered successfully.");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Video pipeline failure: " + e.getMessage());
        }
    }

    /**
     * Identifies a member from an input image using vector search.
     */
    @PostMapping("/identify")
    public ResponseEntity<Map<String, Object>> identifyFace(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            float[] targetEmbedding = faceService.extractEmbeddingsFromFace(file);
            double targetThreshold = 75.0; 
            
            RecognitionResult result = faceService.identifyFace(targetEmbedding, targetThreshold);
            
            response.put("status", "success");
            response.put("identifiedUser", result.userId());
            response.put("confidenceScore", String.format("%.2f%%", result.confidenceScore()));
            response.put("requiredThreshold", targetThreshold + "%");

            if (!"UNKNOWN".equals(result.userId())) {
                try {
                    Member details = faceService.getMemberByMemberId(result.userId());
                    if (details != null) {
                        response.put("memberName", details.getName());
                    }
                } catch (Exception e) {
                    response.put("memberName", "Active Gym Member");
                }
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Retrieves all registered profile IDs.
     */
    @GetMapping("/profiles")
    public ResponseEntity<List<String>> listProfiles() {
        return ResponseEntity.ok(faceService.getAllRegisteredUserIds());
    }

    /**
     * Deletes a member's registered face profiles.
     */
    @DeleteMapping("/remove")
    public ResponseEntity<Map<String, String>> removeProfile(@RequestParam("userId") String userId) {
        Map<String, String> response = new HashMap<>();
        faceService.removeUserProfile(userId);
        response.put("status", "success");
        response.put("message", "Successfully erased all face vector rows for user: " + userId);
        return ResponseEntity.ok(response);
    }  
}