package com.hawkins.xtreamjson.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

public class RecordSerializationTest {

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final XmlMapper xmlMapper = new XmlMapper();

    @Test
    public void testEpgChannelSerialization() throws Exception {
        String xml = "<channel id=\"123\"><display-name>Test Channel</display-name><icon src=\"http://example.com/icon.png\"/></channel>";
        EpgChannel channel = xmlMapper.readValue(xml, EpgChannel.class);

        assertEquals("123", channel.id());
        assertEquals("Test Channel", channel.displayName());
        assertNotNull(channel.icon());
        assertEquals("http://example.com/icon.png", channel.icon().src());

        // Test immutability helpers
        EpgChannel updated = channel.withDisplayName("New Name");
        assertEquals("New Name", updated.displayName());
        assertEquals("123", updated.id());
        assertEquals("Test Channel", channel.displayName()); // Original unchanged
    }

    @Test
    public void testEpgProgrammeSerialization() throws Exception {
        String xml = "<programme start=\"20230101000000 +0000\" stop=\"20230101010000 +0000\" channel=\"123\"><title>Test Show</title><desc>Description</desc></programme>";
        EpgProgramme prog = xmlMapper.readValue(xml, EpgProgramme.class);

        assertEquals("20230101000000 +0000", prog.start());
        assertEquals("Test Show", prog.title());
        assertEquals(60, prog.getDurationMinutes());
    }

    @Test
    public void testSeriesInfoSerialization() throws Exception {
        String json = "{\"info\":{\"name\":\"Series 1\"}, \"episodes\": {\"1\": [{\"id\":\"ep1\", \"episode_num\":1}]}}";
        SeriesInfo info = jsonMapper.readValue(json, SeriesInfo.class);

        assertNotNull(info.info());
        assertEquals("Series 1", info.info().name());
        assertNotNull(info.episodes());
        assertTrue(info.episodes().containsKey("1"));
        assertEquals("ep1", info.episodes().get("1").get(0).id());
    }

    @Test
    public void testEpgProgrammeViewModel() {
        EpgProgrammeViewModel vm = new EpgProgrammeViewModel("Title", "Desc", "Start", "Stop", 10, 100, "style",
                "http://url");
        assertEquals("Title", vm.title());
        assertEquals("Desc", vm.desc());
        assertEquals(10, vm.left());
    }
}
