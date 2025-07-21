package com.nhom4.xoxo.untils;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

public class MapperUntils {
    public static ModelMapper getInstance() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT);
        return modelMapper;
    }

    public static <D> D mapObject(Object source, Class<D> destinationClass) {
        return getInstance().map(source, destinationClass);
    }
}
