package br.jus.tjpb.polvo_api.domain;

import io.hypersistence.tsid.TSID;

public final class TsidGenerator {

    private TsidGenerator() {
    }

    public static Long generateLong() {
        return TSID.fast().toLong();
    }

    public static String generateString() {
        return TSID.fast().toString();
    }
}
