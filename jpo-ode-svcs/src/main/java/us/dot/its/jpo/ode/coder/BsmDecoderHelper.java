package us.dot.its.jpo.ode.coder;

import java.io.BufferedInputStream;
import java.time.ZonedDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usdot.asn1.generated.ieee1609dot2.ieee1609dot2.Ieee1609Dot2Data;
import gov.usdot.cv.security.msg.IEEE1609p2Message;
import us.dot.its.jpo.ode.model.OdeBsmData;
import us.dot.its.jpo.ode.model.OdeBsmMetadata;
import us.dot.its.jpo.ode.model.OdeBsmPayload;
import us.dot.its.jpo.ode.model.OdeData;
import us.dot.its.jpo.ode.model.OdeObject;
import us.dot.its.jpo.ode.model.SerialId;
import us.dot.its.jpo.ode.plugin.j2735.J2735Bsm;
import us.dot.its.jpo.ode.plugin.j2735.J2735MessageFrame;
import us.dot.its.jpo.ode.plugin.j2735.oss.Oss1609dot2Coder;
import us.dot.its.jpo.ode.plugin.j2735.oss.OssJ2735Coder;
import us.dot.its.jpo.ode.security.SecurityManager;
import us.dot.its.jpo.ode.security.SecurityManager.SecurityManagerException;
import us.dot.its.jpo.ode.util.DateTimeUtils;

public class BsmDecoderHelper {

   private static final Logger logger = LoggerFactory.getLogger(BsmDecoderHelper.class);

   private static final OssJ2735Coder j2735Coder = new OssJ2735Coder();
   private static final Oss1609dot2Coder ieee1609dotCoder = new Oss1609dot2Coder();

   private BsmDecoderHelper() {
   }

   public static OdeData decode(BufferedInputStream bis, String fileName, SerialId serialId) throws Exception {
      Ieee1609Dot2Data ieee1609dot2Data = ieee1609dotCoder.decodeIeee1609Dot2DataStream(bis);

      OdeObject bsm = null;
      OdeData odeBsmData = null;
      IEEE1609p2Message message = null;
      if (ieee1609dot2Data != null) {
         logger.debug("Attempting to decode as Ieee1609Dot2Data.");
         try {
            message = IEEE1609p2Message.convert(ieee1609dot2Data);

            if (message != null) {
                bsm = BsmDecoderHelper.getBsmPayload(message);
            }
         } catch (Exception e) {
             logger.debug("Message does not have a valid signature. Assuming it is unsigned message...");
             if (ieee1609dot2Data.getContent() != null &&
                     ieee1609dot2Data.getContent().getSignedData() != null &&
                     ieee1609dot2Data.getContent().getSignedData().getTbsData() != null &&
                     ieee1609dot2Data.getContent().getSignedData().getTbsData().getPayload() != null &&
                     ieee1609dot2Data.getContent().getSignedData().getTbsData().getPayload().getData() != null &&
                     ieee1609dot2Data.getContent().getSignedData().getTbsData().getPayload().getData().getContent() != null &&
                     ieee1609dot2Data.getContent().getSignedData().getTbsData().getPayload().getData().getContent().getUnsecuredData() != null) {
                 bsm = BsmDecoderHelper.decodeBsm(
                    ieee1609dot2Data.getContent().getSignedData().getTbsData().getPayload()
                      .getData().getContent().getUnsecuredData().byteArrayValue());
             }
         }
      } else { // probably raw BSM or MessageFrame
         logger.debug("Attempting to decode as raw BSM or Message Frame.");
         bsm = BsmDecoderHelper.decodeBsm(bis);
      }

      if (bsm != null) {
         logger.debug("Decoded BSM successfully, creating OdeBsmData object.");
         odeBsmData = BsmDecoderHelper.createOdeBsmData((J2735Bsm) bsm, message, fileName, serialId);
      } else {
         logger.debug("Failed to decode BSM.");
      }

      return odeBsmData;
   }


   private static OdeObject decodeBsm(BufferedInputStream is) {
      J2735MessageFrame mf = (J2735MessageFrame) j2735Coder.decodeUPERMessageFrameStream(is);
      if (mf != null) {
         logger.debug("Decoding as a message frame.");
         return mf.getValue();
      } else {
         logger.debug("Decoding as raw BSM.");
         return j2735Coder.decodeUPERBsmStream(is);
      }
   }

   public static OdeObject getBsmPayload(IEEE1609p2Message message) {
      try {
         SecurityManager.validateGenerationTime(message);
      } catch (SecurityManagerException e) {
         logger.error("Error validating message.", e);
      }

      return BsmDecoderHelper.decodeBsm(message.getPayload());
   }

   public static OdeObject decodeBsm(byte[] bytes) {
      J2735MessageFrame mf = (J2735MessageFrame) j2735Coder.decodeUPERMessageFrameBytes(bytes);
      if (mf != null) {

         logger.info("Decoding as message frame...");
         return mf.getValue();
      } else {

         logger.info("Decoding as bsm without message frame...");
         return j2735Coder.decodeUPERBsmBytes(bytes);
      }
   }

   public static OdeBsmData createOdeBsmData(
       J2735Bsm rawBsm, 
       IEEE1609p2Message message, 
       String fileName,
       SerialId serialId) {
      OdeBsmPayload payload = new OdeBsmPayload(rawBsm);

      OdeBsmMetadata metadata = new OdeBsmMetadata(payload);
      metadata.setSerialId(serialId);

      if (message != null) {
         ZonedDateTime generatedAt = DateTimeUtils.isoDateTime(message.getGenerationTime());
         metadata.setGeneratedAt(generatedAt.toString());

         metadata.setValidSignature(true);
      } else {
          /*
           * TODO Temporarily put in place for testing CV PEP. 
           * Should be removed after testing is complete. 
           */
          metadata.setGeneratedAt(metadata.getReceivedAt());
      }

      metadata.getSerialId().addRecordId(1);
      metadata.setLogFileName(fileName);
      return new OdeBsmData(metadata, payload);
   }

}
