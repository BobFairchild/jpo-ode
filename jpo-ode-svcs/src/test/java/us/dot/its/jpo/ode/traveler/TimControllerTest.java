package us.dot.its.jpo.ode.traveler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.event.ResponseEvent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import us.dot.its.jpo.ode.OdeProperties;
import us.dot.its.jpo.ode.dds.DdsDepositor;
import us.dot.its.jpo.ode.dds.DdsStatusMessage;
import us.dot.its.jpo.ode.eventlog.EventLogger;
import us.dot.its.jpo.ode.http.InternalServerErrorException;
import us.dot.its.jpo.ode.j2735.dsrc.TravelerInformation;
import us.dot.its.jpo.ode.model.TravelerInputData;
import us.dot.its.jpo.ode.plugin.RoadSideUnit.RSU;
import us.dot.its.jpo.ode.plugin.j2735.oss.OssTravelerMessageBuilder;
import us.dot.its.jpo.ode.snmp.SNMP;
import us.dot.its.jpo.ode.snmp.SnmpSession;
import us.dot.its.jpo.ode.util.DateTimeUtils;
import us.dot.its.jpo.ode.util.JsonUtils;

@RunWith(JMockit.class)
public class TimControllerTest {

   @Tested
   TimController testTimController;
   @Injectable
   OdeProperties mockOdeProperties;
   @Injectable
   DdsDepositor<DdsStatusMessage> mockDepositor;

   @Mocked
   TravelerInputData mockTravelerInputData;
   @Mocked
   TravelerInformation mockTravelerInfo;
   @Mocked
   OssTravelerMessageBuilder mockBuilder;
   @Mocked
   RSU mockRsu;
   @Mocked
   SNMP mockSnmp;
   @Mocked
   ResponseEvent mockResponseEvent;
   @Mocked
   PDU mockPdu;
   @Mocked
   ScopedPDU mockScopedPdu;
   @Mocked
   AsdMessage mockAsdMsg;

   @Test
   public void emptyRequestShouldReturnError() {

      try {
         ResponseEntity<String> response = testTimController.timMessage(null);
         assertEquals("Empty request.", response.getBody());
      } catch (Exception e) {
         fail("Unexpected exception " + e);
      }

      try {
         ResponseEntity<String> response = testTimController.timMessage("");
         assertEquals("Empty request.", response.getBody());
      } catch (Exception e) {
         fail("Unexpected exception " + e);
      }
   }

   @Test
   public void badRequestShouldThrowException() {

      try {
         new Expectations(JsonUtils.class, DateTimeUtils.class, EventLogger.class, TimController.class) {
            {
               mockTravelerInputData.toString();
               result = "something";
               minTimes = 0;

               JsonUtils.fromJson(anyString, TravelerInputData.class);
               result = new Exception("Bad JSON");
            }
         };
      } catch (Exception e) {
         fail("Unexpected Exception in expectations block: " + e);
      }

      try {
         ResponseEntity<String> response = testTimController.timMessage("test123");
         assertEquals("Malformed JSON.", response.getBody());
      } catch (Exception e) {
         fail("Unexpected exception " + e);
      }

      new Verifications() {
         {
            EventLogger.logger.info(anyString);
         }
      };
   }

   @Test
   public void builderErrorShouldThrowException() {

      try {
         new Expectations(JsonUtils.class, DateTimeUtils.class, EventLogger.class, TimController.class) {
            {
               mockTravelerInputData.toString();
               result = "something";
               minTimes = 0;

               JsonUtils.fromJson(anyString, TravelerInputData.class);
               result = mockTravelerInputData;
               mockTravelerInputData.toJson(true);
               result = "mockTim";

               mockBuilder.buildTravelerInformation(mockTravelerInputData.getTim());
               result = new Exception("Builder Error");
            }
         };
      } catch (Exception e) {
         fail("Unexpected Exception in expectations block: " + e);
      }

      try {
         ResponseEntity<String> response = testTimController.timMessage("test123");
         assertEquals("Request does not match schema.", response.getBody());
      } catch (Exception e) {
         fail("Unexpected exception " + e);
      }

      new Verifications() {
         {
            EventLogger.logger.info(anyString);
         }
      };
   }

   @Test
   public void encodingErrorShouldThrowException() {

      try {
         new Expectations(JsonUtils.class, DateTimeUtils.class, EventLogger.class, TimController.class) {
            {
               mockTravelerInputData.toString();
               result = "something";
               minTimes = 0;

               JsonUtils.fromJson(anyString, TravelerInputData.class);
               result = mockTravelerInputData;
               mockTravelerInputData.toJson(true);
               result = anyString;

               mockBuilder.buildTravelerInformation(mockTravelerInputData.getTim());
               result = mockTravelerInfo;

               mockBuilder.encodeTravelerInformationToHex();
               result = new Exception("Encoding error.");
            }
         };
      } catch (Exception e) {
         fail("Unexpected Exception in expectations block: " + e);
      }

      try {
         ResponseEntity<String> response = testTimController.timMessage("test123");
         assertEquals("Encoding error.", response.getBody());
      } catch (Exception e) {
         fail("Unexpected exception " + e);
      }

      new Verifications() {
         {
            EventLogger.logger.info(anyString);
         }
      };
   }

   @Test
   public void snmpExceptionShouldLogAndReturn() {

      try {
         new Expectations(JsonUtils.class, DateTimeUtils.class, EventLogger.class, TimController.class) {
            {
               mockTravelerInputData.toString();
               result = "something";
               minTimes = 0;

               JsonUtils.fromJson(anyString, TravelerInputData.class);
               result = mockTravelerInputData;
               mockTravelerInputData.toJson(true);
               result = anyString;

               mockBuilder.buildTravelerInformation(mockTravelerInputData.getTim());
               result = mockTravelerInfo;

               mockBuilder.encodeTravelerInformationToHex();
               result = anyString;

               mockTravelerInputData.getRsus();
               result = new RSU[] { mockRsu };

               mockTravelerInputData.getSnmp();
               result = mockSnmp;

               mockRsu.getRsuTarget();
               result = "snmpException";

               TimController.createAndSend(mockSnmp, mockRsu, anyInt, anyString);
               result = new Exception("SNMP Error");

               mockTravelerInputData.getSdw();
               result = null;
            }
         };
      } catch (Exception e) {
         fail("Unexpected Exception in expectations block: " + e);
      }

      ResponseEntity<String> response = testTimController.timMessage("test123");
      assertEquals(
            "{\"rsu_responses\":[{\"target\":\"snmpException\",\"success\":\"false\",\"error\":\"java.lang.Exception\"}],\"dds_deposit\":{\"success\":\"true\"}}",
            response.getBody());

      new Verifications() {
         {
            EventLogger.logger.info(anyString);
         }
      };
   }

   @Test
   public void nullResponseShouldLogAndReturn() {

      try {
         new Expectations(JsonUtils.class, DateTimeUtils.class, EventLogger.class, TimController.class) {
            {
               mockTravelerInputData.toString();
               result = "something";
               minTimes = 0;

               JsonUtils.fromJson(anyString, TravelerInputData.class);
               result = mockTravelerInputData;
               mockTravelerInputData.toJson(true);
               result = "mockTim";

               mockBuilder.buildTravelerInformation(mockTravelerInputData.getTim());
               result = mockTravelerInfo;

               mockBuilder.encodeTravelerInformationToHex();
               result = anyString;

               mockTravelerInputData.getRsus();
               result = new RSU[] { mockRsu };

               mockTravelerInputData.getSnmp();
               result = mockSnmp;

               mockRsu.getRsuTarget();
               result = "nullResponse";

               TimController.createAndSend(mockSnmp, mockRsu, anyInt, anyString);
               result = null;

               mockTravelerInputData.getSdw();
               result = null;
            }
         };
      } catch (Exception e) {
         fail("Unexpected Exception in expectations block: " + e);
      }

      ResponseEntity<String> response = testTimController.timMessage("test123");
      assertEquals(
            "{\"rsu_responses\":[{\"target\":\"nullResponse\",\"success\":\"false\",\"error\":\"Timeout.\"}],\"dds_deposit\":{\"success\":\"true\"}}",
            response.getBody());
   }

   @Test
   public void badResponseShouldLogAndReturn() {

      try {
         new Expectations(JsonUtils.class, DateTimeUtils.class, EventLogger.class, TimController.class) {
            {
               mockTravelerInputData.toString();
               result = "something";
               minTimes = 0;

               JsonUtils.fromJson(anyString, TravelerInputData.class);
               result = mockTravelerInputData;
               mockTravelerInputData.toJson(true);
               result = "mockTim";

               mockBuilder.buildTravelerInformation(mockTravelerInputData.getTim());
               result = mockTravelerInfo;

               mockBuilder.encodeTravelerInformationToHex();
               result = anyString;

               mockTravelerInputData.getRsus();
               result = new RSU[] { mockRsu };

               mockTravelerInputData.getSnmp();
               result = mockSnmp;

               mockRsu.getRsuTarget();
               result = "badResponse";

               TimController.createAndSend(mockSnmp, mockRsu, anyInt, anyString);
               result = mockResponseEvent;
               mockResponseEvent.getResponse();
               result = mockPdu;
               mockPdu.getErrorStatus();
               result = -1;

               mockTravelerInputData.getSdw();
               result = null;
            }
         };
      } catch (Exception e) {
         fail("Unexpected Exception in expectations block: " + e);
      }

      ResponseEntity<String> response = testTimController.timMessage("test123");
      assertEquals(
            "{\"rsu_responses\":[{\"target\":\"badResponse\",\"success\":\"false\",\"error\":\"Error code -1 null\"}],\"dds_deposit\":{\"success\":\"true\"}}",
            response.getBody());

      new Verifications() {
         {
            EventLogger.logger.info(anyString);
         }
      };
   }

   @Test
   public void ddsFailureShouldLogAndReturn() {

      try {
         new Expectations(JsonUtils.class, DateTimeUtils.class, EventLogger.class, TimController.class) {
            {
               mockTravelerInputData.toString();
               result = "something";
               minTimes = 0;

               JsonUtils.fromJson(anyString, TravelerInputData.class);
               result = mockTravelerInputData;
               mockTravelerInputData.toJson(true);
               result = "mockTim";

               mockBuilder.buildTravelerInformation(mockTravelerInputData.getTim());
               result = mockTravelerInfo;

               mockBuilder.encodeTravelerInformationToHex();
               result = anyString;

               mockTravelerInputData.getRsus();
               result = new RSU[] { mockRsu };

               mockTravelerInputData.getSnmp();
               result = mockSnmp;

               mockRsu.getRsuTarget();
               result = "nonexistentialRsu";

               TimController.createAndSend(mockSnmp, mockRsu, anyInt, anyString);
               result = mockResponseEvent;
               mockResponseEvent.getResponse();
               result = mockPdu;
               mockPdu.getErrorStatus();
               result = 0;

               mockTravelerInputData.getSdw();
               result = new InternalServerErrorException("Deposit to SDW Failed");

            }
         };
      } catch (Exception e) {
         fail("Unexpected Exception in expectations block: " + e);
      }

      ResponseEntity<String> response = testTimController.timMessage("test123");
      assertEquals(
            "{\"rsu_responses\":[{\"target\":\"nonexistentialRsu\",\"success\":\"true\",\"message\":\"Success.\"}],\"dds_deposit\":{\"success\":\"false\"}}",
            response.getBody());

      new Verifications() {
         {
            EventLogger.logger.info(anyString);
         }
      };
   }

   @Test
   public void goodResponseShouldLogAndReturn() {

      try {
         new Expectations(JsonUtils.class, DateTimeUtils.class, EventLogger.class, TimController.class) {
            {
               mockTravelerInputData.toString();
               result = "something";
               minTimes = 0;

               JsonUtils.fromJson(anyString, TravelerInputData.class);
               result = mockTravelerInputData;
               mockTravelerInputData.toJson(true);
               result = "mockTim";

               mockBuilder.buildTravelerInformation(mockTravelerInputData.getTim());
               result = mockTravelerInfo;

               mockBuilder.encodeTravelerInformationToHex();
               result = anyString;

               mockTravelerInputData.getRsus();
               result = new RSU[] { mockRsu };

               mockTravelerInputData.getSnmp();
               result = mockSnmp;

               mockRsu.getRsuTarget();
               result = "goodResponse";

               TimController.createAndSend(mockSnmp, mockRsu, anyInt, anyString);
               result = mockResponseEvent;
               mockResponseEvent.getResponse();
               result = mockPdu;
               mockPdu.getErrorStatus();
               result = 0;

               mockTravelerInputData.getSdw();
               result = null;
            }
         };
      } catch (Exception e) {
         fail("Unexpected Exception in expectations block: " + e);
      }

      ResponseEntity<String> response = testTimController.timMessage("test123");
      assertEquals(
            "{\"rsu_responses\":[{\"target\":\"goodResponse\",\"success\":\"true\",\"message\":\"Success.\"}],\"dds_deposit\":{\"success\":\"true\"}}",
            response.getBody());
   }

   @Test
   public void deleteShouldReturnBadRequestWhenNull() {
      assertEquals(HttpStatus.BAD_REQUEST, testTimController.deleteTim(null, 42).getStatusCode());
   }

   @Test
   public void deleteShouldCatchSessionIOException() {
      try {
         new Expectations(SnmpSession.class, JsonUtils.class) {
            {
               new SnmpSession((RSU) any);
               result = new IOException("testException123");
               
               JsonUtils.fromJson(anyString, (Class) any);
               result = null;
            }
         };
      } catch (IOException e) {
         fail("Unexpected Exception in expectations block: " + e);
      }
      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, testTimController.deleteTim("testJsonString", 42).getStatusCode());
   }

   @Test
   public void deleteShouldCatchSessionNullPointerException() {
      try {
         new Expectations(SnmpSession.class, JsonUtils.class) {
            {
               new SnmpSession((RSU) any);
               result = new NullPointerException("testException123");
               
               JsonUtils.fromJson(anyString, (Class) any);
               result = null;
            }
         };
      } catch (IOException e) {
         fail("Unexpected Exception in expectations block: " + e);
      }
      assertEquals(HttpStatus.BAD_REQUEST, testTimController.deleteTim("testJsonString", 42).getStatusCode());
   }

}
