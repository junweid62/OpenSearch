/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.pipeline;

import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.common.xcontent.XContentHelper;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.core.common.bytes.BytesReference;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.xcontent.DeprecationHandler;
import org.opensearch.core.xcontent.MediaType;
import org.opensearch.core.xcontent.MediaTypeRegistry;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ProcessorExecutionDetailTests extends OpenSearchTestCase {

    public void testSerializationRoundtrip() throws IOException {
        ProcessorExecutionDetail detail = new ProcessorExecutionDetail("testProcessor", 123L, Map.of("key", "value"), List.of(1, 2, 3));
        ProcessorExecutionDetail deserialized;
        try (BytesStreamOutput output = new BytesStreamOutput()) {
            detail.writeTo(output);
            try (StreamInput input = output.bytes().streamInput()) {
                deserialized = new ProcessorExecutionDetail(input);
            }
        }
        assertEquals("testProcessor", deserialized.getProcessorName());
        assertEquals(123L, deserialized.getDurationMillis());
        assertEquals(Map.of("key", "value"), deserialized.getInputData());
        assertEquals(List.of(1, 2, 3), deserialized.getOutputData());
    }

    public void testAddMethods() {
        ProcessorExecutionDetail detail = new ProcessorExecutionDetail("testProcessor");
        detail.addTook(456L);
        detail.addInput(Map.of("newKey", "newValue"));
        detail.addOutput(List.of(4, 5, 6));
        assertEquals(456L, detail.getDurationMillis());
        assertEquals(Map.of("newKey", "newValue"), detail.getInputData());
        assertEquals(List.of(4, 5, 6), detail.getOutputData());
    }

    public void testEqualsAndHashCode() {
        ProcessorExecutionDetail detail1 = new ProcessorExecutionDetail("processor1", 100L, "input1", "output1");
        ProcessorExecutionDetail detail2 = new ProcessorExecutionDetail("processor1", 100L, "input1", "output1");
        ProcessorExecutionDetail detail3 = new ProcessorExecutionDetail("processor2", 200L, "input2", "output2");

        assertEquals(detail1, detail2);
        assertNotEquals(detail1, detail3);
        assertEquals(detail1.hashCode(), detail2.hashCode());
        assertNotEquals(detail1.hashCode(), detail3.hashCode());
    }

    public void testToString() {
        ProcessorExecutionDetail detail = new ProcessorExecutionDetail("processorZ", 500L, "inputData", "outputData");
        String expected =
            "ProcessorExecutionDetail{processorName='processorZ', durationMillis=500, inputData=inputData, outputData=outputData}";
        assertEquals(expected, detail.toString());
    }

    public void testToXContent() throws IOException {
        ProcessorExecutionDetail detail = new ProcessorExecutionDetail("testProcessor", 123L, Map.of("key1", "value1"), List.of(1, 2, 3));

        XContentBuilder actualBuilder = XContentBuilder.builder(JsonXContent.jsonXContent);
        detail.toXContent(actualBuilder, ToXContent.EMPTY_PARAMS);

        String expected = "{"
            + "  \"processor_name\": \"testProcessor\","
            + "  \"duration_millis\": 123,"
            + "  \"input_data\": {\"key1\": \"value1\"},"
            + "  \"output_data\": [1, 2, 3]"
            + "}";

        XContentParser expectedParser = JsonXContent.jsonXContent.createParser(
            this.xContentRegistry(),
            DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
            expected
        );
        XContentBuilder expectedBuilder = XContentBuilder.builder(JsonXContent.jsonXContent);
        expectedBuilder.generator().copyCurrentStructure(expectedParser);

        assertEquals(
            XContentHelper.convertToMap(BytesReference.bytes(expectedBuilder), false, (MediaType) MediaTypeRegistry.JSON),
            XContentHelper.convertToMap(BytesReference.bytes(actualBuilder), false, (MediaType) MediaTypeRegistry.JSON)
        );
    }

    public void testFromXContent() throws IOException {
        String json = "{"
            + "  \"processor_name\": \"testProcessor\","
            + "  \"duration_millis\": 123,"
            + "  \"input_data\": {\"key1\": \"value1\"},"
            + "  \"output_data\": [1, 2, 3]"
            + "}";

        try (
            XContentParser parser = JsonXContent.jsonXContent.createParser(
                this.xContentRegistry(),
                DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
                json
            )
        ) {
            ProcessorExecutionDetail detail = ProcessorExecutionDetail.fromXContent(parser);

            assertEquals("testProcessor", detail.getProcessorName());
            assertEquals(123L, detail.getDurationMillis());
            assertEquals(Map.of("key1", "value1"), detail.getInputData());
            assertEquals(List.of(1, 2, 3), detail.getOutputData());
        }
    }
}
