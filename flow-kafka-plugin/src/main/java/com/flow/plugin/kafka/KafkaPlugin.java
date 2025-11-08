package com.flow.plugin.kafka;

import com.flow.adapter.FlowPlugin;
import com.flow.adapter.Model.GraphModel;
import com.flow.plugin.kafka.KafkaScanner;
import com.flow.adapter.util.ConfigLoader;

import java.io.IOException;
import java.nio.file.Path;

public class KafkaPlugin implements FlowPlugin {
    @Override
    public void enrich(GraphModel model, Path srcRoot, ConfigLoader config) {
        try {
            new KafkaScanner(config).scanInto(model, srcRoot);
        } catch (IOException e) {
            throw new RuntimeException("Error scanning Kafka topics", e);
        }
    }
}
