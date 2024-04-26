package io.zeebe.monitor.zeebe.protobuf.importers;

import io.camunda.zeebe.protocol.record.intent.MessageIntent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.zeebe.exporter.proto.Schema;
import io.zeebe.monitor.entity.MessageEntity;
import io.zeebe.monitor.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MessageProtobufImporter {

  private final MessageRepository messageRepository;
  private final Counter counter;

  @Autowired
  public MessageHazelcastImporter(MessageRepository messageRepository, MeterRegistry meterRegistry) {
    this.messageRepository = messageRepository;

    this.counter = Counter.builder("zeebemonitor_importer_message").description("number of processed messages").register(meterRegistry);
  }

  public void importMessage(final Schema.MessageRecord record) {

    final MessageIntent intent = MessageIntent.valueOf(record.getMetadata().getIntent());
    final long key = record.getMetadata().getKey();
    final long timestamp = record.getMetadata().getTimestamp();

    final MessageEntity entity =
        messageRepository
            .findById(key)
            .orElseGet(
                () -> {
                  final MessageEntity newEntity = new MessageEntity();
                  newEntity.setKey(key);
                  newEntity.setName(record.getName());
                  newEntity.setCorrelationKey(record.getCorrelationKey());
                  newEntity.setMessageId(record.getMessageId());
                  newEntity.setPayload(record.getVariables().toString());
                  return newEntity;
                });

    entity.setState(intent.name().toLowerCase());
    entity.setTimestamp(timestamp);
    messageRepository.save(entity);

    counter.increment();
  }
}
