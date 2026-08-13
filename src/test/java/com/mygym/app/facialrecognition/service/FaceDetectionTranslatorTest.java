package com.mygym.app.facialrecognition.service;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.translate.TranslatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaceDetectionTranslatorTest {

    private FaceDetectionTranslator translator;
    private NDManager manager;

    @Mock
    private TranslatorContext ctx;

    @BeforeEach
    void setUp() {
        translator = new FaceDetectionTranslator();
        manager = NDManager.newBaseManager();
    }

    @Test
    @DisplayName("processInput should resize input image to 320x240 and convert to CHW format")
    void testProcessInput_TransformsImageCorrectly() throws Exception {
        when(ctx.getNDManager()).thenReturn(manager);

        BufferedImage dummyImg = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
        Image djlImage = ImageFactory.getInstance().fromImage(dummyImg);

        NDList result = translator.processInput(ctx, djlImage);

        assertNotNull(result);
        assertEquals(1, result.size());

        NDArray tensor = result.get(0);
        assertEquals(new Shape(3, 240, 320), tensor.getShape());
    }

    @Test
    @DisplayName("processOutput should filter faces above 70% confidence threshold")
    void testProcessOutput_FiltersHighConfidenceFaces() {
        float[] scoresData = new float[]{ 0.05f, 0.95f, 0.80f, 0.20f };
        float[] boxesData = new float[]{ 0.1f, 0.1f, 0.5f, 0.5f, 0.2f, 0.2f, 0.6f, 0.6f };

        NDArray scoresTensor = manager.create(scoresData, new Shape(1, 2, 2));
        NDArray boxesTensor = manager.create(boxesData, new Shape(1, 2, 4));

        NDList modelOutput = new NDList(scoresTensor, boxesTensor);

        DetectedObjects detectedObjects = translator.processOutput(ctx, modelOutput);

        assertNotNull(detectedObjects);
        assertEquals(1, detectedObjects.getNumberOfObjects());
        assertEquals("face", detectedObjects.getClassNames().get(0));
        assertTrue(detectedObjects.getProbabilities().get(0) > 0.70);
    }
}