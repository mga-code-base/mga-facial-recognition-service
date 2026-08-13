package com.mygym.app.facialrecognition.service;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DJL Translator converting raw images into detected face bounding box coordinates.
 */
public class FaceDetectionTranslator implements Translator<Image, DetectedObjects> {

    private final double threshold = 0.70;

    @Override
    public NDList processInput(TranslatorContext ctx, Image input) {
        Image resized = input.resize(320, 240, true);
        NDArray array = resized.toNDArray(ctx.getNDManager(), Image.Flag.COLOR);
        
        array = array.div(255.0f).sub(0.5f).div(0.5f);
        array = array.transpose(2, 0, 1);
        
        return new NDList(array);
    }

    @Override
    public DetectedObjects processOutput(TranslatorContext ctx, NDList list) {
        NDArray scores = list.get(0);
        NDArray boxes = list.get(1);
        
        float[] scoreArr = scores.get(0).toFloatArray();
        float[] boxArr = boxes.get(0).toFloatArray();
        
        List<String> classNames = Arrays.asList("face");
        List<Double> probabilities = new ArrayList<>();
        List<BoundingBox> boundingBoxes = new ArrayList<>();

        for (int i = 0; i < scoreArr.length / 2; i++) {
            float faceScore = scoreArr[i * 2 + 1];
            
            if (faceScore > threshold) {
                probabilities.add((double) faceScore);
                
                float xMin = boxArr[i * 4];
                float yMin = boxArr[i * 4 + 1];
                float xMax = boxArr[i * 4 + 2];
                float yMax = boxArr[i * 4 + 3];
                
                boundingBoxes.add(new Rectangle(xMin, yMin, xMax - xMin, yMax - yMin));
            }
        }
        
        return new DetectedObjects(classNames, probabilities, boundingBoxes);
    }
}