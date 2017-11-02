package us.dot.its.jpo.ode.exporter;

import mockit.*;
import org.junit.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import us.dot.its.jpo.ode.OdeProperties;
import us.dot.its.jpo.ode.wrapper.MessageConsumer;
import us.dot.its.jpo.ode.wrapper.MessageProcessor;

public class StompStringExporterTest {

   @Tested
   StompStringExporter testStompExporter;

   @Injectable
   OdeProperties injectableOdeProperties;
   @Injectable
   String stompTopic = "testTopic";
   @Injectable
   SimpMessagingTemplate simpMessagingTemplate;
   @Injectable
   String odeTopic;

   @SuppressWarnings({ "rawtypes", "unchecked" })
   @Test
   public void testSubscribe(@Capturing MessageConsumer capturingMessageConsumer, @Mocked MessageConsumer mockMessageConsumer) {
      new Expectations() {{
         MessageConsumer.defaultStringMessageConsumer(anyString, anyString, (MessageProcessor) any);
         result = mockMessageConsumer;
         
         mockMessageConsumer.setName(anyString);
         times = 1;
         mockMessageConsumer.subscribe(anyString);
         times = 1;
      }};
      testStompExporter.subscribe();
   }
}
