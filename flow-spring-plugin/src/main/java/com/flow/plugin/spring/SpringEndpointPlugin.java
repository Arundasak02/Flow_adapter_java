package com.flow.plugin.spring;

import com.flow.adapter.FlowPlugin;
import com.flow.adapter.Model.GraphModel;
import com.flow.plugin.spring.SpringEndpointScanner;
import com.flow.adapter.util.ConfigLoader;

import java.io.IOException;
import java.nio.file.Path;

public class SpringEndpointPlugin implements FlowPlugin {
    @Override
    public void enrich(GraphModel model, Path srcRoot, ConfigLoader config) {
        try {
            new SpringEndpointScanner(config).scanInto(model, srcRoot);
        } catch (IOException e) {
            throw new RuntimeException("Error scanning Spring endpoints", e);
        }
    }
}
