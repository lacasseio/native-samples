package dev.nokee.samples.externalbuilds.internal;

import org.gradle.api.Transformer;

import java.util.ArrayList;
import java.util.List;

public final class TransformEachTransformer<OUT, IN> implements Transformer<Iterable<OUT>, Iterable<IN>> {
    private final Transformer<OUT, IN> mapper;

    public TransformEachTransformer(Transformer<OUT, IN> mapper) {
        this.mapper = mapper;
    }


    @Override
    public Iterable<OUT> transform(Iterable<IN> ins) {
        final List<OUT> result = new ArrayList<>();
        for (IN in : ins) {
            result.add(mapper.transform(in));
        }
        return result;
    }

    public static <OUT, IN> TransformEachTransformer<OUT, IN> transformEach(Transformer<OUT, IN> mapper) {
        return new TransformEachTransformer<>(mapper);
    }
}
