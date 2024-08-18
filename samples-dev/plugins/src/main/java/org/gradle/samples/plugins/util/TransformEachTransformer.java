package org.gradle.samples.plugins.util;

import org.gradle.api.Transformer;

import java.util.ArrayList;
import java.util.List;

public final class TransformEachTransformer<OUT, IN> implements Transformer<Iterable<OUT>, Iterable<IN>> {
    private final Transformer<OUT, IN> mapper;

    private TransformEachTransformer(Transformer<OUT, IN> mapper) {
        this.mapper = mapper;
    }

    @Override
    public Iterable<OUT> transform(Iterable<IN> ins) {
        List<OUT> result = new ArrayList<>();
        for (IN in : ins) {
            result.add(mapper.transform(in));
        }
        return result;
    }

    public static <OUT, IN> TransformEachTransformer<OUT, IN> transformEach(Transformer<OUT, IN> mapper) {
        return new TransformEachTransformer<>(mapper);
    }
}
