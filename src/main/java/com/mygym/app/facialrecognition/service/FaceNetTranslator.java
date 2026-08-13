package com.mygym.app.facialrecognition.service;

import ai.djl.modality.cv.Image;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;

/**
 * DJL Translator processing cropped face matrices into normalized 512-dimensional floating point embeddings.
 */
public class FaceNetTranslator implements Translator<Image, float[]> {

    @Override
    public NDList processInput(TranslatorContext ctx, Image input) {
        Image resized = input.resize(160, 160, true);
        NDArray array = resized.toNDArray(ctx.getNDManager(), Image.Flag.COLOR);
        
        array = array.transpose(2, 0, 1);
        array = array.toType(ai.djl.ndarray.types.DataType.FLOAT32, false);
        
        NDArray mean = array.mean();
        NDArray variance = array.sub(mean).square().mean();
        NDArray std = variance.sqrt();
        
        float eps = 1e-5f;
        NDArray adjustedStd = std.maximum(eps);
        
        array = array.sub(mean).div(adjustedStd);
        
        return new NDList(array);
    }

    @Override
    public float[] processOutput(TranslatorContext ctx, NDList list) {
        return list.get(0).toFloatArray();
    }
}