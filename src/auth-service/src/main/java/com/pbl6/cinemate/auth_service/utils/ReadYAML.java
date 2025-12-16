package com.pbl6.cinemate.auth_service.utils;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public final class ReadYAML {

    private ReadYAML() {
    }

    public static Map<String, Object> getValueFromYAML(String nameFile) {
        Yaml yaml = new Yaml();
//        tim file trong resource
        InputStream inputStream = ReadYAML.class.getClassLoader().getResourceAsStream(nameFile);
        return yaml.load(inputStream);
    }
}

