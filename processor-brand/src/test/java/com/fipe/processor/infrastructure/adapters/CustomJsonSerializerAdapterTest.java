package com.fipe.processor.infrastructure.adapters;

import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomJsonSerializerAdapterTest {

    private CustomJsonSerializerAdapter<Object> serializer;

    @BeforeEach
    void setUp() {
        serializer = new CustomJsonSerializerAdapter<>();
    }

    @Test
    void shouldSerializeSimpleObject() {
        
        String topic = "test-topic";
        TestObject testObject = new TestObject("John", 30);

        
        byte[] result = serializer.serialize(topic, testObject);

        
        assertNotNull(result);
        String jsonString = new String(result);
        assertTrue(jsonString.contains("John"));
        assertTrue(jsonString.contains("30"));
    }

    @Test
    void shouldSerializeObjectWithLocalDateTime() {
        
        String topic = "test-topic";
        LocalDateTime fixedTime = LocalDateTime.of(2023, 10, 15, 14, 30, 0);
        ObjectWithDate objectWithDate = new ObjectWithDate("Event", fixedTime);

        
        byte[] result = serializer.serialize(topic, objectWithDate);

        
        assertNotNull(result);
        String jsonString = new String(result);
        assertTrue(jsonString.contains("Event"));
        assertTrue(jsonString.contains("2023-10-15"));
        assertTrue(jsonString.contains("14:30:00"));
    }

    @Test
    void shouldSerializeEmptyObject() {
        
        String topic = "test-topic";
        EmptyObject emptyObject = new EmptyObject();

        
        byte[] result = serializer.serialize(topic, emptyObject);

        
        assertNotNull(result);
        String jsonString = new String(result);
        assertEquals("{}", jsonString);
    }

    @Test
    void shouldSerializeNullValue() {
        
        String topic = "test-topic";

        
        byte[] result = serializer.serialize(topic, null);

        
        assertNotNull(result);
        String jsonString = new String(result);
        assertEquals("null", jsonString);
    }

    @Test
    void shouldSerializeComplexNestedObject() {
        
        String topic = "test-topic";
        ComplexObject complexObject = new ComplexObject("Parent",
                new TestObject("Child", 25),
                Map.of("key1", "value1", "key2", "value2"));

        
        byte[] result = serializer.serialize(topic, complexObject);

        
        assertNotNull(result);
        String jsonString = new String(result);
        assertTrue(jsonString.contains("Parent"));
        assertTrue(jsonString.contains("Child"));
        assertTrue(jsonString.contains("25"));
        assertTrue(jsonString.contains("key1"));
        assertTrue(jsonString.contains("value1"));
    }

    @Test
    void shouldThrowRuntimeExceptionWhenSerializationFails() {
        
        String topic = "test-topic";
        ObjectWithCircularReference circularObject = new ObjectWithCircularReference();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> serializer.serialize(topic, circularObject));

        assertEquals("Error serializing object to JSON", exception.getMessage());
        assertNotNull(exception.getCause());
    }

    @Test
    void shouldUseSameObjectMapperInstance() {
        
        String topic = "test-topic";
        TestObject testObject1 = new TestObject("First", 1);
        TestObject testObject2 = new TestObject("Second", 2);

        
        byte[] result1 = serializer.serialize(topic, testObject1);
        byte[] result2 = serializer.serialize(topic, testObject2);

        
        assertNotNull(result1);
        assertNotNull(result2);

        String json1 = new String(result1);
        String json2 = new String(result2);

        assertTrue(json1.contains("First"));
        assertTrue(json2.contains("Second"));
    }

    @Test
    void shouldIgnoreTopicParameter() {
        
        String topic1 = "topic-one";
        String topic2 = "topic-two";
        TestObject testObject = new TestObject("Same", 42);

        
        byte[] result1 = serializer.serialize(topic1, testObject);
        byte[] result2 = serializer.serialize(topic2, testObject);

        
        assertArrayEquals(result1, result2, "Serialization should be independent of topic");
    }

    // Test helper classes
    static class TestObject {
        private String name;
        private int age;

        public TestObject(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public int getAge() { return age; }
    }

    static class ObjectWithDate {
        private String eventName;
        private LocalDateTime eventDate;

        public ObjectWithDate(String eventName, LocalDateTime eventDate) {
            this.eventName = eventName;
            this.eventDate = eventDate;
        }

        public String getEventName() { return eventName; }
        public LocalDateTime getEventDate() { return eventDate; }
    }

    static class EmptyObject {
    }

    static class ComplexObject {
        private String parentName;
        private TestObject child;
        private Map<String, String> properties;

        public ComplexObject(String parentName, TestObject child, Map<String, String> properties) {
            this.parentName = parentName;
            this.child = child;
            this.properties = properties;
        }

        public String getParentName() { return parentName; }
        public TestObject getChild() { return child; }
        public Map<String, String> getProperties() { return properties; }
    }

    static class ObjectWithCircularReference {
        private ObjectWithCircularReference self;

        public ObjectWithCircularReference() {
            this.self = this;
        }

        public ObjectWithCircularReference getSelf() { return self; }
    }
}