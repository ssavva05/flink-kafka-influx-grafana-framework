package de.tuberlin.tubit.gitlab.anton.rudacov;

import de.tuberlin.tubit.gitlab.anton.rudacov.oscon.DataPointSerializationSchema;
import de.tuberlin.tubit.gitlab.anton.rudacov.oscon.KeyedDataPoint;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Properties;
import java.util.stream.Stream;

public class DataGeneratorTime implements Runnable {

    Properties properties;
    private String dataPath;
    private Producer<String, KeyedDataPoint<String>> producer;

    public DataGeneratorTime(String dataPath) {
        this.dataPath = dataPath;
    }

    private void produce() throws Exception {
        this.properties = new Properties();
        this.properties.setProperty("bootstrap.servers", App.KAFKA_BROKER);
        this.properties.setProperty("key.serializer","org.apache.kafka.common.serialization.StringSerializer");
        this.properties.setProperty("value.serializer",DataPointSerializationSchema.class.getCanonicalName());
        //
        Stream<String> dataStream = null;
        try{

            dataStream = Files.lines(Paths.get(dataPath));
            this.producer = new KafkaProducer<>(this.properties);

            dataStream.forEach(element -> sendLine(element));
            this.producer.close();
        } catch (IOException e) {
            App.log('f', "Not able to access data file");
        }
        dataStream.close();
    }

    private void sendLine(String line) {
        String serializedKey = line.split(";")[0];
        String serializedValue = line.split(";")[1];
        LocalDateTime dateTime = LocalDateTime.of(LocalDate.now(), LocalTime.parse(line.split(";")[0]));
        long timestamp = dateTime.atZone(ZoneId.of("Europe/Berlin")).toInstant().toEpochMilli();
        KeyedDataPoint<String> dp = new KeyedDataPoint<>(serializedKey, timestamp, serializedValue);
        //
        this.producer.send(new ProducerRecord(App.KAFKA_TOPIC, (Integer) null, timestamp, serializedKey, dp));
    }

    @Override
    public void run() {
        try {
            App.log('i', "Generator starting...");
            produce();
            App.log('i', "Generator ending...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}