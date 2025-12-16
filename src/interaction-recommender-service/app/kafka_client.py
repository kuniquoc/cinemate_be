"""
Kafka producer and consumer for event streaming
"""
import asyncio
import json
import logging
from datetime import datetime
from typing import Optional, Dict, Any, Callable, Awaitable
from uuid import uuid4

from aiokafka import AIOKafkaProducer, AIOKafkaConsumer
from aiokafka.errors import KafkaError

from app.config import get_settings

logger = logging.getLogger(__name__)

settings = get_settings()


class KafkaManager:
    """Manages Kafka producer and consumer connections"""
    
    def __init__(self):
        self.producer: Optional[AIOKafkaProducer] = None
        self.consumer: Optional[AIOKafkaConsumer] = None
        self._consumer_task: Optional[asyncio.Task] = None
        self._running = False
        self._message_handler: Optional[Callable[[Dict[str, Any]], Awaitable[None]]] = None
    
    async def start_producer(self) -> None:
        """Initialize Kafka producer"""
        if not settings.enable_kafka:
            logger.info("Kafka is disabled, skipping producer start")
            return
        
        try:
            self.producer = AIOKafkaProducer(
                bootstrap_servers=settings.kafka_bootstrap_servers,
                value_serializer=lambda v: json.dumps(v).encode('utf-8'),
                key_serializer=lambda k: k.encode('utf-8') if k else None,
                acks='all',
                retries=settings.max_retries,
            )
            await self.producer.start()
            logger.info("Kafka producer started")
        except Exception as e:
            logger.error(f"Failed to start Kafka producer: {e}")
            raise
    
    async def stop_producer(self) -> None:
        """Stop Kafka producer"""
        if self.producer:
            await self.producer.stop()
            self.producer = None
            logger.info("Kafka producer stopped")
    
    async def start_consumer(
        self, 
        message_handler: Callable[[Dict[str, Any]], Awaitable[None]]
    ) -> None:
        """Initialize and start Kafka consumer"""
        if not settings.enable_kafka or not settings.enable_consumers:
            logger.info("Kafka consumers disabled, skipping consumer start")
            return
        
        self._message_handler = message_handler
        self._running = True
        
        try:
            self.consumer = AIOKafkaConsumer(
                settings.kafka_interaction_topic,
                bootstrap_servers=settings.kafka_bootstrap_servers,
                group_id=settings.kafka_consumer_group,
                auto_offset_reset='earliest',
                enable_auto_commit=False,
                value_deserializer=lambda v: json.loads(v.decode('utf-8')),
            )
            await self.consumer.start()
            logger.info(f"Kafka consumer started for topic: {settings.kafka_interaction_topic}")
            
            # Start consumer loop in background
            self._consumer_task = asyncio.create_task(self._consume_loop())
        except Exception as e:
            logger.error(f"Failed to start Kafka consumer: {e}")
            raise
    
    async def stop_consumer(self) -> None:
        """Stop Kafka consumer"""
        self._running = False
        
        if self._consumer_task:
            self._consumer_task.cancel()
            try:
                await self._consumer_task
            except asyncio.CancelledError:
                pass
            self._consumer_task = None
        
        if self.consumer:
            await self.consumer.stop()
            self.consumer = None
            logger.info("Kafka consumer stopped")
    
    async def _consume_loop(self) -> None:
        """Consumer loop for processing messages"""
        retry_count = 0
        max_retries = settings.max_retries
        
        while self._running:
            try:
                async for msg in self.consumer:
                    if not self._running:
                        break
                    
                    try:
                        await self._process_message(msg)
                        await self.consumer.commit()
                        retry_count = 0  # Reset on success
                    except Exception as e:
                        logger.error(f"Error processing message: {e}")
                        await self._handle_processing_error(msg, e)
                        
            except asyncio.CancelledError:
                logger.info("Consumer loop cancelled")
                break
            except KafkaError as e:
                retry_count += 1
                if retry_count >= max_retries:
                    logger.error(f"Max retries reached, consumer loop stopping: {e}")
                    break
                
                delay = settings.retry_delay_seconds * (2 ** retry_count)
                logger.warning(f"Kafka error, retrying in {delay}s: {e}")
                await asyncio.sleep(delay)
            except Exception as e:
                logger.error(f"Unexpected error in consumer loop: {e}")
                await asyncio.sleep(settings.retry_delay_seconds)
    
    async def _process_message(self, msg) -> None:
        """Process a single message"""
        if self._message_handler:
            data = msg.value
            logger.debug(f"Processing message: {data.get('requestId', 'unknown')}")
            await self._message_handler(data)
    
    async def _handle_processing_error(self, msg, error: Exception) -> None:
        """Handle message processing error - send to DLQ"""
        try:
            dlq_message = {
                "original_message": msg.value,
                "error": str(error),
                "error_type": type(error).__name__,
                "topic": msg.topic,
                "partition": msg.partition,
                "offset": msg.offset,
                "timestamp": datetime.utcnow().isoformat() + "Z",
            }
            await self.publish_to_dlq(dlq_message)
        except Exception as e:
            logger.error(f"Failed to send message to DLQ: {e}")
    
    # ============== Publishing Methods ==============
    
    async def publish_interaction_event(self, event: Dict[str, Any]) -> bool:
        """Publish interaction event to Kafka"""
        return await self._publish(
            settings.kafka_interaction_topic,
            event,
            key=event.get("userId")
        )
    
    async def publish_processed_features(self, features: Dict[str, Any]) -> bool:
        """Publish processed features to Kafka"""
        return await self._publish(
            settings.kafka_features_topic,
            features,
            key=features.get("userId")
        )
    
    async def publish_feedback(self, feedback: Dict[str, Any]) -> bool:
        """Publish model feedback to Kafka"""
        return await self._publish(
            settings.kafka_feedback_topic,
            feedback,
            key=feedback.get("userId")
        )
    
    async def publish_to_dlq(self, message: Dict[str, Any]) -> bool:
        """Publish failed message to DLQ"""
        return await self._publish(
            settings.kafka_dlq_topic,
            message,
            key=str(uuid4())
        )
    
    async def _publish(
        self, 
        topic: str, 
        message: Dict[str, Any], 
        key: Optional[str] = None
    ) -> bool:
        """Publish message to Kafka topic"""
        if not self.producer:
            logger.warning("Kafka producer not available")
            return False
        
        try:
            await self.producer.send_and_wait(topic, value=message, key=key)
            logger.debug(f"Published message to {topic}")
            return True
        except KafkaError as e:
            logger.error(f"Failed to publish to {topic}: {e}")
            return False
    
    async def health_check(self) -> Dict[str, Any]:
        """Check Kafka health"""
        health = {"producer": "unknown", "consumer": "unknown"}
        
        if not settings.enable_kafka:
            return {"status": "disabled"}
        
        if self.producer:
            try:
                # Simple check - producer exists and is connected
                health["producer"] = "healthy"
            except Exception:
                health["producer"] = "unhealthy"
        else:
            health["producer"] = "not_started"
        
        if self.consumer:
            health["consumer"] = "healthy" if self._running else "stopped"
        else:
            health["consumer"] = "not_started"
        
        overall = "healthy" if all(
            v in ("healthy", "disabled", "not_started") 
            for v in health.values()
        ) else "unhealthy"
        
        return {"status": overall, "details": health}


# Global Kafka manager instance
kafka_manager = KafkaManager()


async def get_kafka() -> KafkaManager:
    """Get Kafka manager instance"""
    return kafka_manager
