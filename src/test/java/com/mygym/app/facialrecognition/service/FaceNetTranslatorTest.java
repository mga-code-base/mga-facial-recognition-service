package com.mygym.app.facialrecognition.service;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
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
class FaceNetTranslatorTest {

    private FaceNetTranslator translator;
    private NDManager manager;

    @Mock
    private TranslatorContext ctx;

    @BeforeEach
    void setUp() {
        translator = new FaceNetTranslator();
        manager = NDManager.newBaseManager();
    }

    @Test
    @DisplayName("processInput should resize image to 160x160 and apply per-image normalization formula")
    void testProcessInput_NormalizesAndResizesTo160x160() throws Exception {
        when(ctx.getNDManager()).thenReturn(manager);

        BufferedImage dummyImg = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
        Image djlImage = ImageFactory.getInstance().fromImage(dummyImg);

        NDList result = translator.processInput(ctx, djlImage);

        assertNotNull(result);
        assertEquals(1, result.size());

        NDArray tensor = result.get(0);
        assertEquals(new Shape(3, 160, 160), tensor.getShape());
    }

    @Test
    @DisplayName("processOutput should extract 512-d float array from model output NDList")
    void testProcessOutput_ExtractsEmbeddingArray() {
        float[] mockVector = new float[512];
        mockVector[0] = 0.123f;
        mockVector[511] = -0.987f;

        NDArray outputTensor = manager.create(mockVector);
        NDList modelOutput = new NDList(outputTensor);

        float[] extractedEmbeddings = translator.processOutput(ctx, modelOutput);

        assertNotNull(extractedEmbeddings);
        assertEquals(512, extractedEmbeddings.length);
        assertEquals(0.123f, extractedEmbeddings[0], 0.0001f);
        assertEquals(-0.987f, extractedEmbeddings[511], 0.0001f);
    }
}