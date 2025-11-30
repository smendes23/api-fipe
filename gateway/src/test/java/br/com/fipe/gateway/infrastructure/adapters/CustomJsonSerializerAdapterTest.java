package br.com.fipe.gateway.infrastructure.adapters;

import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class CustomJsonSerializerAdapterTest {

    private CustomJsonSerializerAdapter<Object> serializer;

    @BeforeEach
    void setUp() {
        serializer = new CustomJsonSerializerAdapter<>();
    }

    @Test
    void serialize_ShouldSerializeSimpleObjectToJsonBytes() {
        String topic = "test-topic";
        TestObject testObject = new TestObject("John Doe", 30);

        byte[] result = serializer.serialize(topic, testObject);

        assertNotNull(result);
        String jsonString = new String(result);
        assertTrue(jsonString.contains("John Doe"));
        assertTrue(jsonString.contains("30"));
    }

    @Test
    void serialize_ShouldSerializeObjectWithLocalDateTime() {
        
        String topic = "test-topic";
        LocalDateTime dateTime = LocalDateTime.of(2023, 12, 25, 10, 30, 0);
        ObjectWithDate objectWithDate = new ObjectWithDate("Event", dateTime);

        
        byte[] result = serializer.serialize(topic, objectWithDate);

        assertNotNull(result);
        String jsonString = new String(result);
        assertTrue(jsonString.contains("Event"));
        assertTrue(jsonString.contains("2023-12-25T10:30:00"));
    }

    @Test
    void serialize_ShouldHandleNullData() {

        String topic = "test-topic";


        byte[] result = serializer.serialize(topic, null);

        assertNotNull(result);
        assertEquals("null", new String(result));
    }

    @Test
    void serialize_ShouldHandleEmptyObject() {

        String topic = "test-topic";
        EmptyObject emptyObject = new EmptyObject();


        byte[] result = serializer.serialize(topic, emptyObject);

        assertNotNull(result);
        assertEquals("{}", new String(result));
    }

    @Test
    void serialize_ShouldHandleString() {

        String topic = "test-topic";
        String message = "Hello, World!";


        byte[] result = serializer.serialize(topic, message);

        assertNotNull(result);
        assertEquals("\"Hello, World!\"", new String(result));
    }

    @Test
    void serialize_ShouldHandleNumber() {

        String topic = "test-topic";
        Integer number = 42;


        byte[] result = serializer.serialize(topic, number);

        assertNotNull(result);
        assertEquals("42", new String(result));
    }

    @Test
    void serialize_ShouldHandleMap() {

        String topic = "test-topic";
        Map<String, Object> map = Map.of(
                "name", "John",
                "age", 25,
                "active", true
        );


        byte[] result = serializer.serialize(topic, map);

        assertNotNull(result);
        String jsonString = new String(result);
        assertTrue(jsonString.contains("John"));
        assertTrue(jsonString.contains("25"));
        assertTrue(jsonString.contains("true"));
    }

    @Test
    void serialize_ShouldHandleComplexNestedObject() {

        String topic = "test-topic";
        ComplexObject complexObject = new ComplexObject(
                "Parent",
                new TestObject("Child", 10),
                LocalDateTime.of(2023, 1, 1, 0, 0)
        );


        byte[] result = serializer.serialize(topic, complexObject);

        assertNotNull(result);
        String jsonString = new String(result);
        assertTrue(jsonString.contains("Parent"));
        assertTrue(jsonString.contains("Child"));
        assertTrue(jsonString.contains("10"));
        assertTrue(jsonString.contains("2023-01-01T00:00:00"));
    }

    @Test
    void serialize_ShouldConfigureObjectMapperCorrectly() {

        String topic = "test-topic";
        ObjectWithDate objectWithDate = new ObjectWithDate("Test", LocalDateTime.now());


        byte[] result = serializer.serialize(topic, objectWithDate);

        assertNotNull(result);
        String jsonString = new String(result);

        assertFalse(jsonString.matches(".*\"dateTime\":\\s*\\d+.*"));
        assertTrue(jsonString.contains("T"));
    }

    @Test
    void serialize_ShouldThrowRuntimeExceptionOnSerializationError() {

        String topic = "test-topic";
        ObjectWithCircularReference obj1 = new ObjectWithCircularReference("Obj1");
        ObjectWithCircularReference obj2 = new ObjectWithCircularReference("Obj2");
        obj1.setReference(obj2);
        obj2.setReference(obj1);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> serializer.serialize(topic, obj1));

        assertEquals("Error serializing object to JSON", exception.getMessage());
        assertNotNull(exception.getCause());
    }

    @Test
    void serialize_ShouldHandleDifferentTopics() {

        String topic1 = "topic-1";
        String topic2 = "topic-2";
        TestObject testObject = new TestObject("Same Object", 100);


        byte[] result1 = serializer.serialize(topic1, testObject);
        byte[] result2 = serializer.serialize(topic2, testObject);

        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(new String(result1), new String(result2));
    }

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
        private LocalDateTime dateTime;

        public ObjectWithDate(String eventName, LocalDateTime dateTime) {
            this.eventName = eventName;
            this.dateTime = dateTime;
        }

        public String getEventName() { return eventName; }
        public LocalDateTime getDateTime() { return dateTime; }
    }

    static class EmptyObject {
    }

    static class ComplexObject {
        private String parentName;
        private TestObject child;
        private LocalDateTime createdAt;

        public ComplexObject(String parentName, TestObject child, LocalDateTime createdAt) {
            this.parentName = parentName;
            this.child = child;
            this.createdAt = createdAt;
        }

        public String getParentName() { return parentName; }
        public TestObject getChild() { return child; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    static class ObjectWithCircularReference {
        private String name;
        private ObjectWithCircularReference reference;

        public ObjectWithCircularReference(String name) {
            this.name = name;
        }

        public void setReference(ObjectWithCircularReference reference) {
            this.reference = reference;
        }

        public String getName() { return name; }
        public ObjectWithCircularReference getReference() { return reference; }
    }
}