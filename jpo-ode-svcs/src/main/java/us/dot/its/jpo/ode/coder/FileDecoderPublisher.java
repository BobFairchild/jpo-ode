package us.dot.its.jpo.ode.coder;

import java.io.BufferedInputStream;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import us.dot.its.jpo.ode.OdeProperties;
import us.dot.its.jpo.ode.coder.stream.BinaryDecoderPublisher;
import us.dot.its.jpo.ode.coder.stream.HexDecoderPublisher;
import us.dot.its.jpo.ode.coder.stream.JsonDecoderPublisher;

public class FileDecoderPublisher {

   private static final Logger logger = LoggerFactory.getLogger(FileDecoderPublisher.class);

   private JsonDecoderPublisher jsonDecPub;
   private HexDecoderPublisher hexDecPub;
   private BinaryDecoderPublisher binDecPub;

   @Autowired
   public FileDecoderPublisher(OdeProperties odeProperties) {

      MessagePublisher messagePub = new MessagePublisher(odeProperties);

      this.jsonDecPub = new JsonDecoderPublisher(messagePub);
      this.hexDecPub = new HexDecoderPublisher(messagePub);
      this.binDecPub = new BinaryDecoderPublisher(messagePub);
   }

   public void decodeAndPublishFile(Path filePath, BufferedInputStream fileInputStream) {
      String fileName = filePath.toFile().getName();

      logger.info("Decoding and publishing file {}", fileName);

      try {
         if (filePath.toString().endsWith(".hex") || filePath.toString().endsWith(".txt")) {
            logger.info("Decoding {} as hex file.", filePath);
            hexDecPub.decodeAndPublish(fileInputStream, fileName);
         } else if (filePath.toString().endsWith(".json")) {
            logger.info("Decoding {} as json file.", filePath);
            jsonDecPub.decodeAndPublish(fileInputStream, fileName);
         } else {
            logger.info("Decoding {} as binary/signed file.", filePath);
            binDecPub.decodeAndPublish(fileInputStream, fileName);
         }
      } catch (Exception e) {
         logger.error("Failed to decode and publish file.", e);
      }
   }
}
