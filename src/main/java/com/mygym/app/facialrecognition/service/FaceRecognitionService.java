package com.mygym.app.facialrecognition.service;

import static com.mygym.app.facialrecognition.util.FacialConstants.MEMBER_GET_BY_MEMBERID_ENDPOINT;
import static com.mygym.app.facialrecognition.util.FacialConstants.MEMBER_GET_BY_USERNAME_ENDPOINT;
import static com.mygym.app.facialrecognition.util.FacialConstants.MEMBER_REST_CLIENT_BEAN;
import static com.mygym.app.facialrecognition.util.FacialConstants.MEMBER_UPDATE_FACE_REGIS_ENDPOINT;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import com.mygym.app.facialrecognition.model.FaceProfile;
import com.mygym.app.facialrecognition.model.Member;
import com.mygym.app.facialrecognition.model.RecognitionResult;
import com.mygym.app.facialrecognition.repository.FaceProfileRepository;

import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;

/**
 * Service orchestrating facial detection, embedding extraction, and pgvector persistence.
 */
@Service
public class FaceRecognitionService {
	
    private RestClient restClient;
    private ZooModel<Image, float[]> recognitionModel;
    private Predictor<Image, float[]> faceRecognizer;
    private Net dnnFaceDetector;
    private final FaceProfileRepository profileRepository;
    
    public FaceRecognitionService(@Qualifier(MEMBER_REST_CLIENT_BEAN) RestClient restClient, FaceProfileRepository profileRepository) {
        this.restClient = restClient;
        this.profileRepository = profileRepository;
    }
    
    /**
     * Initializes OpenCV native binaries and DJL PyTorch FaceNet model weights.
     */
    @PostConstruct
    public void init() throws Exception {
        nu.pattern.OpenCV.loadLocally();

        ClassPathResource protoResource = new ClassPathResource("deploy.prototxt");
        ClassPathResource modelResource = new ClassPathResource("res10_300x300_ssd_iter_140000.caffemodel");

        Path tempProto = Files.createTempFile("deploy", ".prototxt");
        Path tempModel = Files.createTempFile("res10", ".caffemodel");

        try (InputStream in = protoResource.getInputStream()) { Files.copy(in, tempProto, StandardCopyOption.REPLACE_EXISTING); }
        try (InputStream in = modelResource.getInputStream()) { Files.copy(in, tempModel, StandardCopyOption.REPLACE_EXISTING); }

        this.dnnFaceDetector = Dnn.readNetFromCaffe(tempProto.toAbsolutePath().toString(), tempModel.toAbsolutePath().toString());
        
        Files.deleteIfExists(tempProto);
        Files.deleteIfExists(tempModel);

        ClassPathResource recResource = new ClassPathResource("facenet.pt");
        Path tempModelDir = Files.createTempDirectory("djl_models");
        Path targetModelFile = tempModelDir.resolve("facenet.pt");
        
        try (InputStream in = recResource.getInputStream()) {
            Files.copy(in, targetModelFile, StandardCopyOption.REPLACE_EXISTING);
        }

        Criteria<Image, float[]> recCriteria = Criteria.builder()
                .setTypes(Image.class, float[].class)
                .optEngine("PyTorch")
                .optModelPath(tempModelDir)
                .optModelName("facenet")
                .optTranslator(new FaceNetTranslator())
                .build();
                
        this.recognitionModel = recCriteria.loadModel();
        this.faceRecognizer = recognitionModel.newPredictor();
    }

    /**
     * Extracts a 512-dimensional embedding vector from an uploaded image file.
     */
    @ConcurrencyLimit(1)
    public float[] extractEmbeddingsFromFace(MultipartFile file) throws Exception {
        String uniqueDir = System.getProperty("java.io.tmpdir");
        Path uniqueTempPath = Paths.get(uniqueDir, "face_upload_" + UUID.randomUUID().toString() + ".jpg");
        
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, uniqueTempPath, StandardCopyOption.REPLACE_EXISTING);
        }
        
        Mat imageMat = Imgcodecs.imread(uniqueTempPath.toAbsolutePath().toString());
        Files.deleteIfExists(uniqueTempPath);
        
        if (imageMat.empty()) {
            throw new IllegalArgumentException("Failed to decode image file matrix.");
        }

        Mat blob = Dnn.blobFromImage(imageMat, 1.0, new Size(300, 300), new Scalar(104.0, 177.0, 123.0), false, false);
        dnnFaceDetector.setInput(blob);
        Mat detections = dnnFaceDetector.forward();
        blob.release();

        Mat detectionMat = detections.reshape(1, detections.size(2));
        detections.release();

        int frameWidth = imageMat.cols();
        int frameHeight = imageMat.rows();
        Rect bestFaceRect = null;
        float maxConfidence = 0.0f;

        for (int i = 0; i < detectionMat.rows(); i++) {
            double[] confData = detectionMat.get(i, 2);
            if (confData == null || confData.length == 0) continue;
            float confidence = (float) confData[0];
            
            if (confidence > 0.50f && confidence > maxConfidence) {
                maxConfidence = confidence;
                
                int x1 = (int) (detectionMat.get(i, 3)[0] * frameWidth);
                int y1 = (int) (detectionMat.get(i, 4)[0] * frameHeight);
                int x2 = (int) (detectionMat.get(i, 5)[0] * frameWidth);
                int y2 = (int) (detectionMat.get(i, 6)[0] * frameHeight);

                int boxWidth = x2 - x1;
                int boxHeight = y2 - y1;

                int paddingX = (int) (boxWidth * 0.15);
                int paddingY = (int) (boxHeight * 0.15);

                x1 = Math.max(0, x1 + paddingX);
                y1 = Math.max(0, y1 + paddingY);
                x2 = Math.min(frameWidth - 1, x2 - paddingX);
                y2 = Math.min(frameHeight - 1, y2 - paddingY);
                
                bestFaceRect = new Rect(x1, y1, x2 - x1, y2 - y1);
            }
        }
        detectionMat.release();

        if (bestFaceRect == null || bestFaceRect.width <= 0 || bestFaceRect.height <= 0) {
            imageMat.release();
            throw new IllegalArgumentException("DNN Face Detection failed.");
        }

        Mat faceMat = new Mat(imageMat, bestFaceRect);
        imageMat.release();

        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".jpg", faceMat, mob);
        faceMat.release();
        
        try (InputStream faceIs = new java.io.ByteArrayInputStream(mob.toArray())) {
            Image croppedDjlImage = ImageFactory.getInstance().fromInputStream(faceIs);
            float[] embeddings = faceRecognizer.predict(croppedDjlImage);
            mob.release();
            return embeddings;
        }
    }
    
    /**
     * Validates member status via external RestClient before proceeding with registration.
     */
    public AbstractMap.SimpleEntry<Member, String> isExistingMember(String username) {
        Member member = getMemberByUsername(username);
        if (Objects.isNull(member)) {
            return new AbstractMap.SimpleEntry<>(null, "Unregistered Member");
        } else if (Objects.nonNull(member)) {
            if (Objects.isNull(member.getMobileNumber())) {
                return new AbstractMap.SimpleEntry<>(null, "Unregistered Member");
            } else if (Objects.nonNull(member.getIsFacialRegCompleted())
                        && member.getIsFacialRegCompleted().equals(true)) {
                return new AbstractMap.SimpleEntry<>(null, "Facial Registration Already completed");
            } else if (Objects.nonNull(member.getIsActive())
                && member.getIsActive().equals(false)) {
                return new AbstractMap.SimpleEntry<>(null, "Member Inactive, Please contact Admin");
            }
        }
        return new AbstractMap.SimpleEntry<>(member, member.getMemberId());
    }
    
    private Member getMemberByUsername(String username) {
        return restClient.get()
                .uri(MEMBER_GET_BY_USERNAME_ENDPOINT, username)
                .retrieve()
                .body(Member.class);
    }

    public Member getMemberByMemberId(String memberId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam(MEMBER_GET_BY_MEMBERID_ENDPOINT, memberId)
                        .build())
                .retrieve()
                .body(Member.class);
    }
    
    private Boolean updateFaceRegistrationStatus(String memberId) {
        return restClient.put()
                .uri(MEMBER_UPDATE_FACE_REGIS_ENDPOINT, memberId)
                .retrieve()
                .body(Boolean.class);
    }

    /**
     * Batch-registers a list of embedding vectors into pgvector storage.
     */
    @Transactional
    public void registerBatchOfFaces(String memberId, List<float[]> embeddingBatch) {
        profileRepository.deleteByMemberId(memberId);

        List<FaceProfile> profilesToSave = new ArrayList<>();

        for (float[] embedding : embeddingBatch) {
            String vectorString = convertFloatArrayToVectorString(embedding);
            
            Optional<FaceProfile> closestMatchOpt = profileRepository.findClosestFaceMatch(vectorString);
            
            if (closestMatchOpt.isPresent()) {
                FaceProfile duplicateCandidate = closestMatchOpt.get();
                
                if (!duplicateCandidate.getUserId().equals(memberId)) {
                    double rawDistance = calculateEuclideanDistance(embedding, duplicateCandidate.getEmbedding());
                    if (rawDistance < 0.65) {
                        throw new IllegalStateException("Registration Rejected! This face already belongs to Member ID: " + duplicateCandidate.getUserId());
                    }
                }
            }

            String frameId = UUID.randomUUID().toString().substring(0, 8);
            profilesToSave.add(new FaceProfile(memberId, frameId, embedding));
        }

        profileRepository.saveAll(profilesToSave);
    }
 
    private String convertFloatArrayToVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        return sb.append("]").toString();
    }

    /**
     * Identifies a matching member profile based on vector distance metrics.
     */
    public RecognitionResult identifyFace(float[] targetEmbedding, double threshold) {
        List<FaceProfile> allSavedProfiles = profileRepository.findAll();
        
        if (allSavedProfiles.isEmpty()) {
            return new RecognitionResult("Unknown", 0.0);
        }

        String bestMatch = "Unknown";
        double lowestDistance = Double.MAX_VALUE; 

        for (FaceProfile savedProfile : allSavedProfiles) {
            double distance = calculateEuclideanDistance(targetEmbedding, savedProfile.getEmbedding());
            if (distance < lowestDistance) {
                lowestDistance = distance;
                bestMatch = savedProfile.getUserId();
            }
        }

        double confidencePercentage = (1.0 / (1.0 + Math.exp((lowestDistance - 0.98) / 0.12))) * 100.0;
        confidencePercentage = Math.max(0.0, Math.min(100.0, confidencePercentage));

        if (confidencePercentage >= threshold) {
            return new RecognitionResult(bestMatch, confidencePercentage);
        } else {
            return new RecognitionResult("Unknown", confidencePercentage);
        }
    }
    
    /**
     * Extracts frames from a video stream and persists calculated face vectors.
     */
    @Transactional
    @ConcurrencyLimit(2)
    public void processVideoAndRegister(String memberId, MultipartFile videoFile) throws Exception {
        String uniqueDir = System.getProperty("java.io.tmpdir");
        Path tempVideoPath = Paths.get(uniqueDir, "video_upload_" + UUID.randomUUID().toString() + ".mp4");
        
        try (InputStream in = videoFile.getInputStream()) {
            Files.copy(in, tempVideoPath, StandardCopyOption.REPLACE_EXISTING);
        }

        VideoCapture videoCapture = new VideoCapture(tempVideoPath.toAbsolutePath().toString());
        if (!videoCapture.isOpened()) {
            Files.deleteIfExists(tempVideoPath);
            throw new IllegalArgumentException("Failed to initialize OpenCV VideoCapture stream.");
        }

        double fps = videoCapture.get(Videoio.CAP_PROP_FPS);
        if (fps <= 0) fps = 30.0;
        int frameInterval = Math.max(1, (int) (fps / 3)); 

        List<float[]> extractedEmbeddings = new ArrayList<>();
        Mat frame = new Mat();
        int frameCounter = 0;

        try (Predictor<Image, float[]> faceRecognizer = recognitionModel.newPredictor()) {
            while (videoCapture.read(frame)) {
                if (frame.empty()) break;

                if (frameCounter % frameInterval == 0) {
                    try {
                        float[] embeddings = extractEmbeddingsFromMatFrame(frame, faceRecognizer);
                        extractedEmbeddings.add(embeddings);
                    } catch (IllegalArgumentException e) {
                        continue; 
                    }
                }
                frameCounter++;
            }
        } finally {
            videoCapture.release();
            frame.release();
            Files.deleteIfExists(tempVideoPath);
        }

        if (extractedEmbeddings.isEmpty()) {
            throw new IllegalArgumentException("Biometric Exception: No human faces detected anywhere inside the uploaded video stream.");
        }

        this.registerBatchOfFaces(memberId, extractedEmbeddings);
        updateFaceRegistrationStatus(memberId);
    }

    private float[] extractEmbeddingsFromMatFrame(Mat imageMat, Predictor<Image, float[]> faceRecognizer) throws Exception {
        Mat blob = Dnn.blobFromImage(imageMat, 1.0, new Size(300, 300), new Scalar(104.0, 177.0, 123.0), false, false);
        dnnFaceDetector.setInput(blob);
        Mat detections = dnnFaceDetector.forward();
        blob.release();

        Mat detectionMat = detections.reshape(1, detections.size(2));
        detections.release();

        int frameWidth = imageMat.cols();
        int frameHeight = imageMat.rows();
        Rect bestFaceRect = null;
        float maxConfidence = 0.0f;

        for (int i = 0; i < detectionMat.rows(); i++) {
            double[] confData = detectionMat.get(i, 2);
            if (confData == null || confData.length == 0) continue;
            float confidence = (float) confData[0];
            
            if (confidence > 0.50f && confidence > maxConfidence) {
                maxConfidence = confidence;
                
                int x1 = (int) (detectionMat.get(i, 3)[0] * frameWidth);
                int y1 = (int) (detectionMat.get(i, 4)[0] * frameHeight);
                int x2 = (int) (detectionMat.get(i, 5)[0] * frameWidth);
                int y2 = (int) (detectionMat.get(i, 6)[0] * frameHeight);

                int boxWidth = x2 - x1;
                int boxHeight = y2 - y1;

                int paddingX = (int) (boxWidth * 0.15);
                int paddingY = (int) (boxHeight * 0.15);

                x1 = Math.max(0, x1 + paddingX);
                y1 = Math.max(0, y1 + paddingY);
                x2 = Math.min(frameWidth - 1, x2 - paddingX);
                y2 = Math.min(frameHeight - 1, y2 - paddingY);
                
                bestFaceRect = new Rect(x1, y1, x2 - x1, y2 - y1);
            }
        }
        
        detectionMat.release(); 

        if (bestFaceRect == null || bestFaceRect.width <= 0 || bestFaceRect.height <= 0) {
            throw new IllegalArgumentException("No face detected in this specific frame slice.");
        }

        Mat faceMat = new Mat(imageMat, bestFaceRect);
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".jpg", faceMat, mob);
        faceMat.release();
        
        try (InputStream faceIs = new java.io.ByteArrayInputStream(mob.toArray())) {
            Image croppedDjlImage = ImageFactory.getInstance().fromInputStream(faceIs);
            float[] embeddings = faceRecognizer.predict(croppedDjlImage);
            mob.release();
            return embeddings;
        }
    }

    public List<String> getAllRegisteredUserIds() {
        return profileRepository.findAll().stream()
                .map(FaceProfile::getUserId)
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeUserProfile(String userId) {
        profileRepository.deleteByMemberId(userId);
    }

    private double calculateEuclideanDistance(float[] vectorA, float[] vectorB) {
        double sum = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            double diff = vectorA[i] - vectorB[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    @PreDestroy
    public void cleanup() {
        if (faceRecognizer != null) faceRecognizer.close();
        if (recognitionModel != null) recognitionModel.close();
    }
}